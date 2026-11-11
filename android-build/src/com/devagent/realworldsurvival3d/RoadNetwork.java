package com.devagent.realworldsurvival3d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * RoadNetwork - Pure Java road graph and BFS/Dijkstra routing engine.
 *
 * Implements Phase 6 World Map navigation:
 *  - Spatial road graph (intersections, POI access nodes, bridge corridors)
 *  - Weighted bidirectional edges with Euclidean distances and blocked edge state
 *  - Nearest node queries
 *  - Shortest-path routing (Dijkstra) with blocked edge avoidance and unreachable handling
 *  - Route distance computation and step-by-step waypoint progression
 */
public class RoadNetwork {

    public static class Node {
        public final int id;
        public final float x, z;
        public final String name;
        public final List<Edge> edges = new ArrayList<>();

        public Node(int id, float x, float z, String name) {
            this.id = id;
            this.x = x;
            this.z = z;
            this.name = name;
        }

        @Override
        public String toString() {
            return name != null ? name : ("Node-" + id + "(" + x + "," + z + ")");
        }
    }

    public static class Edge {
        public final Node target;
        public final float length;
        public boolean blocked;

        public Edge(Node target, float length) {
            this.target = target;
            this.length = length;
            this.blocked = false;
        }
    }

    public static class Route {
        public final List<Node> nodes;
        public final float totalDistance;

        public Route(List<Node> nodes, float totalDistance) {
            this.nodes = Collections.unmodifiableList(nodes);
            this.totalDistance = totalDistance;
        }

        public int size() {
            return nodes.size();
        }

        public Node get(int index) {
            return nodes.get(index);
        }

        public boolean isEmpty() {
            return nodes.isEmpty();
        }
    }

    private final List<Node> nodes = new ArrayList<>();
    private final Map<String, Node> namedNodes = new HashMap<>();

    public RoadNetwork() {
        buildDefaultGraph();
    }

    public Node addNode(float x, float z, String name) {
        int id = nodes.size();
        Node node = new Node(id, x, z, name);
        nodes.add(node);
        if (name != null) {
            namedNodes.put(name, node);
        }
        return node;
    }

    public void connect(int idA, int idB) {
        if (idA < 0 || idA >= nodes.size() || idB < 0 || idB >= nodes.size()) return;
        Node a = nodes.get(idA);
        Node b = nodes.get(idB);
        float dx = a.x - b.x;
        float dz = a.z - b.z;
        float dist = (float) Math.sqrt(dx * dx + dz * dz);

        a.edges.add(new Edge(b, dist));
        b.edges.add(new Edge(a, dist));
    }

    public void setEdgeBlocked(int idA, int idB, boolean blocked) {
        if (idA < 0 || idA >= nodes.size() || idB < 0 || idB >= nodes.size()) return;
        Node a = nodes.get(idA);
        Node b = nodes.get(idB);

        for (Edge e : a.edges) {
            if (e.target.id == idB) e.blocked = blocked;
        }
        for (Edge e : b.edges) {
            if (e.target.id == idA) e.blocked = blocked;
        }
    }

    public List<Node> getNodes() {
        return Collections.unmodifiableList(nodes);
    }

    public Node getNodeByName(String name) {
        return namedNodes.get(name);
    }

    /**
     * Finds the nearest road node to world coordinates (x, z).
     */
    public Node nearestNode(float x, float z) {
        if (nodes.isEmpty()) return null;
        Node best = null;
        float bestDistSq = Float.MAX_VALUE;

        for (int i = 0; i < nodes.size(); i++) {
            Node n = nodes.get(i);
            float dx = n.x - x;
            float dz = n.z - z;
            float d2 = dx * dx + dz * dz;
            if (d2 < bestDistSq) {
                bestDistSq = d2;
                best = n;
            }
        }
        return best;
    }

    /**
     * Finds shortest route between arbitrary world coordinates (startX, startZ) and (targetX, targetZ).
     * Connects to nearest graph nodes and computes Dijkstra shortest path.
     * Returns null if unreachable.
     */
    public Route findRoute(float startX, float startZ, float targetX, float targetZ) {
        Node startNode = nearestNode(startX, startZ);
        Node targetNode = nearestNode(targetX, targetZ);
        if (startNode == null || targetNode == null) return null;

        if (startNode.id == targetNode.id) {
            List<Node> single = new ArrayList<>();
            single.add(startNode);
            return new Route(single, 0f);
        }

        // Dijkstra shortest path
        int n = nodes.size();
        float[] dist = new float[n];
        int[] prev = new int[n];
        boolean[] visited = new boolean[n];

        for (int i = 0; i < n; i++) {
            dist[i] = Float.MAX_VALUE;
            prev[i] = -1;
        }

        dist[startNode.id] = 0f;

        PriorityQueue<Integer> pq = new PriorityQueue<>(Comparator.comparingDouble(a -> dist[a]));
        pq.add(startNode.id);

        while (!pq.isEmpty()) {
            int u = pq.poll();
            if (visited[u]) continue;
            visited[u] = true;

            if (u == targetNode.id) break; // reached destination

            Node uNode = nodes.get(u);
            for (Edge edge : uNode.edges) {
                if (edge.blocked) continue;
                int v = edge.target.id;
                if (!visited[v]) {
                    float newDist = dist[u] + edge.length;
                    if (newDist < dist[v]) {
                        dist[v] = newDist;
                        prev[v] = u;
                        pq.add(v);
                    }
                }
            }
        }

        // Unreachable destination
        if (dist[targetNode.id] == Float.MAX_VALUE) {
            return null;
        }

        // Reconstruct path
        List<Node> path = new ArrayList<>();
        int curr = targetNode.id;
        while (curr != -1) {
            path.add(nodes.get(curr));
            curr = prev[curr];
        }
        Collections.reverse(path);

        return new Route(path, dist[targetNode.id]);
    }

