import com.devagent.realworldsurvival3d.NavigationTracker;
import com.devagent.realworldsurvival3d.RoadNetwork;
import com.devagent.realworldsurvival3d.RoadNetwork.Node;
import com.devagent.realworldsurvival3d.RoadNetwork.Route;

/**
 * RoadNetworkTest - Comprehensive JVM test suite for RoadNetwork & NavigationTracker:
 *  - Graph structure, connectivity, and POI node presence
 *  - Nearest-node spatial queries
 *  - Shortest path (Dijkstra) routing across regions and river bridge
 *  - Blocked edges and unreachable destination handling
 *  - Step-by-step waypoint progression
 *  - Dynamic rerouting on corridor deviation (> 12.0m)
 *  - Arrival detection and remaining distance calculations
 */
public class RoadNetworkTest {
    private static int passed = 0;
    private static int failed = 0;

    private static void check(boolean condition, String description) {
        if (condition) {
            System.out.println("  PASS  " + description);
            passed++;
        } else {
            System.err.println("  FAIL  " + description);
            failed++;
        }
    }

    private static void checkFloat(float actual, float expected, float eps, String description) {
        if (Math.abs(actual - expected) <= eps) {
            System.out.println("  PASS  " + description + " (" + actual + " ~ " + expected + ")");
            passed++;
        } else {
            System.err.println("  FAIL  " + description + " (" + actual + " != " + expected + ", eps=" + eps + ")");
            failed++;
        }
    }

