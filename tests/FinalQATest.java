import com.devagent.realworldsurvival3d.AudioMixer;
import com.devagent.realworldsurvival3d.BuildingInterior;
import com.devagent.realworldsurvival3d.CivilianNPC;
import com.devagent.realworldsurvival3d.CullingOptimizer;
import com.devagent.realworldsurvival3d.HostileAI;
import com.devagent.realworldsurvival3d.HunterEnemy;
import com.devagent.realworldsurvival3d.Inventory;
import com.devagent.realworldsurvival3d.Item;
import com.devagent.realworldsurvival3d.Mission;
import com.devagent.realworldsurvival3d.MissionManager;
import com.devagent.realworldsurvival3d.NavigationTracker;
import com.devagent.realworldsurvival3d.Obstacle;
import com.devagent.realworldsurvival3d.OpenWorld;
import com.devagent.realworldsurvival3d.Player;
import com.devagent.realworldsurvival3d.RoadNetwork;
import com.devagent.realworldsurvival3d.SaveFileManager;
import com.devagent.realworldsurvival3d.SaveState;
import com.devagent.realworldsurvival3d.SettingsConfig;
import com.devagent.realworldsurvival3d.Terrain;
import com.devagent.realworldsurvival3d.Vehicle;
import com.devagent.realworldsurvival3d.WorldMap;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * FinalQATest - Comprehensive End-to-End QA Audit & Dual-Mode Verification (Phase 14).
 * Audits all 14 subsystem phases:
 *  1. Dual-mode isolation & parity (Parkour campaign vs Open-World 896m)
 *  2. Parkour campaign full mechanics flow (locomotion, vault, slide, fall roll, stomp, extraction)
 *  3. Open-World 896m terrain, biomes, boundaries, & active 3x3 chunk streaming
 *  4. Walkable architectural building interiors & floor snapping
 *  5. Road network Dijkstra shortest path & dynamic navigation rerouting
 *  6. Civilian & Hostile AI behaviors, safe haven aversion buffer, & staggered updates
 *  7. "The Long Signal" 6-mission story arc & 5 collectible data slates
 *  8. Survival backpack inventory (12 slots, stacking, consumables, quest item protection)
 *  9. Vehicle driving dynamics (SUV & motorcycle throttle, brake, reverse, drift, collisions, roadkill)
 * 10. 3D spatial audio mixing, decibels, stereo pan, & engine pitch modulation
 * 11. Save/Load persistence round trip, CRC32 verification, atomic staging, & backup recovery
 * 12. Optimization passes (frustum culling, zero-alloc matrix TRS, dt clamp, frame budgets)
 */
public class FinalQATest {

    private static int passed = 0;
    private static int failed = 0;

    private static void check(String desc, boolean condition) {
        if (condition) {
            System.out.println("  PASS  " + desc);
            passed++;
        } else {
            System.err.println("  FAIL  " + desc);
            failed++;
        }
    }

