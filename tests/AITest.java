import com.devagent.realworldsurvival3d.CivilianNPC;
import com.devagent.realworldsurvival3d.HostileAI;
import com.devagent.realworldsurvival3d.HunterEnemy;
import com.devagent.realworldsurvival3d.OpenWorld;
import com.devagent.realworldsurvival3d.Player;
import com.devagent.realworldsurvival3d.Terrain;
import com.devagent.realworldsurvival3d.WorldMap;

import java.util.ArrayList;

/**
 * AITest - Pure JVM verification of Phase 8 AI entities:
 *  - CivilianNPC state machine, dialogue cycling, and talk trigger
 *  - HostileAI states (PATROL, ALERT, CHASE, ATTACK, STUNNED, DEFEATED)
 *  - Safe zone aversion (Central Plaza buffer repulsion)
 *  - Staggered AI update budget (<40m full, 40-100m 5th-frame, >100m frozen)
 *  - Player combat resolutions (stomp from above, slide tackle, hostile strike)
 *  - Articulated character animators across all AI entities
 */
public class AITest {

    private static int passed = 0;
    private static int failed = 0;

    private static void check(String name, boolean cond) {
        if (cond) {
            System.out.println("  PASS  " + name);
            passed++;
        } else {
            System.err.println("  FAIL  " + name);
            failed++;
        }
    }

    private static void checkApprox(String name, float a, float b, float eps) {
        if (Math.abs(a - b) <= eps) {
            System.out.println("  PASS  " + name + " (" + a + " ~ " + b + ")");
            passed++;
        } else {
            System.err.println("  FAIL  " + name + " (" + a + " != " + b + ", eps=" + eps + ")");
            failed++;
        }
    }

    private static Player createPlayer(float x, float y, float z) {
        Player p = new Player();
        p.x = x;
        p.y = y;
        p.z = z;
        return p;
    }

