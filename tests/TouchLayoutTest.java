import com.devagent.realworldsurvival3d.TouchLayout;
import com.devagent.realworldsurvival3d.TouchLayout.ButtonType;

/**
 * TouchLayoutTest - Comprehensive JVM test suite for TouchLayout:
 *  - Screen geometry and button scaling (0.8x to 1.3x)
 *  - Opacity clamping (0.4 to 1.0)
 *  - Non-overlapping action button positions
 *  - Dedicated CROUCH button positioning and hit detection
 *  - Left-handed mode layout mirroring
 *  - Multi-aspect ratio screen scaling (4:3, 16:9, 20:9)
 *  - Hit-testing precision across all 8 buttons and contextual interact
 */
public class TouchLayoutTest {
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
        System.out.println("TouchLayout: default geometry & scaling");
        testScalingAndClamping();

        System.out.println("TouchLayout: non-overlapping button positions");
        testNonOverlappingButtons();

        System.out.println("TouchLayout: dedicated crouch button");
        testDedicatedCrouchButton();

        System.out.println("TouchLayout: left-handed mode mirroring");
        testLeftHandedMirroring();

        System.out.println("TouchLayout: multi-aspect ratio stability");
        testAspectRatioStability();

        System.out.println("TouchLayout: hit-testing precision");
        testHitTestingPrecision();

