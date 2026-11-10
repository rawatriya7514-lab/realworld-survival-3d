package com.devagent.realworldsurvival3d;

import java.util.List;

/**
 * HunterEnemy - Fast Rooftop Chaser / Infected Stalker AI.
 * Patrols rooftops, detects player via line of sight, leaps across chasms,
 * and chases the runner. Can be defeated by drop-kick stomps from above
 * or slide-tackles.
 */
public class HunterEnemy {
    public enum State {
        PATROL,
        ALERT,
        CHASING,
        LEAPING,
        ATTACKING,
        STUNNED,
        DEFEATED
    }

    public float x, y, z;
    public float vx = 0, vy = 0, vz = 0;
    public float patrolStartX, patrolStartZ;
    public float patrolTargetX, patrolTargetZ;
    public float patrolTimer = 0f;
    public float health = 60f;
    public float maxHealth = 60f;
    public float speed = 7.0f;
    public float chaseSpeed = 10.5f;
    public State state = State.PATROL;
    public float attackCooldown = 0f;
    public float stunTimer = 0f;
    public boolean alertSoundPlayed = false;

    public HunterEnemy(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.patrolStartX = x;
        this.patrolStartZ = z;
        this.patrolTargetX = x + (float) (Math.random() * 12 - 6);
        this.patrolTargetZ = z + (float) (Math.random() * 12 - 6);
    }

    public void update(float dt, Player player, List<Obstacle> obstacles) {
        if (state == State.DEFEATED) return;

        if (attackCooldown > 0f) attackCooldown -= dt;
        if (stunTimer > 0f) {
            stunTimer -= dt;
            if (stunTimer <= 0f) state = State.CHASING;
            return;
        }

        float dx = player.x - x;
        float dy = player.y - y;
        float dz = player.z - z;
        float distToPlayer = (float) Math.sqrt(dx * dx + dz * dz);

        // Detection & Chase trigger
        if (distToPlayer < 24.0f && Math.abs(dy) < 8.0f) {
            if (state != State.CHASING && state != State.LEAPING) {
                state = State.CHASING;
                alertSoundPlayed = true;
            }
        } else if (distToPlayer > 35.0f && state == State.CHASING) {
            state = State.PATROL;
            alertSoundPlayed = false;
        }

        if (state == State.CHASING) {
            // Chase runner
            float dirX = dx / Math.max(0.001f, distToPlayer);
            float dirZ = dz / Math.max(0.001f, distToPlayer);
            vx = dirX * chaseSpeed;
            vz = dirZ * chaseSpeed;

            // Attack player
            if (distToPlayer < 2.0f && Math.abs(dy) < 1.5f && attackCooldown <= 0f) {
                // Check if player is drop-kicking or slide-tackling enemy
                if (player.vy < -5.0f && player.y > y + 0.8f) {
                    // Stomp elimination from above!
                    health = 0;
                    state = State.DEFEATED;
                    player.vy = 8.0f; // Bounce up!
                    player.score += 150;
                    return;
                } else if (player.state == Player.ParkourState.SLIDING) {
                    // Slide tackle!
                    health -= 35;
                    stunTimer = 1.5f;
                    state = State.STUNNED;
                    if (health <= 0) {
                        state = State.DEFEATED;
                        player.score += 100;
                    }
                    return;
                }

                // Chaser attacks player
                player.health -= 18.0f;
                attackCooldown = 1.2f;
                state = State.ATTACKING;
                if (player.health <= 0) {
                    player.health = 0;
                    player.state = Player.ParkourState.DEAD;
                }
            } else if (attackCooldown <= 0.6f && state == State.ATTACKING) {
                state = State.CHASING;
            }
        } else if (state == State.PATROL) {
            patrolTimer += dt;
            if (patrolTimer > 4.0f) {
                patrolTimer = 0f;
                patrolTargetX = patrolStartX + (float) (Math.random() * 14 - 7);
                patrolTargetZ = patrolStartZ + (float) (Math.random() * 14 - 7);
            }
            float pdx = patrolTargetX - x;
            float pdz = patrolTargetZ - z;
            float pDist = (float) Math.sqrt(pdx * pdx + pdz * pdz);
            if (pDist > 0.5f) {
                vx = (pdx / pDist) * speed;
                vz = (pdz / pDist) * speed;
            } else {
                vx = 0; vz = 0;
            }
        }

        // Apply movement & simple floor clamp
        x += vx * dt;
        z += vz * dt;

        // Keep on obstacle roof
        for (Obstacle o : obstacles) {
            if (o.type == Obstacle.Type.BUILDING && o.containsXZ(x, z, 0.4f)) {
                y = o.getMaxY();
                break;
            }
        }
    }
}
