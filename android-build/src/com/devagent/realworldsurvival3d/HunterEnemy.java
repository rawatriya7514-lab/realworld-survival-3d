package com.devagent.realworldsurvival3d;

import java.util.List;

/**
 * HunterEnemy - Fast Rooftop Chaser / Infected Stalker AI.
 *
 * AI contract (mobile-friendly, teleport-free):
 *   - PATROL: walks a random target kept INSIDE its current roof bounds.
 *   - ALERT/CHASE: only when the player is within detection range. If the
 *     player is on the same roof it runs straight at them; otherwise it walks
 *     to the roof edge facing the player and LEAPS across the gap if a roof
 *     exists within leap range. If no crossing is possible it gives up after
 *     a while and returns to patrol on its current roof (its new home).
 *   - Enemies never fly: all vertical motion is ground clamping or a
 *     ballistic leap arc. An enemy that leaps off every roof falls into the
 *     hazard pool below and is defeated.
 *   - Attacks require same-roof proximity and a cooldown; drop-stomps from
 *     above and slide tackles defeat them.
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
    public float speed = 6.0f;
    public float chaseSpeed = 10.0f;
    public State state = State.PATROL;
    public float attackCooldown = 0f;
    public float stunTimer = 0f;
    public boolean alertSoundPlayed = false;

    // Roof/leap bookkeeping
    private static final float GRAVITY = -22f;
    private static final float LEAP_RANGE = 9.0f;
    private float leapCooldown = 0f;
    private float blockedTimer = 0f;
    private float giveUpTimer = 0f;
    private float radius = 0.45f;
    private float gravity = GRAVITY;

    public HunterEnemy(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.patrolStartX = x;
        this.patrolStartZ = z;
        this.patrolTargetX = x;
        this.patrolTargetZ = z;
    }

    // =================================================================
    // Main update
    // =================================================================
    public void update(float dt, Player player, WorldMap world) {
        if (state == State.DEFEATED) return;
        List<Obstacle> obstacles = world.obstacles;

        if (attackCooldown > 0f) attackCooldown -= dt;
        if (leapCooldown > 0f) leapCooldown -= dt;

        if (stunTimer > 0f) {
            stunTimer -= dt;
            if (stunTimer <= 0f) state = State.CHASING;
            return;
        }

        // Ballistic leap integration
        if (state == State.LEAPING) {
            updateLeaping(dt, player, obstacles);
            return;
        }

        float dx = player.x - x;
        float dy = player.y - y;
        float dz = player.z - z;
        float distToPlayer = (float) Math.sqrt(dx * dx + dz * dz);

        // ----- Detection & chase trigger -----
        if (distToPlayer < 24.0f && Math.abs(dy) < 8.0f) {
            if (state != State.CHASING && state != State.LEAPING) {
                state = State.CHASING;
                alertSoundPlayed = true;
                giveUpTimer = 6.0f;
            }
        } else if (distToPlayer > 35.0f && state == State.CHASING) {
            state = State.PATROL;
            alertSoundPlayed = false;
        }

        Obstacle myRoof = world.buildingAt(x, z);
        Obstacle playerRoof = world.buildingAt(player.x, player.z);

        if (state == State.CHASING) {
            giveUpTimer -= dt;
            if (giveUpTimer <= 0f) {
                // Can't reach the runner; adopt this roof and go back to patrol
                state = State.PATROL;
                alertSoundPlayed = false;
                if (myRoof != null) {
                    patrolStartX = x;
                    patrolStartZ = z;
                }
            }
            if (myRoof != null) {
                boolean sameRoof = (myRoof == playerRoof);
                if (sameRoof) {
                    chaseDirectly(dt, dx, dz, distToPlayer, myRoof, player);
                } else {
                    chaseTowardEdge(dt, dx, dz, distToPlayer, myRoof, player, obstacles);
                }
            } else {
                // Off any roof (should not normally happen) — fall
                vy += gravity * dt;
                y += vy * dt;
                if (y < -6f) {
                    state = State.DEFEATED; // fell into the hazard
                }
            }
        } else if (state == State.PATROL) {
            updatePatrol(dt, myRoof, obstacles);
        }

        // Ground clamp (walking states)
        if (state != State.LEAPING) {
            for (Obstacle o : obstacles) {
                if (o.type == Obstacle.Type.BUILDING && o.containsXZ(x, z, 0.0f)) {
                    y = o.getMaxY();
                    vy = 0f;
                    break;
                }
            }
        }
    }

    // =================================================================
    // Behaviors
    // =================================================================
    private void chaseDirectly(float dt, float dx, float dz, float dist,
                               Obstacle roof, Player player) {
        float inv = dist > 0.001f ? 1f / dist : 0f;
        float dirX = dx * inv;
        float dirZ = dz * inv;
        vx = dirX * chaseSpeed;
        vz = dirZ * chaseSpeed;
        stepAndClamp(dt, roof, dirX, dirZ);

        // Attack when close (cooldown-gated)
        if (dist < 2.0f && Math.abs(player.y - y) < 1.6f && attackCooldown <= 0f) {
            resolveContact(player);
        } else if (state == State.ATTACKING && attackCooldown <= 0.6f) {
            state = State.CHASING;
        }
    }

    private void chaseTowardEdge(float dt, float dx, float dz, float dist,
                                 Obstacle roof, Player player,
                                 List<Obstacle> obstacles) {
        blockedTimer += dt;
        float inv = dist > 0.001f ? 1f / dist : 0f;
        float dirX = dx * inv;
        float dirZ = dz * inv;

        // Walk toward the edge point nearest the runner
        float step = chaseSpeed * 0.7f * dt;
        float nx = x + dirX * step;
        float nz = z + dirZ * step;
        if (roof.containsXZ(nx, nz, radius)) {
            x = nx;
            z = nz;
            vx = dirX * chaseSpeed * 0.7f;
            vz = dirZ * chaseSpeed * 0.7f;
        } else {
            // At (or beyond) the edge: consider a leap across the gap
            if (leapCooldown <= 0f && blockedTimer > 0.35f) {
                Obstacle landingRoof = findLandingRoof(obstacles, dirX, dirZ);
                if (landingRoof != null) {
                    startLeap(landingRoof);
                    return;
                }
            }
            vx = 0f;
            vz = 0f;
        }

        // Attack only if genuinely adjacent (same height, near)
        if (dist < 2.2f && Math.abs(player.y - y) < 1.6f && attackCooldown <= 0f) {
            resolveContact(player);
        }
    }

    private void updatePatrol(float dt, Obstacle roof, List<Obstacle> obstacles) {
        patrolTimer += dt;
        if (patrolTimer > 4.0f) {
            patrolTimer = 0f;
            if (roof != null) {
                // Keep the patrol target inside this roof's bounds
                float mx = roof.width * 0.5f - radius - 0.3f;
                float mz = roof.depth * 0.5f - radius - 0.3f;
                patrolTargetX = clampIn(roof.getMinX() + radius + 0.3f, roof.getMaxX() - radius - 0.3f,
                        x + (float) (Math.random() * 10 - 5));
                patrolTargetZ = clampIn(roof.getMinZ() + radius + 0.3f, roof.getMaxZ() - radius - 0.3f,
                        z + (float) (Math.random() * 10 - 5));
            } else {
                patrolTargetX = patrolStartX + (float) (Math.random() * 10 - 5);
                patrolTargetZ = patrolStartZ + (float) (Math.random() * 10 - 5);
            }
        }
        float pdx = patrolTargetX - x;
        float pdz = patrolTargetZ - z;
        float pDist = (float) Math.sqrt(pdx * pdx + pdz * pdz);
        if (pDist > 0.5f && roof != null) {
            float inv = 1f / pDist;
            vx = (pdx * inv) * speed;
            vz = (pdz * inv) * speed;
            stepAndClamp(dt, roof, pdx * inv, pdz * inv);
        } else {
            vx = 0f;
            vz = 0f;
        }
    }

    private void stepAndClamp(float dt, Obstacle roof, float dirX, float dirZ) {
        float step = (float) Math.sqrt(vx * vx + vz * vz) * dt;
        if (step <= 0f) return;
        float inv = step > 0f ? 1f / step : 0f;
        float nx = x + vx * dt;
        float nz = z + vz * dt;
        if (roof.containsXZ(nx, nz, radius)) {
            x = nx;
            z = nz;
        } else {
            // Slide along the roof border instead of leaving it
            if (roof.containsXZ(nx, z, radius)) x = nx;
            else if (roof.containsXZ(x, nz, radius)) z = nz;
        }
        blockedTimer += (dirX != 0 || dirZ != 0) ? 0f : dt;
    }

    // =================================================================
    // Leaping
    // =================================================================
    private Obstacle findLandingRoof(List<Obstacle> obstacles, float dirX, float dirZ) {
        for (float d = 2.0f; d <= LEAP_RANGE; d += 1.5f) {
            float sx = x + dirX * d;
            float sz = z + dirZ * d;
            for (Obstacle o : obstacles) {
                if (o.type == Obstacle.Type.BUILDING && o.containsXZ(sx, sz, radius)) {
                    return o;
                }
            }
        }
        return null;
    }

    private void startLeap(Obstacle landingRoof) {
        float cx = landingRoof.x;
        float cz = landingRoof.z;
        float ddx = cx - x;
        float ddz = cz - z;
        float dist = (float) Math.sqrt(ddx * ddx + ddz * ddz);
        if (dist < 0.01f) return;
        float inv = 1f / dist;
        vx = ddx * inv * chaseSpeed * 1.15f;
        vz = ddz * inv * chaseSpeed * 1.15f;
        vy = 8.0f;
        state = State.LEAPING;
        leapCooldown = 3.0f;
        blockedTimer = 0f;
    }

    private void updateLeaping(float dt, Player player, List<Obstacle> obstacles) {
        x += vx * dt;
        z += vz * dt;
        vy += gravity * dt;
        y += vy * dt;

        // Land on the first building roof we drop onto
        if (vy < 0f) {
            for (Obstacle o : obstacles) {
                if (o.type == Obstacle.Type.BUILDING && o.containsXZ(x, z, radius)) {
                    float top = o.getMaxY();
                    if (y <= top && y > top - 6f) {
                        y = top;
                        vy = 0f;
                        state = State.CHASING;
                        giveUpTimer = 6.0f;
                        blockedTimer = 0f;
                        return;
                    }
                }
            }
        }
        if (y < -6f) {
            state = State.DEFEATED; // missed every roof: hazard pool takes it
        }
    }

    // =================================================================
    // Contact / damage
    // =================================================================
    private void resolveContact(Player player) {
        // Stomp elimination from above
        if (player.vy < -5.0f && player.y > y + 0.8f) {
            health = 0;
            state = State.DEFEATED;
            player.vy = 8.0f;
            player.score += 150;
            return;
        }
        // Slide tackle
        if (player.state == Player.ParkourState.SLIDING) {
            health -= 35;
            stunTimer = 1.5f;
            state = State.STUNNED;
            if (health <= 0) {
                state = State.DEFEATED;
                player.score += 100;
            }
            return;
        }
        // Chaser attacks the runner
        player.health -= 18.0f;
        attackCooldown = 1.2f;
        state = State.ATTACKING;
        if (player.health <= 0) {
            player.health = 0;
            player.state = Player.ParkourState.DEAD;
        }
    }

    private static float clampIn(float min, float max, float v) {
        if (max < min) return (min + max) * 0.5f;
        return Math.max(min, Math.min(max, v));
    }
}
