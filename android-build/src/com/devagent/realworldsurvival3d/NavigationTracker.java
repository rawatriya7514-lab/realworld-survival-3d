package com.devagent.realworldsurvival3d;

/**
 * NavigationTracker - Step-by-step waypoint progression and dynamic rerouting.
 *
 * Implements:
 *  - Route waypoint progression: advances to next node when within 8.0m of current node
 *  - Dynamic rerouting: triggers automatic route recalculation if the player strays
 *    off the current route corridor by > 12.0m (TECHNICAL_ARCHITECTURE.md §72)
 *  - Bearing and distance calculations to next waypoint and final destination
 *  - Arrival detection within 5.0m of destination
 */
public class NavigationTracker {
    public static final float WAYPOINT_REACH_RADIUS = 8.0f;
    public static final float REROUTE_DISTANCE_THRESHOLD = 12.0f;
    public static final float DESTINATION_ARRIVE_RADIUS = 5.0f;

    public final RoadNetwork network;

    public boolean hasActiveWaypoint = false;
    public float targetX = 0f;
    public float targetZ = 0f;
    public String destinationName = "";

    public RoadNetwork.Route currentRoute = null;
    public int currentWaypointIndex = 0;
    public boolean arrived = false;
    public int rerouteCount = 0;

    public NavigationTracker(RoadNetwork network) {
        this.network = (network != null) ? network : new RoadNetwork();
    }

    /**
     * Sets a new navigation destination and computes the initial shortest path.
     */
    public boolean setDestination(float tx, float tz, String name, float playerX, float playerZ) {
        this.targetX = tx;
        this.targetZ = tz;
        this.destinationName = (name != null) ? name : "Waypoint";
        this.arrived = false;

        RoadNetwork.Route route = network.findRoute(playerX, playerZ, tx, tz);
        if (route == null || route.isEmpty()) {
            this.currentRoute = null;
            this.hasActiveWaypoint = false;
            return false;
        }

        this.currentRoute = route;
        this.currentWaypointIndex = 0;
        this.hasActiveWaypoint = true;
        return true;
    }

    /**
     * Clears the current navigation target.
     */
    public void clearDestination() {
        this.hasActiveWaypoint = false;
        this.currentRoute = null;
        this.currentWaypointIndex = 0;
        this.arrived = false;
    }

    /**
     * Core update loop called each frame with current player position.
     */
    public void update(float playerX, float playerZ) {
        if (!hasActiveWaypoint || currentRoute == null || currentRoute.isEmpty()) {
            return;
        }

        // Check if player has arrived at final destination
        float finalDx = targetX - playerX;
        float finalDz = targetZ - playerZ;
        if (finalDx * finalDx + finalDz * finalDz <= DESTINATION_ARRIVE_RADIUS * DESTINATION_ARRIVE_RADIUS) {
            arrived = true;
            hasActiveWaypoint = false;
            return;
        }

        // Check if player has reached the current waypoint node
        if (currentWaypointIndex < currentRoute.size()) {
            RoadNetwork.Node targetNode = currentRoute.get(currentWaypointIndex);
            float dx = targetNode.x - playerX;
            float dz = targetNode.z - playerZ;
            float distSq = dx * dx + dz * dz;

            if (distSq <= WAYPOINT_REACH_RADIUS * WAYPOINT_REACH_RADIUS) {
                currentWaypointIndex++;
                if (currentWaypointIndex >= currentRoute.size()) {
                    // Reached the end of the road nodes; head towards final destination point
                    currentWaypointIndex = currentRoute.size();
                }
            }
        }

        // Check for deviation from the planned route corridor (> 12m)
        float deviation = computeRouteDeviation(playerX, playerZ);
        if (deviation > REROUTE_DISTANCE_THRESHOLD) {
            // Trigger dynamic recalculation from player's current location
            RoadNetwork.Route newRoute = network.findRoute(playerX, playerZ, targetX, targetZ);
            if (newRoute != null && !newRoute.isEmpty()) {
                currentRoute = newRoute;
                currentWaypointIndex = 0;
                rerouteCount++;
            }
        }
    }

