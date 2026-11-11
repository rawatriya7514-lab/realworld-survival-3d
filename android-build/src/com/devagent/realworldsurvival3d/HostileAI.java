package com.devagent.realworldsurvival3d;

import java.util.Random;

/**
 * HostileAI - Autonomous infected / stalker entity in the open world (Phase 8).
 *
 * Implements:
 *  - State machine: PATROL, ALERT, CHASE, ATTACK, STUNNED, DEFEATED.
 *  - Safe zone aversion: cannot enter Central Plaza (r=30m + buffer 34m), deflecting outward.
 *  - Staggered AI updates: <40m every frame, 40-100m every 5th frame (5x dt), >100m frozen.
 *  - Combat interactions: slide tackle stun (35 dmg), downward stomp defeat (100 dmg),
 *    and close-range strike (15 dmg, 1.2s cooldown).
 *  - Articulated character animation matching movement speed and state.
 */
public class HostileAI {

    public enum State {
        PATROL,
        ALERT,
        CHASE,
        ATTACK,
        STUNNED,
        DEFEATED
    }

    public static final float SAFE_ZONE_X = 150f;
    public static final float SAFE_ZONE_Z = 20f;
    public static final float SAFE_ZONE_AVOID_RADIUS = 34.0f;

    public final String type;
    public float x, y, z;
    public float vx = 0f, vy = 0f, vz = 0f;
    public float homeX, homeZ;
    public float patrolRadius;
    public float yaw = 0f;
    public State state = State.PATROL;

    public float health = 50f;
    public float maxHealth = 50f;
    public float patrolSpeed = 3.2f;
    public float chaseSpeed = 7.5f;

    public float alertTimer = 0f;
    public float stunTimer = 0f;
    public float attackCooldown = 0f;
    public float targetX, targetZ;
    public float patrolWaitTimer = 1.0f;

    // Articulated animator
    public final CharacterAnimator animator = new CharacterAnimator();
    private final Random rng = new Random();

    // Staggered update bookkeeping
    public int updateCount = 0;
    public boolean wasThrottled = false;
    public boolean wasFrozen = false;

    public HostileAI(String type, float x, float z, float patrolRadius) {
        this.type = type;
        this.x = x;
        this.z = z;
        this.homeX = x;
        this.homeZ = z;
        this.targetX = x;
        this.targetZ = z;
        this.patrolRadius = Math.max(5.0f, patrolRadius);
        this.y = Terrain.heightAt(x, z);
    }

    public void update(float dt, Player player, OpenWorld openWorld, int frameNumber) {
        if (state == State.DEFEATED) {
            animator.update(dt, Player.ParkourState.DEAD, 0f, true);
            return;
        }

        float pdx = player.x - x;
        float pdz = player.z - z;
        float distSq = pdx * pdx + pdz * pdz;

        // Frozen zone (> 100m)
        if (distSq > 100f * 100f) {
            wasFrozen = true;
            wasThrottled = false;
            return;
        }
        wasFrozen = false;

        // Throttled zone (40m - 100m): run only on frameNumber % 5 == 0
        if (distSq > 40f * 40f) {
            wasThrottled = true;
            if (frameNumber % 5 != 0) return;
            dt *= 5.0f;
        } else {
            wasThrottled = false;
        }

        updateCount++;
        float dist = (float) Math.sqrt(distSq);

        if (attackCooldown > 0f) attackCooldown -= dt;
        if (alertTimer > 0f) alertTimer -= dt;

        // Combat resolution against player (stomp or slide tackle)
        if (resolvePlayerInteractions(player, dist, pdx, pdz)) {
            return;
        }

        if (stunTimer > 0f) {
            stunTimer -= dt;
            if (stunTimer <= 0f) {
                state = State.CHASE;
            } else {
                state = State.STUNNED;
                animator.update(dt, Player.ParkourState.STAND, 0f, true);
                return;
            }
        }

        // Safe zone repulsion
        applySafeZoneRepulsion();

        boolean playerInSafeZone = (openWorld != null && openWorld.isSafeZone(player.x, player.z));
        boolean inPursuitRange = (state == State.CHASE) ? (dist <= 35.0f) : (dist < 24.0f);

        // State Machine
        if (!playerInSafeZone && inPursuitRange) {
            if (state == State.PATROL) {
                state = State.ALERT;
                alertTimer = 0.4f;
                yaw = (float) Math.toDegrees(Math.atan2(pdx, -pdz));
            } else if (state == State.ALERT) {
                if (alertTimer <= 0f) {
                    state = State.CHASE;
                }
            } else if (state == State.CHASE) {
                float inv = dist > 0.001f ? 1f / dist : 0f;
                vx = pdx * inv * chaseSpeed;
                vz = pdz * inv * chaseSpeed;
                x += vx * dt;
                z += vz * dt;
                yaw = (float) Math.toDegrees(Math.atan2(pdx, -pdz));

                // Strike player
                if (dist < 1.6f && Math.abs(player.y - y) < 1.8f && attackCooldown <= 0f) {
                    player.health -= 15.0f;
                    attackCooldown = 1.2f;
                    state = State.ATTACK;
                    if (player.health <= 0f) {
                        player.health = 0f;
                        player.state = Player.ParkourState.DEAD;
                    }
                }
            }
        } else {
            // Player lost or retreated into safe zone
            if (state == State.CHASE || state == State.ALERT) {
                state = State.PATROL;
                pickNewPatrolTarget();
            }
        }

        if (state == State.ATTACK) {
            if (attackCooldown < 0.8f) {
                state = State.CHASE;
            }
        }

        if (state == State.PATROL) {
            updatePatrol(dt);
        }

        // Ground clamping
        y = Terrain.heightAt(x, z);

        // Update animation
        updateAnimation(dt);
    }