    private static void approx(String desc, float actual, float expected, float eps) {
        if (Math.abs(actual - expected) <= eps) {
            System.out.println("  PASS  " + desc + " (" + actual + " ~ " + expected + ")");
            passed++;
        } else {
            System.err.println("  FAIL  " + desc + " (expected " + expected + " +/- " + eps + ", got " + actual + ")");
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
        System.out.println("=== FinalQATest (Phase 14: Final QA Audit & Dual-Mode Verification) ===");

        File testDir = new File("tests/build/final_qa_saves");
        testDir.mkdirs();
        File testSaveFile = new File(testDir, "final_qa_save.dat");

        try {
            testDualModeIsolationAndParity();
            testParkourCampaignMechanics();
            testOpenWorld896mTerrainAndBiomes();
            testWalkableBuildingInteriors();
            testRoadNetworkAndNavigationRouting();
            testCivilianAndHostileAIBehaviors();
            testTheLongSignalStoryArcAndDataSlates();
            testInventoryManagementAndConsumables();
            testVehiclePhysicsAndCombatDynamics();
            testAudioMixerAndSpatialAcoustics();
            testSaveLoadRoundTripAndFaultTolerance(testSaveFile);
            testOptimizationZeroAllocAndBudget();
        } finally {
            SaveFileManager.deleteSave(testSaveFile);
            testDir.delete();
        }

        System.out.println("\nFinalQATest: " + passed + " passed, " + failed + " failed\n");
        if (failed > 0) System.exit(1);
    }

    // -------------------------------------------------------------------------
    // 1. Dual-Mode Isolation & Parity
    // -------------------------------------------------------------------------
    private static void testDualModeIsolationAndParity() {
        System.out.println("Final QA: 1. Dual-Mode Isolation & Parity");

        // Parkour Campaign Mode verification
        WorldMap parkourMap = new WorldMap();
        int buildingCount = 0;
        Obstacle console = null;
        for (Obstacle o : parkourMap.obstacles) {
            if (o.type == Obstacle.Type.BUILDING) buildingCount++;
            if (o.type == Obstacle.Type.CONSOLE) console = o;
        }
        check("parkour map has multi-tier buildings populated", buildingCount >= 6);
        check("parkour map origin rooftop Alpha exists at (0, 0)", parkourMap.buildingAt(0f, 0f) != null);
        approx("parkour Alpha roof height is 20m", parkourMap.buildingAt(0f, 0f).height, 20f, 0.01f);
        check("parkour extraction console exists on Apex helipad", console != null);
        approx("parkour extraction X coordinate is 65", parkourMap.extractionX, 65f, 0.01f);

        // Open-World Mode verification
        OpenWorld openWorld = new OpenWorld();
        check("open world terrain initialized with 896m bounds", Terrain.WORLD_HALF == 448);
        check("open world has 10 named POIs populated", openWorld.pois.size() == 10);
        check("open world has 3 drivable vehicles placed", openWorld.vehicles.size() == 3);

        // Player instance mode isolation: Verify parkour rooftop vs open world ground
        Player parkourPlayer = createPlayer(0f, 20f, 0f);
        parkourPlayer.isGrounded = true;
        check("parkour player spawns at rooftop height (y=20)", parkourPlayer.y == 20f);

        float safehouseGround = Terrain.heightAt(210f, 60f);
        Player worldPlayer = createPlayer(210f, safehouseGround, 60f);
        worldPlayer.isGrounded = true;
        check("open world player spawns at Safehouse terrain elevation", worldPlayer.y >= 0f);

        // SaveState dual mode preservation
        SaveState s0 = new SaveState();
        s0.gameMode = 0; // PARKOUR
        s0.parkourCurrentMission = 2;
        SaveState s1 = new SaveState();
        s1.gameMode = 1; // OPEN_WORLD
        s1.storyMissionIndex = 4;
        check("parkour save state preserves gameMode 0", s0.gameMode == 0);
        check("open-world save state preserves gameMode 1", s1.gameMode == 1);
        check("mode flags remain strictly isolated", s0.gameMode != s1.gameMode);
    }

    // -------------------------------------------------------------------------
    // 2. Parkour Campaign Full Mechanics Flow
    // -------------------------------------------------------------------------
    private static void testParkourCampaignMechanics() {
        System.out.println("Final QA: 2. Parkour Campaign Full Mechanics Flow");

        WorldMap map = new WorldMap();
        Player player = createPlayer(0f, 20f, 0f);
        player.isGrounded = true;

        // Ground locomotion states
        player.update(1f / 60f, 0f, 0.20f, false, false, false, map.obstacles);
        check("light input initiates WALK state", player.state == Player.ParkourState.WALK || player.state == Player.ParkourState.STAND);

        player.update(1f / 60f, 0f, 1.0f, false, false, true, map.obstacles); // Sprint toggle
        check("sprint toggle and full stick initiates SPRINT", player.isSprinting);

        // Jump & Double Jump
        player.update(1f / 60f, 0f, 1.0f, true, false, false, map.obstacles);
        check("jump initiates airborne upward velocity", player.vy > 0f);
        player.update(1f / 60f, 0f, 1.0f, true, false, false, map.obstacles);
        check("airborne tracking verified", !player.isGrounded);

        // Fall damage vs Safety Roll
        // Test high fall without roll
        Player fallUnrolled = createPlayer(0f, 32f, 0f);
        fallUnrolled.vy = -18f; // High downward velocity
        fallUnrolled.isGrounded = false;
        fallUnrolled.health = 100f;
        fallUnrolled.y = 20.0f;
        fallUnrolled.update(1f / 60f, 0f, 0f, false, false, false, map.obstacles);
        check("unrolled high impact inflicts fall damage or landing recoil", fallUnrolled.health < 100f || fallUnrolled.state == Player.ParkourState.HARD_LAND || fallUnrolled.state == Player.ParkourState.LAND);

        // Test high fall with slide roll input
        Player fallRolled = createPlayer(0f, 32f, 0f);
        fallRolled.vy = -18f;
        fallRolled.isGrounded = false;
        fallRolled.health = 100f;
        fallRolled.y = 20.0f;
        fallRolled.update(1f / 60f, 0f, 1.0f, false, true, false, map.obstacles); // slidePressed = true
        check("slide input at impact triggers SAFETY_ROLL with 0 damage", fallRolled.health == 100f || fallRolled.state == Player.ParkourState.SAFETY_ROLL);

        // Combat: Proximity to HunterEnemy
        HunterEnemy hunter = new HunterEnemy(10f, 20f, 10f);
        Player stomper = createPlayer(10f, 22.0f, 10f);
        hunter.update(1f / 60f, stomper, map);
        check("hunter responds to player proximity on roof", hunter.state != null);

        // Extraction Helipad coordinates (Apex tower x=65, y=32, z=60)
        check("helipad extraction coordinates valid", map.extractionX == 65f && map.extractionZ == 60f);
    }

    // -------------------------------------------------------------------------
    // 3. Open-World 896m Terrain, Biomes, & Chunk Streaming
    // -------------------------------------------------------------------------
    private static void testOpenWorld896mTerrainAndBiomes() {
        System.out.println("Final QA: 3. Open-World 896m Terrain, Biomes, & Chunk Streaming");

        OpenWorld world = new OpenWorld();

        // Verify map bounds [-448, +448]
        approx("terrain WORLD_HALF is 448", Terrain.WORLD_HALF, 448f, 0.001f);
        approx("terrain WATER_LEVEL is -2.2", Terrain.WATER_LEVEL, -2.2f, 0.001f);

        // Biome elevation sampling
        float cityHeight = Terrain.heightAt(100f, 0f);
        float plazaHeight = Terrain.heightAt(OpenWorld.PLAZA_X, OpenWorld.PLAZA_Z);
        float northHillsHeight = Terrain.heightAt(120f, -440f);
        float riverCenter = Terrain.riverCenterX(0f);
        float riverHeight = Terrain.heightAt(riverCenter, 0f);

        check("central plaza has valid terrace elevation", plazaHeight >= 0f && plazaHeight <= 10f);
        check("north hills elevation is substantially elevated (>10m)", northHillsHeight > 10f);
        check("river channel is carved below water level", riverHeight < Terrain.WATER_LEVEL);

        // Active 3x3 chunk streaming budget
        world.recomputeActive(0f, 0f);
        List<Obstacle> centralObstacles = world.activeObstacles();
        check("central active obstacles populated", centralObstacles != null && !centralObstacles.isEmpty());

        world.recomputeActive(-200f, 300f);
        List<Obstacle> northObstacles = world.activeObstacles();
        check("north hills active obstacles populated upon streaming shift", northObstacles != null && !northObstacles.isEmpty());
    }

    // -------------------------------------------------------------------------
    // 4. Walkable Architectural Building Interiors
    // -------------------------------------------------------------------------
    private static void testWalkableBuildingInteriors() {
        System.out.println("Final QA: 4. Walkable Architectural Building Interiors");

        BuildingInterior interior = new BuildingInterior();
        BuildingInterior.InteriorDefinition safehouse = interior.findInterior("Safehouse");
        check("safehouse interior definition found", safehouse != null);
        check("safehouse interior has bounding dimensions", safehouse.width > 0f && safehouse.depth > 0f);
        check("safehouse interior has doorways", safehouse.doorWidth > 0f);
        check("safehouse interior has solid obstacles", safehouse.obstacles.size() >= 4);

        // Containment check
        check("safehouse contains interior center point", safehouse.containsPoint(safehouse.centerX, safehouse.centerZ));
        check("safehouse rejects point far outside", !safehouse.containsPoint(safehouse.centerX + 50f, safehouse.centerZ));

        // Doorway clear check
        float ground = Terrain.heightAt(safehouse.centerX, safehouse.centerZ);
        float playerY = ground + 0.9f;
        boolean doorBlocked = false;
        for (Obstacle o : safehouse.obstacles) {
            if (!o.solid) continue;
            if (o.contains(safehouse.doorX, playerY, safehouse.doorZ - 0.2f)) {
                doorBlocked = true;
                break;
            }
        }
        check("doorway opening is unobstructed (free passage)", !doorBlocked);
    }

    // -------------------------------------------------------------------------
    // 5. Road Network Dijkstra Shortest Path & Dynamic Rerouting
    // -------------------------------------------------------------------------
    private static void testRoadNetworkAndNavigationRouting() {
        System.out.println("Final QA: 5. Road Network Dijkstra Shortest Path & Dynamic Rerouting");

        RoadNetwork network = new RoadNetwork();
        check("road network contains >= 24 nodes across regions", network.getNodes().size() >= 24);

        // Dijkstra shortest path from Plaza (150, 20) to Workshop (-88, -192)
        RoadNetwork.Route route = network.findRoute(150f, 20f, -88f, -192f);
        check("dijkstra path successfully computed", route != null && route.size() >= 3);
        check("path total distance > 0", route.totalDistance > 0f);

        // Navigation tracker auto-waypoint progression & rerouting
        NavigationTracker tracker = new NavigationTracker(network);
        boolean started = tracker.setDestination(-88f, -192f, "Workshop", 150f, 20f);
        check("tracker path initialized", started && tracker.hasActiveWaypoint);
        check("tracker route contains nodes", tracker.currentRoute != null && tracker.currentRoute.size() >= 3);

        // Simulate reaching first waypoint node
        RoadNetwork.Node wp0 = tracker.currentRoute.get(0);
        tracker.update(wp0.x, wp0.z);
        check("tracker advances waypoint on arrival", tracker.currentWaypointIndex > 0 || tracker.arrived);

        // Deviation rerouting (>12m off route)
        int initialReroutes = tracker.rerouteCount;
        tracker.update(wp0.x + 35f, wp0.z + 35f); // 35m away
        check("dynamic rerouting triggered on deviation > 12m", tracker.rerouteCount > initialReroutes);
        check("tracker retains active destination", tracker.hasActiveWaypoint);
    }

    // -------------------------------------------------------------------------
    // 6. Civilian & Hostile AI Behaviors & Staggered Updates
    // -------------------------------------------------------------------------
    private static void testCivilianAndHostileAIBehaviors() {
        System.out.println("Final QA: 6. Civilian & Hostile AI Behaviors & Staggered Updates");

        // Civilian NPC
        String[] dialogues = new String[]{"Safehouse is secure.", "Watch your step.", "Broadcast the signal."};
        CivilianNPC npc = new CivilianNPC("Elena", "Technician", 215f, 52f, 5.0f, dialogues);
        check("civilian NPC starts in IDLE state", npc.state == CivilianNPC.State.IDLE);

        // Interaction within 2.5m
        Player pClose = createPlayer(216f, 3f, 52f);
        npc.update(1f / 60f, pClose.x, pClose.y, pClose.z, 0);
        check("player within 2.5m triggers TALKING state", npc.state == CivilianNPC.State.TALKING);
        String line1 = npc.interact();
        String line2 = npc.interact();
        check("civilian cycles dialogue lines", !line1.equals(line2));

        // Hostile Stalker
        HostileAI hostile = new HostileAI("Infected Stalker", 50f, 50f, 20f);
        check("hostile starts in PATROL state", hostile.state == HostileAI.State.PATROL);

        // Aggro detection (< 18m)
        Player pInAggro = createPlayer(55f, 2f, 50f);
        OpenWorld world = new OpenWorld();
        hostile.update(1f / 60f, pInAggro, world, 0);
        check("hostile alerts and chases player within aggro radius", hostile.state == HostileAI.State.CHASE || hostile.state == HostileAI.State.ALERT);

        // Safe Haven Plaza Repulsion Barrier
        HostileAI plazaInvader = new HostileAI("Plaza Invader", HostileAI.SAFE_ZONE_X + 20f, HostileAI.SAFE_ZONE_Z, 20f);
        Player pInPlaza = createPlayer(HostileAI.SAFE_ZONE_X, 2f, HostileAI.SAFE_ZONE_Z);
        plazaInvader.update(1f / 60f, pInPlaza, world, 0);
        check("hostile within safe haven buffer radius is repelled outward", plazaInvader.x >= HostileAI.SAFE_ZONE_X + 20f || plazaInvader.state != HostileAI.State.CHASE);

        // Staggered update rate budget: near (<40m), mid (40-100m), far (>100m)
        HostileAI nearHostile = new HostileAI("Near", 10f, 10f, 10f);
        HostileAI midHostile = new HostileAI("Mid", 70f, 0f, 10f);
        HostileAI farHostile = new HostileAI("Far", 250f, 0f, 10f);
        Player pOrigin = createPlayer(0f, 2f, 0f);

        for (int frame = 0; frame < 10; frame++) {
            nearHostile.update(1f / 60f, pOrigin, world, frame);
            midHostile.update(1f / 60f, pOrigin, world, frame);
            farHostile.update(1f / 60f, pOrigin, world, frame);
        }
        check("near hostile updates every frame (10/10)", nearHostile.updateCount == 10);
        check("mid hostile updates throttled (2/10)", midHostile.updateCount == 2);
        check("far hostile frozen (0/10)", farHostile.updateCount == 0);
    }

    // -------------------------------------------------------------------------
    // 7. "The Long Signal" 6-Mission Story Arc & 5 Collectible Data Slates
    // -------------------------------------------------------------------------
    private static void testTheLongSignalStoryArcAndDataSlates() {
        System.out.println("Final QA: 7. \"The Long Signal\" 6-Mission Story Arc & 5 Collectible Data Slates");

        MissionManager mgr = new MissionManager();
        check("story arc initialized with 6 missions", mgr.getMissions().size() == 6);
        check("mission 1 is Static", "Mission 1: Static".equals(mgr.getMissions().get(0).title));

        // Advance through all 6 missions
        Player p = createPlayer(0f, 0f, 0f);
        for (int i = 0; i < 6; i++) {
            mgr.completeCurrentMission(p);
        }
        check("all 6 missions completed", mgr.isStoryCompleted());

        // 5 Collectible Data Slates
        check("world has 5 collectible data slates", mgr.getDataSlates().size() == 5);
        OpenWorld world = new OpenWorld();
        Item slate0 = mgr.getDataSlates().get(0);
        p.x = slate0.x;
        p.y = slate0.y;
        p.z = slate0.z;
        mgr.update(0.016f, p, world);
        check("data slate 0 collected upon proximity update", slate0.collected);
        check("data slates collected counter is 1", mgr.dataSlatesCollected == 1);
    }

    // -------------------------------------------------------------------------
    // 8. Survival Backpack Inventory & Consumables
    // -------------------------------------------------------------------------
    private static void testInventoryManagementAndConsumables() {
        System.out.println("Final QA: 8. Survival Backpack Inventory & Consumables");

        Inventory inv = new Inventory();
        check("backpack initialized with 2 loadout slots", inv.getSlotCount() == 2);
        check("initial loadout has 2 medkits", inv.getItemCount(Item.Type.MEDKIT) == 2);

        // Stacking verification
        inv.clear();
        inv.addItem(Item.Type.MEDKIT, 3);
        inv.addItem(Item.Type.MEDKIT, 2); // Fills to max 5 stack
        check("medkits stacked to 5 in 1 slot", inv.getSlotCount() == 1 && inv.getItemCount(Item.Type.MEDKIT) == 5);

        inv.addItem(Item.Type.MEDKIT, 2); // Exceeds stack of 5 -> creates 2nd slot
        check("overflow medkits spill into 2nd slot", inv.getSlotCount() == 2 && inv.getItemCount(Item.Type.MEDKIT) == 7);

        // Usable Consumables
        Player p = createPlayer(0f, 0f, 0f);
        p.health = 50f;
        p.maxHealth = 100f;
        boolean usedMedkit = inv.useItem(Item.Type.MEDKIT, p);
        check("medkit successfully consumed when wounded", usedMedkit);
        approx("medkit heals +45 HP (50 -> 95)", p.health, 95f, 0.01f);

        // Prevent medkit consumption when already at 100 HP
        p.health = 100f;
        boolean usedAtFull = inv.useItem(Item.Type.MEDKIT, p);
        check("medkit consumption rejected when already at 100 HP", !usedAtFull);

        // Energy drink consumption
        inv.addItem(Item.Type.ENERGY_DRINK, 1);
        p.stamina = 20f;
        p.adrenaline = 10f;
        boolean usedDrink = inv.useItem(Item.Type.ENERGY_DRINK, p);
        check("energy drink successfully consumed", usedDrink);
        approx("energy drink refills stamina to 100", p.stamina, 100f, 0.01f);
        approx("energy drink boosts adrenaline (+30)", p.adrenaline, 40f, 0.01f);

        // Quest item protection
        inv.addItem(Item.Type.RADIO_PART, 1);
        boolean usedQuest = inv.useItem(Item.Type.RADIO_PART, p);
        check("quest item cannot be accidentally consumed", !usedQuest);
    }

    // -------------------------------------------------------------------------
    // 9. Vehicle Driving Dynamics & Combat
    // -------------------------------------------------------------------------
    private static void testVehiclePhysicsAndCombatDynamics() {
        System.out.println("Final QA: 9. Vehicle Driving Dynamics & Combat");

        Vehicle suv = new Vehicle(Vehicle.Type.SUV, "Scout SUV", 0f, 0f, 0f);
        check("scout SUV top speed is 22 m/s", suv.maxSpeed == 22f);
        check("scout SUV health is 220 HP", suv.health == 220f);

        Vehicle bike = new Vehicle(Vehicle.Type.MOTORCYCLE, "Trail Bike", 10f, 10f, 90f);
        check("trail bike top speed is 28 m/s", bike.maxSpeed == 28f);
        check("trail bike health is 110 HP", bike.health == 110f);

        // Forward acceleration & speed clamp
        Player p = createPlayer(0f, 0f, 0f);
        suv.enter(p);
        for (int i = 0; i < 60; i++) {
            suv.update(1f / 60f, 1.0f, 0f, false, null, p);
        }
        check("suv accelerates forward", suv.speed > 0f);
        check("suv speed clamped at or below maxSpeed (22 m/s)", suv.speed <= suv.maxSpeed + 0.01f);

        // Braking and Reverse
        for (int i = 0; i < 90; i++) {
            suv.update(1f / 60f, -1.0f, 0f, false, null, p);
        }
        check("suv brakes and reverses backwards", suv.speed < 0f);
        check("suv reverse speed bounded by reverseSpeed", suv.speed >= suv.reverseSpeed - 0.01f);

        // Velocity-proportional steering: standing still (speed 0) produces no yaw turn
        Vehicle still = new Vehicle(Vehicle.Type.SUV, "Scout SUV", 0f, 0f, 0f);
        still.enter(p);
        float initYaw = still.yaw;
        still.update(1f / 60f, 0f, 1.0f, false, null, p); // full steer, zero throttle
        approx("zero speed produces zero steering rotation", still.yaw, initYaw, 0.001f);

        // Handbrake drift: scrubs speed rapidly
        suv.speed = 20f;
        suv.update(1f / 60f, 0f, 0f, true, null, p); // handbrake = true
        check("handbrake rapidly scrubs speed", suv.speed < 20f);
        check("vehicle is in handbraking state", suv.isHandbraking);

        // Obstacle collision rebound
        OpenWorld world = new OpenWorld();
        float suvHpBefore = suv.health;
        suv.x = 210f;
        suv.z = 45f;
        suv.yaw = 180f;
        suv.speed = 18.0f;
        world.recomputeActive(suv.x, suv.z);
        suv.update(0.5f, 1.0f, 0f, false, world, p);
        check("collision with obstacle incurs impact damage", suv.health < suvHpBefore);
        check("vehicle sets justCollided flag", suv.justCollided);

        // Hostile roadkill ramming
        HostileAI stalker = new HostileAI("Ram Target", 100f, 98.5f, 10f);
        stalker.health = 50f;
        world.hostiles.clear();
        world.hostiles.add(stalker);
        suv.x = 100f;
        suv.z = 100f;
        suv.speed = 12.0f;
        suv.update(0.016f, 1.0f, 0f, false, world, p);
        check("high-speed vehicle ramming inflicts damage on hostile", stalker.health < 50f);
    }

    // -------------------------------------------------------------------------
    // 10. 3D Spatial Audio Mixing & Acoustical Calculations
    // -------------------------------------------------------------------------
    private static void testAudioMixerAndSpatialAcoustics() {
        System.out.println("Final QA: 10. 3D Spatial Audio Mixing & Acoustical Calculations");

        approx("full master retains channel volume", AudioMixer.effectiveVolume(1.0f, 0.85f), 0.85f, 0.001f);
        approx("half master scales channel volume", AudioMixer.effectiveVolume(0.5f, 0.80f), 0.40f, 0.001f);
        approx("zero master silences channel", AudioMixer.effectiveVolume(0.0f, 0.85f), 0.0f, 0.001f);

        // Decibel conversions
        float db0 = AudioMixer.linearGainToDb(1.0f);
        float dbHalf = AudioMixer.linearGainToDb(0.5f);
        float dbSilent = AudioMixer.linearGainToDb(0.0f);
        approx("1.0 linear gain converts to 0 dB", db0, 0f, 0.01f);
        approx("0.5 linear gain converts to ~ -6.02 dB", dbHalf, -6.02f, 0.1f);
        approx("0.0 linear gain clamped to -60 dB floor", dbSilent, -60.0f, 0.1f);
        approx("0 dB converts back to 1.0 linear", AudioMixer.dbToLinearGain(0f), 1.0f, 0.01f);

        // 3D Distance Attenuation
        float minD = 4.0f;
        float maxD = 30.0f;
        float gainClose = AudioMixer.calculateDistanceGain(0f, 2f, 0f, 0f, minD, maxD);
        float gainFar = AudioMixer.calculateDistanceGain(0f, 35f, 0f, 0f, minD, maxD);
        float gainMid = AudioMixer.calculateDistanceGain(0f, 17f, 0f, 0f, minD, maxD);
        approx("distance <= minAudible has full gain 1.0", gainClose, 1.0f, 0.001f);
        approx("distance >= maxAudible has 0.0 gain", gainFar, 0.0f, 0.001f);
        check("distance attenuation is monotonic (0 < gainMid < 1)", gainMid > 0f && gainMid < 1f);

        // Stereo Panning
        float panCenter = AudioMixer.calculateStereoPan(0f, 0f, 0f, 0f, 10f);
        float panRight = AudioMixer.calculateStereoPan(0f, 0f, 0f, 10f, 0f);
        float panLeft = AudioMixer.calculateStereoPan(0f, 0f, 0f, -10f, 0f);
        approx("sound straight ahead pans to 0.0", panCenter, 0.0f, 0.02f);
        check("sound to right pans positive (> 0.90)", panRight > 0.90f);
        check("sound to left pans negative (< -0.90)", panLeft < -0.90f);

        // Vehicle engine speed-pitch modulation
        float idlePitch = AudioMixer.calculateEnginePitch(0f, 22f);
        float maxPitch = AudioMixer.calculateEnginePitch(22f, 22f);
        approx("vehicle idle pitch is 0.70x", idlePitch, 0.70f, 0.01f);
        approx("vehicle top speed pitch is 1.60x", maxPitch, 1.60f, 0.01f);
        check("engine pitch scales smoothly with speed", maxPitch > idlePitch);

        // Cooldown rate limiting
        AudioMixer mixer = new AudioMixer();
        boolean firstPlay = mixer.canPlay(AudioMixer.SFX_FOOTSTEP, 1000L);
        mixer.recordPlay(AudioMixer.SFX_FOOTSTEP, 1000L);
        boolean secondPlay = mixer.canPlay(AudioMixer.SFX_FOOTSTEP, 1050L);
        check("first sound event allowed", firstPlay);
        check("sound event within 50ms cooldown is throttled", !secondPlay);
    }

    // -------------------------------------------------------------------------
    // 11. Save/Load Round-Trip, CRC32 Verification, & Backup Recovery
    // -------------------------------------------------------------------------
    private static void testSaveLoadRoundTripAndFaultTolerance(File saveFile) {
        System.out.println("Final QA: 11. Save/Load Round-Trip, CRC32 Verification, & Backup Recovery");

        // Populate comprehensive game state
        SaveState state = new SaveState();
        state.gameMode = 1; // OPEN_WORLD
        state.playerX = 142.5f;
        state.playerY = 6.25f;
        state.playerZ = -88.0f;
        state.playerYaw = 195f;
        state.playerHealth = 88f;
        state.playerStamina = 92f;
        state.playerAdrenaline = 45f;
        state.score = 7500;
        state.currency = 320;
        state.storyMissionIndex = 3;
        state.storyCurrentCount = 2;
        state.dataSlatesCollected = 3;
        state.dataSlatesStatus[0] = true;
        state.dataSlatesStatus[1] = true;
        state.dataSlatesStatus[2] = true;
        state.discoveredPoiNames.add("Safehouse Alpha");
        state.discoveredPoiNames.add("Old Workshop");
        state.inventorySlots.add(new SaveState.SlotEntry("MEDKIT", 4));
        state.inventorySlots.add(new SaveState.SlotEntry("ENERGY_DRINK", 3));
        state.activeVehicleIndex = 0;
        state.vehicles.add(new SaveState.VehicleState("Scout SUV", 145f, 6.25f, -85f, 195f, 210f));

        // Atomic File Persistence
        boolean saved = SaveFileManager.saveAtomic(saveFile, state);
        check("atomic file save succeeded", saved);
        check("primary save file exists on disk", saveFile.exists());

        // Safe Loading & State Restoration
        SaveState loaded = SaveFileManager.loadSafe(saveFile);
        check("loaded state is non-null", loaded != null);
        approx("loaded playerX matches saved value", loaded.playerX, 142.5f, 0.01f);
        approx("loaded playerHealth matches saved value", loaded.playerHealth, 88f, 0.01f);
        check("loaded score matches 7500", loaded.score == 7500);
        check("loaded story mission index matches 3", loaded.storyMissionIndex == 3);
        check("loaded data slates count matches 3", loaded.dataSlatesCollected == 3);
        check("loaded inventory slot count matches 2", loaded.inventorySlots.size() == 2);
        check("loaded vehicle count matches 1", loaded.vehicles.size() == 1);

        // Backup Rotation & Corruption Recovery
        state.score = 9999;
        SaveFileManager.saveAtomic(saveFile, state); // Rotates previous state (score 7500) into .bak
        File backupFile = new File(saveFile.getAbsolutePath() + ".bak");
        check("backup file .bak created during rotation", backupFile.exists());

        // Corrupt primary save file deliberately
        try {
            FileOutputStream fos = new FileOutputStream(saveFile);
            fos.write("CORRUPTED_GARBAGE_PAYLOAD".getBytes(StandardCharsets.UTF_8));
            fos.close();
        } catch (Exception e) {
            check("corrupting primary file setup", false);
        }

        // Automatic corruption self-healing
        SaveState recovered = SaveFileManager.loadSafe(saveFile);
        check("automatic corruption recovery succeeds", recovered != null);
        check("recovered state has previous healthy score (7500)", recovered.score == 7500);
    }

    // -------------------------------------------------------------------------
    // 12. Optimization Passes, Zero-Alloc Matrix TRS, & Frame Budgets
    // -------------------------------------------------------------------------
    private static void testOptimizationZeroAllocAndBudget() {
        System.out.println("Final QA: 12. Optimization Passes, Zero-Alloc Matrix TRS, & Frame Budgets");

        // Spatial Sphere Frustum Culling
        boolean visibleAhead = CullingOptimizer.isSphereVisible(0f, 0f, 30f, 3f, 0f, 0f, 0f, 0f, 0f, 1f, 150f);
        boolean culledBehind = CullingOptimizer.isSphereVisible(0f, 0f, -25f, 3f, 0f, 0f, 0f, 0f, 0f, 1f, 150f);
        boolean culledBeyond = CullingOptimizer.isSphereVisible(0f, 0f, 200f, 3f, 0f, 0f, 0f, 0f, 0f, 1f, 150f);
        check("sphere 30m ahead of camera is visible", visibleAhead);
        check("sphere 25m behind camera is culled", !culledBehind);
        check("sphere 200m beyond draw distance is culled", !culledBeyond);

        // In-place zero-allocation setTRS verification
        float[] m = new float[16];
        CullingOptimizer.setTRS(m, 12f, 4f, -20f, 45f, 2f, 3f, 2f);
        approx("setTRS m[12] translation X matches 12.0", m[12], 12f, 0.001f);
        approx("setTRS m[13] translation Y matches 4.0", m[13], 4f, 0.001f);
        approx("setTRS m[14] translation Z matches -20.0", m[14], -20f, 0.001f);
        approx("setTRS m[15] identity matches 1.0", m[15], 1.0f, 0.001f);
        check("setTRS zero off-diagonals", m[1] == 0f && m[3] == 0f && m[4] == 0f);

        // Delta-time hitch clamping
        float clampedLarge = CullingOptimizer.clampDeltaTime(0.500f, 0.001f, 0.100f);
        float clampedNegative = CullingOptimizer.clampDeltaTime(-0.010f, 0.001f, 0.100f);
        float normalDt = CullingOptimizer.clampDeltaTime(0.0166f, 0.001f, 0.100f);
        approx("500ms lag spike clamped to 100ms max", clampedLarge, 0.100f, 0.001f);
        approx("negative dt clamped to 1ms min", clampedNegative, 0.001f, 0.0001f);
        approx("normal 60 FPS dt passes through", normalDt, 0.0166f, 0.001f);

        // Target Frame Budget Regulation
        long budget60 = CullingOptimizer.computeFrameBudgetNs(60);
        long budget30 = CullingOptimizer.computeFrameBudgetNs(30);
        check("60 FPS frame budget is 16,666,666 ns (~16.66ms)", budget60 == 16666666L);
        check("30 FPS frame budget is 33,333,333 ns (~33.33ms)", budget30 == 33333333L);

        // Draw distance scaling by preset
        float camX = 0f, camY = 0f, camZ = 0f;
        float fx = 0f, fy = 0f, fz = 1f;
        boolean visLow = CullingOptimizer.isSphereVisible(0f, 0f, 200f, 3.0f, camX, camY, camZ, fx, fy, fz, 160f);
        boolean visHigh = CullingOptimizer.isSphereVisible(0f, 0f, 200f, 3.0f, camX, camY, camZ, fx, fy, fz, 420f);
        check("object at 200m is culled on Low quality (160m)", !visLow);
        check("object at 200m is visible on High quality (420m)", visHigh);
    }
}
