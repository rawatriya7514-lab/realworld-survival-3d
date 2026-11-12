package com.devagent.realworldsurvival3d;

/**
 * CharacterAnimator - Procedural limb animation state machine for box-composite
 * characters (Player and humanoid NPCs).
 *
 * Implements:
 *   - Articulated composite pose: Head, Torso, Left/Right Arms, Left/Right Legs.
 *   - Speed-matched dynamic gait cycle for walk, jog, run, and sprint.
 *   - Distinct procedural poses for all 18 movement & agility states.
 *   - Smooth 0.18s cross-fade blending between consecutive states.
 *   - Pure Java (100% JVM-testable without Android runtime).
 */
public class CharacterAnimator {
    public static final float BLEND_DURATION = 0.18f;

    public enum WuxiaPoseState {
        NONE,
        FIGHTING_IDLE,
        LIGHT_1,
        LIGHT_2,
        LIGHT_3,
        HEAVY_1,
        HEAVY_2,
        CHARGED,
        BLOCK,
        PARRY,
        DODGE,
        GLIDE,
        SWIM,
        AIR_DASH,
        GROUND_SLAM
    }

    public WuxiaPoseState wuxiaState = WuxiaPoseState.NONE;
    public WuxiaPoseState previousWuxiaState = WuxiaPoseState.NONE;

    public static class Pose {
        // Head (degrees & offsets)
        public float headPitch = 0f;
        public float headYaw = 0f;
        public float headRoll = 0f;

        // Torso
        public float torsoPitch = 0f;
        public float torsoYaw = 0f;
        public float torsoRoll = 0f;
        public float torsoOffsetY = 0f;

        // Left Arm
        public float leftArmPitch = 0f;
        public float leftArmYaw = 0f;
        public float leftArmRoll = 0f;

        // Right Arm
        public float rightArmPitch = 0f;
        public float rightArmYaw = 0f;
        public float rightArmRoll = 0f;

        // Left Leg
        public float leftLegPitch = 0f;
        public float leftLegYaw = 0f;
        public float leftLegRoll = 0f;

        // Right Leg
        public float rightLegPitch = 0f;
        public float rightLegYaw = 0f;
        public float rightLegRoll = 0f;

        public void reset() {
            this.headPitch = 0f; this.headYaw = 0f; this.headRoll = 0f;
            this.torsoPitch = 0f; this.torsoYaw = 0f; this.torsoRoll = 0f; this.torsoOffsetY = 0f;
            this.leftArmPitch = 0f; this.leftArmYaw = 0f; this.leftArmRoll = 0f;
            this.rightArmPitch = 0f; this.rightArmYaw = 0f; this.rightArmRoll = 0f;
            this.leftLegPitch = 0f; this.leftLegYaw = 0f; this.leftLegRoll = 0f;
            this.rightLegPitch = 0f; this.rightLegYaw = 0f; this.rightLegRoll = 0f;
        }

        public void copyFrom(Pose other) {
            this.headPitch = other.headPitch;
            this.headYaw = other.headYaw;
            this.headRoll = other.headRoll;
            this.torsoPitch = other.torsoPitch;
            this.torsoYaw = other.torsoYaw;
            this.torsoRoll = other.torsoRoll;
            this.torsoOffsetY = other.torsoOffsetY;
            this.leftArmPitch = other.leftArmPitch;
            this.leftArmYaw = other.leftArmYaw;
            this.leftArmRoll = other.leftArmRoll;
            this.rightArmPitch = other.rightArmPitch;
            this.rightArmYaw = other.rightArmYaw;
            this.rightArmRoll = other.rightArmRoll;
            this.leftLegPitch = other.leftLegPitch;
            this.leftLegYaw = other.leftLegYaw;
            this.leftLegRoll = other.leftLegRoll;
            this.rightLegPitch = other.rightLegPitch;
            this.rightLegYaw = other.rightLegYaw;
            this.rightLegRoll = other.rightLegRoll;
        }

