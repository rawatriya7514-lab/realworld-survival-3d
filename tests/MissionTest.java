import com.devagent.realworldsurvival3d.CivilianNPC;
import com.devagent.realworldsurvival3d.HostileAI;
import com.devagent.realworldsurvival3d.Item;
import com.devagent.realworldsurvival3d.Mission;
import com.devagent.realworldsurvival3d.MissionManager;
import com.devagent.realworldsurvival3d.NavigationTracker;
import com.devagent.realworldsurvival3d.OpenWorld;
import com.devagent.realworldsurvival3d.Player;
import com.devagent.realworldsurvival3d.RoadNetwork;

import java.util.List;

/**
 * MissionTest - Pure JVM verification of Phase 9 story missions ("The Long Signal"):
 *  - 6-mission story arc sequence
 *  - Objective progression (locations, item salvage, NPC dialogue, elevation, wave defense, transmission)
 *  - Hidden Data Slate side collectibles
 *  - NavigationTracker auto-waypoint synchronization
 *  - Mission failure / retry state resilience
 */
public class MissionTest {

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
        testMissionInitialization();
        testMission1ReachLocation();
        testMission2CollectParts();
        testMission3TalkNpc();
        testMission4HighGround();
        testMission5SurviveWave();
        testMission6TransmitSignal();
        testDataSlatesCollectibles();
        testNavWaypointAutoSync();
        testMissionFailureRetry();

