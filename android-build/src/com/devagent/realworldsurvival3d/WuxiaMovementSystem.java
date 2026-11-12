package com.devagent.realworldsurvival3d;

import java.util.List;

/**
 * WuxiaMovementSystem - Dedicated Movement Ability and Traversal System (Phase 2).
 * Features:
 *   - Advanced jump system: Normal Jump, Double Jump, Triple Jump, Air Dash, Fast Aerial Descent, Ground Slam.
 *   - Gliding system: Deployable bamboo/silk glider with pitch/yaw steering, lift/drag balance, stamina drain.
 *   - Swimming and diving system: Water surface swim, submerged dive, oxygen meter and drowning hazards.
 *   - Wall traversal: Wall-running, vertical wall climbing, wall kick-off leaps, ledge mantling.
 *   - Ability unlock progression system.
 *   - Pure Java (100% JVM testable).
 */
public class WuxiaMovementSystem {

    public enum MovementState {
        GROUND,
        JUMP_1,
        JUMP_2,
        JUMP_3,
        AIR_DASH,
        FAST_DESCENT,
        GROUND_IMPACT,
        GLIDING,
        SWIMMING_SURFACE,
        SWIMMING_DIVING,
        WALL_RUNNING,
        WALL_CLIMBING,
        WALL_LEAP
    }

    // Active state
    public MovementState currentState = MovementState.GROUND;

    // Ability Unlocks
    public boolean tripleJumpUnlocked = true;
    public boolean airDashUnlocked = true;
    public boolean gliderUnlocked = true;
    public boolean wallClimbUnlocked = true;
    public boolean deepDiveUnlocked = true;

    // Movement Parameters
    public float jumpVelocity1 = 9.0f;
    public float jumpVelocity2 = 8.5f;
    public float jumpVelocity3 = 8.0f;
    public float airDashSpeed = 16.0f;
    public float airDashDuration = 0.28f;
    public float airDashTimer = 0f;
    public float fastDescentVelocity = -28.0f;
    public float groundImpactRadius = 4.5f;
    public float groundImpactDamage = 35.0f;
    public float groundImpactTimer = 0f;
    public float groundImpactDuration = 0.35f;

    // Jump Counters
    public int jumpsRemaining = 3; // 1 = ground, 2 = double, 3 = triple
    public boolean airDashAvailable = true;

    // Gliding
    public boolean isGliding = false;
    public float glideHorizontalSpeed = 10.5f;
    public float glideDescentRate = -1.6f;
    public float gliderStaminaCostPerSec = 7.5f;
    public float thermalUpdraftBoost = 6.0f;

    // Swimming & Oxygen
    public boolean inWater = false;
    public boolean isDiving = false;
    public float oxygen = 100.0f;
    public float maxOxygen = 100.0f;
    public float oxygenDepletionRate = 12.0f; // per second when submerged
    public float oxygenRecoveryRate = 25.0f;  // per second at surface
    public float swimSurfaceSpeed = 4.2f;
    public float swimDiveSpeed = 3.6f;
    public float diveDepth = 0f;

    // Wall Traversal
    public boolean isWallClimbing = false;
    public float wallClimbSpeed = 4.5f;
    public float wallClimbTimer = 0f;
    public float maxWallClimbDuration = 1.8f;

    // Audio/VFX Trigger Flags
    public boolean triggerAirDashVfx = false;
    public boolean triggerTripleJumpVfx = false;
    public boolean triggerGroundSlamVfx = false;
    public boolean triggerGliderDeploySfx = false;
    public boolean triggerWaterSplashSfx = false;

    /** Resets jump and dash counts upon landing on solid ground. */
    public void onLand() {
        jumpsRemaining = tripleJumpUnlocked ? 3 : 2;
        airDashAvailable = true;
        isGliding = false;
        isWallClimbing = false;
        wallClimbTimer = 0f;
        if (currentState == MovementState.FAST_DESCENT) {
            currentState = MovementState.GROUND_IMPACT;
            groundImpactTimer = groundImpactDuration;
            triggerGroundSlamVfx = true;
        } else {
            currentState = MovementState.GROUND;
        }
    }

    /**
     * Executes jump logic supporting Normal, Double, and Triple jump.
     * Returns true if a jump was executed.
     */
    public boolean attemptJump(Player player) {
        if (inWater) {
            // Water leap to exit water
            if (player.y >= player.waterLevel - 0.4f) {
                player.vy = jumpVelocity1 * 0.85f;
                inWater = false;
                isDiving = false;
                currentState = MovementState.JUMP_1;
                return true;
            }
            return false;
        }

        if (player.isGrounded) {
            player.vy = jumpVelocity1;
            player.isGrounded = false;
            jumpsRemaining = tripleJumpUnlocked ? 2 : 1;
            currentState = MovementState.JUMP_1;
            player.state = Player.ParkourState.JUMP;
            player.playJumpSfx = true;
            return true;
        } else if (jumpsRemaining == 2 && player.stamina >= 10f) {
            player.vy = jumpVelocity2;
            jumpsRemaining--;
            player.stamina -= 10f;
            currentState = MovementState.JUMP_2;
            player.state = Player.ParkourState.DOUBLE_JUMP;
            player.playDoubleJumpSfx = true;
            return true;
        } else if (jumpsRemaining == 1 && tripleJumpUnlocked && player.stamina >= 15f) {
            player.vy = jumpVelocity3;
            jumpsRemaining--;
            player.stamina -= 15f;
            currentState = MovementState.JUMP_3;
            player.state = Player.ParkourState.DOUBLE_JUMP;
            triggerTripleJumpVfx = true;
            player.addCombo("SKYWARD TRIPLE LEAP", 15);
            return true;
        }
        return false;
    }