    public static void main(String[] args) {
        System.out.println("RoadNetwork: graph connectivity & POIs");
        testGraphStructure();

        System.out.println("RoadNetwork: nearest node queries");
        testNearestNodeQueries();

        System.out.println("RoadNetwork: Dijkstra routing across regions & bridge");
        testDijkstraRouting();

        System.out.println("RoadNetwork: blocked edges & unreachable handling");
        testBlockedEdges();

        System.out.println("NavigationTracker: waypoint progression & arrival");
        testWaypointProgression();

        System.out.println("NavigationTracker: dynamic rerouting (> 12m deviation)");
        testDynamicRerouting();

        System.out.println("\nRoadNetworkTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }

    private static void testGraphStructure() {
        RoadNetwork net = new RoadNetwork();
        check(net.getNodes().size() >= 24, "graph contains >= 24 nodes: " + net.getNodes().size());

        // Check key named nodes
        check(net.getNodeByName("Central Plaza") != null, "Central Plaza node exists");
        check(net.getNodeByName("River Bridge East") != null, "Bridge East node exists");
        check(net.getNodeByName("River Bridge West") != null, "Bridge West node exists");
        check(net.getNodeByName("Workshop") != null, "Workshop node exists");
        check(net.getNodeByName("Watchtower") != null, "Watchtower node exists");
        check(net.getNodeByName("Ranger Hut") != null, "Ranger Hut node exists");

        // Verify bridge spans the river
        Node bEast = net.getNodeByName("River Bridge East");
        Node bWest = net.getNodeByName("River Bridge West");
        boolean bridgeConnected = false;
        for (RoadNetwork.Edge e : bEast.edges) {
            if (e.target.id == bWest.id) bridgeConnected = true;
        }
        check(bridgeConnected, "River Bridge East directly connects to Bridge West across river");
    }

    private static void testNearestNodeQueries() {
        RoadNetwork net = new RoadNetwork();

        Node nPlaza = net.nearestNode(152f, 22f);
        check(nPlaza != null && "Central Plaza".equals(nPlaza.name),
                "nearest node to (152, 22) is Central Plaza: " + (nPlaza != null ? nPlaza.name : "null"));

        Node nWorkshop = net.nearestNode(-85f, -190f);
        check(nWorkshop != null && "Workshop".equals(nWorkshop.name),
                "nearest node to (-85, -190) is Workshop: " + (nWorkshop != null ? nWorkshop.name : "null"));

        Node nTower = net.nearestNode(125f, -355f);
        check(nTower != null && "Watchtower".equals(nTower.name),
                "nearest node to (125, -355) is Watchtower: " + (nTower != null ? nTower.name : "null"));
    }

    private static void testDijkstraRouting() {
        RoadNetwork net = new RoadNetwork();

        // Route within City: (48, -88) to (240, 104)
        Route cityRoute = net.findRoute(48f, -88f, 240f, 104f);
        check(cityRoute != null && cityRoute.size() >= 4, "city grid route found with >= 4 nodes: "
                + (cityRoute != null ? cityRoute.size() : 0));
        check(cityRoute.totalDistance > 0f, "city route total distance > 0: " + cityRoute.totalDistance);

        // Cross-river route: Workshop (-88, -192) to Riverside Village (-216, 240)
        Route crossRiver = net.findRoute(-88f, -192f, -216f, 240f);
        check(crossRiver != null, "cross-river route found between Workshop and Riverside Village");

        boolean crossedBridge = false;
        for (int i = 0; i < crossRiver.size(); i++) {
            if ("River Bridge East".equals(crossRiver.get(i).name) ||
                "River Bridge West".equals(crossRiver.get(i).name)) {
                crossedBridge = true;
            }
        }
        check(crossedBridge, "cross-river route correctly routes through River Bridge");
    }

    private static void testBlockedEdges() {
        RoadNetwork net = new RoadNetwork();
        Node bEast = net.getNodeByName("River Bridge East");
        Node bWest = net.getNodeByName("River Bridge West");

        // Verify reachable before blocking
        Route routeBefore = net.findRoute(-88f, -192f, -216f, 240f);
        check(routeBefore != null, "bridge route reachable before blocking");

        // Block bridge
        net.setEdgeBlocked(bEast.id, bWest.id, true);

        Route routeBlocked = net.findRoute(-88f, -192f, -216f, 240f);
        check(routeBlocked == null, "cross-river route is null when River Bridge is blocked (unreachable destination handled cleanly)");

        // Unblock bridge
        net.setEdgeBlocked(bEast.id, bWest.id, false);
        Route routeRestored = net.findRoute(-88f, -192f, -216f, 240f);
        check(routeRestored != null, "cross-river route restored after unblocking bridge");
    }

    private static void testWaypointProgression() {
        RoadNetwork net = new RoadNetwork();
        NavigationTracker tracker = new NavigationTracker(net);

        // Destination: Workshop (-88, -192) from City (48, -88)
        boolean started = tracker.setDestination(-88f, -192f, "Workshop", 48f, -88f);
        check(started, "setDestination returns true for valid path");
        check(tracker.hasActiveWaypoint, "hasActiveWaypoint is true");
        check(tracker.currentRoute.size() >= 3, "route has >= 3 nodes");

        int initialIndex = tracker.currentWaypointIndex;
        check(initialIndex == 0, "starts at waypoint index 0");

        // Move player close to first waypoint node
        RoadNetwork.Node wp0 = tracker.currentRoute.get(0);
        tracker.update(wp0.x, wp0.z);
        check(tracker.currentWaypointIndex > 0, "advances to next waypoint when reaching node within 8m: " + tracker.currentWaypointIndex);

        // Move player directly to final destination
        tracker.update(-88f, -192f);
        check(tracker.arrived, "arrives at destination within 5m");
        check(!tracker.hasActiveWaypoint, "active waypoint cleared upon arrival");
    }

    private static void testDynamicRerouting() {
        RoadNetwork net = new RoadNetwork();
        NavigationTracker tracker = new NavigationTracker(net);

        tracker.setDestination(240f, 104f, "City SE", 48f, -88f);
        check(tracker.hasActiveWaypoint, "tracker initialized with route");
        int initialReroutes = tracker.rerouteCount;

        // Player stays near route (2m off)
        RoadNetwork.Node wp0 = tracker.currentRoute.get(0);
        tracker.update(wp0.x + 2f, wp0.z);
        check(tracker.rerouteCount == initialReroutes, "no reroute when deviation <= 12m");

        // Player strays far away (35m off route)
        tracker.update(wp0.x + 35f, wp0.z + 35f);
        check(tracker.rerouteCount > initialReroutes, "dynamic rerouting triggered when deviation > 12m (reroutes=" + tracker.rerouteCount + ")");
        check(tracker.hasActiveWaypoint, "retains active waypoint after reroute");
        check(tracker.currentRoute != null, "new valid route computed from player position");
    }
}