        System.out.println("\nMissionTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }

    private static void testMissionInitialization() {
        System.out.println("MissionManager: arc structure & initialization");
        MissionManager mgr = new MissionManager();
        List<Mission> missions = mgr.getMissions();

        check("contains exactly 6 story missions", missions.size() == 6);
        check("mission 1 is Static", "Mission 1: Static".equals(missions.get(0).title));
        check("mission 2 is Salvage", "Mission 2: Salvage".equals(missions.get(1).title));
        check("mission 3 is The Engineer", "Mission 3: The Engineer".equals(missions.get(2).title));
        check("mission 4 is High Ground", "Mission 4: High Ground".equals(missions.get(3).title));
        check("mission 5 is Nightfall", "Mission 5: Nightfall".equals(missions.get(4).title));
        check("mission 6 is The Long Signal", "Mission 6: The Long Signal".equals(missions.get(5).title));

        check("mission 1 is active initially", missions.get(0).isActive);
        check("mission 2 is inactive initially", !missions.get(1).isActive);
        check("story is not completed initially", !mgr.isStoryCompleted());
        check("starts at mission index 0", mgr.getCurrentMissionIndex() == 0);
    }

    private static void testMission1ReachLocation() {
        System.out.println("Mission 1: Static (Reach Safehouse)");
        MissionManager mgr = new MissionManager();
        Player player = createPlayer(150f, 0f, 20f); // Central Plaza
        OpenWorld world = new OpenWorld();

        mgr.update(0.016f, player, world);
        check("at Central Plaza, mission 1 remains active", mgr.getCurrentMissionIndex() == 0);

        // Move player to Safehouse (210, 60)
        player.x = 210f;
        player.z = 60f;
        int scoreBefore = player.score;
        int currBefore = player.currency;

        mgr.update(0.016f, player, world);
        check("reaching Safehouse advances to mission 2", mgr.getCurrentMissionIndex() == 1);
        check("mission 1 marked completed", mgr.getMissions().get(0).isCompleted);
        check("awards +250 score reward", player.score == scoreBefore + 250);
        check("awards +50 currency reward", player.currency == currBefore + 50);
        check("completion toast triggered", mgr.lastToast != null && mgr.lastToast.contains("Mission Complete"));
    }

    private static void testMission2CollectParts() {
        System.out.println("Mission 2: Salvage (Collect 4 Radio Parts)");
        MissionManager mgr = new MissionManager();
        Player player = createPlayer(210f, 0f, 60f);
        OpenWorld world = new OpenWorld();

        // Advance to mission 2
        mgr.completeCurrentMission(player);
        check("currently on mission 2", mgr.getCurrentMissionIndex() == 1);

        List<Item> parts = mgr.getMissionPickups();
        check("4 radio parts spawned", parts.size() == 4);

        // Collect part 1
        Item p1 = parts.get(0);
        player.x = p1.x;
        player.y = p1.y;
        player.z = p1.z;
        mgr.update(0.016f, player, world);
        check("part 1 collected", p1.collected);
        check("mission 2 progress: 1/4", mgr.getCurrentMission().currentCount == 1);

        // Collect parts 2, 3, 4
        for (int i = 1; i < 4; i++) {
            Item pi = parts.get(i);
            player.x = pi.x;
            player.y = pi.y;
            player.z = pi.z;
            mgr.update(0.016f, player, world);
        }
        check("all 4 parts collected advances to mission 3", mgr.getCurrentMissionIndex() == 2);
        check("mission 2 marked completed", mgr.getMissions().get(1).isCompleted);
    }

    private static void testMission3TalkNpc() {
        System.out.println("Mission 3: The Engineer (Dialogue with Jack)");
        MissionManager mgr = new MissionManager();
        Player player = createPlayer(0f, 0f, 0f);

        // Fast-forward to mission 3
        mgr.completeCurrentMission(player);
        mgr.completeCurrentMission(player);
        check("currently on mission 3", mgr.getCurrentMissionIndex() == 2);

        // Talk to wrong NPC (Maya)
        CivilianNPC maya = new CivilianNPC("Maya", "Scout", 146f, 18f, 5f, null);
        mgr.onPlayerTalk(maya);
        check("talking to Maya does not advance mission 3", mgr.getCurrentMissionIndex() == 2);

        // Talk to Jack
        CivilianNPC jack = new CivilianNPC("Jack", "Scavenger", 212f, 62f, 4f, null);
        mgr.onPlayerTalk(jack);
        check("talking to Jack completes mission 3", mgr.getMissions().get(2).isCompleted);
        check("advances to mission 4", mgr.getCurrentMissionIndex() == 3);
    }

    private static void testMission4HighGround() {
        System.out.println("Mission 4: High Ground (Watchtower Apex)");
        MissionManager mgr = new MissionManager();
        Player player = createPlayer(120f, 5f, -360f); // at Watchtower base (y=5 < 30)
        OpenWorld world = new OpenWorld();

        // Fast-forward to mission 4
        for (int i = 0; i < 3; i++) mgr.completeCurrentMission(player);
        check("currently on mission 4", mgr.getCurrentMissionIndex() == 3);

        mgr.update(0.016f, player, world);
        check("at base (y < 30m), mission 4 does not complete", mgr.getCurrentMissionIndex() == 3);

        // Climb to summit (y >= 30m)
        player.y = 35.0f;
        mgr.update(0.016f, player, world);
        check("climbing summit (y >= 30m) completes mission 4", mgr.getMissions().get(3).isCompleted);
        check("advances to mission 5", mgr.getCurrentMissionIndex() == 4);
    }

    private static void testMission5SurviveWave() {
        System.out.println("Mission 5: Nightfall (River Bridge Wave Defense)");
        MissionManager mgr = new MissionManager();
        Player player = createPlayer(0f, 0f, 0f);
        OpenWorld world = new OpenWorld();

        // Fast-forward to mission 5
        for (int i = 0; i < 4; i++) mgr.completeCurrentMission(player);
        check("currently on mission 5", mgr.getCurrentMissionIndex() == 4);

        // Player not at bridge -> timer does not tick
        float initialTimer = mgr.getCurrentMission().timer;
        mgr.update(1.0f, player, world);
        checkApprox("timer unchanged while away from bridge", mgr.getCurrentMission().timer, initialTimer, 0.01f);

        // Move to River Bridge (-155, 240)
        player.x = -155f;
        player.y = 1.4f;
        player.z = 240f;

        // Defeat 3 hostiles during defense
        HostileAI stalker = new HostileAI("Bridge Stalker", -155f, 240f, 10f);
        mgr.onHostileDefeated(stalker);
        mgr.onHostileDefeated(stalker);
        mgr.onHostileDefeated(stalker);

        mgr.update(0.016f, player, world);
        check("defeating 3 hostiles completes bridge defense", mgr.getMissions().get(4).isCompleted);
        check("advances to mission 6", mgr.getCurrentMissionIndex() == 5);
    }

    private static void testMission6TransmitSignal() {
        System.out.println("Mission 6: The Long Signal (Finale)");
        MissionManager mgr = new MissionManager();
        Player player = createPlayer(210f, 0f, 60f); // ground floor
        OpenWorld world = new OpenWorld();

        // Fast-forward to mission 6
        for (int i = 0; i < 5; i++) mgr.completeCurrentMission(player);
        check("currently on mission 6", mgr.getCurrentMissionIndex() == 5);

        mgr.update(0.016f, player, world);
        check("on ground floor, transmission not reached", !mgr.isStoryCompleted());

        // Climb to rooftop transmission console (y=6.3)
        player.y = 6.3f;
        mgr.update(0.016f, player, world);
        check("reaching rooftop console completes story", mgr.isStoryCompleted());
        check("HUD title shows Transmitted", mgr.getHudTitle().contains("Transmitted"));
    }

    private static void testDataSlatesCollectibles() {
        System.out.println("Data Slates: hidden collectibles");
        MissionManager mgr = new MissionManager();
        Player player = createPlayer(0f, 0f, 0f);
        OpenWorld world = new OpenWorld();

        List<Item> slates = mgr.getDataSlates();
        check("5 data slates placed across world", slates.size() == 5);

        // Collect slate 1 (Central Plaza rooftop 150, 14.5, 20)
        Item s1 = slates.get(0);
        player.x = s1.x;
        player.y = s1.y;
        player.z = s1.z;
        int scoreBefore = player.score;

        mgr.update(0.016f, player, world);
        check("slate collected", s1.collected);
        check("data slates collected counter is 1", mgr.dataSlatesCollected == 1);
        check("awards +150 score for hidden data slate", player.score == scoreBefore + 150);
        check("toast triggered with 1/5", mgr.lastToast != null && mgr.lastToast.contains("1/5"));
    }

    private static void testNavWaypointAutoSync() {
        System.out.println("NavigationTracker: auto-waypoint synchronization");
        MissionManager mgr = new MissionManager();
        RoadNetwork roads = new RoadNetwork();
        NavigationTracker tracker = new NavigationTracker(roads);

        check("tracker has no waypoint initially", !tracker.hasActiveWaypoint);

        // Auto-sync feeds Mission 1 Safehouse (210, 60)
        mgr.syncNavWaypoint(tracker);
        check("tracker received mission waypoint", tracker.hasActiveWaypoint);
        check("target matches Safehouse", tracker.destinationName.contains("Static") || tracker.destinationName.contains("Safehouse"));
        checkApprox("target X matches mission target", tracker.targetX, 210f, 0.1f);
        checkApprox("target Z matches mission target", tracker.targetZ, 60f, 0.1f);

        // Custom player waypoint is not overwritten
        tracker.setDestination("Custom Spot", 50f, 50f);
        mgr.syncNavWaypoint(tracker);
        check("custom player waypoint preserved", "Custom Spot".equals(tracker.destinationName));
    }

    private static void testMissionFailureRetry() {
        System.out.println("MissionManager: failure & retry resilience");
        MissionManager mgr = new MissionManager();
        Player player = createPlayer(210f, 0f, 60f);

        // Complete mission 1
        mgr.completeCurrentMission(player);
        check("on mission 2", mgr.getCurrentMissionIndex() == 1);

        // Partially collect parts
        mgr.getCurrentMission().currentCount = 2;

        // Player dies
        mgr.onPlayerDeath();
        check("still on mission 2 after death (story progress not lost)", mgr.getCurrentMissionIndex() == 1);
        check("current mission progress reset for retry", mgr.getCurrentMission().currentCount == 0);
        check("failure toast displayed", mgr.lastToast != null && mgr.lastToast.contains("Failed"));
    }
}
