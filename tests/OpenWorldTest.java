import com.devagent.realworldsurvival3d.Obstacle;
import com.devagent.realworldsurvival3d.OpenWorld;
import com.devagent.realworldsurvival3d.Player;
import com.devagent.realworldsurvival3d.Terrain;

/**
 * OpenWorldTest - JVM tests for the open-world foundation: analytic terrain,
 * chunk meshes, regions, deterministic chunk generation, 3x3 streaming,
 * bridge/water structures, and the Player terrain/water integration.
 * Run: tests/run.sh
 */
public class OpenWorldTest {

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

    public static void main(String[] args) {
        testTerrainContinuity();
        testCityFlat();
        testRiverCarveAndBanks();
        testNorthHills();
        testBridgeCorridor();
        testChunkMeshes();
        testMeshEdgeContinuity();
        testRegions();
        testDeterministicChunks();
        testActiveSetAndBridge();
        testPois();
        testPlayerTerrainLanding();
        testRoadSlabsNonSolid();
        testDeepWaterBlock();
        testWadingSlowdown();

        System.out.println("\n" + passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }

    // -----------------------------------------------------------------
    static void testTerrainContinuity() {
        System.out.println("Terrain: continuity");
        float maxStep = 0f;
        float maxRiverStep = 0f;
        for (float x = -440f; x <= 440f; x += 40f) {
            for (float z = -440f; z <= 440f; z += 40f) {
                float h = Terrain.heightAt(x, z);
                float sx = Math.abs(Terrain.heightAt(x + 2f, z) - h);
                float sz = Math.abs(Terrain.heightAt(x, z + 2f) - h);
                if (OpenWorld.regionAt(x, z) == OpenWorld.Region.RIVER) {
                    // the river carves a canyon through the hills - bounded, not flat
                    maxRiverStep = Math.max(maxRiverStep, Math.max(sx, sz));
                } else {
                    maxStep = Math.max(maxStep, Math.max(sx, sz));
                }
            }
        }
        check(maxStep < 0.8f, "no cliff steps outside the river canyon (max " + maxStep + ")");
        check(maxRiverStep < 6f, "river canyon walls bounded (max " + maxRiverStep + ")");
        check(!Float.isNaN(Terrain.heightAt(0f, 0f)), "heightAt is never NaN");
    }

    static void testCityFlat() {
        System.out.println("Terrain: city flat zone");
        float maxDev = 0f;
        for (float x = 48f; x <= 264f; x += 36f) {
            for (float z = -104f; z <= 104f; z += 26f) {
                if (OpenWorld.regionAt(x, z) != OpenWorld.Region.CITY_CORE) continue;
                maxDev = Math.max(maxDev, Math.abs(Terrain.heightAt(x, z)));
            }
        }
        check(maxDev < 0.06f, "city interior is level ground (max dev " + maxDev + ")");
    }

    static void testRiverCarveAndBanks() {
        System.out.println("Terrain: river");
        float cx = Terrain.riverCenterX(0f);
        check(Terrain.heightAt(cx, 0f) < -3.5f, "river channel bed is carved deep");
        check(Terrain.waterDepthAt(cx, 0f) > Terrain.WADE_MAX,
                "channel center is deeper than the wade limit");
        check(Terrain.heightAt(cx + 60f, 0f) > Terrain.WATER_LEVEL,
                "east floodplain is dry (above water level)");
        check(Terrain.heightAt(cx - 60f, 0f) > Terrain.WATER_LEVEL,
                "west bank is dry (above water level)");
        check(Terrain.riverCenterX(240f) != Terrain.riverCenterX(0f),
                "river meanders (center line depends on z)");
    }

    static void testNorthHills() {
        System.out.println("Terrain: hills");
        float hEdge = Terrain.heightAt(120f, -440f);
        float hBase = Terrain.heightAt(120f, 0f);
        check(hEdge > 14f, "north edge is high ground (" + hEdge + ")");
        check(hEdge > hBase + 10f, "hills rise above the lowlands");
    }

    static void testBridgeCorridor() {
        System.out.println("Terrain: bridge corridor");
        float bcx = Terrain.riverCenterX(Terrain.BRIDGE_Z);
        float bankE = Terrain.heightAt(bcx + 45f, Terrain.BRIDGE_Z);
        float bankW = Terrain.heightAt(bcx - 45f, Terrain.BRIDGE_Z);
        check(Math.abs(bankE - 1.2f) < 0.06f, "east approach flattened to 1.2 (" + bankE + ")");
        check(Math.abs(bankW - 1.2f) < 0.06f, "west approach flattened to 1.2 (" + bankW + ")");
        check(Math.abs(Terrain.heightAt(-200f, Terrain.BRIDGE_Z) - 1.2f) < 0.06f,
                "west approach road strip sits on flat 1.2 ground");
        check(Math.abs(Terrain.heightAt(-100f, Terrain.BRIDGE_Z) - 1.2f) < 0.06f,
                "east approach road strip sits on flat 1.2 ground");
        check(Terrain.heightAt(bcx, Terrain.BRIDGE_Z) < -3.5f,
                "channel still carved under the bridge");
    }

    // -----------------------------------------------------------------
    static void testChunkMeshes() {
        System.out.println("Terrain: chunk meshes");
        int[][] chunks = {{7, 7}, {10, 7}, {3, 3}, {9, 11}, {13, 13}, {0, 0}};
        for (int[] cr : chunks) {
            Terrain.MeshData m = Terrain.buildChunkMesh(cr[0], cr[1]);
            check(m.vertexCount == 96, "chunk (" + cr[0] + "," + cr[1] + ") has 96 verts");
            check(m.verts.length == 96 * 8, "stride 8 layout");
            float x0 = cr[0] * 64f - 448f, z0 = cr[1] * 64f - 448f;
            boolean inBounds = true, normalsOk = true;
            for (int v = 0; v < m.vertexCount; v++) {
                float x = m.verts[v * 8], y = m.verts[v * 8 + 1], z = m.verts[v * 8 + 2];
                if (x < x0 - 0.01f || x > x0 + 64f + 0.01f
                        || z < z0 - 0.01f || z > z0 + 64f + 0.01f) inBounds = false;
                float nx = m.verts[v * 8 + 3], ny = m.verts[v * 8 + 4], nz = m.verts[v * 8 + 5];
                float len = nx * nx + ny * ny + nz * nz;
                if (Math.abs(len - 1f) > 0.01f) normalsOk = false;
            }
            check(inBounds, "chunk (" + cr[0] + "," + cr[1] + ") verts inside chunk bounds");
            check(normalsOk, "chunk (" + cr[0] + "," + cr[1] + ") normals normalized");
        }
    }

    static void testMeshEdgeContinuity() {
        System.out.println("Terrain: mesh seams");
        Terrain.MeshData west = Terrain.buildChunkMesh(5, 10);
        Terrain.MeshData east = Terrain.buildChunkMesh(6, 10);
        float seamX = 6 * 64f - 448f; // -64
        // Collect seam vertices (x within eps of seam), keyed by z
        float[] wz = new float[west.vertexCount / 4];
        float[] wy = new float[wz.length];
        int wn = 0;
        for (int v = 0; v < west.vertexCount; v++) {
            if (Math.abs(west.verts[v * 8] - seamX) < 0.01f) {
                wz[wn] = west.verts[v * 8 + 2];
                wy[wn] = west.verts[v * 8 + 1];
                wn++;
            }
        }
        boolean allMatch = wn > 0;
        for (int i = 0; i < wn && allMatch; i++) {
            boolean found = false;
            for (int v = 0; v < east.vertexCount; v++) {
                if (Math.abs(east.verts[v * 8] - seamX) < 0.01f
                        && Math.abs(east.verts[v * 8 + 2] - wz[i]) < 0.01f
                        && Math.abs(east.verts[v * 8 + 1] - wy[i]) < 0.01f) {
                    found = true;
                    break;
                }
            }
            if (!found) allMatch = false;
        }
        check(allMatch, "chunk 5/10 seam heights match chunk 6 exactly");
    }

    // -----------------------------------------------------------------
    static void testRegions() {
        System.out.println("Regions");
        check(OpenWorld.regionAt(150f, 20f) == OpenWorld.Region.CITY_CORE, "plaza is City Core");
        check(OpenWorld.regionAt(-20f, -200f) == OpenWorld.Region.RESIDENTIAL, "workshop is Residential");
        check(OpenWorld.regionAt(-40f, 300f) == OpenWorld.Region.RIVERSIDE_VILLAGE, "village region");
        check(OpenWorld.regionAt(-330f, 60f) == OpenWorld.Region.WEST_FOREST, "ranger hut is West Forest");
        check(OpenWorld.regionAt(120f, -360f) == OpenWorld.Region.NORTH_HILLS, "watchtower is North Hills");
        float cx = Terrain.riverCenterX(0f);
        check(OpenWorld.regionAt(cx, 0f) == OpenWorld.Region.RIVER, "river band region");
        check(OpenWorld.regionAt(400f, 400f) == OpenWorld.Region.OUTSKIRTS, "far corner is Outskirts");
        // Unique IDs
        java.util.Set<Integer> ids = new java.util.HashSet<>();
        for (OpenWorld.Region r : OpenWorld.Region.values()) ids.add(r.id);
        check(ids.size() == OpenWorld.Region.values().length, "every region has a unique ID");
        check(OpenWorld.Region.values().length == 7, "7 regions defined");
    }

    // -----------------------------------------------------------------
    static void testDeterministicChunks() {
        System.out.println("Chunk generation");
        java.util.List<Obstacle> a = new OpenWorld().chunkObstacles(10, 7);
        java.util.List<Obstacle> b = new OpenWorld().chunkObstacles(10, 7);
        check(a.size() == b.size(), "chunk generation is deterministic (size)");
        boolean same = a.size() == b.size();
        for (int i = 0; i < a.size() && same; i++) {
            Obstacle oa = a.get(i), ob = b.get(i);
            if (oa.type != ob.type
                    || Math.abs(oa.x - ob.x) > 0.001f || Math.abs(oa.y - ob.y) > 0.001f
                    || Math.abs(oa.z - ob.z) > 0.001f) same = false;
        }
        check(same, "chunk generation is deterministic (content)");
        java.util.List<Obstacle> c = new OpenWorld().chunkObstacles(3, 3);
        check(a.size() != c.size(), "different chunks produce different content");
    }

    static void testActiveSetAndBridge() {
        System.out.println("Streaming");
        OpenWorld ow = new OpenWorld();
        ow.recomputeActive(150f, 20f);
        java.util.List<Obstacle> active = ow.activeObstacles();
        check(!active.isEmpty(), "active set is populated");
        java.util.List<Obstacle> center = ow.chunkObstacles(10, 7);
        check(active.size() >= center.size() + 2,
                "3x3 active set covers the player chunk plus neighbours");
        // Bridge is a global structure, always active
        boolean bridge = false;
        for (Obstacle o : active) if (o.type == Obstacle.Type.BRIDGE) bridge = true;
        check(bridge, "river bridge is part of the active set");
        float bcx = Terrain.riverCenterX(Terrain.BRIDGE_Z);
        Obstacle bridgeO = null;
        for (Obstacle o : active) {
            if (o.type == Obstacle.Type.BRIDGE && Math.abs(o.x - bcx) < 40f
                    && Math.abs(o.z - Terrain.BRIDGE_Z) < 5f) bridgeO = o;
        }
        check(bridgeO != null, "bridge sits on the river crossing");
        if (bridgeO != null) {
            check(bridgeO.width >= 60f, "bridge spans the channel (" + bridgeO.width + "m)");
            check(Math.abs(bridgeO.getMaxY() - 1.4f) < 0.05f, "bridge deck top is 1.4");
            check(bridgeO.solid, "bridge deck is solid");
        }
        // floorAt sees the bridge deck above water
        float floorOnBridge = ow.floorAt(bcx, Terrain.BRIDGE_Z, 5f);
        check(Math.abs(floorOnBridge - 1.4f) < 0.05f, "floorAt returns deck on the bridge");
        float floorUnderBridge = ow.floorAt(bcx, Terrain.BRIDGE_Z, -3f);
        check(floorUnderBridge < -3f, "floorAt returns riverbed under the deck");
    }

    static void testPois() {
        System.out.println("POIs");
        OpenWorld ow = new OpenWorld();
        check(ow.pois.size() == 10, "10 named POIs");
        float[][] expected = {{150, 20}, {210, 60}, {120, -60}, {60, 40}, {-20, -200},
                {120, -360}, {240, -300}, {-330, 60}, {-150, 300}, {-155, 240}};
        boolean all = true;
        for (float[] e : expected) {
            boolean found = false;
            for (OpenWorld.Poi p : ow.pois) {
                if (Math.abs(p.x - e[0]) < 2f && Math.abs(p.z - e[1]) < 2f) found = true;
            }
            if (!found) all = false;
        }
        check(all, "all POIs at their designed coordinates");
        check(ow.isSafeZone(150f, 20f), "Central Plaza is the safe zone");
        check(!ow.isSafeZone(0f, 0f), "outside the plaza is not safe");
        check(ow.discoveredCount() == 0, "nothing discovered at start");
        ow.discoverNear(151f, 21f);
        check(ow.discoveredCount() > 0 && ow.lastDiscovered != null, "discovery marks nearby POI");
    }

    // -----------------------------------------------------------------
    static void testPlayerTerrainLanding() {
        System.out.println("Player: terrain");
        Player p = new Player();
        p.terrain = new Terrain();
        p.x = 150f; p.z = 20f;          // city interior (flat, height 0)
        p.y = Terrain.heightAt(150f, 20f) + 4.5f;
        p.vy = 0f;
        p.isGrounded = false;
        p.peakFallY = p.y;
        for (int i = 0; i < 120; i++) {
            p.update(1f / 60f, 0f, 0f, false, false, false, new java.util.ArrayList<Obstacle>());
        }
        check(p.isGrounded, "player lands on the terrain heightfield");
        check(Math.abs(p.y - Terrain.heightAt(150f, 20f)) < 0.15f,
                "player rests at ground height (y=" + p.y + ")");
        check(p.health >= p.maxHealth - 0.01f, "short fall causes no damage");
    }

    static void testRoadSlabsNonSolid() {
        System.out.println("Player: roads");
        Player p = new Player();
        p.terrain = new Terrain();
        p.x = 48f; p.z = 0f;            // middle of a city road strip
        p.y = Terrain.heightAt(48f, 0f) + 4.5f;
        p.vy = 0f;
        p.peakFallY = p.y;
        OpenWorld ow = new OpenWorld();
        ow.recomputeActive(48f, 0f);
        for (int i = 0; i < 120; i++) {
            p.update(1f / 60f, 0f, 0f, false, false, false, ow.activeObstacles());
        }
        check(Math.abs(p.y - Terrain.heightAt(48f, 0f)) < 0.12f,
                "player walks on terrain through non-solid road slabs (y=" + p.y + ")");
        boolean sawRoad = false;
        for (Obstacle o : ow.activeObstacles()) {
            if (o.type == Obstacle.Type.ROAD_SLAB && !o.solid) sawRoad = true;
        }
        check(sawRoad, "road slabs exist and are non-solid");
    }

    static void testDeepWaterBlock() {
        System.out.println("Player: deep water block");
        float cx = Terrain.riverCenterX(0f);
        float bankX = cx + 27f; // wading ledge, depth near the wade limit
        Player p = new Player();
        p.terrain = new Terrain();
        p.x = bankX; p.z = 0f;
        p.y = Terrain.heightAt(bankX, 0f) + 0.05f;
        p.isGrounded = true;
        p.peakFallY = p.y;
        p.yaw = 90f; // forward +x, inputY=1 walks backward (-x) toward the channel
        for (int i = 0; i < 90; i++) {
            p.update(1f / 60f, 0f, 1f, false, false, false, new java.util.ArrayList<Obstacle>());
        }
        float depth = Terrain.waterDepthAt(p.x, p.z);
        check(depth <= Terrain.WADE_MAX + 0.06f,
                "walking into the river is blocked at the wade limit (depth " + depth + ")");
        check(p.x > cx + 20f, "player never reached the channel center");

        // Escape: dropped into deep water, walking back toward the bank works
        Player q = new Player();
        q.terrain = new Terrain();
        q.x = cx; q.z = 0f;
        q.y = Terrain.heightAt(cx, 0f) + 0.05f;
        q.isGrounded = true;
        q.peakFallY = q.y;
        float startX = q.x;
        q.yaw = -90f; // forward -x, inputY=1 walks backward (+x) toward the east bank
        // The flat channel bed spans |dx| <= 20 m; give the runner enough
        // time (15 s) to cross it and climb the far bank.
        for (int i = 0; i < 900; i++) {
            q.update(1f / 60f, 0f, 1f, false, false, false, new java.util.ArrayList<Obstacle>());
        }
        check(q.x > startX + 2f, "escape from deep water toward the bank is allowed");
        check(q.x > cx + 20f, "runner crossed the flat bed (dx "
                + (q.x - cx) + ")");
        check(Terrain.waterDepthAt(q.x, q.z) < Terrain.waterDepthAt(startX, q.z),
                "escape reduced the water depth");
    }

    static void testWadingSlowdown() {
        System.out.println("Player: wading");
        float cx = Terrain.riverCenterX(0f);
        // Find a real wading spot east of the channel: depth between 1.0 and 1.4
        float startX = 0f;
        boolean shelfFound = false;
        for (float d = 20f; d <= 33f; d += 0.5f) {
            float depth = Terrain.waterDepthAt(cx + d, 0f);
            if (depth >= 1.0f && depth <= 1.4f) {
                startX = cx + d;
                shelfFound = true;
                break;
            }
        }
        check(shelfFound, "a wading shelf exists east of the channel");
        if (!shelfFound) {
            for (float d = 18f; d <= 40f; d += 0.5f) {
                System.out.println("SCAN d=" + d + " depth="
                        + Terrain.waterDepthAt(cx + d, 0f));
            }
            return;
        }

        // Measure a short 0.3 s window so the runner stays in water throughout
        Player w = new Player();
        w.terrain = new Terrain();
        w.x = startX; w.z = 0f;
        w.y = Terrain.heightAt(w.x, 0f) + 0.05f;
        w.isGrounded = true;
        w.peakFallY = w.y;
        float wx0 = w.x;
        w.yaw = -90f; // forward -x, inputY=1 walks backward (+x) toward the bank
        for (int i = 0; i < 18; i++) {
            w.update(1f / 60f, 0f, 1f, false, false, false, new java.util.ArrayList<Obstacle>());
        }
        float wDist = w.x - wx0;
        check(Terrain.waterDepthAt(w.x, w.z) > 0.3f,
                "wading test stayed in water (depth "
                        + Terrain.waterDepthAt(w.x, w.z) + ")");

        // Dry land reference run in the city, same duration
        Player d = new Player();
        d.terrain = new Terrain();
        d.x = 150f; d.z = 20f;
        d.y = Terrain.heightAt(150f, 20f) + 0.05f;
        d.isGrounded = true;
        d.peakFallY = d.y;
        float dx0 = d.x;
        d.yaw = -90f;
        for (int i = 0; i < 18; i++) {
            d.update(1f / 60f, 0f, 1f, false, false, false, new java.util.ArrayList<Obstacle>());
        }
        float dDist = d.x - dx0;
        check(wDist > 0.5f, "wading still makes progress (" + wDist + "m)");
        check(wDist < dDist * 0.72f, "wading is slower than dry running ("
                + wDist + " vs " + dDist + ")");
    }
}
