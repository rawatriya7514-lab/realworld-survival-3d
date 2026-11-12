package com.devagent.realworldsurvival3d;

import java.util.List;

/**
 * WuxiaCombatSystem - Core martial arts action combat engine (Phases 3 & 4).
 * Features:
 *   - Light/Heavy/Charged attacks, combos (L-L-L, L-H, H-H, Aerial, Dodge Riposte)
 *   - Perfect parry system (0.20s window with enemy interrupt & counter opportunity)
 *   - Stamina-based block with guard break
 *   - Directional dodge rolls with invulnerability frames (i-frames)
 *   - Hit-stop micro-freeze and impact screen shake triggers
 *   - Pure Java (100% JVM testable)
 */
public class WuxiaCombatSystem {

    public enum CombatState {
        NEUTRAL,
        LIGHT_1,
        LIGHT_2,
        LIGHT_3,
        HEAVY_1,
        HEAVY_2,
        CHARGING,
        CHARGED_RELEASE,
        AERIAL_STRIKE,
        DODGE_ATTACK,
        BLOCKING,
        PARRYING,
        PARRY_SUCCESS,
        GUARD_BROKEN,
        DODGING
    }

    public CombatState state = CombatState.NEUTRAL;

    // Timing parameters
    public float attackTimer = 0f;
    public float attackDuration = 0.35f;
    public float comboWindowTimer = 0f;
    public static final float COMBO_WINDOW = 0.50f;

    // Perfect Parry Window (0.20s)
    public float parryTimer = 0f;
    public static final float PARRY_WINDOW = 0.20f;
    public boolean parryActive = false;
    public float counterWindowTimer = 0f;
    public static final float COUNTER_WINDOW = 1.20f;

    // Guard Break
    public float guardBreakTimer = 0f;
    public static final float GUARD_BREAK_DURATION = 1.0f;

    // Dodge & Invulnerability Frames (i-frames)
    public float dodgeTimer = 0f;
    public static final float DODGE_DURATION = 0.35f;
    public static final float IFRAME_DURATION = 0.25f;
    public boolean isInvulnerable = false;

    // Charge Attack
    public float chargeTime = 0f;
    public static final float MAX_CHARGE_TIME = 0.85f;

    // Lock-on Target
    public Object lockedTarget = null; // Can be WuxiaEnemy or HostileAI
    public float targetDistance = 0f;

    // Event & Feedback Flags
    public boolean triggerParryVfx = false;
    public boolean triggerHitStop = false;
    public float hitStopDuration = 0.04f;
    public boolean triggerScreenShake = false;
    public float screenShakeIntensity = 0f;
    public boolean playAttackSfx = false;
    public boolean playParrySfx = false;
    public boolean playGuardBreakSfx = false;

    /** Executes Light Attack or advances the combo chain. */
    public boolean triggerLightAttack(Player player, WeaponSystem weapons) {
        if (state == CombatState.GUARD_BROKEN || state == CombatState.DODGING) return false;
        if (player.stamina < 8f) return false;

        player.stamina -= 8f;
        playAttackSfx = true;

        if (counterWindowTimer > 0f) {
            // Lethal Counterattack after successful parry!
            state = CombatState.DODGE_ATTACK;
            attackDuration = 0.30f / weapons.getActiveWeapon().getAttackSpeed();
            attackTimer = attackDuration;
            counterWindowTimer = 0f;
            player.addCombo("MARTIAL COUNTER RIPOSTE", 25);
            return true;
        }

        if (!player.isGrounded) {
            state = CombatState.AERIAL_STRIKE;
            attackDuration = 0.40f;
            attackTimer = attackDuration;
            player.vy = -12.0f; // Rapid downward thrust
            player.addCombo("AERIAL CLEAVE", 15);
            return true;
        }

        if (state == CombatState.DODGING && dodgeTimer > 0f) {
            state = CombatState.DODGE_ATTACK;
            attackDuration = 0.32f / weapons.getActiveWeapon().getAttackSpeed();
            attackTimer = attackDuration;
            player.addCombo("SWIFT STEP STRIKE", 14);
            return true;
        }

        // Combo advancement
        if (state == CombatState.LIGHT_1 && comboWindowTimer > 0f) {
            state = CombatState.LIGHT_2;
            attackDuration = 0.28f / weapons.getActiveWeapon().getAttackSpeed();
            attackTimer = attackDuration;
            comboWindowTimer = COMBO_WINDOW;
            player.addCombo("LIGHT COMBO II", 12);
        } else if (state == CombatState.LIGHT_2 && comboWindowTimer > 0f) {
            state = CombatState.LIGHT_3;
            attackDuration = 0.35f / weapons.getActiveWeapon().getAttackSpeed();
            attackTimer = attackDuration;
            comboWindowTimer = 0f;
            player.addCombo("LIGHT FINISHER III", 20);
        } else {
            state = CombatState.LIGHT_1;
            attackDuration = 0.26f / weapons.getActiveWeapon().getAttackSpeed();
            attackTimer = attackDuration;
            comboWindowTimer = COMBO_WINDOW;
            player.addCombo("LIGHT STRIKE I", 10);
        }
        return true;
    }

