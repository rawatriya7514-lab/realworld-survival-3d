import com.devagent.realworldsurvival3d.CameraRig;
import com.devagent.realworldsurvival3d.Obstacle;
import com.devagent.realworldsurvival3d.Player;
import com.devagent.realworldsurvival3d.Terrain;

import java.util.ArrayList;
import java.util.List;

/**
 * CameraTest - Comprehensive JVM test suite for CameraRig:
 *  - Target tracking and smooth follow lag
 *  - Crouch eye-level dynamic lowering and rising
 *  - Orbit yaw and pitch clamping [-80, +80]
 *  - Dutch angle roll and up-vector tilt
 *  - Zoom distance scaling and presets (CLOSE, NORMAL, FAR)
 *  - Obstruction raycast pull-in (AABB slab math + obstacle avoidance)
 *  - Terrain-aware minimum height and water clearance
 *  - First-person view transitions
 *  - Screen shake exponential decay
 *  - View matrix orthonormality and numerical stability
 */
public class CameraTest {
    private static int passed = 0;
    private static int failed = 0;

    private static void check(boolean condition, String description) {
        if (condition) {
            System.out.println("  PASS  " + description);
            passed++;
        } else {
            System.err.println("  FAIL  " + description);
            failed++;
        }
    }

    private static void checkFloat(float actual, float expected, float eps, String description) {
        if (Math.abs(actual - expected) <= eps) {
            System.out.println("  PASS  " + description + " (" + actual + " ~ " + expected + ")");
            passed++;
        } else {
            System.err.println("  FAIL  " + description + " (" + actual + " != " + expected + ", eps=" + eps + ")");
            failed++;
        }
    }

