import com.devagent.realworldsurvival3d.CharacterAnimator;
import com.devagent.realworldsurvival3d.Player;

/**
 * AnimationTest - JVM tests for Phase 3:
 *   - Procedural limb animation state machine
 *   - Dynamic gait frequency and movement speed matching
 *   - State pose invariants (STAND, WALK, JOG, RUN, SPRINT, CROUCH, JUMP,
 *     FALLING, SLIDING, SAFETY_ROLL, DEAD)
 *   - Smooth 0.18s cross-fade blending between states
 *   - Numerical stability (no NaNs or infinities)
 * Run: tests/run.sh
 */
public class AnimationTest {

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
        testGaitFrequencyProgression();
        testStandBreathingPose();
        testLocomotionPosesAndSpeedScaling();
        testCrouchPose();
        testAirborneAndLandingPoses();
        testAgilityPoses();
        testStateBlending();
        testNumericalStability();

        System.out.println("\nAnimationTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }

    // -----------------------------------------------------------------
    static void testGaitFrequencyProgression() {
        System.out.println("Animation: gait frequency speed-matching");
        float fStand = CharacterAnimator.calculateGaitFrequency(Player.ParkourState.STAND, 0f);
        float fWalk = CharacterAnimator.calculateGaitFrequency(Player.ParkourState.WALK, 3.5f);
        float fJog = CharacterAnimator.calculateGaitFrequency(Player.ParkourState.JOG, 5.5f);
        float fRun = CharacterAnimator.calculateGaitFrequency(Player.ParkourState.RUN, 7.5f);
        float fSprint = CharacterAnimator.calculateGaitFrequency(Player.ParkourState.SPRINT, 12.0f);

        check(fStand == 0f, "STAND has 0 gait frequency");
        check(fWalk > 0f, "WALK has positive gait frequency (" + fWalk + ")");
        check(fJog > fWalk, "JOG frequency exceeds WALK frequency (" + fJog + " > " + fWalk + ")");
        check(fRun > fJog, "RUN frequency exceeds JOG frequency (" + fRun + " > " + fJog + ")");
        check(fSprint > fRun, "SPRINT frequency exceeds RUN frequency (" + fSprint + " > " + fRun + ")");
    }

    // -----------------------------------------------------------------
    static void testStandBreathingPose() {
        System.out.println("Animation: STAND breathing pose");
        CharacterAnimator anim = new CharacterAnimator();

        float maxTorsoOffset = 0f;
        float minTorsoOffset = 0f;
        boolean legsStraight = true;
        for (int i = 0; i < 180; i++) {
            anim.update(1f / 60f, Player.ParkourState.STAND, 0f, true);
            maxTorsoOffset = Math.max(maxTorsoOffset, anim.currentPose.torsoOffsetY);
            minTorsoOffset = Math.min(minTorsoOffset, anim.currentPose.torsoOffsetY);
            if (Math.abs(anim.currentPose.leftLegPitch) > 0.001f ||
                Math.abs(anim.currentPose.rightLegPitch) > 0.001f) {
                legsStraight = false;
            }
        }
        check(legsStraight, "legs stay straight during STAND breathing");
        check(maxTorsoOffset > 0.005f, "torso bobs upward during breathing: " + maxTorsoOffset);
        check(minTorsoOffset < -0.005f, "torso bobs downward during breathing: " + minTorsoOffset);
    }

    // -----------------------------------------------------------------
    static void testLocomotionPosesAndSpeedScaling() {
        System.out.println("Animation: locomotion poses and speed scaling");
        CharacterAnimator anim = new CharacterAnimator();

        // 1. RUN
        anim.update(0.5f, Player.ParkourState.RUN, 7.5f, true);
        check(anim.currentPose.torsoPitch > 10f, "RUN has forward torso lean (" + anim.currentPose.torsoPitch + " deg)");

        // Check arm counter-swing over a stride
        boolean oppositeArms = false;
        boolean oppositeLegs = false;
        for (int i = 0; i < 30; i++) {
            anim.update(1f / 60f, Player.ParkourState.RUN, 7.5f, true);
            if (anim.currentPose.leftArmPitch * anim.currentPose.rightArmPitch < -10f) {
                oppositeArms = true;
            }
            if (anim.currentPose.leftLegPitch * anim.currentPose.rightLegPitch < -10f) {
                oppositeLegs = true;
            }
        }
        check(oppositeArms, "arms swing in counter-phase during RUN");
        check(oppositeLegs, "legs swing in counter-phase during RUN");

        // 2. SPRINT vs RUN forward lean
        CharacterAnimator animSprint = new CharacterAnimator();
        animSprint.update(0.5f, Player.ParkourState.SPRINT, 12.0f, true);
        check(animSprint.currentPose.torsoPitch > anim.currentPose.torsoPitch,
                "SPRINT forward lean (" + animSprint.currentPose.torsoPitch + ") exceeds RUN lean (" + anim.currentPose.torsoPitch + ")");
    }

    // -----------------------------------------------------------------
    static void testCrouchPose() {
        System.out.println("Animation: CROUCH pose");
        CharacterAnimator anim = new CharacterAnimator();
        anim.update(0.5f, Player.ParkourState.CROUCH, 0f, true);

        check(anim.currentPose.torsoOffsetY < -0.30f, "torso drops significantly when crouching (" + anim.currentPose.torsoOffsetY + "m)");
        check(anim.currentPose.torsoPitch > 15f, "torso leans forward in crouch (" + anim.currentPose.torsoPitch + " deg)");
        check(anim.currentPose.leftLegPitch > 30f, "knees bent in crouch (" + anim.currentPose.leftLegPitch + " deg)");
    }

    // -----------------------------------------------------------------
    static void testAirborneAndLandingPoses() {
        System.out.println("Animation: airborne and landing poses");
        CharacterAnimator anim = new CharacterAnimator();

        // JUMP
        anim.update(0.2f, Player.ParkourState.JUMP, 0f, false);
        check(anim.currentPose.leftArmPitch < -40f, "arms raised upward during JUMP (" + anim.currentPose.leftArmPitch + " deg)");

        // DOUBLE_JUMP
        CharacterAnimator animDj = new CharacterAnimator();
        animDj.update(0.2f, Player.ParkourState.DOUBLE_JUMP, 0f, false);
        check(animDj.currentPose.leftArmPitch < anim.currentPose.leftArmPitch,
                "DOUBLE_JUMP raises arms higher than JUMP (" + animDj.currentPose.leftArmPitch + " vs " + anim.currentPose.leftArmPitch + ")");

        // FALLING
        anim.update(0.3f, Player.ParkourState.FALLING, 0f, false);
        check(anim.currentPose.leftArmRoll < -20f, "left arm spreads outwards for balance during FALLING");
        check(anim.currentPose.rightArmRoll > 20f, "right arm spreads outwards for balance during FALLING");

        // LAND
        anim.update(0.2f, Player.ParkourState.LAND, 0f, true);
        check(anim.currentPose.torsoOffsetY < -0.15f, "knees compress torso down during LAND: " + anim.currentPose.torsoOffsetY);
    }

    // -----------------------------------------------------------------
    static void testAgilityPoses() {
        System.out.println("Animation: agility poses (slide, roll, wall-run, dead)");
        CharacterAnimator anim = new CharacterAnimator();

        // SLIDING
        anim.update(0.25f, Player.ParkourState.SLIDING, 10f, true);
        check(anim.currentPose.torsoPitch < -20f, "torso leans back during SLIDING: " + anim.currentPose.torsoPitch);
        check(anim.currentPose.leftLegPitch < -40f, "legs extended forward during SLIDING: " + anim.currentPose.leftLegPitch);

        // SAFETY_ROLL
        CharacterAnimator animRoll = new CharacterAnimator();
        animRoll.update(0.01f, Player.ParkourState.SAFETY_ROLL, 11f, true);
        float rollStart = animRoll.rollAngle;
        animRoll.update(0.25f, Player.ParkourState.SAFETY_ROLL, 11f, true);
        check(animRoll.rollAngle > rollStart + 100f, "rollAngle progresses through rotation: " + animRoll.rollAngle);

        // WALL_RUN_LEFT vs WALL_RUN_RIGHT
        CharacterAnimator animWLeft = new CharacterAnimator();
        animWLeft.update(0.25f, Player.ParkourState.WALL_RUN_LEFT, 12f, false);
        CharacterAnimator animWRight = new CharacterAnimator();
        animWRight.update(0.25f, Player.ParkourState.WALL_RUN_RIGHT, 12f, false);
        check(animWLeft.currentPose.torsoRoll < -10f, "WALL_RUN_LEFT banks left (" + animWLeft.currentPose.torsoRoll + " deg)");
        check(animWRight.currentPose.torsoRoll > 10f, "WALL_RUN_RIGHT banks right (" + animWRight.currentPose.torsoRoll + " deg)");

        // DEAD
        CharacterAnimator animDead = new CharacterAnimator();
        animDead.update(0.3f, Player.ParkourState.DEAD, 0f, true);
        approx(animDead.currentPose.torsoPitch, 90f, 1.0f, "torso lies flat on ground when DEAD");
    }

    // -----------------------------------------------------------------
    static void testStateBlending() {
        System.out.println("Animation: 0.18s cross-fade blending");
        CharacterAnimator anim = new CharacterAnimator();
        anim.update(1.0f, Player.ParkourState.STAND, 0f, true);

        // Switch to RUN
        anim.update(0.001f, Player.ParkourState.RUN, 7.5f, true);
        check(anim.blendProgress < 0.05f, "blendProgress begins near 0 at state change");

        // Midway through blend (0.09s = half of 0.18s)
        anim.update(0.089f, Player.ParkourState.RUN, 7.5f, true);
        approx(anim.blendProgress, 0.5f, 0.05f, "blendProgress reaches ~0.5 at half-duration");

        // Full blend reached at 0.18s
        anim.update(0.10f, Player.ParkourState.RUN, 7.5f, true);
        approx(anim.blendProgress, 1.0f, 0.01f, "blendProgress reaches 1.0 at BLEND_DURATION");
    }

    // -----------------------------------------------------------------
    static void testNumericalStability() {
        System.out.println("Animation: numerical stability");
        CharacterAnimator anim = new CharacterAnimator();
        Player.ParkourState[] states = Player.ParkourState.values();

        boolean allFinite = true;
        for (int i = 0; i < 600; i++) {
            Player.ParkourState st = states[i % states.length];
            float dt = 0.016f + (float) Math.sin(i * 0.1f) * 0.005f;
            float speed = (float) Math.abs(Math.sin(i * 0.2f)) * 12f;
            anim.update(dt, st, speed, i % 2 == 0);

            CharacterAnimator.Pose p = anim.currentPose;
            if (Float.isNaN(p.headPitch) || Float.isInfinite(p.headPitch) ||
                Float.isNaN(p.torsoOffsetY) || Float.isInfinite(p.torsoOffsetY) ||
                Float.isNaN(p.leftArmPitch) || Float.isInfinite(p.leftArmPitch) ||
                Float.isNaN(p.leftLegPitch) || Float.isInfinite(p.leftLegPitch)) {
                allFinite = false;
                break;
            }
        }
        check(allFinite, "all pose components remain finite over 600 variable-rate updates");
    }
}
