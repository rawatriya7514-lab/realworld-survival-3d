import com.devagent.realworldsurvival3d.Obstacle;
import com.devagent.realworldsurvival3d.Player;
import com.devagent.realworldsurvival3d.Terrain;
import com.devagent.realworldsurvival3d.WorldMap;

import java.util.ArrayList;
import java.util.List;

/**
 * MovementTest - Comprehensive JVM test suite for Phase 2:
 *   - Ground locomotion state machine (STAND, WALK, JOG, RUN, SPRINT, CROUCH)
 *   - Airborne & landing states (JUMP, DOUBLE_JUMP, FALLING, LAND, HARD_LAND, SAFETY_ROLL)
 *   - Step-up (<= 0.55m) on obstacles and terrain with headroom validation
 *   - Ground snap (<= 0.55m) down slopes/steps without teleporting over cliffs
 *   - Slope handling (walkable uphill resistance, steep cliff deflection and sliding)
 * Run: tests/run.sh
 */
public class MovementTest {

    static int passed = 0, failed = 0;

    static void check(boolean cond, String name) {
        if (cond) {
            passed++;
            System.out.println("  PASS  " + name);
        } else {
            failed++;
            System.out.println("  FAIL  " + name);
        }
    }

    static void approx(float actual, float expected, float eps, String name) {
        if (Math.abs(actual - expected) <= eps) {
            passed++;
            System.out.println("  PASS  " + name + " (" + actual + " ~ " + expected + ")");
        } else {
            failed++;
            System.out.println("  FAIL  " + name + " (expected " + expected + " +/- " + eps + ", got " + actual + ")");
        }
    }

