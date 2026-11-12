package com.devagent.realworldsurvival3d;

import java.util.List;

/**
 * Player - Advanced 3D Parkour & Survival Character Controller.
 * Features:
 *   - Ground locomotion (Walk, Run, Turbo Sprint)
 *   - Ground Jump & Mid-Air Double Jump / Wall Kick
 *   - Wall-Running (Horizontal stick & glide on left/right walls)
 *   - Ledge-Grab & Smooth Mantling up onto rooftops
 *   - Vaulting over waist-high obstacles
 *   - Low-clearance Crouch-Sliding
 *   - High-Fall Safety Roll (negates fall damage when timed upon impact)
 *   - Fall Damage & Screen Shake on unrolled hard landing
 *   - Dynamic Stamina & Adrenaline Combo multiplier
 */
public class Player {
    public enum ParkourState {
        STAND,
        WALK,
        JOG,
        RUN,
        SPRINT,
        CROUCH,
        JUMP,
        DOUBLE_JUMP,
        WALL_RUN_LEFT,
        WALL_RUN_RIGHT,
        LEDGE_HANG,
        MANTLING,
        VAULTING,
        SLIDING,
        SAFETY_ROLL,
        FALLING,
        LAND,
        HARD_LAND,
        DEAD
    }

    // Transform
    public float x = 0f;
    public float y = 22f; // Spawn on Tower Alpha roof (y=20, player height offset)
    public float z = 0f;
    public float vx = 0f, vy = 0f, vz = 0f;
    public float yaw = 0f;   // Horizontal look rotation in degrees
    public float pitch = 0f; // Vertical look pitch in degrees (-80 to 80)
    public float cameraRoll = 0f; // Camera tilt (Dutch angle on wall runs)

    // Bounding Box
    public float radius = 0.5f;
    public float standingHeight = 1.8f;
    public float crouchingHeight = 0.8f;
    public float currentHeight = 1.8f;

    // Survival Stats
    public float health = 100f;
    public float maxHealth = 100f;
    public float stamina = 100f;
    public float maxStamina = 100f;
    public float adrenaline = 0f;
    public float maxAdrenaline = 100f;
    public int score = 0;
    public int currency = 0;

    // Movement Speeds (m/s)
    public float walkSpeed = 3.5f;
    public float jogSpeed = 5.5f;
    public float runSpeed = 7.5f;
    public float sprintSpeed = 12.0f;
    public float crouchSpeed = 2.5f;
    public float wallRunSpeed = 12.5f;
    public float jumpImpulse = 9.0f;
    public float gravity = -22.0f;

    // Movement & Collision Constants
    public static final float MAX_STEP_UP = 0.55f;
    public static final float MAX_STEP_DOWN = 0.55f;
    public static final float MAX_WALKABLE_SLOPE_COS = 0.60f; // ~53 degrees

    // State Tracking
    public ParkourState state = ParkourState.STAND;
    public boolean isGrounded = false;
    public boolean isSprinting = false;
    public boolean isCrouching = false;
    public boolean firstPersonView = false;
    public boolean adrenalineRush = false;

    // Timers
    public float wallRunTimer = 0f;
    public float maxWallRunDuration = 2.0f;
    public float mantleTimer = 0f;
    public float mantleDuration = 0.40f;
    public float mantleStartY = 0f;
    public float mantleTargetY = 0f;
    public float mantleTargetZ = 0f;
    public float mantleTargetX = 0f;

    public float vaultTimer = 0f;
    public float vaultDuration = 0.35f;
    public float slideTimer = 0f;
    public float slideDuration = 0.65f;
    public float rollTimer = 0f;
    public float rollDuration = 0.50f;
    public float hardLandTimer = 0f;
    public float landTimer = 0f;
    public float landDuration = 0.12f;

    // Fall Tracking
    public float peakFallY = 22f;
    public float lastImpactVelocity = 0f;
    public boolean canDoubleJump = true;
    /** Cooldown between hazard-pool damage ticks. */
    public float hazardCooldown = 0f;

    // Combos & Notifications
    public int comboCount = 0;
    public float comboTimer = 0f;
    public String lastParkourAction = "";
    public float actionNotifyTimer = 0f;

    // Audio Trigger Flags
    public boolean playJumpSfx = false;
    public boolean playDoubleJumpSfx = false;
    public boolean playWallRunSfx = false;
    public boolean playVaultSfx = false;
    public boolean playLedgeGrabSfx = false;
    public boolean playMantleSfx = false;
    public boolean playSlideSfx = false;
    public boolean playRollSfx = false;
    public boolean playHardLandSfx = false;
    public boolean playFootstepSfx = false;
    private float footstepTimer = 0f;

