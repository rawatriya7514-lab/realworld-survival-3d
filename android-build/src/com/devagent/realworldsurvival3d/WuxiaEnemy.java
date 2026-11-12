package com.devagent.realworldsurvival3d;

/**
 * WuxiaEnemy - Advanced Martial Arts Enemy AI with telegraphs and stagger mechanics (Phase 8).
 * Features:
 *   - 7 Enemy Classes (Basic, Fast, Heavy, Ranged, Defensive, Elite, Boss)
 *   - Attack anticipation telegraphs (red warning window eligible for player perfect parry)
 *   - Stagger posture gauge and vulnerability state for executions
 *   - Multi-phase boss support
 *   - Pure Java (100% JVM testable)
 */
public class WuxiaEnemy {

    public enum EnemyType {
        BASIC_FIGHTER("Mountain Bandit", 80.0f, 14.0f, 15.0f, 0.60f, 50.0f),
        FAST_ATTACKER("Shadow Scout", 65.0f, 12.0f, 10.0f, 0.40f, 40.0f),
        HEAVY_ATTACKER("Iron Gate Brute", 160.0f, 32.0f, 50.0f, 0.85f, 120.0f),
        RANGED_ATTACKER("Bamboo Archer", 60.0f, 18.0f, 12.0f, 0.70f, 35.0f),
        DEFENSIVE_FIGHTER("Temple Shield Guard", 130.0f, 16.0f, 25.0f, 0.65f, 90.0f),
        ELITE_FIGHTER("Shadow Sect Enforcer", 220.0f, 26.0f, 30.0f, 0.50f, 140.0f),
        BOSS("General Murong, Iron Claws", 600.0f, 35.0f, 45.0f, 0.55f, 250.0f);

        public final String name;
        public final float maxHealth;
        public final float attackDamage;
        public final float guardDamage;
        public final float telegraphDuration;
        public final float maxPosture;

        EnemyType(String name, float maxHealth, float attackDamage, float guardDamage,
                  float telegraphDuration, float maxPosture) {
            this.name = name;
            this.maxHealth = maxHealth;
            this.attackDamage = attackDamage;
            this.guardDamage = guardDamage;
            this.telegraphDuration = telegraphDuration;
            this.maxPosture = maxPosture;
        }
    }

    public enum State {
        PATROL,
        CHASE,
        TELEGRAPH,
        ATTACKING,
        BLOCKING,
        STAGGERED,
        KNOCKBACK,
        DEFEATED
    }

    public EnemyType type;
    public State state = State.PATROL;

    // Transform
    public float x = 0f, y = 0f, z = 0f;
    public float yaw = 0f;
    public float spawnX = 0f, spawnZ = 0f;

    // Combat Stats
    public float health;
    public float maxHealth;
    public float posture;       // Stagger gauge
    public float maxPosture;

    // Timers
    public float telegraphTimer = 0f;
    public float attackTimer = 0f;
    public float attackCooldown = 0f;
    public float staggerTimer = 0f;
    public float patrolTimer = 0f;

    // Boss Phase (1, 2, 3)
    public int bossPhase = 1;

    // Procedural limb animator
    public final CharacterAnimator animator = new CharacterAnimator();

    // Event Flags
    public boolean telegraphVisible = false; // Red glint / parry window
    public boolean attackConnected = false;
    public boolean isParryable = true;

    public WuxiaEnemy(EnemyType type, float x, float y, float z) {
        this.type = type;
        this.x = x; this.y = y; this.z = z;
        this.spawnX = x; this.spawnZ = z;
        this.health = type.maxHealth;
        this.maxHealth = type.maxHealth;
        this.posture = type.maxPosture;
        this.maxPosture = type.maxPosture;
    }