        System.out.println("\nTouchLayoutTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }

    private static void testScalingAndClamping() {
        TouchLayout layout = new TouchLayout();
        layout.update(1920f, 1080f, 1.0f, 0.65f, false);

        float defaultJumpR = layout.btnJumpRadius;
        check(defaultJumpR > 0f, "default button radius is positive: " + defaultJumpR);

        // Scale up to 1.3x
        layout.update(1920f, 1080f, 1.3f, 0.65f, false);
        checkFloat(layout.buttonScale, 1.3f, 0.01f, "buttonScale reaches 1.3x");
        checkFloat(layout.btnJumpRadius, defaultJumpR * 1.3f, 0.1f, "button radius scales up with buttonScale");

        // Scale down to 0.8x
        layout.update(1920f, 1080f, 0.8f, 0.65f, false);
        checkFloat(layout.buttonScale, 0.8f, 0.01f, "buttonScale reaches 0.8x");
        checkFloat(layout.btnJumpRadius, defaultJumpR * 0.8f, 0.1f, "button radius scales down with buttonScale");

        // Scale clamping
        layout.update(1920f, 1080f, 2.5f, 0.65f, false);
        checkFloat(layout.buttonScale, 1.3f, 0.01f, "scale > 1.3 clamps at 1.3x");

        layout.update(1920f, 1080f, 0.2f, 0.65f, false);
        checkFloat(layout.buttonScale, 0.8f, 0.01f, "scale < 0.8 clamps at 0.8x");

        // Opacity clamping
        layout.update(1920f, 1080f, 1.0f, 0.1f, false);
        checkFloat(layout.buttonOpacity, 0.4f, 0.01f, "opacity < 0.4 clamps to 0.4");

        layout.update(1920f, 1080f, 1.0f, 1.5f, false);
        checkFloat(layout.buttonOpacity, 1.0f, 0.01f, "opacity > 1.0 clamps to 1.0");

        layout.update(1920f, 1080f, 1.0f, 0.72f, false);
        checkFloat(layout.buttonOpacity, 0.72f, 0.01f, "valid opacity 0.72 is preserved");
    }

    private static void testNonOverlappingButtons() {
        TouchLayout layout = new TouchLayout();
        layout.update(1920f, 1080f, 1.0f, 0.65f, false);

        float[][] btns = {
                {layout.btnJumpX, layout.btnJumpY, layout.btnJumpRadius},
                {layout.btnCrouchX, layout.btnCrouchY, layout.btnCrouchRadius},
                {layout.btnVaultX, layout.btnVaultY, layout.btnVaultRadius},
                {layout.btnSlideX, layout.btnSlideY, layout.btnSlideRadius},
                {layout.btnSprintX, layout.btnSprintY, layout.btnSprintRadius},
                {layout.btnCamX, layout.btnCamY, layout.btnCamRadius},
                {layout.btnInteractX, layout.btnInteractY, layout.btnInteractRadius},
                {layout.btnPauseX, layout.btnPauseY, layout.btnPauseRadius}
        };

        boolean allDisjoint = true;
        for (int i = 0; i < btns.length; i++) {
            for (int j = i + 1; j < btns.length; j++) {
                float dx = btns[i][0] - btns[j][0];
                float dy = btns[i][1] - btns[j][1];
                float dist = (float) Math.sqrt(dx * dx + dy * dy);
                float minSafeDist = btns[i][2] + btns[j][2];
                if (dist < minSafeDist - 1.0f) { // allowance for touch margin
                    allDisjoint = false;
                    System.err.println("Overlap between button " + i + " and " + j + ": dist=" + dist + " < " + minSafeDist);
                }
            }
        }
        check(allDisjoint, "all 8 touch HUD buttons are pairwise disjoint (non-overlapping)");
    }

    private static void testDedicatedCrouchButton() {
        TouchLayout layout = new TouchLayout();
        layout.update(1920f, 1080f, 1.0f, 0.65f, false);

        check(layout.btnCrouchRadius > 0f, "crouch button radius > 0");
        check(layout.btnCrouchX > 0f && layout.btnCrouchX < layout.screenWidth, "crouch button within screen width");
        check(layout.btnCrouchY > 0f && layout.btnCrouchY < layout.screenHeight, "crouch button within screen height");

        // Hit testing crouch
        ButtonType hit = layout.testButton(layout.btnCrouchX, layout.btnCrouchY, false);
        check(hit == ButtonType.CROUCH, "touching crouch button center returns CROUCH: " + hit);
    }

    private static void testLeftHandedMirroring() {
        TouchLayout layout = new TouchLayout();

        // Right-handed (default)
        layout.update(1920f, 1080f, 1.0f, 0.65f, false);
        check(!layout.leftHanded, "right-handed mode: leftHanded is false");
        check(layout.joystickBaseX < 1920f * 0.5f, "right-handed joystick is on left side (X=" + layout.joystickBaseX + ")");
        check(layout.btnJumpX > 1920f * 0.5f, "right-handed jump button is on right side (X=" + layout.btnJumpX + ")");
        check(layout.isJoystickZone(200f, 500f), "touch on left is joystick zone");
        check(layout.isCameraZone(1500f, 500f), "touch on right is camera zone");

        // Left-handed mode
        layout.update(1920f, 1080f, 1.0f, 0.65f, true);
        check(layout.leftHanded, "left-handed mode: leftHanded is true");
        check(layout.joystickBaseX > 1920f * 0.5f, "left-handed joystick is on right side (X=" + layout.joystickBaseX + ")");
        check(layout.btnJumpX < 1920f * 0.5f, "left-handed jump button is on left side (X=" + layout.btnJumpX + ")");
        check(layout.isJoystickZone(1500f, 500f), "touch on right is joystick zone in left-handed mode");
        check(layout.isCameraZone(200f, 500f), "touch on left is camera zone in left-handed mode");
    }

    private static void testAspectRatioStability() {
        TouchLayout layout = new TouchLayout();

        float[][] screens = {
                {800f, 600f},   // 4:3
                {1280f, 720f},  // 16:9
                {1920f, 1080f}, // 16:9 1080p
                {2400f, 1080f}  // 20:9 ultra-wide
        };

        boolean allWithinBounds = true;
        for (float[] scr : screens) {
            layout.update(scr[0], scr[1], 1.0f, 0.65f, false);

            float[][] btns = {
                    {layout.joystickBaseX, layout.joystickBaseY, layout.joystickRadius},
                    {layout.btnJumpX, layout.btnJumpY, layout.btnJumpRadius},
                    {layout.btnCrouchX, layout.btnCrouchY, layout.btnCrouchRadius},
                    {layout.btnVaultX, layout.btnVaultY, layout.btnVaultRadius},
                    {layout.btnSlideX, layout.btnSlideY, layout.btnSlideRadius},
                    {layout.btnSprintX, layout.btnSprintY, layout.btnSprintRadius},
                    {layout.btnCamX, layout.btnCamY, layout.btnCamRadius},
                    {layout.btnInteractX, layout.btnInteractY, layout.btnInteractRadius},
                    {layout.btnPauseX, layout.btnPauseY, layout.btnPauseRadius}
            };

            for (float[] b : btns) {
                if (b[0] - b[2] < -1f || b[0] + b[2] > scr[0] + 1f ||
                    b[1] - b[2] < -1f || b[1] + b[2] > scr[1] + 1f) {
                    allWithinBounds = false;
                }
            }
        }

        check(allWithinBounds, "controls remain strictly inside screen bounds across 4:3, 16:9, and 20:9 displays");
    }

    private static void testHitTestingPrecision() {
        TouchLayout layout = new TouchLayout();
        layout.update(1920f, 1080f, 1.0f, 0.65f, false);

        check(layout.testButton(layout.btnJumpX, layout.btnJumpY, false) == ButtonType.JUMP, "hit test JUMP");
        check(layout.testButton(layout.btnCrouchX, layout.btnCrouchY, false) == ButtonType.CROUCH, "hit test CROUCH");
        check(layout.testButton(layout.btnVaultX, layout.btnVaultY, false) == ButtonType.VAULT, "hit test VAULT");
        check(layout.testButton(layout.btnSlideX, layout.btnSlideY, false) == ButtonType.SLIDE, "hit test SLIDE");
        check(layout.testButton(layout.btnSprintX, layout.btnSprintY, false) == ButtonType.SPRINT, "hit test SPRINT");
        check(layout.testButton(layout.btnCamX, layout.btnCamY, false) == ButtonType.CAM, "hit test CAM");
        check(layout.testButton(layout.btnPauseX, layout.btnPauseY, false) == ButtonType.PAUSE, "hit test PAUSE");

        // Contextual interact
        check(layout.testButton(layout.btnInteractX, layout.btnInteractY, true) == ButtonType.INTERACT, "hit test INTERACT when visible");
        check(layout.testButton(layout.btnInteractX, layout.btnInteractY, false) == ButtonType.NONE, "interact button ignored when not visible");

        // Dead space (center of screen)
        check(layout.testButton(960f, 540f, true) == ButtonType.NONE, "center of screen returns NONE");
    }
}