        public void lerp(Pose from, Pose to, float t) {
            float s = smooth01(t);
            this.headPitch = mix(from.headPitch, to.headPitch, s);
            this.headYaw = mix(from.headYaw, to.headYaw, s);
            this.headRoll = mix(from.headRoll, to.headRoll, s);
            this.torsoPitch = mix(from.torsoPitch, to.torsoPitch, s);
            this.torsoYaw = mix(from.torsoYaw, to.torsoYaw, s);
            this.torsoRoll = mix(from.torsoRoll, to.torsoRoll, s);
            this.torsoOffsetY = mix(from.torsoOffsetY, to.torsoOffsetY, s);
            this.leftArmPitch = mix(from.leftArmPitch, to.leftArmPitch, s);
            this.leftArmYaw = mix(from.leftArmYaw, to.leftArmYaw, s);
            this.leftArmRoll = mix(from.leftArmRoll, to.leftArmRoll, s);
            this.rightArmPitch = mix(from.rightArmPitch, to.rightArmPitch, s);
            this.rightArmYaw = mix(from.rightArmYaw, to.rightArmYaw, s);
            this.rightArmRoll = mix(from.rightArmRoll, to.rightArmRoll, s);
            this.leftLegPitch = mix(from.leftLegPitch, to.leftLegPitch, s);
            this.leftLegYaw = mix(from.leftLegYaw, to.leftLegYaw, s);
            this.leftLegRoll = mix(from.leftLegRoll, to.leftLegRoll, s);
            this.rightLegPitch = mix(from.rightLegPitch, to.rightLegPitch, s);
            this.rightLegYaw = mix(from.rightLegYaw, to.rightLegYaw, s);
            this.rightLegRoll = mix(from.rightLegRoll, to.rightLegRoll, s);
        }

        private static float smooth01(float x) {
            if (x <= 0f) return 0f;
            if (x >= 1f) return 1f;
            return x * x * (3f - 2f * x);
        }

        private static float mix(float a, float b, float t) {
            return a + (b - a) * t;
        }
    }

    // Active Poses
    public final Pose currentPose = new Pose();
    private final Pose previousPose = new Pose();
    private final Pose targetPose = new Pose();

    // State & Blending
    public Player.ParkourState currentState = Player.ParkourState.STAND;
    public Player.ParkourState previousState = Player.ParkourState.STAND;
    public float blendProgress = 1.0f; // 1.0 = fully transitioned
    public float gaitPhase = 0f;       // Radians 0..2PI
    public float breathPhase = 0f;     // Radians for idle breathing
    public float rollAngle = 0f;       // Deg for safety roll (0..360)

    public CharacterAnimator() {
        evaluateStatePose(Player.ParkourState.STAND, 0f, true, currentPose);
        previousPose.copyFrom(currentPose);
    }

    public void update(float dt, Player.ParkourState state, float horizSpeed, boolean isGrounded) {
        if (state != currentState) {
            previousState = currentState;
            currentState = state;
            previousPose.copyFrom(currentPose);
            blendProgress = 0f;
            if (state == Player.ParkourState.SAFETY_ROLL) {
                rollAngle = 0f;
            }
        }

        // Advance blending
        if (blendProgress < 1.0f) {
            blendProgress = Math.min(1.0f, blendProgress + dt / BLEND_DURATION);
        }

        // Idle breathing oscillation
        breathPhase += dt * 2.2f;
        if (breathPhase > (float) (Math.PI * 2)) breathPhase -= (float) (Math.PI * 2);

        // Gait cycle
        float gaitFreq = calculateGaitFrequency(currentState, horizSpeed);
        gaitPhase += dt * gaitFreq;
        if (gaitPhase > (float) (Math.PI * 2)) gaitPhase -= (float) (Math.PI * 2);

        // Safety roll continuous rotation
        if (currentState == Player.ParkourState.SAFETY_ROLL) {
            rollAngle += dt * (360f / 0.50f); // 360 deg over 0.5s rollDuration
        }

        // Evaluate target pose for currentState or active wuxia pose
        if (wuxiaState != WuxiaPoseState.NONE) {
            evaluateWuxiaPose(wuxiaState, targetPose);
        } else {
            evaluateStatePose(currentState, horizSpeed, isGrounded, targetPose);
        }

        // Smoothly blend previousPose into targetPose -> currentPose
        currentPose.lerp(previousPose, targetPose, blendProgress);
    }

    public void updateWuxia(float dt, WuxiaPoseState poseState, float horizSpeed, boolean isGrounded) {
        if (poseState != wuxiaState) {
            previousWuxiaState = wuxiaState;
            wuxiaState = poseState;
            previousPose.copyFrom(currentPose);
            blendProgress = 0f;
        }
        update(dt, currentState, horizSpeed, isGrounded);
    }