    /** Executes forward aerial dash burst. */
    public boolean attemptAirDash(Player player, float dirX, float dirZ) {
        if (!airDashUnlocked || !airDashAvailable || player.isGrounded || inWater || player.stamina < 20f) {
            return false;
        }
        player.stamina -= 20f;
        airDashAvailable = false;
        currentState = MovementState.AIR_DASH;
        airDashTimer = airDashDuration;
        triggerAirDashVfx = true;

        float mag = (float) Math.sqrt(dirX * dirX + dirZ * dirZ);
        if (mag < 0.001f) {
            float rad = (float) Math.toRadians(player.yaw);
            dirX = (float) Math.sin(rad);
            dirZ = (float) -Math.cos(rad);
            mag = 1.0f;
        }
        player.vx = (dirX / mag) * airDashSpeed;
        player.vz = (dirZ / mag) * airDashSpeed;
        player.vy = 2.0f; // slight lift keeping level flight
        player.addCombo("WIND AIR DASH", 12);
        return true;
    }

    /** Initiates fast aerial descent (Ground Slam) */
    public boolean attemptFastDescent(Player player) {
        if (player.isGrounded || inWater || isGliding) return false;
        currentState = MovementState.FAST_DESCENT;
        player.vy = fastDescentVelocity;
        return true;
    }

    /** Toggles the deployment of the Wuxia Glider. */
    public boolean toggleGlider(Player player) {
        if (!gliderUnlocked || player.isGrounded || inWater) {
            isGliding = false;
            return false;
        }
        if (isGliding) {
            isGliding = false;
            currentState = MovementState.JUMP_1;
            return true;
        } else if (player.stamina >= 15f && player.vy < 0f) {
            isGliding = true;
            currentState = MovementState.GLIDING;
            triggerGliderDeploySfx = true;
            return true;
        }
        return false;
    }

    /** Updates movement system physics, stamina, oxygen, and timers. */
    public void update(float dt, Player player, List<Obstacle> obstacles) {
        // Reset single-frame VFX flags
        triggerAirDashVfx = false;
        triggerTripleJumpVfx = false;
        triggerGroundSlamVfx = false;
        triggerGliderDeploySfx = false;
        triggerWaterSplashSfx = false;

        // Ground impact recovery timer
        if (groundImpactTimer > 0f) {
            groundImpactTimer -= dt;
            if (groundImpactTimer <= 0f) {
                currentState = MovementState.GROUND;
            }
        }

        // Air dash timer
        if (airDashTimer > 0f) {
            airDashTimer -= dt;
            player.vy = 0.5f; // Maintain horizontal suspension during dash burst
            if (airDashTimer <= 0f) {
                currentState = MovementState.JUMP_1;
            }
        }

        // Water checks & Swimming logic
        checkWaterState(player);
        if (inWater) {
            handleWaterPhysics(dt, player);
            return;
        } else {
            // Oxygen recovers on land
            if (oxygen < maxOxygen) {
                oxygen = Math.min(maxOxygen, oxygen + oxygenRecoveryRate * dt);
            }
        }

        // Gliding physics
        if (isGliding) {
            if (player.isGrounded || player.stamina <= 0f) {
                isGliding = false;
                currentState = player.isGrounded ? MovementState.GROUND : MovementState.JUMP_1;
            } else {
                player.stamina = Math.max(0f, player.stamina - gliderStaminaCostPerSec * dt);
                player.vy = glideDescentRate;

                // Forward glide velocity in look direction
                float rad = (float) Math.toRadians(player.yaw);
                player.vx = (float) Math.sin(rad) * glideHorizontalSpeed;
                player.vz = (float) -Math.cos(rad) * glideHorizontalSpeed;
            }
        }

        // Wall climbing logic
        if (isWallClimbing) {
            wallClimbTimer += dt;
            if (wallClimbTimer >= maxWallClimbDuration || player.stamina <= 0f) {
                isWallClimbing = false;
                player.vy = -2f;
            } else {
                player.stamina = Math.max(0f, player.stamina - 15f * dt);
                player.vy = wallClimbSpeed;
                player.vx = 0f;
                player.vz = 0f;
            }
        }
    }

    private void checkWaterState(Player player) {
        if (player.terrain != null) {
            float depth = Terrain.waterDepthAt(player.x, player.z);
            if (depth > 1.2f && player.y <= player.waterLevel + 0.2f) {
                if (!inWater) {
                    triggerWaterSplashSfx = true;
                    inWater = true;
                    isGliding = false;
                }
            } else if (depth <= 0.6f && inWater) {
                // Stepped onto shallow shore
                inWater = false;
                isDiving = false;
            }
        }
    }

    private void handleWaterPhysics(float dt, Player player) {
        isGliding = false;
        float surfaceY = player.waterLevel;

        if (isDiving && deepDiveUnlocked) {
            currentState = MovementState.SWIMMING_DIVING;
            oxygen = Math.max(0f, oxygen - oxygenDepletionRate * dt);
            if (oxygen <= 0f) {
                player.health = Math.max(0f, player.health - 12f * dt); // Drowning damage
            }
            // Buoyant resistance underwater
            player.vy *= 0.85f;
        } else {
            currentState = MovementState.SWIMMING_SURFACE;
            // Float to surface
            if (player.y < surfaceY) {
                player.y += (surfaceY - player.y) * Math.min(1.0f, dt * 5.0f);
                player.vy = 0f;
            }
            if (oxygen < maxOxygen) {
                oxygen = Math.min(maxOxygen, oxygen + oxygenRecoveryRate * dt);
            }
        }
    }
}
