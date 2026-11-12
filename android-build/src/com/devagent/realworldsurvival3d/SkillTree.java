package com.devagent.realworldsurvival3d;

import java.util.HashMap;
import java.util.Map;

/**
 * SkillTree - Comprehensive character martial arts skill progression system (Phase 7).
 * Features:
 *   - 7 Mastery Branches (Sword, Spear, Dual Blades, Defensive, Movement, Mystic, Survival)
 *   - Inner Ways passive abilities
 *   - Skill point allocation, level caps, upgrade costs
 *   - Pure Java (100% JVM testable)
 */
public class SkillTree {

    public enum Branch {
        SWORD_MASTERY("Sword Mastery", "Refines fluid blade strikes, parry counters, and edge sharpness."),
        SPEAR_MASTERY("Spear Mastery", "Extends attack reach, thrust velocity, and wide sweeping arcs."),
        DUAL_WEAPON_MASTERY("Dual Weapon Mastery", "Maximizes attack speed and high-count combo flurries."),
        DEFENSIVE_MASTERY("Defensive Mastery", "Strengthens guard stamina, parry timing, and damage reduction."),
        MOVEMENT_MASTERY("Movement Mastery", "Unlocks triple jumps, air dashes, glider efficiency, and water running."),
        MYSTIC_MASTERY("Mystic Mastery", "Expands Qi capacity, accelerates regeneration, and empowers special arts."),
        SURVIVAL_MASTERY("Survival Mastery", "Enhances maximum health, stamina recovery, and herb alchemy potency.");

        public final String title;
        public final String description;

        Branch(String title, String description) {
            this.title = title;
            this.description = description;
        }
    }

    public enum InnerWay {
        FLOWING_RIVER("Flowing River Stance", "Qi regenerates 30% faster while the player is moving."),
        MOUNTAIN_UNSHAKEN("Mountain Unshaken", "Stamina drained from blocking enemy strikes reduced by 40%."),
        VIPERS_PRECISION("Viper's Precision", "Critical hit strike chance +20% for 3 seconds following a perfect parry."),
        WINDRIDERS_GRACE("Windrider's Grace", "Glider stamina consumption reduced by 50%."),
        TIGERS_FEROCITY("Tiger's Ferocity", "Each combo hit escalates attack damage by +8% (stacks up to +40%).");

        public final String title;
        public final String effect;

        InnerWay(String title, String effect) {
            this.title = title;
            this.effect = effect;
        }
    }

    public int availableSkillPoints = 5;
    public int totalPointsSpent = 0;

    // Branch Levels (0 to 5)
    public final Map<Branch, Integer> branchLevels = new HashMap<>();

    // Active Inner Way passive
    public InnerWay activeInnerWay = InnerWay.FLOWING_RIVER;

    public SkillTree() {
        for (Branch b : Branch.values()) {
            branchLevels.put(b, 0);
        }
    }

    public int getBranchLevel(Branch branch) {
        Integer lvl = branchLevels.get(branch);
        return lvl != null ? lvl : 0;
    }

    /**
     * Attempts to upgrade a mastery branch by 1 rank.
     * Returns true if successful.
     */
    public boolean upgradeBranch(Branch branch) {
        int current = getBranchLevel(branch);
        if (current >= 5 || availableSkillPoints < 1) {
            return false;
        }
        branchLevels.put(branch, current + 1);
        availableSkillPoints--;
        totalPointsSpent++;
        return true;
    }

    /** Sets the player's active Inner Way passive philosophy. */
    public void setInnerWay(InnerWay innerWay) {
        if (innerWay != null) {
            this.activeInnerWay = innerWay;
        }
    }

    /** Grants additional skill points (e.g. from discovering an ancient stele or defeating a boss). */
    public void awardSkillPoints(int points) {
        if (points > 0) {
            availableSkillPoints += points;
        }
    }

    // Passive bonus calculations
    public float getAttackDamageBonus() {
        int swordLvl = getBranchLevel(Branch.SWORD_MASTERY);
        int spearLvl = getBranchLevel(Branch.SPEAR_MASTERY);
        int dualLvl = getBranchLevel(Branch.DUAL_WEAPON_MASTERY);
        return (swordLvl + spearLvl + dualLvl) * 0.06f; // +6% per combat branch level
    }

    public float getDefenseReductionBonus() {
        int defLvl = getBranchLevel(Branch.DEFENSIVE_MASTERY);
        float bonus = defLvl * 0.05f; // +5% damage reduction per level
        if (activeInnerWay == InnerWay.MOUNTAIN_UNSHAKEN) {
            bonus += 0.10f;
        }
        return bonus;
    }

    public float getGliderStaminaMultiplier() {
        float mult = 1.0f - (getBranchLevel(Branch.MOVEMENT_MASTERY) * 0.08f);
        if (activeInnerWay == InnerWay.WINDRIDERS_GRACE) {
            mult *= 0.50f;
        }
        return Math.max(0.25f, mult);
    }

    public float getQiRegenMultiplier(boolean isMoving) {
        float mult = 1.0f + (getBranchLevel(Branch.MYSTIC_MASTERY) * 0.12f);
        if (isMoving && activeInnerWay == InnerWay.FLOWING_RIVER) {
            mult *= 1.30f;
        }
        return mult;
    }

    public String serialize() {
        StringBuilder sb = new StringBuilder();
        sb.append(availableSkillPoints).append(":").append(totalPointsSpent).append(":");
        sb.append(activeInnerWay.name()).append("|");
        Branch[] branches = Branch.values();
        for (int i = 0; i < branches.length; i++) {
            sb.append(branches[i].name()).append("=").append(getBranchLevel(branches[i]));
            if (i < branches.length - 1) sb.append(";");
        }
        return sb.toString();
    }

    public void deserialize(String str) {
        if (str == null || str.trim().isEmpty()) return;
        try {
            String[] parts = str.split("\\|");
            if (parts.length >= 1) {
                String[] p = parts[0].split(":");
                availableSkillPoints = Integer.parseInt(p[0]);
                totalPointsSpent = Integer.parseInt(p[1]);
                activeInnerWay = InnerWay.valueOf(p[2]);
            }
            if (parts.length >= 2) {
                String[] list = parts[1].split(";");
                for (String item : list) {
                    String[] kv = item.split("=");
                    if (kv.length == 2) {
                        Branch b = Branch.valueOf(kv[0]);
                        int lvl = Integer.parseInt(kv[1]);
                        branchLevels.put(b, lvl);
                    }
                }
            }
        } catch (Exception ignored) {}
    }
}