    public static void main(String[] args) {
        System.out.println("Camera: target tracking & follow smoothing");
        testTargetTracking();

        System.out.println("Camera: crouch eye-level adaptation");
        testCrouchEyeLevel();

        System.out.println("Camera: orbit & pitch clamping");
        testOrbitAndPitchClamping();

        System.out.println("Camera: zoom multiplier & presets");
        testZoomAndPresets();

        System.out.println("Camera: obstruction raycast pull-in");
        testObstructionPullIn();

        System.out.println("Camera: terrain-aware min height clamp");
        testTerrainAwareClamping();

        System.out.println("Camera: first-person mode");
        testFirstPersonMode();

        System.out.println("Camera: screen shake decay");
        testScreenShakeDecay();

        System.out.println("Camera: view matrix math & numerical stability");
        testViewMatrixAndStability();

        System.out.println("\nCameraTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }

    private static void testTargetTracking() {
        CameraRig rig = new CameraRig();
        Player player = new Player();
        player.x = 10f;
        player.y = 5f;
        player.z = 20f;
        player.yaw = 0f;
        player.pitch = 0f;

        rig.snapToTarget(player);
        checkFloat(rig.camTX, 10f, 0.01f, "snapToTarget sets camTX to player.x");
        checkFloat(rig.camTZ, 20f, 0.01f, "snapToTarget sets camTZ to player.z");
        checkFloat(rig.camTY, 5f + rig.standingEyeHeight, 0.01f, "snapToTarget sets camTY to player standing eye height");

        // Player moves forward
        player.z += 10f;
        for (int i = 0; i < 30; i++) {
            rig.update(0.016f, player, null, false);
        }
        checkFloat(rig.camTZ, 30f, 0.05f, "camera target smoothly catches up to moving player");
        check(rig.camZ > rig.camTZ, "camera sits behind player along +Z when yaw=0");
    }

    private static void testCrouchEyeLevel() {
        CameraRig rig = new CameraRig();
        Player player = new Player();
        player.x = 0f;
        player.y = 0f;
        player.z = 0f;
        rig.snapToTarget(player);

        float standFocusY = rig.currentFocusY;
        checkFloat(standFocusY, rig.standingEyeHeight, 0.01f, "standing focus height is ~1.45m");

        // Player toggles crouch
        player.isCrouching = true;
        player.state = Player.ParkourState.CROUCH;

        // Update over several frames to observe smooth descent
        float prevFocusY = standFocusY;
        boolean strictlyDecreasing = true;
        for (int i = 0; i < 30; i++) {
            rig.update(0.016f, player, null, false);
            if (rig.currentFocusY > prevFocusY + 0.001f) strictlyDecreasing = false;
            prevFocusY = rig.currentFocusY;
        }

        check(strictlyDecreasing, "focus height smoothly decreases when crouching");
        checkFloat(rig.currentFocusY, rig.crouchingEyeHeight, 0.05f, "focus height settles near crouchingEyeHeight (0.65m)");

        // Player stands back up
        player.isCrouching = false;
        player.state = Player.ParkourState.STAND;
        for (int i = 0; i < 30; i++) {
            rig.update(0.016f, player, null, false);
        }
        checkFloat(rig.currentFocusY, rig.standingEyeHeight, 0.05f, "focus height rises back to standingEyeHeight");
    }

    private static void testOrbitAndPitchClamping() {
        CameraRig rig = new CameraRig();
        Player player = new Player();
        player.x = 0f;
        player.y = 0f;
        player.z = 0f;

        // Yaw orbit test: 0 deg puts camera behind along +Z, 90 deg puts camera at -X
        player.yaw = 0f;
        player.pitch = 0f;
        rig.snapToTarget(player);
        check(rig.camZ > 3.0f, "yaw 0 puts camera along +Z");

        player.yaw = 90f;
        rig.update(0.1f, player, null, false);
        rig.update(0.1f, player, null, false);
        check(rig.camX < -2.0f, "yaw 90 puts camera along -X");

        // Pitch clamp test
        player.pitch = 120f; // Extreme look down
        rig.update(0.016f, player, null, false);
        check(rig.pitch <= 80f, "pitch +120 is clamped to <= 80 deg: " + rig.pitch);

        player.pitch = -110f; // Extreme look up
        rig.update(0.016f, player, null, false);
        check(rig.pitch >= -80f, "pitch -110 is clamped to >= -80 deg: " + rig.pitch);

        // Wall-run roll test
        player.pitch = 0f;
        player.cameraRoll = -15f; // Wall run left
        for (int i = 0; i < 35; i++) {
            rig.update(0.016f, player, null, false);
        }
        checkFloat(rig.roll, -15f, 0.5f, "camera roll matches wall-run angle (-15 deg)");
        check(rig.camUpX < -0.2f, "camera up-vector tilts left: " + rig.camUpX);
    }

    private static void testZoomAndPresets() {
        CameraRig rig = new CameraRig();
        checkFloat(rig.currentDistance, CameraRig.BASE_DISTANCE, 0.01f, "default camera distance is BASE_DISTANCE (4.8m)");

        // Continuous zoom adjustment
        rig.adjustZoom(0.3f);
        checkFloat(rig.zoomMultiplier, 1.3f, 0.01f, "adjustZoom increases multiplier");

        // Max zoom clamp
        rig.adjustZoom(2.0f);
        checkFloat(rig.zoomMultiplier, CameraRig.MAX_ZOOM, 0.01f, "zoom multiplier clamps at MAX_ZOOM (1.6x)");

        // Min zoom clamp
        rig.adjustZoom(-3.0f);
        checkFloat(rig.zoomMultiplier, CameraRig.MIN_ZOOM, 0.01f, "zoom multiplier clamps at MIN_ZOOM (0.8x)");

        // Presets
        rig.setPreset(CameraRig.Preset.CLOSE);
        checkFloat(rig.targetDistance, 2.5f, 0.01f, "CLOSE preset sets target distance to 2.5m");

        rig.cyclePreset();
        checkFloat(rig.targetDistance, 4.8f, 0.01f, "cyclePreset from CLOSE advances to NORMAL (4.8m)");

        rig.cyclePreset();
        checkFloat(rig.targetDistance, 7.2f, 0.01f, "cyclePreset from NORMAL advances to FAR (7.2m)");

        rig.cyclePreset();
        checkFloat(rig.targetDistance, 2.5f, 0.01f, "cyclePreset wraps back to CLOSE");
    }

    private static void testObstructionPullIn() {
        CameraRig rig = new CameraRig();
        Player player = new Player();
        player.x = 0f;
        player.y = 10f;
        player.z = 0f;
        player.yaw = 0f; // looking -Z, camera extends into +Z
        player.pitch = 0f;
        rig.snapToTarget(player);

        // Place a solid building 2.5m behind the player (z in [2.0, 4.0])
        List<Obstacle> obstacles = new ArrayList<>();
        Obstacle wall = new Obstacle(Obstacle.Type.BUILDING, 0f, 10f, 3.0f, 10f, 10f, 2.0f);
        obstacles.add(wall);

        // Update camera
        rig.update(0.016f, player, obstacles, false);

        check(rig.isObstructed, "camera detects building obstruction");
        check(rig.currentDistance < 2.5f, "camera pulls in closer than building front face: " + rig.currentDistance);
        check(rig.camZ < wall.getMinZ(), "camera stays outside building front face (camZ=" + rig.camZ + " < " + wall.getMinZ() + ")");

        // Non-solid obstacle test (bush)
        obstacles.clear();
        Obstacle bush = new Obstacle(Obstacle.Type.BUSH, 0f, 10f, 3.0f, 10f, 10f, 2.0f);
        bush.solid = false;
        obstacles.add(bush);

        // Update camera several frames to recover
        for (int i = 0; i < 40; i++) {
            rig.update(0.016f, player, obstacles, false);
        }

        check(!rig.isObstructed, "non-solid bush is ignored by camera obstruction");
        checkFloat(rig.currentDistance, CameraRig.BASE_DISTANCE, 0.25f, "camera extends back to full distance through non-solid prop");
    }

    private static void testTerrainAwareClamping() {
        CameraRig rig = new CameraRig();
        Player player = new Player();
        // In OpenWorld hills, terrain can be elevated
        player.x = 0f;
        player.y = Terrain.heightAt(0f, 0f);
        player.z = 0f;
        player.yaw = 0f;
        player.pitch = -40f; // looking up tilts camera downward toward ground

        for (int i = 0; i < 20; i++) {
            rig.update(0.016f, player, null, true);
        }

        float groundUnderCam = Terrain.heightAt(rig.camX, rig.camZ);
        check(rig.camY >= groundUnderCam + rig.minGroundClearance - 0.01f,
                "camera Y is clamped above terrain surface (camY=" + rig.camY + " >= ground=" + groundUnderCam + " + " + rig.minGroundClearance + ")");

        // Position player near river
        player.x = 0f;
        player.z = 80f; // river channel zone
        player.y = Terrain.heightAt(player.x, player.z);

        for (int i = 0; i < 20; i++) {
            rig.update(0.016f, player, null, true);
        }

        check(rig.camY >= Terrain.WATER_LEVEL + 0.40f,
                "camera Y remains above river water level (camY=" + rig.camY + " >= water=" + Terrain.WATER_LEVEL + ")");
    }

    private static void testFirstPersonMode() {
        CameraRig rig = new CameraRig();
        Player player = new Player();
        player.x = 15f;
        player.y = 8f;
        player.z = -25f;
        player.yaw = 45f;
        player.pitch = 10f;
        player.firstPersonView = true;

        rig.snapToTarget(player);
        rig.update(0.016f, player, null, false);

        checkFloat(rig.camX, player.x, 0.20f, "first-person camX matches player.x");
        checkFloat(rig.camZ, player.z, 0.20f, "first-person camZ matches player.z");
        checkFloat(rig.camY, player.y + rig.standingEyeHeight, 0.05f, "first-person camY matches player eye height");

        // Crouch in first person
        player.isCrouching = true;
        player.state = Player.ParkourState.CROUCH;
        for (int i = 0; i < 30; i++) {
            rig.update(0.016f, player, null, false);
        }
        checkFloat(rig.camY, player.y + rig.crouchingEyeHeight, 0.05f, "first-person eye height drops during crouch");
    }

    private static void testScreenShakeDecay() {
        CameraRig rig = new CameraRig();
        Player player = new Player();
        player.state = Player.ParkourState.HARD_LAND;

        rig.update(0.016f, player, null, false);
        check(rig.screenShake > 0.30f, "hard land triggers screen shake: " + rig.screenShake);

        player.state = Player.ParkourState.STAND;
        float prevShake = rig.screenShake;
        boolean decaying = true;
        for (int i = 0; i < 30; i++) {
            rig.update(0.016f, player, null, false);
            if (rig.screenShake > prevShake) decaying = false;
            prevShake = rig.screenShake;
        }

        check(decaying, "screen shake decays continuously");
        check(rig.screenShake < 0.02f, "screen shake dissipates to near zero: " + rig.screenShake);
    }

    private static void testViewMatrixAndStability() {
        CameraRig rig = new CameraRig();
        Player player = new Player();
        player.x = 5f;
        player.y = 2f;
        player.z = 10f;
        rig.snapToTarget(player);

        float[] matrix = new float[16];
        rig.computeViewMatrix(matrix);

        // Check matrix orthonormality: column vectors S (m[0..2]), U (m[4..6]), -F (m[8..10])
        float sLenSq = matrix[0] * matrix[0] + matrix[1] * matrix[1] + matrix[2] * matrix[2];
        float uLenSq = matrix[4] * matrix[4] + matrix[5] * matrix[5] + matrix[6] * matrix[6];
        float fLenSq = matrix[8] * matrix[8] + matrix[9] * matrix[9] + matrix[10] * matrix[10];

        checkFloat(sLenSq, 1.0f, 0.01f, "view matrix side vector is unit length");
        checkFloat(uLenSq, 1.0f, 0.01f, "view matrix up vector is unit length");
        checkFloat(fLenSq, 1.0f, 0.01f, "view matrix forward vector is unit length");

        float dotSU = matrix[0] * matrix[4] + matrix[1] * matrix[5] + matrix[2] * matrix[6];
        checkFloat(dotSU, 0.0f, 0.01f, "view matrix side and up vectors are orthogonal");

        // Numerical stability over 500 variable updates
        boolean allFinite = true;
        for (int i = 0; i < 500; i++) {
            player.x += (float) Math.sin(i * 0.1);
            player.yaw = (i * 7.3f) % 360f;
            player.pitch = (float) Math.sin(i * 0.05) * 75f;
            rig.update(0.016f, player, null, true);
            rig.computeViewMatrix(matrix);

            for (float v : matrix) {
                if (Float.isNaN(v) || Float.isInfinite(v)) allFinite = false;
            }
        }
        check(allFinite, "500 variable frames produce 100% finite view matrices with 0 NaNs");
    }
}