    /**
     * Computes the shortest distance from the player to the current route segment.
     */
    public float computeRouteDeviation(float px, float pz) {
        if (currentRoute == null || currentRoute.isEmpty()) return 0f;

        // If at the end of route, measure distance to final destination
        if (currentWaypointIndex >= currentRoute.size()) {
            float dx = targetX - px;
            float dz = targetZ - pz;
            return (float) Math.sqrt(dx * dx + dz * dz);
        }

        RoadNetwork.Node nextNode = currentRoute.get(currentWaypointIndex);

        // If at first node, measure distance to that first node
        if (currentWaypointIndex == 0) {
            float dx = nextNode.x - px;
            float dz = nextNode.z - pz;
            return (float) Math.sqrt(dx * dx + dz * dz);
        }

        // Otherwise measure perpendicular distance to segment [prevNode -> nextNode]
        RoadNetwork.Node prevNode = currentRoute.get(currentWaypointIndex - 1);
        return distanceToSegment(px, pz, prevNode.x, prevNode.z, nextNode.x, nextNode.z);
    }

    /**
     * Gets the next waypoint position (x, z).
     * If all road nodes are passed, returns the final target coordinate.
     */
    public float[] getNextWaypoint(float[] out) {
        if (out == null || out.length < 2) out = new float[2];
        if (!hasActiveWaypoint || currentRoute == null) {
            out[0] = targetX;
            out[1] = targetZ;
            return out;
        }

        if (currentWaypointIndex < currentRoute.size()) {
            RoadNetwork.Node node = currentRoute.get(currentWaypointIndex);
            out[0] = node.x;
            out[1] = node.z;
        } else {
            out[0] = targetX;
            out[1] = targetZ;
        }
        return out;
    }

    /**
     * Gets the world heading angle in radians from player to next waypoint.
     */
    public float getHeadingToNextWaypoint(float playerX, float playerZ) {
        float[] wp = getNextWaypoint(null);
        float dx = wp[0] - playerX;
        float dz = wp[1] - playerZ;
        return (float) Math.atan2(dx, -dz);
    }

    /**
     * Computes remaining distance to travel along the route to destination.
     */
    public float getRemainingDistance(float playerX, float playerZ) {
        if (!hasActiveWaypoint) return 0f;
        if (currentRoute == null || currentWaypointIndex >= currentRoute.size()) {
            float dx = targetX - playerX;
            float dz = targetZ - playerZ;
            return (float) Math.sqrt(dx * dx + dz * dz);
        }

        RoadNetwork.Node nextNode = currentRoute.get(currentWaypointIndex);
        float dx = nextNode.x - playerX;
        float dz = nextNode.z - playerZ;
        float dist = (float) Math.sqrt(dx * dx + dz * dz);

        for (int i = currentWaypointIndex; i < currentRoute.size() - 1; i++) {
            RoadNetwork.Node a = currentRoute.get(i);
            RoadNetwork.Node b = currentRoute.get(i + 1);
            float segDx = b.x - a.x;
            float segDz = b.z - a.z;
            dist += (float) Math.sqrt(segDx * segDx + segDz * segDz);
        }

        RoadNetwork.Node last = currentRoute.get(currentRoute.size() - 1);
        float toDestX = targetX - last.x;
        float toDestZ = targetZ - last.z;
        dist += (float) Math.sqrt(toDestX * toDestX + toDestZ * toDestZ);

        return dist;
    }

    private static float distanceToSegment(float px, float pz, float x1, float z1, float x2, float z2) {
        float vx = x2 - x1;
        float vz = z2 - z1;
        float lenSq = vx * vx + vz * vz;
        if (lenSq < 1e-6f) {
            float dx = px - x1;
            float dz = pz - z1;
            return (float) Math.sqrt(dx * dx + dz * dz);
        }

        float t = ((px - x1) * vx + (pz - z1) * vz) / lenSq;
        t = Math.max(0f, Math.min(1f, t));
        float projX = x1 + t * vx;
        float projZ = z1 + t * vz;
        float dx = px - projX;
        float dz = pz - projZ;
        return (float) Math.sqrt(dx * dx + dz * dz);
    }
}
