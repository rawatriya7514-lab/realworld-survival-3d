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
        RUN,
        SPRINT,
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
    public float walkSpeed = 5.0f;
    public float runSpeed = 7.5f;
    public float sprintSpeed = 12.0f;
    public float wallRunSpeed = 12.5f;
    public float jumpImpulse = 9.0f;
    public float gravity = -22.0f;

    // State Tracking
    public ParkourState state = ParkourState.STAND;
    public boolean isGrounded = false;
    public boolean isSprinting = false;
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

    public void update(float dt, float moveInputX, float moveInputY, boolean jumpPressed,
                       boolean slidePressed, boolean sprintToggled, List<Obstacle> obstacles) {
        if (state == ParkourState.DEAD) return;

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
        switch (state) {
            case MANTLING:
                updateMantling(dt);
                return;
            case VAULTING:
                updateVaulting(dt);
                return;
            case SLIDING:
                updateSliding(dt, jumpPressed);
                return;
            case SAFETY_ROLL:
                updateSafetyRoll(dt);
                return;
            case HARD_LAND:
                updateHardLand(dt);
                return;
            case WALL_RUN_LEFT:
            case WALL_RUN_RIGHT:
                updateWallRunning(dt, jumpPressed, obstacles);
                return;
            default:
                break;
        }

        // Normal Ground & Air locomotion
        handleMovement(dt, moveInputX, moveInputY, jumpPressed, slidePressed, sprintToggled, obstacles);
    }

    private void handleMovement(float dt, float inputX, float inputY, boolean jumpPressed,
                                boolean slidePressed, boolean sprintToggled, List<Obstacle> obstacles) {
        if (sprintToggled && stamina > 15f) {
            isSprinting = !isSprinting;
        }

        float speed = walkSpeed;
        if (isSprinting && stamina > 0f) {
            speed = sprintSpeed;
            if (adrenalineRush) speed *= 1.25f;
        } else if (inputX != 0 || inputY != 0) {
            speed = runSpeed;
        }

        // Direction from yaw
        float radYaw = (float) Math.toRadians(yaw);
        float forwardX = (float) Math.sin(radYaw);
        float forwardZ = (float) -Math.cos(radYaw);
        float rightX = (float) Math.cos(radYaw);
        float rightZ = (float) Math.sin(radYaw);

        float targetVx = (forwardX * -inputY + rightX * inputX) * speed;
        float targetVz = (forwardZ * -inputY + rightZ * inputX) * speed;

        float accel = isGrounded ? 15f : 6f;
        vx += (targetVx - vx) * Math.min(1.0f, dt * accel);
        vz += (targetVz - vz) * Math.min(1.0f, dt * accel);

        // Slide trigger
        if (isGrounded && isSprinting && slidePressed && (inputX != 0 || inputY != 0)) {
            startSlide();
            return;
        }

        // Jump trigger
        if (jumpPressed) {
            if (isGrounded) {
                // Check if approaching a vault barrier
                Obstacle vaultTarget = checkVaultBarrierAhead(obstacles, forwardX, forwardZ);
                if (vaultTarget != null && isSprinting) {
                    startVault(vaultTarget);
                    return;
                }

                vy = jumpImpulse;
                isGrounded = false;
                canDoubleJump = true;
                state = ParkourState.JUMP;
                peakFallY = y;
                playJumpSfx = true;
                addCombo("HIGH LEAP", 5);
            } else if (canDoubleJump) {
                // Air boost / double jump
                vy = jumpImpulse * 0.9f;
                canDoubleJump = false;
                state = ParkourState.DOUBLE_JUMP;
                playDoubleJumpSfx = true;
                addCombo("AIR BOOST", 10);
            }
        }

        // Apply gravity if airborne
        if (!isGrounded) {
            vy += gravity * dt;
            if (vy < -35f) vy = -35f; // Terminal velocity
            if (y > peakFallY) peakFallY = y;

            // Check for Wall Run if falling or mid-air
            checkWallRun(obstacles, forwardX, forwardZ, rightX, rightZ);

            // Check for Ledge Grab / Mantle
            checkLedgeGrab(obstacles, forwardX, forwardZ);
        }

        // Integrate position
        float nextX = x + vx * dt;
        float nextY = y + vy * dt;
        float nextZ = z + vz * dt;

        // Collisions with world obstacles
        resolveWorldCollisions(nextX, nextY, nextZ, obstacles, slidePressed);
        if (state == ParkourState.DEAD) return; // died this frame (hazard / fall damage)

        // Update state — but never clobber the landing states that
        // resolveWorldCollisions may have just entered (hard land / safety roll).
        if (state != ParkourState.HARD_LAND && state != ParkourState.SAFETY_ROLL) {
            if (isGrounded) {
                float horizSpeed = (float) Math.sqrt(vx * vx + vz * vz);
                if (horizSpeed > 8.0f) {
                    state = ParkourState.SPRINT;
                } else if (horizSpeed > 1.0f) {
                    state = ParkourState.RUN;
                } else {
                    state = ParkourState.STAND;
                }

                // Footsteps
                if (horizSpeed > 1.5f) {
                    footstepTimer += dt * (horizSpeed / 5.0f);
                    if (footstepTimer >= 0.45f) {
                        footstepTimer = 0f;
                        playFootstepSfx = true;
                    }
                }
            } else {
                if (state != ParkourState.DOUBLE_JUMP && state != ParkourState.JUMP) {
                    state = ParkourState.FALLING;
                }
            }
        }
    }

    private void resolveWorldCollisions(float nx, float ny, float nz, List<Obstacle> obstacles, boolean slidePressed) {
        // ---- Pass 1: vertical landing on the highest surface we cross ----
        boolean groundHit = false;
        float floorY = -999f;
        Obstacle landedOn = null;

        for (Obstacle o : obstacles) {
            if (o.containsXZ(nx, nz, radius * 0.7f)) {
                float topY = o.getMaxY();
                // If player was above or crossing the top surface while falling
                if (y >= topY - 0.4f && ny <= topY + 0.1f && vy <= 0f) {
                    if (topY > floorY) {
                        floorY = topY;
                        groundHit = true;
                        landedOn = o;
                    }
                }
            }
        }

        if (groundHit && landedOn != null) {
            // Jump Pad trigger
            if (landedOn.type == Obstacle.Type.JUMP_PAD) {
                vy = 20.0f; // High-pressure boost!
                isGrounded = false;
                playDoubleJumpSfx = true;
                addCombo("FAN BOOST!", 15);
                x = nx;
                z = nz;
                return;
            }
        }

        // ---- Pass 2: horizontal wall push-out ----
        // Only when the body actually intersects the wall volume: feet below
        // the obstacle top (with tolerance) and body bottom above its base.
        // The obstacle we are landing on never pushes (that is the floor).
        for (Obstacle o : obstacles) {
            if (o == landedOn) continue;
            if (ny >= o.getMaxY() - 0.05f) continue;      // feet at/above the top: that is a floor, not a wall
            if (ny + currentHeight <= o.getMinY()) continue; // body entirely below the obstacle
            if (nx + radius > o.getMinX() && nx - radius < o.getMaxX() &&
                nz + radius > o.getMinZ() && nz - radius < o.getMaxZ()) {
                // Push out horizontally along smallest penetration axis
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

        if (groundHit) {
            float dropDist = peakFallY - floorY;
            lastImpactVelocity = vy;
            y = floorY;
            vy = 0f;
            isGrounded = true;
            canDoubleJump = true;

            // Hazard pool contact (ticking damage with a short cooldown)
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
                return; // hazard handles its own landing (no fall damage math)
            }

            // Landing calculation: High fall check
            if (dropDist > 5.0f) {
                if (slidePressed) {
                    // Executed safety roll! Negates fall damage!
                    startSafetyRoll();
                } else {
                    // Hard impact landing! Fall damage!
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
            }
            peakFallY = y;
        } else {
            y = ny;
            isGrounded = false;
        }

        x = nx;
        z = nz;
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

    private void updateSliding(float dt, boolean jumpPressed) {
        slideTimer -= dt;
        float radYaw = (float) Math.toRadians(yaw);
        float fx = (float) Math.sin(radYaw);
        float fz = (float) -Math.cos(radYaw);
        float slideSpeed = 11.0f * (slideTimer / slideDuration);
        x += fx * slideSpeed * dt;
        z += fz * slideSpeed * dt;

        // Slide cancel with Jump
        if (jumpPressed) {
            currentHeight = standingHeight;
            vy = jumpImpulse * 0.9f;
            isGrounded = false;
            state = ParkourState.JUMP;
            playJumpSfx = true;
            addCombo("SLIDE KICK!", 15);
            return;
        }

        if (slideTimer <= 0f) {
            currentHeight = standingHeight;
            state = isGrounded ? ParkourState.RUN : ParkourState.FALLING;
        }
    }

    private void startSafetyRoll() {
        state = ParkourState.SAFETY_ROLL;
        rollTimer = rollDuration;
        currentHeight = crouchingHeight;
        playRollSfx = true;
        addCombo("SAFETY ROLL! (0 DMG)", 25);
    }

    private void updateSafetyRoll(float dt) {
        rollTimer -= dt;
        float radYaw = (float) Math.toRadians(yaw);
        float fx = (float) Math.sin(radYaw);
        float fz = (float) -Math.cos(radYaw);
        x += fx * 11.5f * dt;
        z += fz * 11.5f * dt;

        if (rollTimer <= 0f) {
            currentHeight = standingHeight;
            state = ParkourState.SPRINT;
            isGrounded = true;
        }
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

    private void addCombo(String actionName, int points) {
        comboCount++;
        comboTimer = 3.5f;
        lastParkourAction = actionName + " (x" + comboCount + ")";
        actionNotifyTimer = 1.8f;
        score += points * comboCount;
        adrenaline = Math.min(maxAdrenaline, adrenaline + points * 0.8f);
    }
}