    public static float calculateGaitFrequency(Player.ParkourState state, float horizSpeed) {
        switch (state) {
            case STAND:
            case HARD_LAND:
            case DEAD:
                return 0f;
            case WALK:
                return 8.5f;   // ~1.35 Hz
            case JOG:
                return 12.0f;  // ~1.9 Hz
            case RUN:
                return 15.0f;  // ~2.4 Hz
            case SPRINT:
                return 19.0f;  // ~3.0 Hz
            case CROUCH:
                return horizSpeed > 0.2f ? 6.5f : 0f;
            default:
                return horizSpeed > 0.2f ? Math.min(18f, Math.max(5f, horizSpeed * 2.2f)) : 0f;
        }
    }

    private void evaluateStatePose(Player.ParkourState state, float speed, boolean isGrounded, Pose out) {
        float breath = (float) Math.sin(breathPhase);
        float swing = (float) Math.sin(gaitPhase);
        float bounce = (float) Math.abs(Math.sin(gaitPhase * 2f));

        // Default reset
        out.headPitch = 0f; out.headYaw = 0f; out.headRoll = 0f;
        out.torsoPitch = 0f; out.torsoYaw = 0f; out.torsoRoll = 0f;
        out.torsoOffsetY = 0f;
        out.leftArmPitch = 0f; out.leftArmYaw = 0f; out.leftArmRoll = -8f;
        out.rightArmPitch = 0f; out.rightArmYaw = 0f; out.rightArmRoll = 8f;
        out.leftLegPitch = 0f; out.leftLegYaw = 0f; out.leftLegRoll = 0f;
        out.rightLegPitch = 0f; out.rightLegYaw = 0f; out.rightLegRoll = 0f;

        switch (state) {
            case STAND:
                out.torsoOffsetY = breath * 0.015f;
                out.torsoPitch = breath * 1.2f;
                out.leftArmPitch = breath * 2.0f;
                out.rightArmPitch = -breath * 2.0f;
                out.leftArmRoll = -7f;
                out.rightArmRoll = 7f;
                break;

            case WALK:
                out.torsoPitch = 4.0f;
                out.torsoOffsetY = bounce * 0.025f;
                out.leftArmPitch = swing * 22f;
                out.rightArmPitch = -swing * 22f;
                out.leftLegPitch = -swing * 25f;
                out.rightLegPitch = swing * 25f;
                out.leftArmRoll = -9f;
                out.rightArmRoll = 9f;
                break;

            case JOG:
                out.torsoPitch = 9.0f;
                out.torsoOffsetY = bounce * 0.04f;
                out.leftArmPitch = swing * 38f;
                out.rightArmPitch = -swing * 38f;
                out.leftLegPitch = -swing * 38f;
                out.rightLegPitch = swing * 38f;
                out.leftArmRoll = -11f;
                out.rightArmRoll = 11f;
                break;

            case RUN:
                out.torsoPitch = 15.0f;
                out.torsoOffsetY = bounce * 0.055f;
                out.leftArmPitch = swing * 52f;
                out.rightArmPitch = -swing * 52f;
                out.leftLegPitch = -swing * 50f;
                out.rightLegPitch = swing * 50f;
                out.leftArmRoll = -12f;
                out.rightArmRoll = 12f;
                break;

            case SPRINT:
                out.torsoPitch = 24.0f;
                out.torsoOffsetY = bounce * 0.07f;
                out.leftArmPitch = swing * 68f;
                out.rightArmPitch = -swing * 68f;
                out.leftLegPitch = -swing * 64f;
                out.rightLegPitch = swing * 64f;
                out.leftArmRoll = -14f;
                out.rightArmRoll = 14f;
                break;

            case CROUCH:
                out.torsoOffsetY = -0.42f;
                out.torsoPitch = 24.0f;
                out.headPitch = -12.0f;
                if (speed > 0.2f) {
                    float cSwing = swing * 16f;
                    out.leftLegPitch = 42f - cSwing;
                    out.rightLegPitch = 42f + cSwing;
                    out.leftArmPitch = cSwing;
                    out.rightArmPitch = -cSwing;
                } else {
                    out.leftLegPitch = 42f;
                    out.rightLegPitch = 42f;
                }
                out.leftArmRoll = -15f;
                out.rightArmRoll = 15f;
                break;

            case JUMP:
                out.torsoOffsetY = 0.04f;
                out.torsoPitch = -6.0f;
                out.leftArmPitch = -65f;
                out.rightArmPitch = -65f;
                out.leftArmRoll = -18f;
                out.rightArmRoll = 18f;
                out.leftLegPitch = 24f;
                out.rightLegPitch = 18f;
                break;

            case DOUBLE_JUMP:
                out.torsoOffsetY = 0.08f;
                out.torsoPitch = -12.0f;
                out.leftArmPitch = -85f;
                out.rightArmPitch = -85f;
                out.leftArmRoll = -24f;
                out.rightArmRoll = 24f;
                out.leftLegPitch = 38f;
                out.rightLegPitch = 32f;
                break;

            case FALLING:
                out.torsoOffsetY = 0.02f;
                out.torsoPitch = 6.0f;
                out.leftArmPitch = -24f;
                out.rightArmPitch = -24f;
                out.leftArmRoll = -38f;
                out.rightArmRoll = 38f;
                out.leftLegPitch = -12f;
                out.rightLegPitch = -10f;
                out.leftLegRoll = -6f;
                out.rightLegRoll = 6f;
                break;

            case LAND:
                out.torsoOffsetY = -0.24f;
                out.torsoPitch = 18.0f;
                out.leftLegPitch = 32f;
                out.rightLegPitch = 32f;
                out.leftArmPitch = 20f;
                out.rightArmPitch = 20f;
                out.leftArmRoll = -16f;
                out.rightArmRoll = 16f;
                break;

            case HARD_LAND:
                out.torsoOffsetY = -0.45f;
                out.torsoPitch = 34.0f;
                out.headPitch = 24.0f;
                out.leftLegPitch = 52f;
                out.rightLegPitch = 52f;
                out.leftArmPitch = 30f;
                out.rightArmPitch = 30f;
                out.leftArmRoll = -18f;
                out.rightArmRoll = 18f;
                break;

            case SAFETY_ROLL:
                out.torsoOffsetY = -0.35f;
                out.torsoPitch = rollAngle;
                out.leftLegPitch = 55f;
                out.rightLegPitch = 55f;
                out.leftArmPitch = 55f;
                out.rightArmPitch = 55f;
                break;

            case SLIDING:
                out.torsoOffsetY = -0.48f;
                out.torsoPitch = -32.0f;
                out.leftLegPitch = -68f;
                out.rightLegPitch = -68f;
                out.leftArmPitch = 42f;
                out.rightArmPitch = 42f;
                out.leftArmRoll = -22f;
                out.rightArmRoll = 22f;
                break;

            case WALL_RUN_LEFT:
                out.torsoRoll = -20f;
                out.torsoPitch = 10f;
                out.leftLegPitch = swing * 40f;
                out.rightLegPitch = -swing * 40f;
                out.leftLegRoll = 15f;
                out.rightLegRoll = 15f;
                out.leftArmPitch = swing * 45f;
                out.rightArmPitch = -swing * 45f;
                out.rightArmRoll = 25f;
                break;

            case WALL_RUN_RIGHT:
                out.torsoRoll = 20f;
                out.torsoPitch = 10f;
                out.leftLegPitch = swing * 40f;
                out.rightLegPitch = -swing * 40f;
                out.leftLegRoll = -15f;
                out.rightLegRoll = -15f;
                out.leftArmPitch = swing * 45f;
                out.rightArmPitch = -swing * 45f;
                out.leftArmRoll = -25f;
                break;

            case MANTLING:
                out.torsoOffsetY = 0.15f;
                out.torsoPitch = 25f;
                out.leftArmPitch = -80f;
                out.rightArmPitch = -80f;
                out.leftLegPitch = 40f;
                out.rightLegPitch = 20f;
                break;

            case VAULTING:
                out.torsoOffsetY = 0.10f;
                out.torsoPitch = 20f;
                out.torsoRoll = 15f;
                out.leftArmPitch = -50f;
                out.rightArmPitch = 30f;
                out.leftLegPitch = -40f;
                out.rightLegPitch = -20f;
                break;

            case DEAD:
                out.torsoOffsetY = -0.75f;
                out.torsoPitch = 90f;
                out.headPitch = -25f;
                out.leftArmPitch = 80f;
                out.rightArmPitch = -55f;
                out.leftLegPitch = 25f;
                out.rightLegPitch = -15f;
                break;

            default:
                break;
        }
    }

