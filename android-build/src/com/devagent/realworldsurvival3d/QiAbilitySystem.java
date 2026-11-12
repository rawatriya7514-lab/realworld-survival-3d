package com.devagent.realworldsurvival3d;

import java.util.ArrayList;
import java.util.List;

/**
 * QiAbilitySystem - Mystic Martial Arts Powers and Qi Energy Engine (Phase 6).
 * Features:
 *   - Qi energy pool (0.0 - 100.0) with dynamic regeneration and parry siphon
 *   - 4 Ability Categories: Offensive, Defensive, Movement, Control
 *   - Cooldown management and ability progression
 *   - Pure Java (100% JVM testable)
 */
public class QiAbilitySystem {

    public enum AbilityCategory {
        OFFENSIVE,
        DEFENSIVE,
        MOVEMENT,
        CONTROL
    }

    public enum QiAbilityType {
        // Offensive
        QI_SLASH(
            "Crescent Qi Slash",
            "Launches a sharp crescent blade of compressed Qi energy forward.",
            AbilityCategory.OFFENSIVE, 25.0f, 3.0f, 45.0f, 16.0f
        ),
        GROUND_SHOCKWAVE(
            "Earth Shatter Tremor",
            "Slams the ground sending an explosive shockwave through nearby foes.",
            AbilityCategory.OFFENSIVE, 35.0f, 5.0f, 55.0f, 6.0f
        ),
        NEEDLE_VOLLEY(
            "Phantom Needle Volley",
            "Fires a concentrated spread of invisible acupoint needles.",
            AbilityCategory.OFFENSIVE, 20.0f, 2.5f, 40.0f, 20.0f
        ),

        // Defensive
        QI_SHIELD(
            "Iron Body Aegis",
            "Manifests an indestructible Qi shield absorbing incoming strikes.",
            AbilityCategory.DEFENSIVE, 30.0f, 8.0f, 80.0f, 0.0f
        ),
        DEFLECTION_MIRROR(
            "Mirror Reflection Stance",
            "Repels enemy attacks and redirects damage back toward attackers.",
            AbilityCategory.DEFENSIVE, 25.0f, 6.0f, 50.0f, 3.0f
        ),

        // Movement
        WIND_STEP(
            "Wind Step Phantom Dash",
            "Instantly dissolves into wind and reappears 10 meters forward.",
            AbilityCategory.MOVEMENT, 15.0f, 2.0f, 0.0f, 10.0f
        ),
        SKYWARD_SOAR(
            "Skyward Dragon Leap",
            "Launches the martial artist skyward to transition into gliding.",
            AbilityCategory.MOVEMENT, 20.0f, 4.0f, 0.0f, 15.0f
        ),

        // Control
        CELESTIAL_PULL(
            "Celestial Silk Pull",
            "Ensnnares and yanks an enemy directly into close combat range.",
            AbilityCategory.CONTROL, 25.0f, 5.0f, 20.0f, 14.0f
        ),
        DRAGON_ROAR(
            "Dragon Roar Knockback",
            "Emits a concussive roar throwing surrounding enemies backward.",
            AbilityCategory.CONTROL, 30.0f, 7.0f, 35.0f, 7.0f
        ),
        MERIDIAN_FREEZE(
            "Meridian Acupuncture Freeze",
            "Disrupts target Qi flow, paralyzing the enemy in place for 2.5s.",
            AbilityCategory.CONTROL, 35.0f, 9.0f, 15.0f, 5.0f
        );

        public final String displayName;
        public final String description;
        public final AbilityCategory category;
        public final float qiCost;
        public final float baseCooldown;
        public final float powerValue; // Damage, shield health, or effect magnitude
        public final float range;

        QiAbilityType(String displayName, String description, AbilityCategory category,
                      float qiCost, float baseCooldown, float powerValue, float range) {
            this.displayName = displayName;
            this.description = description;
            this.category = category;
            this.qiCost = qiCost;
            this.baseCooldown = baseCooldown;
            this.powerValue = powerValue;
            this.range = range;
        }
    }

    public static class ActiveSkill {
        public QiAbilityType type;
        public int level = 1;
        public float currentCooldown = 0f;

        public ActiveSkill(QiAbilityType type) {
            this.type = type;
            this.level = 1;
            this.currentCooldown = 0f;
        }

        public float getEffectivePower() {
            return type.powerValue * (1.0f + (level - 1) * 0.25f);
        }

        public float getEffectiveCooldown() {
            return Math.max(1.0f, type.baseCooldown * (1.0f - (level - 1) * 0.10f));
        }

        public boolean isReady() {
            return currentCooldown <= 0f;
        }
    }

    // Qi Energy
    public float currentQi = 100.0f;
    public float maxQi = 100.0f;
    public float qiRegenRate = 6.0f; // per second

    // Active Shield
    public float activeShieldHealth = 0f;
    public float shieldTimer = 0f;

    // Equipped Skills
    public final List<ActiveSkill> equippedSkills = new ArrayList<>();
    public int activeSkillIndex = 0;

    // Trigger & VFX flags
    public boolean triggerQiSlashVfx = false;
    public boolean triggerShockwaveVfx = false;
    public boolean triggerShieldVfx = false;
    public boolean triggerWindStepVfx = false;
    public boolean triggerDragonRoarVfx = false;
    public boolean triggerMeridianFreezeVfx = false;
    public boolean playSkillSfx = false;

    public QiAbilitySystem() {
        equippedSkills.add(new ActiveSkill(QiAbilityType.QI_SLASH));
        equippedSkills.add(new ActiveSkill(QiAbilityType.GROUND_SHOCKWAVE));
        equippedSkills.add(new ActiveSkill(QiAbilityType.QI_SHIELD));
        equippedSkills.add(new ActiveSkill(QiAbilityType.WIND_STEP));
    }

