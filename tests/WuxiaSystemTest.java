import com.devagent.realworldsurvival3d.*;

import java.util.ArrayList;
import java.util.List;

/**
 * WuxiaSystemTest - Comprehensive JVM unit test suite verifying all 12 Phases:
 *   - Phase 1: Character Customization & Presets
 *   - Phase 2: Advanced Movement (Triple Jump, Air Dash, Gliding, Swimming/Oxygen, Ground Slam)
 *   - Phase 3 & 4: Martial Arts Combat, Combos, Perfect Parry, Block, Dodge i-frames
 *   - Phase 5: 9-Weapon System & Instant Weapon Switching
 *   - Phase 6: Mystic Qi Energy & Abilities (Offensive, Defensive, Movement, Control)
 *   - Phase 7: Skill Tree Mastery & Inner Ways Passives
 *   - Phase 8: Enemy AI & Multi-Phase Boss State Machine
 *   - Phase 9: Open World Regions & Fast Travel Shrines
 *   - Phase 10: Reputation (Hero/Villain Karma), Bounty & NPCs
 *   - Phase 11: Touch Combat Layout Hit-Testing
 *   - Phase 12: Combat Camera Lock-On & Impact Shake
 * Run: tests/run.sh
 */
public class WuxiaSystemTest {

    static int passed = 0, failed = 0;

    static void check(boolean cond, String name) {
        if (cond) {
            passed++;
            System.out.println("  PASS  " + name);
        } else {
            failed++;
            System.out.println("  FAIL  " + name);
        }
    }

    static void approx(float actual, float expected, float eps, String name) {
        if (Math.abs(actual - expected) <= eps) {
            passed++;
            System.out.println("  PASS  " + name + " (" + actual + " ~ " + expected + ")");
        } else {
            failed++;
            System.out.println("  FAIL  " + name + " (expected " + expected + " +/- " + eps + ", got " + actual + ")");
        }
    }