    // ------------------------------------------------------------------
    // Open-world terrain support (null = classic parkour behaviour).
    // When set, the analytic heightfield is the ground and deep water is
    // impassable; the update signature and parkour logic stay untouched.
    // ------------------------------------------------------------------
    public Terrain terrain = null;
    public float waterLevel = Terrain.WATER_LEVEL;

    /** Procedural limb animation state machine. */
    public final CharacterAnimator animator = new CharacterAnimator();

    // Wuxia Subsystems (Phases 1-7)
    public final CharacterCustomization customization = new CharacterCustomization();
    public final WuxiaMovementSystem wuxiaMovement = new WuxiaMovementSystem();
    public final WeaponSystem weapons = new WeaponSystem();
    public final WuxiaCombatSystem combat = new WuxiaCombatSystem();
    public final QiAbilitySystem qiAbilities = new QiAbilitySystem();
    public final SkillTree skillTree = new SkillTree();
    public boolean wuxiaMode = false;

    public void update(float dt, float moveInputX, float moveInputY, boolean jumpPressed,
                       boolean slidePressed, boolean sprintToggled, List<Obstacle> obstacles) {
        update(dt, moveInputX, moveInputY, jumpPressed, slidePressed, sprintToggled, false, obstacles);
    }

    public void update(float dt, float moveInputX, float moveInputY, boolean jumpPressed,
                       boolean slidePressed, boolean sprintToggled, boolean crouchPressed,
                       List<Obstacle> obstacles) {
        if (state == ParkourState.DEAD) {
            animator.update(dt, state, 0f, isGrounded);
            return;
        }

        // Combo decay
        if (comboTimer > 0f) {
            comboTimer -= dt;
            if (comboTimer <= 0f) comboCount = 0;
        }
        if (actionNotifyTimer > 0f) {
            actionNotifyTimer -= dt;
        }
        if (hazardCooldown > 0f) {
            hazardCooldown -= dt;
        }

        // Adrenaline Rush
        if (adrenaline >= maxAdrenaline) {
            adrenalineRush = true;
        }
        if (adrenalineRush) {
            adrenaline -= 15f * dt;
            if (adrenaline <= 0f) {
                adrenaline = 0f;
                adrenalineRush = false;
            }
        }

        // Stamina logic
        if (isSprinting && (moveInputX != 0 || moveInputY != 0) && !adrenalineRush) {
            stamina -= 12f * dt;
            if (stamina <= 0f) {
                stamina = 0f;
                isSprinting = false;
            }
        } else {
            stamina += 18f * dt;
            if (stamina > maxStamina) stamina = maxStamina;
        }

        // Camera roll decay towards 0 (Dutch angle resets when not wall running)
        if (state != ParkourState.WALL_RUN_LEFT && state != ParkourState.WALL_RUN_RIGHT) {
            cameraRoll += (0f - cameraRoll) * Math.min(1.0f, dt * 10f);
        }

        // Execute specific state updates
        boolean handled = false;
        switch (state) {
            case MANTLING:
                updateMantling(dt);
                handled = true;
                break;
            case VAULTING:
                updateVaulting(dt);
                handled = true;
                break;
            case SLIDING:
                updateSliding(dt, jumpPressed, obstacles);
                handled = true;
                break;
            case SAFETY_ROLL:
                updateSafetyRoll(dt, obstacles);
                handled = true;
                break;
            case HARD_LAND:
                updateHardLand(dt);
                handled = true;
                break;
            case LAND:
                if (!updateLand(dt, moveInputX, moveInputY)) {
                    handled = true;
                }
                break;
            case WALL_RUN_LEFT:
            case WALL_RUN_RIGHT:
                updateWallRunning(dt, jumpPressed, obstacles);
                handled = true;
                break;
            default:
                break;
        }

        if (!handled) {
            handleMovement(dt, moveInputX, moveInputY, jumpPressed, slidePressed, sprintToggled, crouchPressed, obstacles);
        }

        if (wuxiaMode) {
            wuxiaMovement.update(dt, this, obstacles);
            weapons.update(dt);
            combat.update(dt, this);
            qiAbilities.update(dt);

            if (isGrounded) {
                wuxiaMovement.onLand();
            }

            // Map Wuxia states to procedural animator poses
            if (combat.state != WuxiaCombatSystem.CombatState.NEUTRAL) {
                switch (combat.state) {
                    case LIGHT_1: animator.wuxiaState = CharacterAnimator.WuxiaPoseState.LIGHT_1; break;
                    case LIGHT_2: animator.wuxiaState = CharacterAnimator.WuxiaPoseState.LIGHT_2; break;
                    case LIGHT_3: animator.wuxiaState = CharacterAnimator.WuxiaPoseState.LIGHT_3; break;
                    case HEAVY_1: animator.wuxiaState = CharacterAnimator.WuxiaPoseState.HEAVY_1; break;
                    case HEAVY_2: animator.wuxiaState = CharacterAnimator.WuxiaPoseState.HEAVY_2; break;
                    case CHARGING:
                    case CHARGED_RELEASE: animator.wuxiaState = CharacterAnimator.WuxiaPoseState.CHARGED; break;
                    case BLOCKING: animator.wuxiaState = CharacterAnimator.WuxiaPoseState.BLOCK; break;
                    case PARRYING:
                    case PARRY_SUCCESS: animator.wuxiaState = CharacterAnimator.WuxiaPoseState.PARRY; break;
                    case DODGING: animator.wuxiaState = CharacterAnimator.WuxiaPoseState.DODGE; break;
                    default: animator.wuxiaState = CharacterAnimator.WuxiaPoseState.FIGHTING_IDLE; break;
                }
            } else if (wuxiaMovement.isGliding) {
                animator.wuxiaState = CharacterAnimator.WuxiaPoseState.GLIDE;
            } else if (wuxiaMovement.inWater) {
                animator.wuxiaState = CharacterAnimator.WuxiaPoseState.SWIM;
            } else if (wuxiaMovement.currentState == WuxiaMovementSystem.MovementState.AIR_DASH) {
                animator.wuxiaState = CharacterAnimator.WuxiaPoseState.AIR_DASH;
            } else if (wuxiaMovement.currentState == WuxiaMovementSystem.MovementState.FAST_DESCENT) {
                animator.wuxiaState = CharacterAnimator.WuxiaPoseState.GROUND_SLAM;
            } else {
                animator.wuxiaState = CharacterAnimator.WuxiaPoseState.NONE;
            }
        }

        // Advance procedural character animator
        float horizSpeed = (float) Math.sqrt(vx * vx + vz * vz);
        animator.update(dt, state, horizSpeed, isGrounded);
    }

