package com.devagent.realworldsurvival3d;

/**
 * WuxiaWeapon - Model for the 9 distinct original Wuxia weapon classes (Phase 5).
 * Each weapon possesses a unique combat profile, speed, range, guard impact, and special technique.
 */
public class WuxiaWeapon {

    public enum WeaponType {
        SWORD(
            "Iron Crescent Sword",
            "Balanced martial blade with fluid slashes and rapid counter-angles.",
            25.0f, 1.10f, 2.2f, 18.0f, "Crescent Moon Step"
        ),
        DUAL_BLADES(
            "Twin Phoenix Blades",
            "Ultra-fast twin daggers delivering devastating combo flurries.",
            16.0f, 1.55f, 1.7f, 12.0f, "Lotus Whirlwind"
        ),
        SPEAR(
            "Azure Dragon Spear",
            "Long-range polearm keeping enemies at bay with piercing thrusts.",
            32.0f, 0.95f, 3.5f, 25.0f, "Dragon Vault Thrust"
        ),
        GREATSWORD(
            "Mountain Cleaver",
            "Massive two-handed broadsword delivering crushing guard-breaking blows.",
            55.0f, 0.65f, 2.8f, 60.0f, "Earthquake Cleave"
        ),
        STAFF(
            "Bending Willow Staff",
            "Flexible hardwood staff featuring rapid 360 sweeps and vault takedowns.",
            24.0f, 1.20f, 3.0f, 28.0f, "Pole Vault Sweep"
        ),
        BOW(
            "Wind Piercer Bow",
            "Precision composite recurve bow releasing high-velocity arrows.",
            35.0f, 0.85f, 24.0f, 15.0f, "Rain of Iron Needles"
        ),
        FAN(
            "Iron Feather Fan",
            "Weighted steel combat fan generating aerodynamic wind-shear blades.",
            20.0f, 1.35f, 2.0f, 16.0f, "Gale Force Repel"
        ),
        UMBRELLA(
            "Celestial Silk Umbrella",
            "Reinforced umbrella functioning as a parry shield and needle launcher.",
            22.0f, 1.15f, 2.2f, 22.0f, "Needle Storm Flurry"
        ),
        ROPE_DART(
            "Viper Shadow Rope Dart",
            "Flexible tether dart that ensnares and reels in distant foes.",
            28.0f, 1.05f, 5.2f, 22.0f, "Serpent Reel Hook"
        );

        public final String displayName;
        public final String description;
        public final float baseDamage;
        public final float attackSpeed; // multiplier
        public final float attackRange; // meters
        public final float guardDamage; // stamina damage to blocking foes
        public final String specialSkillName;

        WeaponType(String displayName, String description, float baseDamage,
                   float attackSpeed, float attackRange, float guardDamage,
                   String specialSkillName) {
            this.displayName = displayName;
            this.description = description;
            this.baseDamage = baseDamage;
            this.attackSpeed = attackSpeed;
            this.attackRange = attackRange;
            this.guardDamage = guardDamage;
            this.specialSkillName = specialSkillName;
        }
    }

    public WeaponType type;
    public int level = 1;
    public int maxLevel = 5;

    public WuxiaWeapon(WeaponType type) {
        this.type = type;
        this.level = 1;
    }

    public float getDamage() {
        return type.baseDamage * (1.0f + (level - 1) * 0.22f);
    }

    public float getAttackSpeed() {
        return type.attackSpeed * (1.0f + (level - 1) * 0.05f);
    }

    public float getAttackRange() {
        return type.attackRange;
    }

    public float getGuardDamage() {
        return type.guardDamage * (1.0f + (level - 1) * 0.20f);
    }

    public boolean upgrade() {
        if (level < maxLevel) {
            level++;
            return true;
        }
        return false;
    }

    public String serialize() {
        return type.name() + ":" + level;
    }

    public static WuxiaWeapon deserialize(String str) {
        if (str == null || str.trim().isEmpty()) return new WuxiaWeapon(WeaponType.SWORD);
        try {
            String[] parts = str.split(":");
            WeaponType t = WeaponType.valueOf(parts[0].trim());
            WuxiaWeapon w = new WuxiaWeapon(t);
            if (parts.length > 1) {
                w.level = Math.max(1, Math.min(5, Integer.parseInt(parts[1].trim())));
            }
            return w;
        } catch (Exception e) {
            return new WuxiaWeapon(WeaponType.SWORD);
        }
    }
}