    /** Executes Heavy Attack or Light-Heavy combo mixup. */
    public boolean triggerHeavyAttack(Player player, WeaponSystem weapons) {
        if (state == CombatState.GUARD_BROKEN || state == CombatState.DODGING) return false;
        if (player.stamina < 15f) return false;

        player.stamina -= 15f;
        playAttackSfx = true;

        if (state == CombatState.LIGHT_1 || state == CombatState.LIGHT_2) {
            // Light into Heavy mixup
            state = CombatState.HEAVY_2;
            attackDuration = 0.42f / weapons.getActiveWeapon().getAttackSpeed();
            attackTimer = attackDuration;
            comboWindowTimer = 0f;
            player.addCombo("FLOWING HEAVY SWEEP", 18);
        } else {
            state = CombatState.HEAVY_1;
            attackDuration = 0.48f / weapons.getActiveWeapon().getAttackSpeed();
            attackTimer = attackDuration;
            comboWindowTimer = COMBO_WINDOW;
            player.addCombo("HEAVY STRIKE I", 15);
        }
        return true;
    }

    /** Initiates or holds Charged Attack */
    public void startCharging() {
        if (state == CombatState.NEUTRAL) {
            state = CombatState.CHARGING;
            chargeTime = 0f;
        }
    }

    /** Releases Charged Attack */
    public boolean releaseChargedAttack(Player player, WeaponSystem weapons) {
        if (state == CombatState.CHARGING) {
            boolean fullyCharged = chargeTime >= MAX_CHARGE_TIME;
            state = CombatState.CHARGED_RELEASE;
            attackDuration = 0.45f;
            attackTimer = attackDuration;
            playAttackSfx = true;
            if (fullyCharged) {
                triggerScreenShake = true;
                screenShakeIntensity = 0.6f;
                player.addCombo("QI-CHARGED EARTH CRUSHER", 30);
            } else {
                player.addCombo("CHARGED SLASH", 18);
            }
            return true;
        }
        return false;
    }

    /** Initiates Block and opens the Perfect Parry window */
    public void startBlock() {
        if (state == CombatState.NEUTRAL || state == CombatState.BLOCKING) {
            state = CombatState.PARRYING;
            parryTimer = PARRY_WINDOW;
            parryActive = true;
        }
    }

    /** Stops blocking */
    public void releaseBlock() {
        if (state == CombatState.BLOCKING || state == CombatState.PARRYING) {
            state = CombatState.NEUTRAL;
            parryActive = false;
            parryTimer = 0f;
        }
    }