    public static void main(String[] args) {
        testCharacterCustomization();
        testAdvancedMovement();
        testMartialArtsCombat();
        testWeaponSystem();
        testQiAbilities();
        testSkillTree();
        testEnemyAIAndBoss();
        testWuxiaWorldAndFastTravel();
        testReputationAndNPCs();
        testCombatTouchLayout();
        testCombatCamera();

        System.out.println("\nWuxiaSystemTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }

    // -----------------------------------------------------------------
    static void testCharacterCustomization() {
        System.out.println("Phase 1: Character Customization & Presets");
        CharacterCustomization c = new CharacterCustomization();

        // 1. Preset verification
        c.applyPreset(CharacterCustomization.Preset.WANDERING_SWORDSMAN);
        check(c.gender == CharacterCustomization.Gender.MALE, "Wandering Swordsman is Male");
        check(c.outfit == CharacterCustomization.Outfit.WANDERING_ROBE, "Equipped with Wandering Robe");
        check(c.accessory == CharacterCustomization.Accessory.BAMBOO_HAT, "Equipped with Bamboo Hat");

        c.applyPreset(CharacterCustomization.Preset.SHADOW_BLADE);
        check(c.outfit == CharacterCustomization.Outfit.SHADOW_SILK, "Shadow Blade equipped with Shadow Silk");
        check(c.accessory == CharacterCustomization.Accessory.FACE_MASK, "Equipped with Face Mask");

        c.applyPreset(CharacterCustomization.Preset.MYSTIC_SCHOLAR);
        check(c.outfit == CharacterCustomization.Outfit.SCHOLAR_HANFU, "Mystic Scholar equipped with Scholar Hanfu");

        c.applyPreset(CharacterCustomization.Preset.DRAGON_VANGUARD);
        check(c.armor == CharacterCustomization.ArmorStyle.CHEST_LAMELLAR, "Dragon Vanguard equipped with Chest Lamellar");

        c.applyPreset(CharacterCustomization.Preset.JADE_LOTUS_PRODIGY);
        check(c.gender == CharacterCustomization.Gender.FEMALE, "Jade Lotus Prodigy is Female");
        check(c.eyeColor == CharacterCustomization.EyeColor.JADE_GREEN, "Prodigy has Jade Green eyes");

        // 2. Serialization & Deserialization
        String serialized = c.serialize();
        CharacterCustomization restored = new CharacterCustomization();
        restored.deserialize(serialized);
        check(restored.gender == c.gender, "Restored gender matches");
        check(restored.outfit == c.outfit, "Restored outfit matches");
        approx(restored.skinR, c.skinR, 0.01f, "Restored skin color matches");
    }

    // -----------------------------------------------------------------
    static void testAdvancedMovement() {
        System.out.println("Phase 2: Advanced Movement (Triple Jump, Air Dash, Gliding, Swimming, Ground Slam)");
        Player player = new Player();
        player.wuxiaMode = true;
        WuxiaMovementSystem movement = player.wuxiaMovement;
        List<Obstacle> obstacles = new ArrayList<Obstacle>();

        // Triple Jump
        player.isGrounded = true;
        player.stamina = 100f;
        check(movement.attemptJump(player), "Jump 1 (Ground leap) succeeds");
        check(player.state == Player.ParkourState.JUMP, "Player enters JUMP state");

        check(movement.attemptJump(player), "Jump 2 (Double jump) succeeds");
        check(player.state == Player.ParkourState.DOUBLE_JUMP, "Player enters DOUBLE_JUMP state");

        check(movement.attemptJump(player), "Jump 3 (Triple skyward jump) succeeds");
        check(movement.jumpsRemaining == 0, "All jumps consumed");
        check(!movement.attemptJump(player), "No fourth jump allowed");

        // Air Dash
        check(movement.airDashAvailable, "Air dash is initially available");
        check(movement.attemptAirDash(player, 1f, 0f), "Air dash executes forward burst");
        check(movement.currentState == WuxiaMovementSystem.MovementState.AIR_DASH, "Enters AIR_DASH state");
        check(!movement.airDashAvailable, "Air dash consumed in mid-air");

        // Fast Aerial Descent (Ground Slam)
        check(movement.attemptFastDescent(player), "Initiates fast aerial descent");
        approx(player.vy, movement.fastDescentVelocity, 1.0f, "Fast descent velocity reached");

        // Landing & Shockwave
        movement.onLand();
        check(movement.currentState == WuxiaMovementSystem.MovementState.GROUND_IMPACT, "Landing from fast descent triggers GROUND_IMPACT");
        check(movement.triggerGroundSlamVfx, "Ground slam shockwave VFX triggered");
        check(movement.airDashAvailable, "Air dash refreshed upon landing");
        check(movement.jumpsRemaining == 3, "Jumps refreshed to 3 upon landing");

        // Gliding
        player.isGrounded = false;
        player.vy = -5.0f;
        player.stamina = 100f;
        check(movement.toggleGlider(player), "Glider deploys when airborne");
        check(movement.isGliding, "Glider active flag is true");
        movement.update(1.0f, player, obstacles);
        approx(player.vy, movement.glideDescentRate, 0.5f, "Descent rate bounded by glider lift");
        check(player.stamina < 100f, "Stamina consumed during flight");
        movement.toggleGlider(player);
        check(!movement.isGliding, "Glider closed by player command");

        // Swimming & Oxygen
        movement.inWater = true;
        movement.isDiving = true;
        movement.oxygen = 100f;
        movement.update(1.0f, player, obstacles);
        check(movement.oxygen < 100f, "Oxygen depletes while diving submerged");
        movement.isDiving = false;
        movement.update(1.0f, player, obstacles);
        check(movement.oxygen > 88f, "Oxygen recovers when surfaced");
    }

    // -----------------------------------------------------------------
    static void testMartialArtsCombat() {
        System.out.println("Phase 3 & 4: Martial Arts Combat, Combos, Perfect Parry, Defense");
        Player player = new Player();
        WuxiaCombatSystem combat = player.combat;
        WeaponSystem weapons = player.weapons;

        player.stamina = 100f;
        player.isGrounded = true;

        // Light 3-hit combo
        check(combat.triggerLightAttack(player, weapons), "Light Attack 1 executed");
        check(combat.state == WuxiaCombatSystem.CombatState.LIGHT_1, "Enters LIGHT_1 state");
        combat.update(0.15f, player);

        check(combat.triggerLightAttack(player, weapons), "Light Attack 2 executed");
        check(combat.state == WuxiaCombatSystem.CombatState.LIGHT_2, "Enters LIGHT_2 state");
        combat.update(0.15f, player);

        check(combat.triggerLightAttack(player, weapons), "Light Finisher 3 executed");
        check(combat.state == WuxiaCombatSystem.CombatState.LIGHT_3, "Enters LIGHT_3 state");

        // Light into Heavy mixup
        combat.state = WuxiaCombatSystem.CombatState.LIGHT_1;
        combat.comboWindowTimer = 0.4f;
        check(combat.triggerHeavyAttack(player, weapons), "Heavy Attack mixup executed");
        check(combat.state == WuxiaCombatSystem.CombatState.HEAVY_2, "Enters HEAVY_2 combo finisher");

        // Perfect Parry
        combat.state = WuxiaCombatSystem.CombatState.NEUTRAL;
        combat.startBlock();
        check(combat.parryActive, "Parry window is initially active");
        float dealtDamage = combat.onIncomingAttack(40.0f, 20.0f, player);
        approx(dealtDamage, 0f, 0.01f, "Perfect parry completely negates incoming damage");
        check(combat.triggerParryVfx, "Parry spark VFX triggered");
        check(combat.triggerHitStop, "Hit-stop triggered on parry");
        check(combat.counterWindowTimer > 0f, "Counterattack riposte window opened");

        // Counterattack Riposte
        check(combat.triggerLightAttack(player, weapons), "Counterattack executed during riposte window");
        check(combat.state == WuxiaCombatSystem.CombatState.DODGE_ATTACK, "Enters DODGE_ATTACK riposte stance");

        // Normal Block & Damage Reduction
        combat.state = WuxiaCombatSystem.CombatState.BLOCKING;
        combat.parryActive = false;
        player.stamina = 100f;
        dealtDamage = combat.onIncomingAttack(50.0f, 20.0f, player);
        approx(dealtDamage, 15.0f, 0.5f, "Normal block reduces damage by 70%");
        check(player.stamina < 100f, "Stamina consumed while blocking");

        // Guard Break
        player.stamina = 5.0f;
        combat.onIncomingAttack(50.0f, 40.0f, player);
        check(combat.state == WuxiaCombatSystem.CombatState.GUARD_BROKEN, "Guard breaks when stamina exhausted");

        // Dodge roll i-frames
        combat.state = WuxiaCombatSystem.CombatState.NEUTRAL;
        player.stamina = 50f;
        check(combat.triggerDodge(player, 1f, 0f), "Dodge roll executed");
        check(combat.isInvulnerable, "Player granted i-frame invulnerability");
        dealtDamage = combat.onIncomingAttack(100.0f, 50.0f, player);
        approx(dealtDamage, 0f, 0.01f, "i-frames negate damage during dodge");
    }

    // -----------------------------------------------------------------
    static void testWeaponSystem() {
        System.out.println("Phase 5: 9-Weapon System & Instant Weapon Switching");
        WeaponSystem ws = new WeaponSystem();

        // 9 Weapon classes check
        WuxiaWeapon.WeaponType[] allTypes = WuxiaWeapon.WeaponType.values();
        check(allTypes.length == 9, "All 9 original Wuxia weapon classes present");

        WuxiaWeapon sword = new WuxiaWeapon(WuxiaWeapon.WeaponType.SWORD);
        WuxiaWeapon greatsword = new WuxiaWeapon(WuxiaWeapon.WeaponType.GREATSWORD);
        WuxiaWeapon spear = new WuxiaWeapon(WuxiaWeapon.WeaponType.SPEAR);
        WuxiaWeapon dual = new WuxiaWeapon(WuxiaWeapon.WeaponType.DUAL_BLADES);

        check(greatsword.getDamage() > sword.getDamage(), "Greatsword damage exceeds Sword");
        check(spear.getAttackRange() > sword.getAttackRange(), "Spear range exceeds Sword");
        check(dual.getAttackSpeed() > sword.getAttackSpeed(), "Dual blades attack speed exceeds Sword");

        // Weapon upgrading
        check(sword.level == 1, "Sword starts at Level 1");
        float baseDmg = sword.getDamage();
        check(sword.upgrade(), "Sword upgraded to Level 2");
        check(sword.getDamage() > baseDmg, "Level 2 sword damage increased");

        // Dual Weapon loadout & instant switching
        ws.primaryWeapon = sword;
        ws.secondaryWeapon = spear;
        ws.activeSlot = 0;
        check(ws.getActiveWeapon() == sword, "Primary weapon is active");
        check(ws.switchWeapon(), "Instant weapon switch triggered");
        check(ws.activeSlot == 1, "Active slot swapped to secondary");
        check(ws.getActiveWeapon() == spear, "Spear is now active weapon");

        // Serialization
        String ser = ws.serialize();
        WeaponSystem restored = new WeaponSystem();
        restored.deserialize(ser);
        check(restored.activeSlot == ws.activeSlot, "Restored active slot matches");
        check(restored.getActiveWeapon().type == ws.getActiveWeapon().type, "Restored active weapon type matches");
    }

    // -----------------------------------------------------------------
    static void testQiAbilities() {
        System.out.println("Phase 6: Mystic Qi Energy & Abilities");
        Player player = new Player();
        QiAbilitySystem qi = player.qiAbilities;

        // Qi Pool & Cost deduction
        qi.currentQi = 100.0f;
        QiAbilitySystem.ActiveSkill skill = qi.getSelectedSkill();
        check(skill != null, "Active Qi skill selected");
        float cost = skill.type.qiCost;
        check(qi.castActiveSkill(player), "Qi skill cast succeeds");
        approx(qi.currentQi, 100.0f - cost, 0.5f, "Qi cost deducted from energy meter");
        check(!skill.isReady(), "Skill enters cooldown");
        check(!qi.castActiveSkill(player), "Cannot cast skill while on cooldown");

        // Qi Siphoning
        qi.siphonQi(25.0f);
        check(qi.currentQi > 100.0f - cost, "Qi siphoned into energy meter");

        // Iron Body Shield damage absorption
        qi.activeShieldHealth = 80.0f;
        float remainingDamage = qi.absorbDamage(50.0f);
        approx(remainingDamage, 0f, 0.01f, "Qi shield fully absorbed incoming attack");
        approx(qi.activeShieldHealth, 30.0f, 0.01f, "Shield has 30 HP remaining");

        remainingDamage = qi.absorbDamage(45.0f);
        approx(remainingDamage, 15.0f, 0.01f, "Exceeded shield health deals remaining 15 damage");
        approx(qi.activeShieldHealth, 0f, 0.01f, "Shield broken");
    }

    // -----------------------------------------------------------------
    static void testSkillTree() {
        System.out.println("Phase 7: Skill Tree Mastery & Inner Ways");
        SkillTree st = new SkillTree();

        check(st.availableSkillPoints == 5, "Starts with 5 available skill points");
        check(st.getBranchLevel(SkillTree.Branch.SWORD_MASTERY) == 0, "Sword mastery starts at Rank 0");

        check(st.upgradeBranch(SkillTree.Branch.SWORD_MASTERY), "Sword mastery upgraded");
        check(st.getBranchLevel(SkillTree.Branch.SWORD_MASTERY) == 1, "Sword mastery is now Rank 1");
        check(st.availableSkillPoints == 4, "Skill point deducted");

        // Passive bonus
        check(st.getAttackDamageBonus() > 0f, "Attack damage bonus active from mastery");

        // Inner Ways
        st.setInnerWay(SkillTree.InnerWay.WINDRIDERS_GRACE);
        check(st.activeInnerWay == SkillTree.InnerWay.WINDRIDERS_GRACE, "Windrider's Grace active");
        approx(st.getGliderStaminaMultiplier(), 0.50f, 0.05f, "Windrider's Grace halves glider stamina consumption");
    }

    // -----------------------------------------------------------------
    static void testEnemyAIAndBoss() {
        System.out.println("Phase 8: Enemy AI & Multi-Phase Boss System");
        Player player = new Player();
        player.x = 0f; player.z = 0f;

        // Basic Enemy
        WuxiaEnemy bandit = new WuxiaEnemy(WuxiaEnemy.EnemyType.BASIC_FIGHTER, 5f, 0f, 0f);
        bandit.update(0.1f, player);
        check(bandit.state == WuxiaEnemy.State.CHASE, "Bandit alerts and chases player within aggro radius");

        // Boss Encounter (3 Phases)
        WuxiaEnemy boss = new WuxiaEnemy(WuxiaEnemy.EnemyType.BOSS, 10f, 0f, 0f);
        check(boss.bossPhase == 1, "Boss starts in Phase 1");
        boss.takeDamage(220f, 50f);
        check(boss.bossPhase == 2, "Boss transitions into Phase 2 after taking damage");
        boss.takeDamage(220f, 50f);
        check(boss.bossPhase == 3, "Boss transitions into Enraged Phase 3");

        // Stagger & Execution vulnerability
        boss.takeDamage(0f, boss.maxPosture + 50f);
        check(boss.state == WuxiaEnemy.State.STAGGERED, "Boss posture broken, enters STAGGERED state");
        check(boss.staggerTimer > 0f, "Stagger timer active for martial finisher");

        // Parry reaction
        WuxiaEnemy enforcer = new WuxiaEnemy(WuxiaEnemy.EnemyType.ELITE_FIGHTER, 2f, 0f, 0f);
        enforcer.onParried();
        check(enforcer.state == WuxiaEnemy.State.STAGGERED, "Parried enemy is staggered");
    }

    // -----------------------------------------------------------------
    static void testWuxiaWorldAndFastTravel() {
        System.out.println("Phase 9: Open World Regions & Fast Travel");
        WuxiaWorld world = new WuxiaWorld();
        Player player = new Player();

        // Region coordinate mapping
        WuxiaWorld.WuxiaRegion city = world.getRegionAt(150f, 20f);
        check(city == WuxiaWorld.WuxiaRegion.ANCIENT_CITY, "Center-east maps to ANCIENT_CITY");

        WuxiaWorld.WuxiaRegion temple = world.getRegionAt(120f, -380f);
        check(temple == WuxiaWorld.WuxiaRegion.ANCIENT_TEMPLE, "North mountain ridge maps to ANCIENT_TEMPLE");

        WuxiaWorld.WuxiaRegion bamboo = world.getRegionAt(-280f, 120f);
        check(bamboo == WuxiaWorld.WuxiaRegion.BAMBOO_FOREST, "West forest maps to BAMBOO_FOREST");

        // Fast Travel Shrines
        check(world.shrines.size() >= 5, "5 Fast Travel Shrines registered");
        world.shrines.get(1).discovered = true; // Bamboo Shrine
        check(world.fastTravelTo("bamboo_shrine", player), "Fast travel teleport succeeds");
        approx(player.x, -280f, 1.0f, "Player teleported to Bamboo Shrine X");
        approx(player.z, 120f, 1.0f, "Player teleported to Bamboo Shrine Z");

        // Exploration progress
        check(world.getExplorationPercentage() > 0, "Exploration percentage calculated");
    }

    // -----------------------------------------------------------------
    static void testReputationAndNPCs() {
        System.out.println("Phase 10: Reputation, Bounty, & NPCs");
        ReputationSystem rep = new ReputationSystem();
        Player player = new Player();

        check(!rep.isWanted(), "Player starts without a bounty");
        check(!rep.isVillain(), "Player starts in good standing");

        // Record virtue
        rep.recordVirtue(25);
        check(rep.karma >= 40, "Karma increased by virtuous deeds");
        check(rep.isHero(), "Player recognized as a righteous Jianghu Hero");
        approx(rep.getMerchantPriceMultiplier(), 0.80f, 0.05f, "Hero receives 20% merchant discount");

        // Record crime
        rep.recordCrime(80, 500);
        check(rep.isVillain(), "Karma dropped into Villain status");
        check(rep.isWanted(), "Player now carries a bounty");
        check(rep.bounty == 500, "Bounty is 500 gold");
        approx(rep.getMerchantPriceMultiplier(), 1.40f, 0.05f, "Villain pays 40% markup");

        // Clear bounty
        player.currency = 1000;
        check(rep.payBounty(player), "Bounty successfully paid to magistrates");
        check(!rep.isWanted(), "Bounty cleared");

        // NPC Dialogue
        WuxiaNPC master = new WuxiaNPC(WuxiaNPC.Role.WANDERING_MASTER, 150f, 0f, 20f, 0f);
        String dialogue = master.interact(player, rep);
        check(dialogue != null && !dialogue.isEmpty(), "Master NPC provides martial dialogue");
    }

    // -----------------------------------------------------------------
    static void testCombatTouchLayout() {
        System.out.println("Phase 11: Touch Combat Layout Hit-Testing");
        CombatTouchLayout layout = new CombatTouchLayout();
        layout.update(1920f, 1080f, 1.0f, 0.70f, false);

        // Standard layout hit-testing
        check(layout.hitTest(layout.btnLightX, layout.btnLightY) == CombatTouchLayout.CombatButton.LIGHT_ATTACK,
                "Hit test identifies Light Attack button");
        check(layout.hitTest(layout.btnHeavyX, layout.btnHeavyY) == CombatTouchLayout.CombatButton.HEAVY_ATTACK,
                "Hit test identifies Heavy Attack button");
        check(layout.hitTest(layout.btnBlockX, layout.btnBlockY) == CombatTouchLayout.CombatButton.BLOCK_PARRY,
                "Hit test identifies Block/Parry button");
        check(layout.hitTest(layout.btnDodgeX, layout.btnDodgeY) == CombatTouchLayout.CombatButton.DODGE,
                "Hit test identifies Dodge button");
        check(layout.hitTest(layout.btnJumpX, layout.btnJumpY) == CombatTouchLayout.CombatButton.JUMP,
                "Hit test identifies Jump button");
        check(layout.hitTest(layout.btnWeaponSwitchX, layout.btnWeaponSwitchY) == CombatTouchLayout.CombatButton.WEAPON_SWITCH,
                "Hit test identifies Weapon Switch button");
        check(layout.hitTest(layout.btnQiSkillX, layout.btnQiSkillY) == CombatTouchLayout.CombatButton.QI_SKILL,
                "Hit test identifies Qi Skill button");

        // Left-handed layout mirroring
        layout.update(1920f, 1080f, 1.0f, 0.70f, true);
        check(layout.btnLightX < 500f, "Left-handed mode places action buttons on the left");
        check(layout.joystickBaseX > 1000f, "Left-handed mode places joystick on the right");
    }

    // -----------------------------------------------------------------
    static void testCombatCamera() {
        System.out.println("Phase 12: Combat Camera Lock-On & Shake");
        WuxiaCamera cam = new WuxiaCamera();
        Player player = new Player();
        player.x = 0f; player.y = 0f; player.z = 0f;
        List<Obstacle> obstacles = new ArrayList<Obstacle>();

        // Free orbit update
        cam.update(0.016f, player, obstacles);
        check(cam.camY > player.y, "Camera positioned above player");

        // Target Lock-On tracking
        cam.isLockedOn = true;
        cam.lockedTargetX = 0f;
        cam.lockedTargetY = 0f;
        cam.lockedTargetZ = 15f;
        cam.update(0.016f, player, obstacles);
        check(cam.isLockedOn, "Lock-on tracking active");

        // Screen Shake
        cam.triggerShake(0.8f, 0.35f);
        cam.update(0.016f, player, obstacles);
        check(cam.shakeIntensity > 0f, "Screen shake trauma applied");
    }
}