    public boolean canStandUp(List<Obstacle> obstacles) {
        if (obstacles == null) return true;
        float headTargetY = y + standingHeight;
        for (Obstacle o : obstacles) {
            if (!o.solid) continue;
            if (o.containsXZ(x, z, radius)) {
                if (o.getMinY() < headTargetY && o.getMaxY() > y + crouchingHeight) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean canStepUp(float targetY, float nx, float nz, List<Obstacle> obstacles) {
        if (obstacles == null) return true;
        float targetHeadY = targetY + currentHeight;
        for (Obstacle o : obstacles) {
            if (!o.solid) continue;
            if (o.containsXZ(nx, nz, radius)) {
                if (o.getMinY() < targetHeadY && o.getMaxY() > targetY + 0.05f) {
                    return false;
                }
            }
        }
        return true;
    }

    public void setCrouching(boolean crouch, List<Obstacle> obstacles) {
        if (!crouch) {
            if (canStandUp(obstacles)) {
                isCrouching = false;
                currentHeight = standingHeight;
            }
        } else {
            isCrouching = true;
            currentHeight = crouchingHeight;
            isSprinting = false;
        }
    }

    private boolean updateLand(float dt, float inputX, float inputY) {
        landTimer -= dt;
        if (landTimer <= 0f || Math.abs(inputX) > 0.1f || Math.abs(inputY) > 0.1f) {
            state = ParkourState.STAND;
            return true;
        }
        return false;
    }

    private void handleMovement(float dt, float inputX, float inputY, boolean jumpPressed,
                                boolean slidePressed, boolean sprintToggled, boolean crouchPressed,
                                List<Obstacle> obstacles) {
        if (sprintToggled && stamina > 15f && !isCrouching) {
            isSprinting = !isSprinting;
        }

        // Crouch trigger: explicit crouchPressed, or slidePressed when grounded and not sprinting
        if (crouchPressed || (slidePressed && !isSprinting && isGrounded)) {
            if (isCrouching) {
                if (canStandUp(obstacles)) {
                    isCrouching = false;
                    currentHeight = standingHeight;
                }
            } else {
                isCrouching = true;
                currentHeight = crouchingHeight;
                isSprinting = false;
            }
        } else if (isCrouching && !canStandUp(obstacles)) {
            currentHeight = crouchingHeight;
        }

        float inputMag = (float) Math.sqrt(inputX * inputX + inputY * inputY);
        float speed = 0f;
        if (isCrouching) {
            speed = crouchSpeed;
        } else if (isSprinting && stamina > 0f) {
            speed = sprintSpeed;
            if (adrenalineRush) speed *= 1.25f;
        } else if (inputMag > 0.75f) {
            speed = runSpeed;
        } else if (inputMag > 0.35f) {
            speed = jogSpeed;
        } else if (inputMag > 0.05f) {
            speed = walkSpeed;
        }

        // Direction from yaw
        float radYaw = (float) Math.toRadians(yaw);
        float forwardX = (float) Math.sin(radYaw);
        float forwardZ = (float) -Math.cos(radYaw);
        float rightX = (float) Math.cos(radYaw);
        float rightZ = (float) Math.sin(radYaw);

        float targetVx = 0f;
        float targetVz = 0f;
        if (inputMag > 0.001f && speed > 0f) {
            float dirX = (forwardX * -inputY + rightX * inputX) / inputMag;
            float dirZ = (forwardZ * -inputY + rightZ * inputX) / inputMag;
            targetVx = dirX * speed;
            targetVz = dirZ * speed;
        }

        // Wading through water halves ground speed (open world only)
        if (terrain != null) {
            float depth = Terrain.waterDepthAt(x, z);
            if (depth > 0.15f) {
                targetVx *= 0.55f;
                targetVz *= 0.55f;
            }
        }

        float accel = isGrounded ? 15f : 6f;
        vx += (targetVx - vx) * Math.min(1.0f, dt * accel);
        vz += (targetVz - vz) * Math.min(1.0f, dt * accel);

        // Slide trigger (only when sprinting and grounded)
        if (isGrounded && isSprinting && slidePressed && (inputX != 0 || inputY != 0)) {
            startSlide();
            return;
        }

        // Jump trigger
        if (jumpPressed) {
            if (isGrounded) {
                if (isCrouching && !canStandUp(obstacles)) {
                    // Headroom blocked by ceiling, cannot jump
                } else {
                    if (isCrouching) {
                        isCrouching = false;
                        currentHeight = standingHeight;
                    }
                    Obstacle vaultTarget = checkVaultBarrierAhead(obstacles, forwardX, forwardZ);
                    if (vaultTarget != null && isSprinting) {
                        startVault(vaultTarget);
                        return;
                    }

                    vy = jumpImpulse;
                    isGrounded = false;
                    canDoubleJump = true;
                    if (wuxiaMode) {
                        wuxiaMovement.jumpsRemaining = wuxiaMovement.tripleJumpUnlocked ? 2 : 1;
                    }
                    state = ParkourState.JUMP;
                    peakFallY = y;
                    playJumpSfx = true;
                    addCombo("HIGH LEAP", 5);
                }
            } else if (canDoubleJump) {
                vy = jumpImpulse * 0.9f;
                canDoubleJump = false;
                if (wuxiaMode && wuxiaMovement.tripleJumpUnlocked) {
                    wuxiaMovement.jumpsRemaining = 1;
                }
                state = ParkourState.DOUBLE_JUMP;
                playDoubleJumpSfx = true;
                addCombo("AIR BOOST", 10);
            } else if (wuxiaMode && wuxiaMovement.tripleJumpUnlocked && wuxiaMovement.jumpsRemaining > 0 && stamina >= 15f) {
                vy = wuxiaMovement.jumpVelocity3;
                wuxiaMovement.jumpsRemaining = 0;
                stamina -= 15f;
                state = ParkourState.DOUBLE_JUMP;
                wuxiaMovement.triggerTripleJumpVfx = true;
                addCombo("SKYWARD TRIPLE LEAP", 15);
            }
        }

        // Apply gravity if airborne
        if (!isGrounded) {
            vy += gravity * dt;
            if (vy < -35f) vy = -35f; // Terminal velocity
            if (y > peakFallY) peakFallY = y;

            checkWallRun(obstacles, forwardX, forwardZ, rightX, rightZ);
            checkLedgeGrab(obstacles, forwardX, forwardZ);
        }

        // Slope handling on terrain
        if (terrain != null) {
            float[] normal = new float[3];
            Terrain.normalAt(x, z, normal);
            float normalY = normal[1];
            float hSpeed = (float) Math.sqrt(vx * vx + vz * vz);

            if (normalY < MAX_WALKABLE_SLOPE_COS) {
                // Steep slope / cliff: deflect velocity along slope tangent
                float dot = vx * normal[0] + vz * normal[2];
                if (dot < 0f) {
                    vx -= dot * normal[0];
                    vz -= dot * normal[2];
                }
                // Slide down along horizontal slope normal
                float slideAcc = (1.0f - normalY) * 12.0f;
                vx += normal[0] * slideAcc * dt;
                vz += normal[2] * slideAcc * dt;
            } else if (hSpeed > 0.1f) {
                // Walkable slope: mild uphill resistance
                float dot = (vx * normal[0] + vz * normal[2]) / hSpeed;
                if (dot < 0f) {
                    float uphillFactor = 1.0f - 0.25f * (-dot);
                    vx *= uphillFactor;
                    vz *= uphillFactor;
                }
            }
        }

        // Integrate position
        float nextX = x + vx * dt;
        float nextY = y + vy * dt;
        float nextZ = z + vz * dt;

        // Collisions with world obstacles & terrain
        resolveWorldCollisions(nextX, nextY, nextZ, obstacles, slidePressed);
        if (state == ParkourState.DEAD) return;

        // Update state — but never clobber landing states
        if (state != ParkourState.HARD_LAND && state != ParkourState.SAFETY_ROLL && state != ParkourState.LAND) {
            if (isGrounded) {
                float horizSpeed = (float) Math.sqrt(vx * vx + vz * vz);
                if (isCrouching) {
                    state = ParkourState.CROUCH;
                } else if (horizSpeed > 8.5f || (isSprinting && horizSpeed > 5.5f)) {
                    state = ParkourState.SPRINT;
                } else if (horizSpeed > 6.0f) {
                    state = ParkourState.RUN;
                } else if (horizSpeed > 3.8f) {
                    state = ParkourState.JOG;
                } else if (horizSpeed > 0.2f) {
                    state = ParkourState.WALK;
                } else {
                    state = ParkourState.STAND;
                }

                // Footsteps (silent when crouching)
                if (horizSpeed > 1.5f && !isCrouching) {
                    footstepTimer += dt * (horizSpeed / 5.0f);
                    if (footstepTimer >= 0.45f) {
                        footstepTimer = 0f;
                        playFootstepSfx = true;
                    }
                }
            } else {
                if (state != ParkourState.DOUBLE_JUMP && state != ParkourState.JUMP
                        && state != ParkourState.WALL_RUN_LEFT && state != ParkourState.WALL_RUN_RIGHT) {
                    state = ParkourState.FALLING;
                } else if (vy < 0f) {
                    state = ParkourState.FALLING;
                }
            }
        }
    }

    private void resolveWorldCollisions(float nx, float ny, float nz, List<Obstacle> obstacles, boolean slidePressed) {
        // ---- Open world: block horizontal movement into deeper water ----
        if (terrain != null) {
            float curDepth = Terrain.waterDepthAt(x, z);
            float nextDepth = Terrain.waterDepthAt(nx, nz);
            if (ny < waterLevel + 0.4f
                    && nextDepth > Terrain.WADE_MAX + 0.01f
                    && nextDepth > curDepth + 0.0001f) {
                nx = x;
                nz = z;
                vx = 0f;
                vz = 0f;
            }
        }

        boolean groundHit = false;
        boolean wasAirborne = !isGrounded;
        float floorY = -999f;
        Obstacle landedOn = null;

        if (isGrounded && vy <= 0.001f) {
            // ---- GROUNDED LOCOMOTION: STEP-UP AND GROUND SNAP ----
            float stepCandidateY = -999f;
            Obstacle stepCandidateObstacle = null;
            float snapCandidateY = -999f;
            Obstacle snapCandidateObstacle = null;

            if (obstacles != null) {
                for (Obstacle o : obstacles) {
                    if (!o.solid) continue;
                    if (o.containsXZ(nx, nz, radius)) {
                        float topY = o.getMaxY();
                        if (topY > y && topY <= y + MAX_STEP_UP + 0.02f) {
                            if (topY > stepCandidateY && canStepUp(topY, nx, nz, obstacles)) {
                                stepCandidateY = topY;
                                stepCandidateObstacle = o;
                            }
                        } else if (topY <= y + 0.05f && topY >= y - MAX_STEP_DOWN) {
                            if (topY > snapCandidateY) {
                                snapCandidateY = topY;
                                snapCandidateObstacle = o;
                            }
                        }
                    }
                }
            }

            if (terrain != null) {
                float th = Terrain.heightAt(nx, nz);
                if (th > y && th <= y + MAX_STEP_UP + 0.02f) {
                    if (th > stepCandidateY && canStepUp(th, nx, nz, obstacles)) {
                        stepCandidateY = th;
                        stepCandidateObstacle = null;
                    }
                } else if (th <= y + 0.05f && th >= y - MAX_STEP_DOWN) {
                    if (th > snapCandidateY) {
                        snapCandidateY = th;
                        snapCandidateObstacle = null;
                    }
                }
            }

            if (stepCandidateY > y) {
                floorY = stepCandidateY;
                landedOn = stepCandidateObstacle;
                groundHit = true;
                y = floorY;
                ny = floorY;
                vy = 0f;
                isGrounded = true;
            } else if (snapCandidateY > -990f) {
                floorY = snapCandidateY;
                landedOn = snapCandidateObstacle;
                groundHit = true;
                y = floorY;
                ny = floorY;
                vy = 0f;
                isGrounded = true;
            } else {
                // Walked off a ledge or cliff
                isGrounded = false;
                peakFallY = y;
                y = ny;
            }
        } else {
            // ---- AIRBORNE: LANDING DETECTION ----
            if (obstacles != null) {
                for (Obstacle o : obstacles) {
                    if (!o.solid) continue;
                    if (o.containsXZ(nx, nz, radius)) {
                        float topY = o.getMaxY();
                        if (y >= topY - 0.4f && ny <= topY + 0.1f && vy <= 0f) {
                            if (topY > floorY) {
                                floorY = topY;
                                groundHit = true;
                                landedOn = o;
                            }
                        }
                    }
                }
            }

            if (terrain != null) {
                float th = Terrain.heightAt(nx, nz);
                if (y >= th - 0.4f && ny <= th + 0.1f && vy <= 0f && th > floorY) {
                    floorY = th;
                    groundHit = true;
                    landedOn = null;
                }
            }
        }

        if (groundHit && landedOn != null) {
            // Jump Pad trigger
            if (landedOn.type == Obstacle.Type.JUMP_PAD) {
                vy = 20.0f;
                isGrounded = false;
                playDoubleJumpSfx = true;
                addCombo("FAN BOOST!", 15);
                x = nx;
                z = nz;
                return;
            }
        }

        // ---- Pass 2: horizontal wall push-out ----
        if (obstacles != null) {
            for (Obstacle o : obstacles) {
                if (!o.solid) continue;
                if (o == landedOn) continue;
                if (ny >= o.getMaxY() - 0.05f) continue;
                if (ny + currentHeight <= o.getMinY()) continue;
                if (isGrounded && y >= o.getMaxY() - 0.05f) continue;

                if (nx + radius > o.getMinX() && nx - radius < o.getMaxX() &&
                    nz + radius > o.getMinZ() && nz - radius < o.getMaxZ()) {
                    float dx1 = Math.abs((nx + radius) - o.getMinX());
                    float dx2 = Math.abs((nx - radius) - o.getMaxX());
                    float dz1 = Math.abs((nz + radius) - o.getMinZ());
                    float dz2 = Math.abs((nz - radius) - o.getMaxZ());
                    float minPen = Math.min(Math.min(dx1, dx2), Math.min(dz1, dz2));

                    if (minPen == dx1) { nx = o.getMinX() - radius; vx = 0; }
                    else if (minPen == dx2) { nx = o.getMaxX() + radius; vx = 0; }
                    else if (minPen == dz1) { nz = o.getMinZ() - radius; vz = 0; }
                    else { nz = o.getMaxZ() + radius; vz = 0; }
                }
            }
        }

        if (groundHit) {
            float dropDist = peakFallY - floorY;
            lastImpactVelocity = vy;
            y = floorY;
            vy = 0f;
            isGrounded = true;
            canDoubleJump = true;

            // Hazard pool contact
            if (landedOn != null && landedOn.type == Obstacle.Type.HAZARD_POOL) {
                if (hazardCooldown <= 0f) {
                    health -= 35f;
                    hazardCooldown = 1.0f;
                    playHardLandSfx = true;
                    if (health <= 0f) {
                        health = 0f;
                        state = ParkourState.DEAD;
                        peakFallY = y;
                        x = nx;
                        z = nz;
                        return;
                    }
                }
                peakFallY = y;
                x = nx;
                z = nz;
                return;
            }

            if (wasAirborne) {
                // Landing calculation: High fall check vs Soft land
                if (dropDist > 5.0f) {
                    if (slidePressed) {
                        startSafetyRoll();
                    } else {
                        float dmg = (dropDist - 6.0f) * 6.5f;
                        if (dmg > 0f) {
                            health -= dmg;
                            playHardLandSfx = true;
                            hardLandTimer = 0.45f;
                            state = ParkourState.HARD_LAND;
                            addCombo("ROUGH LANDING", 0);
                            if (health <= 0) {
                                health = 0;
                                state = ParkourState.DEAD;
                            }
                        }
                    }
                } else if (dropDist > 1.2f || lastImpactVelocity < -4.0f) {
                    state = ParkourState.LAND;
                    landTimer = landDuration;
                    playFootstepSfx = true;
                }
            }
            peakFallY = y;
        } else {
            y = ny;
            isGrounded = false;
        }

        x = nx;
        z = nz;

        // Ground-height adherence on open-world terrain after push-out
        if (isGrounded && terrain != null && landedOn == null) {
            float finalTh = Terrain.heightAt(x, z);
            if (Math.abs(y - finalTh) <= MAX_STEP_UP) {
                y = finalTh;
            }
        }
    }

    private Obstacle checkVaultBarrierAhead(List<Obstacle> obstacles, float fx, float fz) {
        float probeX = x + fx * 1.5f;
        float probeZ = z + fz * 1.5f;
        for (Obstacle o : obstacles) {
            if (o.vaultable && o.containsXZ(probeX, probeZ, 0.4f)) {
                if (Math.abs(o.getMaxY() - y) <= 1.4f) {
                    return o;
                }
            }
        }
        return null;
    }

    private void startVault(Obstacle obstacle) {
        state = ParkourState.VAULTING;
        vaultTimer = vaultDuration;
        vy = 4.0f;
        playVaultSfx = true;
        stamina -= 8f;
        addCombo("VAULT!", 15);
    }

    private void updateVaulting(float dt) {
        vaultTimer -= dt;
        float radYaw = (float) Math.toRadians(yaw);
        float fx = (float) Math.sin(radYaw);
        float fz = (float) -Math.cos(radYaw);
        x += fx * 9.5f * dt;
        z += fz * 9.5f * dt;
        y += vy * dt;
        vy += gravity * 0.5f * dt;

        if (vaultTimer <= 0f) {
            state = ParkourState.RUN;
            isGrounded = true;
        }
    }

    private void startSlide() {
        state = ParkourState.SLIDING;
        slideTimer = slideDuration;
        currentHeight = crouchingHeight;
        playSlideSfx = true;
        stamina -= 10f;
        addCombo("SLIDE!", 10);
    }

    private void updateSliding(float dt, boolean jumpPressed, List<Obstacle> obstacles) {
        slideTimer -= dt;
        float radYaw = (float) Math.toRadians(yaw);
        float fx = (float) Math.sin(radYaw);
        float fz = (float) -Math.cos(radYaw);
        float slideSpeed = 11.0f * (slideTimer / slideDuration);
        x += fx * slideSpeed * dt;
        z += fz * slideSpeed * dt;

        // Slide cancel with Jump
        if (jumpPressed) {
            if (canStandUp(obstacles)) {
                currentHeight = standingHeight;
                isCrouching = false;
                vy = jumpImpulse * 0.9f;
                isGrounded = false;
                state = ParkourState.JUMP;
                playJumpSfx = true;
                addCombo("SLIDE KICK!", 15);
                return;
            }
        }

        if (slideTimer <= 0f) {
            if (canStandUp(obstacles)) {
                currentHeight = standingHeight;
                isCrouching = false;
                state = isGrounded ? ParkourState.RUN : ParkourState.FALLING;
            } else {
                currentHeight = crouchingHeight;
                isCrouching = true;
                state = ParkourState.CROUCH;
            }
        }
    }

    private void updateSliding(float dt, boolean jumpPressed) {
        updateSliding(dt, jumpPressed, null);
    }

    private void startSafetyRoll() {
        state = ParkourState.SAFETY_ROLL;
        rollTimer = rollDuration;
        currentHeight = crouchingHeight;
        playRollSfx = true;
        addCombo("SAFETY ROLL! (0 DMG)", 25);
    }

    private void updateSafetyRoll(float dt, List<Obstacle> obstacles) {
        rollTimer -= dt;
        float radYaw = (float) Math.toRadians(yaw);
        float fx = (float) Math.sin(radYaw);
        float fz = (float) -Math.cos(radYaw);
        x += fx * 11.5f * dt;
        z += fz * 11.5f * dt;

        if (rollTimer <= 0f) {
            if (canStandUp(obstacles)) {
                currentHeight = standingHeight;
                isCrouching = false;
                state = ParkourState.SPRINT;
            } else {
                currentHeight = crouchingHeight;
                isCrouching = true;
                state = ParkourState.CROUCH;
            }
            isGrounded = true;
        }
    }

    private void updateSafetyRoll(float dt) {
        updateSafetyRoll(dt, null);
    }

    private void updateHardLand(float dt) {
        hardLandTimer -= dt;
        vx = 0; vz = 0;
        if (hardLandTimer <= 0f) {
            state = ParkourState.STAND;
        }
    }

    private void checkWallRun(List<Obstacle> obstacles, float fx, float fz, float rx, float rz) {
        if (stamina < 15f) return;

        // Check 1.2m to left and right
        float leftX = x - rx * 1.3f;
        float leftZ = z - rz * 1.3f;
        float rightX = x + rx * 1.3f;
        float rightZ = z + rz * 1.3f;

        for (Obstacle o : obstacles) {
            if (!o.wallRunnable) continue;

            // Never wall-run on the obstacle the player is inside/standing on:
            // its "side surface" under the roof is not a wall.
            if (o.containsXZ(x, z, 0f)) continue;
            // Also require the player to be clearly below the obstacle top so a
            // jump from the roof edge cannot stick to the roof's own band.
            if (y > o.getMaxY() - 0.5f) continue;

            if (o.containsXZ(leftX, leftZ, 0.2f) && y >= o.getMinY()) {
                startWallRun(ParkourState.WALL_RUN_LEFT);
                return;
            } else if (o.containsXZ(rightX, rightZ, 0.2f) && y >= o.getMinY()) {
                startWallRun(ParkourState.WALL_RUN_RIGHT);
                return;
            }
        }
    }

    private void startWallRun(ParkourState side) {
        state = side;
        wallRunTimer = maxWallRunDuration;
        vy = 1.5f; // Slight upward stick
        cameraRoll = (side == ParkourState.WALL_RUN_LEFT) ? -15f : 15f;
        playWallRunSfx = true;
        addCombo("WALL RUN!", 20);
    }

    private void updateWallRunning(float dt, boolean jumpPressed, List<Obstacle> obstacles) {
        wallRunTimer -= dt;
        stamina -= 14f * dt;

        // High forward speed along wall
        float radYaw = (float) Math.toRadians(yaw);
        float fx = (float) Math.sin(radYaw);
        float fz = (float) -Math.cos(radYaw);
        float rx = (float) Math.cos(radYaw);
        float rz = (float) Math.sin(radYaw);

        x += fx * wallRunSpeed * dt;
        z += fz * wallRunSpeed * dt;
        y += vy * dt;
        vy += gravity * 0.25f * dt; // 75% gravity reduction!

        // Wall Kick jump
        if (jumpPressed) {
            float kickDir = (state == ParkourState.WALL_RUN_LEFT) ? 1.0f : -1.0f;
            vx = (fx + rx * kickDir * 0.8f) * 11.0f;
            vz = (fz + rz * kickDir * 0.8f) * 11.0f;
            vy = jumpImpulse * 1.1f;
            state = ParkourState.JUMP;
            playJumpSfx = true;
            addCombo("WALL KICK!", 25);
            return;
        }

        if (wallRunTimer <= 0f || stamina <= 0f) {
            state = ParkourState.FALLING;
        }
    }

    private void checkLedgeGrab(List<Obstacle> obstacles, float fx, float fz) {
        if (vy > 2.0f) return; // Only grab when falling or near apex

        float reachX = x + fx * 0.9f;
        float reachZ = z + fz * 0.9f;

        for (Obstacle o : obstacles) {
            if (!o.climbable) continue;
            float topY = o.getMaxY();
            // If top ledge is near player chest/head
            if (y >= topY - 1.8f && y <= topY - 0.2f && o.containsXZ(reachX, reachZ, 0.4f)) {
                startMantle(topY, reachX + fx * 0.5f, reachZ + fz * 0.5f);
                return;
            }
        }
    }

    private void startMantle(float topY, float tx, float tz) {
        state = ParkourState.MANTLING;
        mantleTimer = mantleDuration;
        mantleStartY = y;
        mantleTargetY = topY;
        mantleTargetX = tx;
        mantleTargetZ = tz;
        vx = 0; vy = 0; vz = 0;
        playMantleSfx = true;
        stamina -= 8f;
        addCombo("LEDGE MANTLE!", 20);
    }

    private void updateMantling(float dt) {
        mantleTimer -= dt;
        float t = 1.0f - (mantleTimer / mantleDuration);
        t = Math.max(0f, Math.min(1.0f, t));
        // Smooth sine interpolation up onto roof
        y = mantleStartY + (mantleTargetY - mantleStartY) * (float) Math.sin(t * Math.PI * 0.5);
        x += (mantleTargetX - x) * 0.2f;
        z += (mantleTargetZ - z) * 0.2f;

        if (mantleTimer <= 0f) {
            y = mantleTargetY;
            isGrounded = true;
            state = ParkourState.STAND;
        }
    }

    public void addCombo(String actionName, int points) {
        comboCount++;
        comboTimer = 3.5f;
        lastParkourAction = actionName + " (x" + comboCount + ")";
        actionNotifyTimer = 1.8f;
        score += points * comboCount;
        adrenaline = Math.min(maxAdrenaline, adrenaline + points * 0.8f);
    }
}