    /**
     * Updates enemy AI, distance checks, attack telegraphs, and state machine.
     */
    public void update(float dt, Player player) {
        if (state == State.DEFEATED) {
            animator.update(dt, Player.ParkourState.DEAD, 0f, true);
            return;
        }

        // Distance to player
        float dx = player.x - x;
        float dz = player.z - z;
        float distSq = dx * dx + dz * dz;
        float dist = (float) Math.sqrt(distSq);

        // Face player if alerted
        if (dist > 0.01f && dist < 35f) {
            yaw = (float) Math.toDegrees(Math.atan2(dx, dz));
        }

        if (attackCooldown > 0f) {
            attackCooldown -= dt;
        }

        // Stagger recovery
        if (state == State.STAGGERED) {
            staggerTimer -= dt;
            animator.update(dt, Player.ParkourState.HARD_LAND, 0f, true);
            if (staggerTimer <= 0f) {
                posture = maxPosture;
                state = State.CHASE;
            }
            return;
        }

        // Telegraph state (Active window where player can execute Perfect Parry!)
        if (state == State.TELEGRAPH) {
            telegraphTimer -= dt;
            telegraphVisible = true;
            animator.update(dt, Player.ParkourState.STAND, 0f, true);
            if (telegraphTimer <= 0f) {
                // Strike hits!
                telegraphVisible = false;
                state = State.ATTACKING;
                attackTimer = 0.25f;
                attackConnected = false;
            }
            return;
        }

        // Attacking active frame
        if (state == State.ATTACKING) {
            attackTimer -= dt;
            animator.update(dt, Player.ParkourState.VAULTING, 0f, true);
            if (!attackConnected && dist <= (type == EnemyType.RANGED_ATTACKER ? 22f : 2.5f)) {
                attackConnected = true;
                // Deliver hit to player
                float dmg = type.attackDamage * (type == EnemyType.BOSS ? (1.0f + (bossPhase - 1) * 0.35f) : 1.0f);
                float guardDmg = type.guardDamage;
                // Note: Player's combat system handles parry, block, dodge
            }
            if (attackTimer <= 0f) {
                state = State.CHASE;
                attackCooldown = type == EnemyType.FAST_ATTACKER ? 0.8f : (type == EnemyType.BOSS ? 0.9f : 1.6f);
            }
            return;
        }

        // Aggro and chasing
        float aggroRange = (type == EnemyType.BOSS) ? 40f : 20f;
        float attackRange = (type == EnemyType.RANGED_ATTACKER) ? 18f : (type == EnemyType.ELITE_FIGHTER ? 3.2f : 2.0f);

        if (dist <= aggroRange) {
            state = State.CHASE;
            if (dist <= attackRange && attackCooldown <= 0f) {
                // Start attack telegraph
                state = State.TELEGRAPH;
                telegraphTimer = type.telegraphDuration / (type == EnemyType.BOSS ? (1.0f + (bossPhase - 1) * 0.25f) : 1.0f);
                telegraphVisible = true;
            } else if (dist > attackRange * 0.9f) {
                // Move towards player
                float speed = (type == EnemyType.FAST_ATTACKER) ? 6.5f : (type == EnemyType.HEAVY_ATTACKER ? 3.5f : 5.0f);
                x += (dx / dist) * speed * dt;
                z += (dz / dist) * speed * dt;
                animator.update(dt, Player.ParkourState.RUN, speed, true);
            } else {
                animator.update(dt, Player.ParkourState.STAND, 0f, true);
            }
        } else {
            // Idle / Patrol near spawn
            state = State.PATROL;
            animator.update(dt, Player.ParkourState.STAND, 0f, true);
        }
    }

    private boolean SPEAR_ENEMY() {
        return type == EnemyType.ELITE_FIGHTER;
    }

    /**
     * Applies damage to the enemy from player attacks.
     * Depletes health and posture. If posture is broken, enters Staggered state.
     */
    public void takeDamage(float damage, float postureDamage) {
        if (state == State.DEFEATED) return;

        health -= damage;
        posture -= postureDamage;

        // Check Boss Phase Transition
        if (type == EnemyType.BOSS) {
            if (health <= maxHealth * 0.33f && bossPhase < 3) {
                bossPhase = 3;
                posture = maxPosture; // Reset posture on phase shift
            } else if (health <= maxHealth * 0.66f && bossPhase < 2) {
                bossPhase = 2;
                posture = maxPosture;
            }
        }

        if (health <= 0f) {
            health = 0f;
            state = State.DEFEATED;
            return;
        }

        if (posture <= 0f) {
            posture = 0f;
            state = State.STAGGERED;
            staggerTimer = 2.5f; // Vulnerable to martial execution finisher!
        }
    }

    /**
     * Triggered when the player successfully parries this enemy's attack.
     * Instantly breaks enemy rhythm and incurs heavy posture damage.
     */
    public void onParried() {
        telegraphVisible = false;
        state = State.STAGGERED;
        staggerTimer = 1.8f;
        takeDamage(0f, maxPosture * 0.60f);
    }
}