    /**
     * Handles incoming enemy attack damage.
     * Evaluates perfect parry, standard block, i-frames, and guard break.
     * Returns the actual damage dealt to the player.
     */
    public float onIncomingAttack(float incomingDamage, float enemyGuardDamage, Player player) {
        // 1. Invulnerability frames during dodge
        if (isInvulnerable) {
            return 0f;
        }

        // 2. Perfect Parry window!
        if (parryActive && parryTimer > 0f) {
            triggerParryVfx = true;
            triggerHitStop = true;
            triggerScreenShake = true;
            screenShakeIntensity = 0.5f;
            playParrySfx = true;
            state = CombatState.PARRY_SUCCESS;
            counterWindowTimer = COUNTER_WINDOW;
            player.addCombo("PERFECT PARRY DEFLECTION!", 30);
            // Parrying completely negates incoming damage and staggers attacker
            return 0f;
        }

        // 3. Normal Block
        if (state == CombatState.BLOCKING || state == CombatState.PARRYING) {
            float staminaCost = enemyGuardDamage * 0.75f;
            if (player.stamina >= staminaCost) {
                player.stamina -= staminaCost;
                playParrySfx = true;
                return incomingDamage * 0.30f; // 70% damage reduction
            } else {
                // Guard Break!
                player.stamina = 0f;
                state = CombatState.GUARD_BROKEN;
                guardBreakTimer = GUARD_BREAK_DURATION;
                playGuardBreakSfx = true;
                triggerScreenShake = true;
                screenShakeIntensity = 0.4f;
                return incomingDamage * 0.85f;
            }
        }

        // 4. Unblocked clean hit
        return incomingDamage;
    }

    /** Initiates directional dodge roll with i-frames */
    public boolean triggerDodge(Player player, float dirX, float dirZ) {
        if (state == CombatState.GUARD_BROKEN || state == CombatState.DODGING) return false;
        if (player.stamina < 12f) return false;

        player.stamina -= 12f;
        state = CombatState.DODGING;
        dodgeTimer = DODGE_DURATION;
        isInvulnerable = true;

        float mag = (float) Math.sqrt(dirX * dirX + dirZ * dirZ);
        if (mag > 0.001f) {
            player.vx = (dirX / mag) * 11.0f;
            player.vz = (dirZ / mag) * 11.0f;
        }
        player.addCombo("EVASIVE DODGE", 8);
        return true;
    }

    /** Updates combat timers, animations, and state transitions */
    public void update(float dt, Player player) {
        // Reset single-frame event flags
        triggerParryVfx = false;
        triggerHitStop = false;
        triggerScreenShake = false;
        playAttackSfx = false;
        playParrySfx = false;
        playGuardBreakSfx = false;

        // Counter window countdown
        if (counterWindowTimer > 0f) {
            counterWindowTimer = Math.max(0f, counterWindowTimer - dt);
        }

        // Guard break recovery
        if (state == CombatState.GUARD_BROKEN) {
            guardBreakTimer -= dt;
            if (guardBreakTimer <= 0f) {
                state = CombatState.NEUTRAL;
            }
            return;
        }

        // Dodge roll and i-frames
        if (state == CombatState.DODGING) {
            dodgeTimer -= dt;
            if (dodgeTimer <= DODGE_DURATION - IFRAME_DURATION) {
                isInvulnerable = false; // i-frames expire
            }
            if (dodgeTimer <= 0f) {
                state = CombatState.NEUTRAL;
                isInvulnerable = false;
            }
            return;
        }

        // Charging hold
        if (state == CombatState.CHARGING) {
            chargeTime += dt;
            return;
        }

        // Parry active window
        if (state == CombatState.PARRYING) {
            parryTimer -= dt;
            if (parryTimer <= 0f) {
                parryActive = false;
                state = CombatState.BLOCKING; // Transitions into standard block
            }
        }

        // Attack recovery & combo chaining
        if (isAttackingState(state)) {
            attackTimer -= dt;
            if (attackTimer <= 0f) {
                state = CombatState.NEUTRAL;
            }
        }

        // Combo decay window
        if (comboWindowTimer > 0f) {
            comboWindowTimer -= dt;
        }
    }

    public static boolean isAttackingState(CombatState s) {
        return s == CombatState.LIGHT_1 || s == CombatState.LIGHT_2 || s == CombatState.LIGHT_3
                || s == CombatState.HEAVY_1 || s == CombatState.HEAVY_2 || s == CombatState.CHARGED_RELEASE
                || s == CombatState.AERIAL_STRIKE || s == CombatState.DODGE_ATTACK;
    }
}