    /**
     * Builds the complete open-world road network graph:
     * - 4x4 City grid intersections
     * - Safe zone Central Plaza
     * - Residential connection to Workshop
     * - Village roads, Old Docks, and River Bridge spanning the channel
     * - North Hills watchtower and relay mast
     * - West Forest ranger hut
     */
    private void buildDefaultGraph() {
        // City Grid Verticals: X = 48, 112, 176, 240
        // City Grid Horizontals: Z = -88, -24, 40, 104
        float[] cityX = {48f, 112f, 176f, 240f};
        float[] cityZ = {-88f, -24f, 40f, 104f};

        int[][] grid = new int[4][4];

        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                String name = "City-" + col + "-" + row;
                Node node = addNode(cityX[col], cityZ[row], name);
                grid[row][col] = node.id;
            }
        }

        // Connect Manhattan grid edges
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                if (col < 3) connect(grid[row][col], grid[row][col + 1]); // East
                if (row < 3) connect(grid[row][col], grid[row + 1][col]); // South
            }
        }

        // POI access nodes in City Core
        Node plaza = addNode(150f, 20f, "Central Plaza"); // Safe Zone
        connect(plaza.id, grid[1][1]); // connect to (112, -24)
        connect(plaza.id, grid[1][2]); // connect to (176, -24)
        connect(plaza.id, grid[2][1]); // connect to (112, 40)
        connect(plaza.id, grid[2][2]); // connect to (176, 40)

        Node market = addNode(120f, -60f, "Market Street");
        connect(market.id, grid[0][1]); // (112, -88)
        connect(market.id, grid[1][1]); // (112, -24)

        Node garage = addNode(60f, 40f, "Garage");
        connect(garage.id, grid[2][0]); // (48, 40)

        Node safehouse = addNode(210f, 60f, "Safehouse");
        connect(safehouse.id, grid[2][2]); // (176, 40)
        connect(safehouse.id, grid[2][3]); // (240, 40)

        // Residential zone nodes and connection
        Node resConn = addNode(-8f, -192f, "Res-East");
        connect(resConn.id, grid[0][0]); // connect to City NW corner (48, -88)

        Node workshop = addNode(-88f, -192f, "Workshop");
        connect(workshop.id, resConn.id);

        // River Bridge corridor (Z = 240)
        // City south link
        Node citySouthLink = addNode(-8f, 240f, "City-South-Link");
        connect(citySouthLink.id, grid[3][0]); // connect to City SW corner (48, 104)

        Node villageEast = addNode(-118f, 240f, "Village-East");
        connect(villageEast.id, citySouthLink.id);

        Node bridgeEast = addNode(-135f, 240f, "River Bridge East");
        connect(bridgeEast.id, villageEast.id);

        Node bridgeWest = addNode(-175f, 240f, "River Bridge West");
        connect(bridgeWest.id, bridgeEast.id); // Spans the river channel!

        Node villageWest = addNode(-216f, 240f, "Riverside Village");
        connect(villageWest.id, bridgeWest.id);

        Node docks = addNode(-150f, 300f, "Old Docks");
        connect(docks.id, bridgeWest.id);

        // North Hills nodes
        Node hillsEntrance = addNode(176f, -160f, "Hills-Entrance");
        connect(hillsEntrance.id, grid[0][2]); // connect to (176, -88)

        Node relayMast = addNode(240f, -300f, "Relay Mast");
        connect(relayMast.id, hillsEntrance.id);

        Node watchtower = addNode(120f, -360f, "Watchtower");
        connect(watchtower.id, relayMast.id);

        // West Forest nodes
        Node forestTrail = addNode(-260f, 150f, "Forest-Trail");
        connect(forestTrail.id, villageWest.id);

        Node rangerHut = addNode(-330f, 60f, "Ranger Hut");
        connect(rangerHut.id, forestTrail.id);
    }
}
