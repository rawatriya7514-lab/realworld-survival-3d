package com.devagent.realworldsurvival3d;

/**
 * WuxiaNPC - Interactive Jianghu World Characters (Phase 10).
 * Features:
 *   - NPC Roles: Wandering Master, Village Blacksmith, Herb Alchemist, Teahouse Merchant, City Guard
 *   - Schedules, dialogue trees, trade, crafting, and training interactions
 *   - Reacts dynamically to player reputation (Hero vs Villain)
 *   - Pure Java (100% JVM testable)
 */
public class WuxiaNPC {

    public enum Role {
        WANDERING_MASTER("Master Gu", "Wandering Swordsman Master", "True martial mastery requires an unclouded mind and a swift blade."),
        BLACKSMITH("Master Tie", "Ancient Forge Smith", "Bring me Star Iron and Crimson Jade, and I will forge blades that split wind."),
        HERB_ALCHEMIST("Healer Lan", "Spirit Herb Alchemist", "These spirit herbs restore vitality and nourish the golden elixir within."),
        TEAHOUSE_MERCHANT("Merchant Zhao", "Jianghu Teahouse Keeper", "A cup of cloud mist tea, and the latest gossip from the four corners of the realm."),
        CITY_GUARD("Captain Song", "Walled Pavilion Guard Captain", "Keep your weapon sheathed within city walls, wanderer. Law is sovereign here.");

        public final String name;
        public final String title;
        public final String initialDialogue;

        Role(String name, String title, String initialDialogue) {
            this.name = name;
            this.title = title;
            this.initialDialogue = initialDialogue;
        }
    }

    public Role role;
    public float x, y, z;
    public float yaw;
    public final CharacterAnimator animator = new CharacterAnimator();
    public boolean isInteracting = false;
    public float interactCooldown = 0f;

    public WuxiaNPC(Role role, float x, float y, float z, float yaw) {
        this.role = role;
        this.x = x; this.y = y; this.z = z;
        this.yaw = yaw;
    }

    /**
     * Interacts with the player based on karma and distance.
     * Returns the dialogue or action response.
     */
    public String interact(Player player, ReputationSystem rep) {
        if (rep != null && rep.isWanted() && role == Role.CITY_GUARD) {
            return "Halt, criminal! There is a " + rep.bounty + " coin bounty upon your head!";
        }
        if (rep != null && rep.karma < -40 && role != Role.TEAHOUSE_MERCHANT) {
            return "Your hands are stained with wicked deeds. I have nothing to say to a rogue.";
        }
        if (rep != null && rep.karma > 50) {
            return "Honored hero, your righteousness is celebrated throughout Jianghu! " + role.initialDialogue;
        }
        return role.initialDialogue;
    }

    public void update(float dt) {
        if (interactCooldown > 0f) {
            interactCooldown -= dt;
        }
        animator.update(dt, Player.ParkourState.STAND, 0f, true);
    }
}