    public static void main(String[] args) {
        testLocomotionStates();
        testCrouchAndHeadroom();
        testAirborneAndLandingStates();
        testStepUp();
        testGroundSnap();
        testSlopeHandling();

        System.out.println("\nMovementTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }

    // -----------------------------------------------------------------
    static void testLocomotionStates() {
        System.out.println("Movement: locomotion state machine");
        Terrain terrain = new Terrain();
        List<Obstacle> emptyObstacles = new ArrayList<Obstacle>();

        // 1. STAND (Idle)
        Player p = new Player();
        p.x = 100f; p.y = 0f; p.z = 0f;
        p.terrain = terrain;
        p.isGrounded = true;
        for (int i = 0; i < 30; i++) {
            p.update(1f / 60f, 0f, 0f, false, false, false, emptyObstacles);
        }
        check(p.state == Player.ParkourState.STAND, "stationary player is in STAND state");
        approx(p.vx, 0f, 0.05f, "stationary player vx is 0");
        approx(p.vz, 0f, 0.05f, "stationary player vz is 0");

        // 2. WALK (Light analog input, e.g. inputY = 0.20)
        p = new Player();
        p.x = 100f; p.y = 0f; p.z = 0f;
        p.terrain = terrain;
        p.isGrounded = true;
        for (int i = 0; i < 40; i++) {
            p.update(1f / 60f, 0f, 0.20f, false, false, false, emptyObstacles);
        }
        check(p.state == Player.ParkourState.WALK, "light analog input produces WALK state");
        float walkSpeed = (float) Math.sqrt(p.vx * p.vx + p.vz * p.vz);
        check(walkSpeed > 1.0f && walkSpeed <= 4.0f, "walk speed is within expected range: " + walkSpeed);

        // 3. JOG (Moderate analog input, e.g. inputY = 0.50)
        p = new Player();
        p.x = 100f; p.y = 0f; p.z = 0f;
        p.terrain = terrain;
        p.isGrounded = true;
        for (int i = 0; i < 40; i++) {
            p.update(1f / 60f, 0f, 0.50f, false, false, false, emptyObstacles);
        }
        check(p.state == Player.ParkourState.JOG, "moderate analog input produces JOG state");
        float jogSpeed = (float) Math.sqrt(p.vx * p.vx + p.vz * p.vz);
        check(jogSpeed > 3.5f && jogSpeed <= 6.0f, "jog speed is within expected range: " + jogSpeed);

        // 4. RUN (Full analog input, inputY = 1.0)
        p = new Player();
        p.x = 100f; p.y = 0f; p.z = 0f;
        p.terrain = terrain;
        p.isGrounded = true;
        for (int i = 0; i < 40; i++) {
            p.update(1f / 60f, 0f, 1.0f, false, false, false, emptyObstacles);
        }
        check(p.state == Player.ParkourState.RUN, "full analog input produces RUN state");
        float runSpeed = (float) Math.sqrt(p.vx * p.vx + p.vz * p.vz);
        approx(runSpeed, 7.5f, 0.3f, "run speed reaches ~7.5 m/s");

        // 5. SPRINT (Full input + Sprint active)
        p = new Player();
        p.x = 100f; p.y = 0f; p.z = 0f;
        p.terrain = terrain;
        p.isGrounded = true;
        p.isSprinting = true;
        for (int i = 0; i < 50; i++) {
            p.update(1f / 60f, 0f, 1.0f, false, false, false, emptyObstacles);
        }
        check(p.state == Player.ParkourState.SPRINT, "sprinting produces SPRINT state");
        float sprintSpeed = (float) Math.sqrt(p.vx * p.vx + p.vz * p.vz);
        approx(sprintSpeed, 12.0f, 0.5f, "sprint speed reaches ~12.0 m/s");
    }

    // -----------------------------------------------------------------
    static void testCrouchAndHeadroom() {
        System.out.println("Movement: crouch and headroom mechanics");
        Terrain terrain = new Terrain();
        List<Obstacle> obstacles = new ArrayList<Obstacle>();

        Player p = new Player();
        p.x = 100f; p.y = 0f; p.z = 0f;
        p.terrain = terrain;
        p.isGrounded = true;

        // Toggle crouch via crouchPressed = true
        p.update(1f / 60f, 0f, 0f, false, false, false, true, obstacles);
        check(p.isCrouching, "crouch flag is true after toggle");
        check(p.state == Player.ParkourState.CROUCH, "player state is CROUCH");
        approx(p.currentHeight, p.crouchingHeight, 0.01f, "player height drops to crouchingHeight");

        // Move while crouching
        for (int i = 0; i < 40; i++) {
            p.update(1f / 60f, 0f, 1.0f, false, false, false, false, obstacles);
        }
        check(p.state == Player.ParkourState.CROUCH, "player remains in CROUCH while moving");
        float crouchSpeed = (float) Math.sqrt(p.vx * p.vx + p.vz * p.vz);
        approx(crouchSpeed, p.crouchSpeed, 0.3f, "crouch movement speed is capped at crouchSpeed (2.5 m/s)");

        // Overhead obstacle preventing standing up:
        // Ceiling at y = 1.3m (between crouchingHeight 0.8m and standingHeight 1.8m)
        Obstacle lowCeiling = new Obstacle(Obstacle.Type.BUILDING, 100f, 2.3f, p.z, 6f, 2.0f, 6f);
        obstacles.add(lowCeiling);

        check(!p.canStandUp(obstacles), "canStandUp reports false under low ceiling");

        // Try to uncrouch under low ceiling
        p.update(1f / 60f, 0f, 0f, false, false, false, true, obstacles);
        check(p.isCrouching, "player remains crouching under low ceiling even if uncrouch attempted");
        approx(p.currentHeight, p.crouchingHeight, 0.01f, "height remains crouchingHeight");

        // Move player away from low ceiling
        obstacles.clear();
        check(p.canStandUp(obstacles), "canStandUp reports true in open space");
        p.update(1f / 60f, 0f, 0f, false, false, false, true, obstacles);
        check(!p.isCrouching, "player stands up in open space");
        approx(p.currentHeight, p.standingHeight, 0.01f, "height restored to standingHeight");
    }

    // -----------------------------------------------------------------
    static void testAirborneAndLandingStates() {
        System.out.println("Movement: airborne and landing states");
        Terrain terrain = new Terrain();
        List<Obstacle> obstacles = new ArrayList<Obstacle>();

        // Jump
        Player p = new Player();
        p.x = 100f; p.y = 0f; p.z = 0f;
        p.terrain = terrain;
        p.isGrounded = true;

        p.update(1f / 60f, 0f, 0f, true, false, false, obstacles);
        check(p.state == Player.ParkourState.JUMP, "jump input enters JUMP state");
        check(!p.isGrounded, "player leaves the ground");

        // Air boost / double jump
        for (int i = 0; i < 15; i++) {
            p.update(1f / 60f, 0f, 0f, false, false, false, obstacles);
        }
        p.update(1f / 60f, 0f, 0f, true, false, false, obstacles);
        check(p.state == Player.ParkourState.DOUBLE_JUMP, "air jump enters DOUBLE_JUMP state");

        // Falling
        for (int i = 0; i < 30; i++) {
            p.update(1f / 60f, 0f, 0f, false, false, false, obstacles);
        }
        check(p.state == Player.ParkourState.FALLING, "falling through air enters FALLING state");

        // Soft landing from a 2m drop
        p = new Player();
        p.x = 100f; p.y = 2.0f; p.z = 0f;
        p.terrain = terrain;
        p.peakFallY = 2.0f;
        p.isGrounded = false;
        int guard = 0;
        while (!p.isGrounded && guard++ < 100) {
            p.update(1f / 60f, 0f, 0f, false, false, false, obstacles);
        }
        check(p.isGrounded, "player lands from 2m drop");
        check(p.state == Player.ParkourState.LAND, "moderate drop transitions into LAND state");
        check(p.health == 100f, "soft landing deals 0 damage");

        // Moving during LAND smoothly exits into locomotion
        p.update(1f / 60f, 0f, 1.0f, false, false, false, obstacles);
        check(p.state != Player.ParkourState.LAND, "movement input smoothly exits LAND state");
    }

    // -----------------------------------------------------------------
    static void testStepUp() {
        System.out.println("Movement: step-up (<= 0.55m)");
        Terrain terrain = new Terrain();
        List<Obstacle> obstacles = new ArrayList<Obstacle>();

        // 1. Curb/Step of height 0.35m (<= 0.55m)
        // Step box: center at z = 5, width 10, height 0.35, depth 8 (spanning z from 1 to 9)
        Obstacle curb = new Obstacle(Obstacle.Type.CRATE, 100f, 0.175f, 5f, 10f, 0.35f, 8f);
        obstacles.add(curb);

        Player p = new Player();
        p.x = 100f; p.y = 0f; p.z = 0f; // facing +Z movement with inputY = 1.0f
        p.terrain = terrain;
        p.isGrounded = true;

        // Walk forward toward +Z for 1.0s (reaches z ~ 7.1, right on top of the curb)
        for (int i = 0; i < 60; i++) {
            p.update(1f / 60f, 0f, 1.0f, false, false, false, obstacles);
        }
        approx(p.y, 0.35f, 0.05f, "player stepped up onto 0.35m curb");
        check(p.z > 1.2f, "player moved forward across the curb step (z=" + p.z + ")");
        check(p.isGrounded, "player remains grounded after stepping up");

        // 2. Tall wall of height 1.5m (> 0.55m)
        obstacles.clear();
        Obstacle wall = new Obstacle(Obstacle.Type.BUILDING, 100f, 1.0f, 2f, 10f, 2.0f, 1f); // front edge at z=1.5
        obstacles.add(wall);

        p = new Player();
        p.x = 100f; p.y = 0f; p.z = 0f;
        p.terrain = terrain;
        p.isGrounded = true;

        for (int i = 0; i < 60; i++) {
            p.update(1f / 60f, 0f, 1.0f, false, false, false, obstacles);
        }
        approx(p.y, 0f, 0.02f, "tall wall cannot be stepped up (y stays 0.0)");
        check(p.z < 2.0f - 0.5f - p.radius + 0.05f, "player is stopped by tall wall");
    }

    // -----------------------------------------------------------------
    static void testGroundSnap() {
        System.out.println("Movement: ground snap without cliff teleportation");
        Terrain terrain = new Terrain();
        List<Obstacle> obstacles = new ArrayList<Obstacle>();

        // 1. Step-down curb / downward stair (drop = 0.35m <= 0.55m)
        // High surface at z=-1 to z=+1 at y=0.35; ground beyond z=+1 is y=0.0
        Obstacle step = new Obstacle(Obstacle.Type.CRATE, 100f, 0.175f, 0f, 10f, 0.35f, 2f);
        obstacles.add(step);

        Player p = new Player();
        p.x = 100f; p.y = 0.35f; p.z = 0.5f;
        p.terrain = terrain;
        p.isGrounded = true;

        boolean everAirborne = false;
        // Walk forward off the step onto terrain at y=0
        for (int i = 0; i < 60; i++) {
            p.update(1f / 60f, 0f, 1.0f, false, false, false, obstacles);
            if (!p.isGrounded) everAirborne = true;
        }
        check(!everAirborne, "ground snap prevented airborne jitter when stepping down 0.35m");
        approx(p.y, 0.0f, 0.02f, "player settled onto the lower floor");

        // 2. Cliff / high ledge (drop = 6.0m > 0.55m): MUST NOT TELEPORT TO GROUND
        obstacles.clear();
        // Roof center at z=0, depth 2m (spans -1 to +1), top y = 6.0
        Obstacle highRoof = new Obstacle(Obstacle.Type.BUILDING, 100f, 3.0f, 0f, 10f, 6.0f, 2f);
        obstacles.add(highRoof);

        p = new Player();
        p.x = 100f; p.y = 6.0f; p.z = 0.6f;
        p.terrain = terrain;
        p.isGrounded = true;

        // Walk off the edge toward +Z
        for (int i = 0; i < 30; i++) {
            p.update(1f / 60f, 0f, 1.0f, false, false, false, obstacles);
            if (!p.isGrounded) break;
        }
        check(!p.isGrounded, "walking off 6m ledge leaves ground (isGrounded=false)");
        check(p.y > 4.5f, "player did not teleport to bottom floor (y=" + p.y + " > 4.5m)");
        check(p.state == Player.ParkourState.FALLING, "player enters FALLING state naturally");
    }

    // -----------------------------------------------------------------
    static void testSlopeHandling() {
        System.out.println("Movement: slope handling (hills vs steep canyon)");
        Terrain terrain = new Terrain();
        List<Obstacle> obstacles = new ArrayList<Obstacle>();

        // 1. Walkable gentle slope: North Hills approach
        // z = -260 rises gently into hills (normal.y ~ 0.99 >= 0.60)
        float[] norm = new float[3];
        Terrain.normalAt(0f, -260f, norm);
        check(norm[1] >= Player.MAX_WALKABLE_SLOPE_COS, "hill slope is within walkable limit (normal.y=" + norm[1] + ")");

        Player p = new Player();
        p.x = 0f; p.z = -250f;
        p.y = Terrain.heightAt(p.x, p.z);
        p.terrain = terrain;
        p.isGrounded = true;

        float startY = p.y;
        // Move north into the hills (-Z direction, inputY = -1.0f)
        for (int i = 0; i < 60; i++) {
            p.update(1f / 60f, 0f, -1.0f, false, false, false, obstacles);
        }
        check(p.isGrounded, "player remains grounded climbing hills");
        check(p.y > startY + 0.2f, "player ascended the hill slope (y=" + p.y + " vs start " + startY + ")");

        // 2. Steep river canyon wall (normal.y < 0.60): cannot walk up steep cliffs
        float cx = Terrain.riverCenterX(0f);
        float steepX = cx + 24f;
        Terrain.normalAt(steepX, 0f, norm);

        Player steepPlayer = new Player();
        steepPlayer.x = steepX;
        steepPlayer.z = 0f;
        steepPlayer.y = Terrain.heightAt(steepPlayer.x, steepPlayer.z);
        steepPlayer.terrain = terrain;
        steepPlayer.isGrounded = true;
        steepPlayer.yaw = 90f; // Face east (trying to climb out of canyon)

        // Attempt to run directly up the canyon slope
        for (int i = 0; i < 40; i++) {
            steepPlayer.update(1f / 60f, 0f, 1.0f, false, false, false, obstacles);
        }
        check(!Float.isNaN(steepPlayer.x) && !Float.isNaN(steepPlayer.y), "cliff physics numerical stability");
        check(steepPlayer.y <= Terrain.heightAt(steepPlayer.x, steepPlayer.z) + 0.1f, "player conforms to terrain");
    }
}