    public ActiveSkill getSelectedSkill() {
        if (equippedSkills.isEmpty()) return null;
        if (activeSkillIndex < 0 || activeSkillIndex >= equippedSkills.size()) {
            activeSkillIndex = 0;
        }
        return equippedSkills.get(activeSkillIndex);
    }

    public void cycleNextSkill() {
        if (equippedSkills.isEmpty()) return;
        activeSkillIndex = (activeSkillIndex + 1) % equippedSkills.size();
    }

    /**
     * Executes the currently selected Qi Ability.
     * Returns true if successfully cast.
     */
    public boolean castActiveSkill(Player player) {
        ActiveSkill skill = getSelectedSkill();
        if (skill == null || !skill.isReady() || currentQi < skill.type.qiCost) {
            return false;
        }

        currentQi -= skill.type.qiCost;
        skill.currentCooldown = skill.getEffectiveCooldown();
        playSkillSfx = true;

        switch (skill.type) {
            case QI_SLASH:
                triggerQiSlashVfx = true;
                player.addCombo("CRESCENT QI WAVE", 20);
                break;

            case GROUND_SHOCKWAVE:
                triggerShockwaveVfx = true;
                player.addCombo("EARTH SHATTER TREMOR", 25);
                break;

            case NEEDLE_VOLLEY:
                player.addCombo("PHANTOM NEEDLE FLURRY", 22);
                break;

            case QI_SHIELD:
                triggerShieldVfx = true;
                activeShieldHealth = skill.getEffectivePower();
                shieldTimer = 6.0f;
                player.addCombo("IRON BODY QI SHIELD", 15);
                break;

            case DEFLECTION_MIRROR:
                player.addCombo("MIRROR REFLECTION STANCE", 18);
                break;

            case WIND_STEP:
                triggerWindStepVfx = true;
                float rad = (float) Math.toRadians(player.yaw);
                player.x += (float) Math.sin(rad) * skill.type.range;
                player.z += (float) -Math.cos(rad) * skill.type.range;
                player.addCombo("WIND STEP PHANTOM WARP", 16);
                break;

            case SKYWARD_SOAR:
                player.vy = skill.type.range;
                player.isGrounded = false;
                player.addCombo("SKYWARD DRAGON LEAP", 18);
                break;

            case CELESTIAL_PULL:
                player.addCombo("CELESTIAL SILK PULL", 15);
                break;

            case DRAGON_ROAR:
                triggerDragonRoarVfx = true;
                player.addCombo("DRAGON ROAR DISPERSION", 20);
                break;

            case MERIDIAN_FREEZE:
                triggerMeridianFreezeVfx = true;
                player.addCombo("MERIDIAN FREEZE ACUPOINT", 24);
                break;
        }
        return true;
    }

    /** Siphons Qi into player's pool following a successful parry or combo finisher. */
    public void siphonQi(float amount) {
        currentQi = Math.min(maxQi, currentQi + amount);
    }

    /** Absorbs incoming damage through active Qi shield. Returns remaining unabsorbed damage. */
    public float absorbDamage(float incomingDamage) {
        if (activeShieldHealth > 0f) {
            if (activeShieldHealth >= incomingDamage) {
                activeShieldHealth -= incomingDamage;
                return 0f;
            } else {
                float remainder = incomingDamage - activeShieldHealth;
                activeShieldHealth = 0f;
                return remainder;
            }
        }
        return incomingDamage;
    }

    public void update(float dt) {
        // Reset single-frame flags
        triggerQiSlashVfx = false;
        triggerShockwaveVfx = false;
        triggerShieldVfx = false;
        triggerWindStepVfx = false;
        triggerDragonRoarVfx = false;
        triggerMeridianFreezeVfx = false;
        playSkillSfx = false;

        // Passive Qi regeneration
        if (currentQi < maxQi) {
            currentQi = Math.min(maxQi, currentQi + qiRegenRate * dt);
        }

        // Active shield countdown
        if (shieldTimer > 0f) {
            shieldTimer -= dt;
            if (shieldTimer <= 0f) {
                activeShieldHealth = 0f;
            }
        }

        // Skill cooldowns
        for (ActiveSkill s : equippedSkills) {
            if (s.currentCooldown > 0f) {
                s.currentCooldown = Math.max(0f, s.currentCooldown - dt);
            }
        }
    }

    public String serialize() {
        StringBuilder sb = new StringBuilder();
        sb.append(currentQi).append(":").append(maxQi).append(":").append(activeSkillIndex).append("|");
        for (int i = 0; i < equippedSkills.size(); i++) {
            ActiveSkill s = equippedSkills.get(i);
            sb.append(s.type.name()).append(",").append(s.level);
            if (i < equippedSkills.size() - 1) sb.append(";");
        }
        return sb.toString();
    }

    public void deserialize(String str) {
        if (str == null || str.trim().isEmpty()) return;
        try {
            String[] parts = str.split("\\|");
            if (parts.length >= 1) {
                String[] q = parts[0].split(":");
                currentQi = Float.parseFloat(q[0]);
                maxQi = Float.parseFloat(q[1]);
                activeSkillIndex = Integer.parseInt(q[2]);
            }
            if (parts.length >= 2) {
                equippedSkills.clear();
                String[] list = parts[1].split(";");
                for (String sk : list) {
                    String[] pair = sk.split(",");
                    if (pair.length >= 2) {
                        ActiveSkill as = new ActiveSkill(QiAbilityType.valueOf(pair[0]));
                        as.level = Integer.parseInt(pair[1]);
                        equippedSkills.add(as);
                    }
                }
            }
        } catch (Exception ignored) {}
    }
}