    public static void main(String[] args) {
        testCivilianBasicsAndDialogue();
        testCivilianProximityAndPatrol();
        testCivilianStaggeredThrottling();
        testHostileStateAndChase();
        testHostileSafeZoneRepulsion();
        testHostileCombatInteractions();
        testHostileStaggeredThrottling();
        testHunterEnemyArticulation();
        testOpenWorldRosterAndQueries();

        System.out.println("\nAITest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }

    private static void testCivilianBasicsAndDialogue() {
        System.out.println("CivilianNPC: basics & dialogue cycling");
        String[] lines = {"First line", "Second line", "Third line"};
        CivilianNPC npc = new CivilianNPC("Maya", "Scout", 146f, 18f, 5.0f, lines);

        check("NPC initialized with name Maya", "Maya".equals(npc.name));
        check("NPC initialized with role Scout", "Scout".equals(npc.role));
        checkApprox("home X matches initial X", npc.homeX, 146f, 0.01f);
        checkApprox("home Z matches initial Z", npc.homeZ, 18f, 0.01f);
        check("animator is instantiated", npc.animator != null);

        // Dialogue cycling
        String d1 = npc.interact();
        check("dialogue 1 matches First line", "First line".equals(d1));
        String d2 = npc.interact();
        check("dialogue 2 matches Second line", "Second line".equals(d2));
        String d3 = npc.interact();
        check("dialogue 3 matches Third line", "Third line".equals(d3));
        String d4 = npc.interact();
        check("dialogue wraps back to First line", "First line".equals(d4));
    }

    private static void testCivilianProximityAndPatrol() {
        System.out.println("CivilianNPC: proximity conversation & patrol bounds");
        CivilianNPC npc = new CivilianNPC("Goran", "Quartermaster", 154f, 22f, 4.0f,
                new String[]{"Hey runner"});

        // Player is 2.0m away (within 2.5m talk threshold)
        npc.update(0.016f, 154f, 0f, 20f, 0);
        check("enters TALKING state when player within 2.5m", npc.state == CivilianNPC.State.TALKING);
        check("stops moving while talking", !npc.isMoving);

        // Player moves far away (25m away)
        npc.idleTimer = 0f; // force patrol transition
        npc.update(0.016f, 180f, 0f, 22f, 0);
        check("transitions to PATROL when player leaves and idle expires", npc.state == CivilianNPC.State.PATROL);

        // Run patrol for several seconds
        for (int i = 0; i < 200; i++) {
            npc.update(0.016f, 180f, 0f, 22f, i);
        }
        float dx = npc.x - npc.homeX;
        float dz = npc.z - npc.homeZ;
        float distFromHome = (float) Math.sqrt(dx * dx + dz * dz);
        check("patrol movement remains bounded near home (< patrolRadius + 2m)",
                distFromHome <= npc.patrolRadius + 2.0f);
        check("animator pose exists", npc.animator.currentPose != null);
    }

    private static void testCivilianStaggeredThrottling() {
        System.out.println("CivilianNPC: performance budget staggered throttling");
        CivilianNPC npc = new CivilianNPC("Elena", "Tech", 0f, 0f, 5.0f, null);

        // 1. Player < 40m away: updated every frame
        npc.updateCount = 0;
        npc.update(0.016f, 10f, 0f, 0f, 1);
        check("near player (<40m) updates every frame", npc.updateCount == 1);
        check("not throttled", !npc.wasThrottled);
        check("not frozen", !npc.wasFrozen);

        // 2. Player between 40m and 100m away (e.g. 60m): updated only when frameNumber % 5 == 0
        npc.updateCount = 0;
        npc.update(0.016f, 60f, 0f, 0f, 1); // frame 1 -> skipped
        check("mid-distance (40-100m) skipped on frame % 5 != 0", npc.updateCount == 0);
        check("marked as throttled", npc.wasThrottled);

        npc.update(0.016f, 60f, 0f, 0f, 5); // frame 5 -> executed
        check("mid-distance executed on frame % 5 == 0", npc.updateCount == 1);

        // 3. Player > 100m away (e.g. 150m): completely frozen
        npc.updateCount = 0;
        npc.update(0.016f, 150f, 0f, 0f, 5);
        check("distant entity (>100m) is frozen", npc.updateCount == 0);
        check("marked as frozen", npc.wasFrozen);
    }

    private static void testHostileStateAndChase() {
        System.out.println("HostileAI: detection & pursuit state machine");
        HostileAI hostile = new HostileAI("Infected Stalker", 300f, 150f, 15.0f);
        Player player = createPlayer(300f, 0f, 170f); // 20m away (within 24m detection)
        OpenWorld openWorld = new OpenWorld();

        check("starts in PATROL state", hostile.state == HostileAI.State.PATROL);

        // First frame: enters ALERT
        hostile.update(0.016f, player, openWorld, 0);
        check("enters ALERT when player within 24m", hostile.state == HostileAI.State.ALERT);

        // Alert timer expires -> enters CHASE
        hostile.alertTimer = 0f;
        hostile.update(0.016f, player, openWorld, 1);
        check("transitions to CHASE after alert timer", hostile.state == HostileAI.State.CHASE);

        // Move towards player
        float initialDist = (float) Math.sqrt((player.x - hostile.x) * (player.x - hostile.x)
                + (player.z - hostile.z) * (player.z - hostile.z));
        hostile.update(0.1f, player, openWorld, 2);
        float newDist = (float) Math.sqrt((player.x - hostile.x) * (player.x - hostile.x)
                + (player.z - hostile.z) * (player.z - hostile.z));
        check("moves closer to player during CHASE", newDist < initialDist);

        // Player moves far away (> 35m)
        player.z = 210f; // 60m away
        hostile.update(0.016f, player, openWorld, 5);
        check("returns to PATROL when player escapes > 35m", hostile.state == HostileAI.State.PATROL);
    }

    private static void testHostileSafeZoneRepulsion() {
        System.out.println("HostileAI: Central Plaza safe zone aversion");
        // Place hostile directly on boundary of safe zone (Central Plaza is 150, 20, r=30, buffer 34)
        HostileAI hostile = new HostileAI("Stalker", 155f, 20f, 10.0f); // only 5m from plaza center!
        Player player = createPlayer(150f, 0f, 20f); // player inside safe zone
        OpenWorld openWorld = new OpenWorld();

        check("player is recognized in safe zone", openWorld.isSafeZone(player.x, player.z));

        // Update hostile
        hostile.update(0.016f, player, openWorld, 0);

        float dx = hostile.x - HostileAI.SAFE_ZONE_X;
        float dz = hostile.z - HostileAI.SAFE_ZONE_Z;
        float distFromPlaza = (float) Math.sqrt(dx * dx + dz * dz);

        check("repelled outward to or beyond safe zone buffer (>= 34m)", distFromPlaza >= 33.9f);
        check("cannot chase player while player is in safe zone", hostile.state == HostileAI.State.PATROL);
    }

    private static void testHostileCombatInteractions() {
        System.out.println("HostileAI: player combat interactions (stomp, slide tackle, strike)");

        // 1. Hostile strikes player
        HostileAI hostile = new HostileAI("Stalker", 100f, 0f, 10.0f);
        Player player = createPlayer(100f, hostile.y, 0.8f); // 0.8m away
        OpenWorld openWorld = new OpenWorld();
        hostile.state = HostileAI.State.CHASE;
        player.health = 100f;

        hostile.update(0.016f, player, openWorld, 0);
        check("hostile strikes player dealing 15 damage", player.health == 85.0f);
        check("hostile enters ATTACK state with cooldown", hostile.state == HostileAI.State.ATTACK);
        check("attack cooldown set (> 1.0s)", hostile.attackCooldown > 1.0f);

        // 2. Slide tackle stun & damage
        HostileAI hostile2 = new HostileAI("Runner", 50f, 0f, 10.0f);
        Player player2 = createPlayer(50f, hostile2.y, 1.0f); // 1.0m away
        player2.state = Player.ParkourState.SLIDING;
        hostile2.health = 50f;

        hostile2.update(0.016f, player2, openWorld, 0);
        check("slide tackle deals 35 damage to hostile (50 -> 15)", hostile2.health == 15.0f);
        check("slide tackle stuns hostile", hostile2.state == HostileAI.State.STUNNED);
        check("stun timer active (> 1.5s)", hostile2.stunTimer > 1.5f);

        // Second slide tackle eliminates hostile
        hostile2.attackCooldown = 0f;
        hostile2.update(0.016f, player2, openWorld, 0);
        check("second slide tackle defeats hostile (health=0)", hostile2.health == 0f);
        check("hostile state is DEFEATED", hostile2.state == HostileAI.State.DEFEATED);

        // 3. Drop stomp from above
        HostileAI hostile3 = new HostileAI("Stalker", 80f, 0f, 10.0f);
        Player player3 = createPlayer(80f, 2.0f, 0.2f); // above hostile
        player3.vy = -6.0f; // fast downward velocity
        int initialScore = player3.score;

        hostile3.update(0.016f, player3, openWorld, 0);
        check("stomp from above instantly eliminates hostile", hostile3.state == HostileAI.State.DEFEATED);
        check("stomp bounces player upward (vy > 5.0)", player3.vy > 5.0f);
        check("stomp awards +150 score", player3.score == initialScore + 150);
    }

    private static void testHostileStaggeredThrottling() {
        System.out.println("HostileAI: performance budget staggered throttling");
        HostileAI hostile = new HostileAI("Stalker", 0f, 0f, 10.0f);
        Player player = createPlayer(0f, 0f, 20f); // 20m away (< 40m)
        OpenWorld openWorld = new OpenWorld();

        hostile.updateCount = 0;
        hostile.update(0.016f, player, openWorld, 1);
        check("within 40m runs every frame", hostile.updateCount == 1);
        check("not throttled", !hostile.wasThrottled);

        // Mid distance (60m away)
        player.z = 60f;
        hostile.updateCount = 0;
        hostile.update(0.016f, player, openWorld, 1);
        check("60m away throttled on frame % 5 != 0", hostile.updateCount == 0);
        check("marked throttled", hostile.wasThrottled);

        hostile.update(0.016f, player, openWorld, 5);
        check("60m away executes on frame % 5 == 0", hostile.updateCount == 1);

        // Distant (120m away)
        player.z = 120f;
        hostile.updateCount = 0;
        hostile.update(0.016f, player, openWorld, 5);
        check("120m away is completely frozen", hostile.updateCount == 0);
        check("marked frozen", hostile.wasFrozen);
    }

    private static void testHunterEnemyArticulation() {
        System.out.println("HunterEnemy: articulated limb animator (Phase 8 upgrade)");
        HunterEnemy hunter = new HunterEnemy(34f, 25f, 0f);
        Player player = createPlayer(34f, 25f, 10f);
        WorldMap map = new WorldMap();

        check("HunterEnemy has non-null animator", hunter.animator != null);
        check("initial animator pose exists", hunter.animator.currentPose != null);

        // Update hunter
        hunter.update(0.016f, player, map);
        check("animator updates during patrol/chase", hunter.animator.currentPose != null);

        // Defeat hunter
        hunter.health = 0f;
        hunter.state = HunterEnemy.State.DEFEATED;
        hunter.update(0.016f, player, map);
        check("animator enters DEAD state on defeat", hunter.animator.currentState == Player.ParkourState.DEAD);
    }

    private static void testOpenWorldRosterAndQueries() {
        System.out.println("OpenWorld: NPC roster & nearest query");
        OpenWorld world = new OpenWorld();

        check("civilians roster populated (>= 6)", world.civilians.size() >= 6);
        check("hostiles roster populated (>= 8)", world.hostiles.size() >= 8);

        // Query nearest near Maya (146, 18)
        CivilianNPC near = world.nearestCivilian(146f, 18.5f, 3.0f);
        check("nearest query finds Maya near (146, 18)", near != null && "Maya".equals(near.name));

        // Query far away
        CivilianNPC none = world.nearestCivilian(0f, 0f, 2.0f);
        check("nearest query in empty area returns null", none == null);
    }
}