    private void applySafeZoneRepulsion() {
        float dx = x - SAFE_ZONE_X;
        float dz = z - SAFE_ZONE_Z;
        float sq = dx * dx + dz * dz;
        if (sq < SAFE_ZONE_AVOID_RADIUS * SAFE_ZONE_AVOID_RADIUS) {
            float d = (float) Math.sqrt(sq);
            if (d > 0.001f) {
                float nx = dx / d;
                float nz = dz / d;
                x = SAFE_ZONE_X + nx * SAFE_ZONE_AVOID_RADIUS;
                z = SAFE_ZONE_Z + nz * SAFE_ZONE_AVOID_RADIUS;
                vx = nx * patrolSpeed;
                vz = nz * patrolSpeed;
            }
            if (state == State.CHASE || state == State.ALERT) {
                state = State.PATROL;
                pickNewPatrolTarget();
            }
        }
    }

    private boolean resolvePlayerInteractions(Player player, float dist, float pdx, float pdz) {
        // Downward stomp defeat: Player dropping fast from above
        if (player.vy < -4.0f && player.y > y + 0.8f && dist < 1.5f) {
            takeDamage(100f);
            player.vy = 7.5f; // Bounce
            player.score += 150;
            return true;
        }

        // Slide tackle: Player in sliding parkour state
        if (player.state == Player.ParkourState.SLIDING && dist < 1.6f && attackCooldown <= 0.2f) {
            takeDamage(35f);
            if (health > 0) {
                stunTimer = 2.0f;
                state = State.STUNNED;
                attackCooldown = 2.2f;
            } else {
                player.score += 100;
            }
            return true;
        }

        return false;
    }

    private void updatePatrol(float dt) {
        if (patrolWaitTimer > 0f) {
            patrolWaitTimer -= dt;
            vx = 0f;
            vz = 0f;
            return;
        }

        float tdx = targetX - x;
        float tdz = targetZ - z;
        float dist = (float) Math.sqrt(tdx * tdx + tdz * tdz);

        if (dist <= 0.6f) {
            patrolWaitTimer = 1.5f + rng.nextFloat() * 2.5f;
            pickNewPatrolTarget();
            vx = 0f;
            vz = 0f;
        } else {
            float inv = 1f / dist;
            vx = tdx * inv * patrolSpeed;
            vz = tdz * inv * patrolSpeed;
            float step = Math.min(dist, patrolSpeed * dt);
            x += tdx * inv * step;
            z += tdz * inv * step;
            yaw = (float) Math.toDegrees(Math.atan2(tdx, -tdz));
        }
    }

    private void pickNewPatrolTarget() {
        double angle = rng.nextDouble() * 2.0 * Math.PI;
        double r = rng.nextDouble() * patrolRadius;
        targetX = homeX + (float) (Math.cos(angle) * r);
        targetZ = homeZ + (float) (Math.sin(angle) * r);

        // Keep patrol target out of safe zone
        float dx = targetX - SAFE_ZONE_X;
        float dz = targetZ - SAFE_ZONE_Z;
        if (dx * dx + dz * dz < SAFE_ZONE_AVOID_RADIUS * SAFE_ZONE_AVOID_RADIUS) {
            targetX = homeX;
            targetZ = homeZ;
        }
    }

    public void takeDamage(float amount) {
        health = Math.max(0f, health - amount);
        if (health <= 0f) {
            state = State.DEFEATED;
            vx = 0f;
            vz = 0f;
        }
    }

    private void updateAnimation(float dt) {
        if (state == State.DEFEATED) {
            animator.update(dt, Player.ParkourState.DEAD, 0f, true);
        } else if (state == State.STUNNED) {
            animator.update(dt, Player.ParkourState.STAND, 0f, true);
        } else if (state == State.CHASE) {
            animator.update(dt, Player.ParkourState.RUN, chaseSpeed, true);
        } else if (state == State.ATTACK) {
            animator.update(dt, Player.ParkourState.VAULTING, chaseSpeed, true);
        } else if (state == State.ALERT) {
            animator.update(dt, Player.ParkourState.STAND, 0f, true);
        } else {
            float hSpeed = (float) Math.sqrt(vx * vx + vz * vz);
            if (hSpeed > 0.4f) {
                animator.update(dt, Player.ParkourState.WALK, hSpeed, true);
            } else {
                animator.update(dt, Player.ParkourState.STAND, 0f, true);
            }
        }
    }
}