    public static void evaluateWuxiaPose(WuxiaPoseState state, Pose out) {
        out.reset();
        switch (state) {
            case FIGHTING_IDLE:
                out.torsoPitch = 12f;
                out.torsoYaw = -10f;
                out.leftArmPitch = -35f;
                out.rightArmPitch = -55f;
                out.leftLegPitch = 15f;
                out.rightLegPitch = -12f;
                break;

            case LIGHT_1:
                out.torsoPitch = 15f;
                out.torsoYaw = 25f;
                out.rightArmPitch = -75f;
                out.rightArmYaw = 40f;
                out.leftArmRoll = -20f;
                out.leftLegPitch = -15f;
                out.rightLegPitch = 20f;
                break;

            case LIGHT_2:
                out.torsoPitch = -8f;
                out.torsoYaw = -20f;
                out.rightArmPitch = 30f;
                out.rightArmRoll = 60f;
                out.leftArmPitch = -30f;
                break;

            case LIGHT_3:
                out.torsoPitch = 18f;
                out.torsoYaw = 45f;
                out.rightArmPitch = -85f;
                out.rightArmRoll = 25f;
                out.leftArmPitch = 35f;
                out.leftLegPitch = 25f;
                break;

            case HEAVY_1:
                out.torsoPitch = 35f;
                out.rightArmPitch = -110f;
                out.leftArmPitch = -90f;
                out.leftLegPitch = 30f;
                out.rightLegPitch = -25f;
                break;

            case HEAVY_2:
                out.torsoPitch = 10f;
                out.torsoYaw = -45f;
                out.rightArmPitch = -60f;
                out.rightArmYaw = -40f;
                out.leftArmRoll = 30f;
                break;

            case CHARGED:
                out.torsoOffsetY = -0.22f;
                out.torsoPitch = 15f;
                out.leftArmPitch = 25f;
                out.rightArmPitch = 25f;
                out.leftArmRoll = -25f;
                out.rightArmRoll = 25f;
                out.leftLegPitch = 28f;
                out.rightLegPitch = 28f;
                break;

            case BLOCK:
                out.torsoPitch = 5f;
                out.leftArmPitch = -65f;
                out.rightArmPitch = -65f;
                out.leftArmYaw = 35f;
                out.rightArmYaw = -35f;
                out.leftLegPitch = 12f;
                out.rightLegPitch = -10f;
                break;

            case PARRY:
                out.torsoOffsetY = 0.05f;
                out.torsoRoll = -15f;
                out.rightArmPitch = -85f;
                out.rightArmRoll = 35f;
                out.leftArmPitch = 40f;
                break;

            case DODGE:
                out.torsoOffsetY = -0.32f;
                out.torsoPitch = 32f;
                out.leftArmPitch = 45f;
                out.rightArmPitch = 45f;
                out.leftLegPitch = 35f;
                out.rightLegPitch = 35f;
                break;

            case GLIDE:
                out.torsoPitch = 42f;
                out.leftArmPitch = -10f;
                out.rightArmPitch = -10f;
                out.leftArmRoll = -75f;
                out.rightArmRoll = 75f;
                out.leftLegPitch = -22f;
                out.rightLegPitch = -22f;
                break;

            case SWIM:
                out.torsoPitch = 70f;
                out.leftArmPitch = -60f;
                out.rightArmPitch = 40f;
                out.leftLegPitch = 18f;
                out.rightLegPitch = -18f;
                break;

            case AIR_DASH:
                out.torsoPitch = 82f;
                out.leftArmPitch = 60f;
                out.rightArmPitch = 60f;
                out.leftLegPitch = -10f;
                out.rightLegPitch = -10f;
                break;

            case GROUND_SLAM:
                out.torsoPitch = 65f;
                out.rightArmPitch = -120f;
                out.leftArmPitch = 45f;
                out.leftLegPitch = -25f;
                out.rightLegPitch = -25f;
                break;

            default:
                break;
        }
    }
}
