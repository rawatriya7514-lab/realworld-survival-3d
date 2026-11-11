package com.devagent.realworldsurvival3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * OpenWorld - The open-world layout and streaming system (PURE JAVA).
 *
 * World: 896 m x 896 m (-448..+448), split into 64 m modular chunks
 * (14 x 14 = 196). Chunks generate their content deterministically from
 * (col, row) with seed 1337, so the same chunk always produces the same
 * obstacles. Only the 3x3 chunks around the player are active.
 *
 * Regions group chunks and carry unique IDs:
 *   CITY_CORE(1), RESIDENTIAL(2), RIVERSIDE_VILLAGE(3), WEST_FOREST(4),
 *   NORTH_HILLS(5), RIVER(6), OUTSKIRTS(7).
 *
 * Named POIs drive discovery, the safe zone and (in later phases) the map
 * markers and mission targets.
 */
public class OpenWorld {

    public static final int CHUNK_SIZE = 64;
    public static final int WORLD_HALF = 448;
    public static final int CHUNKS_PER_SIDE = 14;
    public static final int ACTIVE_RADIUS = 1; // 3x3 window

    // ------------------------------------------------------------------
    // Regions (unique identifiers)
    // ------------------------------------------------------------------
    public enum Region {
        CITY_CORE(1, "City Core"),
        RESIDENTIAL(2, "Residential"),
        RIVERSIDE_VILLAGE(3, "Riverside Village"),
        WEST_FOREST(4, "West Forest"),
        NORTH_HILLS(5, "North Hills"),
        RIVER(6, "River"),
        OUTSKIRTS(7, "Outskirts");

        public final int id;
        public final String display;

        Region(int id, String display) {
            this.id = id;
            this.display = display;
        }
    }

    /** Region lookup with priority: river band, then flat zones, then wilds. */
    public static Region regionAt(float x, float z) {
        float dx = Math.abs(x - Terrain.riverCenterX(z));
        if (dx < 34f) return Region.RIVER;
        if (x >= Terrain.CITY_X0 && x <= Terrain.CITY_X1
                && z >= Terrain.CITY_Z0 && z <= Terrain.CITY_Z1) return Region.CITY_CORE;
        if (x >= Terrain.RES_X0 && x <= Terrain.RES_X1
                && z >= Terrain.RES_Z0 && z <= Terrain.RES_Z1) return Region.RESIDENTIAL;
        if (x >= Terrain.VIL_X0 && x <= Terrain.VIL_X1
                && z >= Terrain.VIL_Z0 && z <= Terrain.VIL_Z1) return Region.RIVERSIDE_VILLAGE;
        if (x < -240f) return Region.WEST_FOREST;
        if (z < -240f) return Region.NORTH_HILLS;
        return Region.OUTSKIRTS;
    }

    // ------------------------------------------------------------------
    // POIs
    // ------------------------------------------------------------------
    public static final int TYPE_SHELTER = 0, TYPE_SHOP = 1, TYPE_LANDMARK = 2;

    public static final class Poi {
        public final String name;
        public final float x, z;
        public final int type;
        public final float discoverRadius;
        public boolean discovered;

        Poi(String name, float x, float z, int type, float radius) {
            this.name = name;
            this.x = x;
            this.z = z;
            this.type = type;
            this.discoverRadius = radius;
        }
    }

    public final List<Poi> pois = new ArrayList<>();
    /** Name of the POI discovered most recently, consumed by the game loop. */
    public String lastDiscovered = null;

    // Central Plaza is the safe zone (no hostiles, slow healing - phase 8+).
    public static final float PLAZA_X = 150f, PLAZA_Z = 20f, PLAZA_RADIUS = 30f;

    public boolean isSafeZone(float x, float z) {
        float dx = x - PLAZA_X, dz = z - PLAZA_Z;
        return dx * dx + dz * dz < PLAZA_RADIUS * PLAZA_RADIUS;
    }

    // ------------------------------------------------------------------
    // Roads (flat-zone strips: x0, x1, z0, z1). City grid + village/res links.
    // ------------------------------------------------------------------
    private static final float[][] ROADS = {
            // City verticals (32 m wide, x strips)
            {32f, 64f, -120f, 120f},
            {96f, 128f, -120f, 120f},
            {160f, 192f, -120f, 120f},
            {224f, 256f, -120f, 120f},
            // City horizontals
            {32f, 280f, -104f, -72f},
            {32f, 280f, -40f, -8f},
            {32f, 280f, 24f, 56f},
            {32f, 280f, 88f, 120f},
            // Residential connector
            {-88f, -8f, -204f, -180f},
            // Village road west of the bridge + east link
            {-216f, -188f, 228f, 252f},
            {-118f, -8f, 228f, 252f},
    };

    // ------------------------------------------------------------------
    // Chunk streaming
    // ------------------------------------------------------------------
    public final BuildingInterior buildingInterior = new BuildingInterior();
    private final HashMap<Long, List<Obstacle>> chunkCache = new HashMap<>();
    private final List<Obstacle> global = new ArrayList<>(); // bridge + pier
    private final List<Obstacle> active = new ArrayList<>();
    private long activeKey = Long.MIN_VALUE;

    public OpenWorld() {
        buildPois();
        buildGlobalStructures();
    }

    private void buildPois() {
        pois.add(new Poi("Central Plaza", PLAZA_X, PLAZA_Z, TYPE_SHELTER, 22f));
        pois.add(new Poi("Safehouse", 210f, 60f, TYPE_SHELTER, 22f));
        pois.add(new Poi("Market Street", 120f, -60f, TYPE_SHOP, 22f));
        pois.add(new Poi("Garage", 60f, 40f, TYPE_SHOP, 22f));
        pois.add(new Poi("Workshop", -20f, -200f, TYPE_SHOP, 22f));
        pois.add(new Poi("Watchtower", 120f, -360f, TYPE_LANDMARK, 28f));
        pois.add(new Poi("Relay Mast", 240f, -300f, TYPE_LANDMARK, 28f));
        pois.add(new Poi("Ranger Hut", -330f, 60f, TYPE_LANDMARK, 28f));
        pois.add(new Poi("Old Docks", -150f, 300f, TYPE_LANDMARK, 24f));
        pois.add(new Poi("River Bridge", -155f, 240f, TYPE_LANDMARK, 20f));
    }

    private void buildGlobalStructures() {
        // River bridge: solid deck, top at y=1.4, spanning the channel at z=240.
        float bcx = Terrain.riverCenterX(Terrain.BRIDGE_Z);
        global.add(new Obstacle(Obstacle.Type.BRIDGE, bcx, 1.15f, Terrain.BRIDGE_Z,
                68f, 0.5f, 26f));
        // Old Docks pier: walkable planks from the west bank out over the water.
        global.add(new Obstacle(Obstacle.Type.BRIDGE, -150f, 0.5f, 300f,
                24f, 0.6f, 6f));
    }

    public static int chunkIndex(float v) {
        int i = (int) Math.floor((v + WORLD_HALF) / (float) CHUNK_SIZE);
        if (i < 0) i = 0;
        if (i > CHUNKS_PER_SIDE - 1) i = CHUNKS_PER_SIDE - 1;
        return i;
    }

    private static long chunkKey(int col, int row) {
        return col * 10000L + row;
    }

    /** Rebuilds the 3x3 active set when the player crosses a chunk border. */
    public void recomputeActive(float px, float pz) {
        int pc = chunkIndex(px), pr = chunkIndex(pz);
        long key = chunkKey(pc, pr);
        if (key == activeKey) return;
        activeKey = key;
        active.clear();
        for (int dc = -ACTIVE_RADIUS; dc <= ACTIVE_RADIUS; dc++) {
            for (int dr = -ACTIVE_RADIUS; dr <= ACTIVE_RADIUS; dr++) {
                int c = pc + dc, r = pr + dr;
                if (c < 0 || c >= CHUNKS_PER_SIDE || r < 0 || r >= CHUNKS_PER_SIDE) continue;
                active.addAll(chunkObstacles(c, r));
            }
        }
        active.addAll(global);
    }

    public List<Obstacle> activeObstacles() {
        return active;
    }

    /** Cached deterministic content of one chunk (generated on first need). */
    public List<Obstacle> chunkObstacles(int col, int row) {
        Long key = chunkKey(col, row);
        List<Obstacle> list = chunkCache.get(key);
        if (list == null) {
            list = generateChunk(col, row);
            chunkCache.put(key, list);
        }
        return list;
    }

    /** Highest solid surface under (x, z) at/below nearY: terrain + structures. */
    public float floorAt(float x, float z, float nearY) {
        float best = Terrain.heightAt(x, z);
        for (Obstacle o : global) {
            if (!o.solid) continue;
            if (!o.containsXZ(x, z, 0f)) continue;
            float top = o.getMaxY();
            if (top <= nearY + 0.6f && top > best) best = top;
        }
        for (Obstacle o : active) {
            if (!o.solid) continue;
            if (!o.containsXZ(x, z, 0f)) continue;
            float top = o.getMaxY();
            if (top <= nearY + 0.6f && top > best) best = top;
        }
        return best;
    }

    /** Marks POIs within their discovery radius; sets lastDiscovered. */
    public void discoverNear(float px, float pz) {
        for (Poi p : pois) {
            if (p.discovered) continue;
            float dx = px - p.x, dz = pz - p.z;
            if (dx * dx + dz * dz < p.discoverRadius * p.discoverRadius) {
                p.discovered = true;
                lastDiscovered = p.name;
            }
        }
    }

    public int discoveredCount() {
        int n = 0;
        for (Poi p : pois) if (p.discovered) n++;
        return n;
    }

    // ==================================================================
    // Deterministic chunk generation (seed 1337)
    // ==================================================================
    private List<Obstacle> generateChunk(int col, int row) {
        List<Obstacle> out = new ArrayList<>();
        float x0 = col * CHUNK_SIZE - WORLD_HALF;
        float z0 = row * CHUNK_SIZE - WORLD_HALF;
        float cx0 = x0 + CHUNK_SIZE / 2f, cz0 = z0 + CHUNK_SIZE / 2f;
        java.util.Random rng = new java.util.Random(
                1337L + col * 73856093L + row * 19349663L);

        // --- roads (16 m slab segments follow the flat terrain exactly) ---
        for (float[] road : ROADS) {
            float ox0 = Math.max(road[0], x0), ox1 = Math.min(road[1], x0 + CHUNK_SIZE);
            float oz0 = Math.max(road[2], z0), oz1 = Math.min(road[3], z0 + CHUNK_SIZE);
            if (ox1 <= ox0 || oz1 <= oz0) continue;
            float ow = ox1 - ox0, od = oz1 - oz0;
            if (ow >= od) {
                int segs = (int) Math.ceil(ow / 16f);
                for (int s = 0; s < segs; s++) {
                    float sx0 = ox0 + s * (ow / segs), sx1 = ox0 + (s + 1) * (ow / segs);
                    addRoadSlab(out, (sx0 + sx1) / 2f, (oz0 + oz1) / 2f, sx1 - sx0, od);
                }
            } else {
                int segs = (int) Math.ceil(od / 16f);
                for (int s = 0; s < segs; s++) {
                    float sz0 = oz0 + s * (od / segs), sz1 = oz0 + (s + 1) * (od / segs);
                    addRoadSlab(out, (ox0 + ox1) / 2f, (sz0 + sz1) / 2f, ow, sz1 - sz0);
                }
            }
        }

        // --- water plane where the river channel passes through ---
        if (Math.abs(cx0 - Terrain.riverCenterX(cz0)) < 34f + 45f) {
            boolean hasWater = false;
            for (int i = 0; i <= 4 && !hasWater; i++) {
                for (int j = 0; j <= 2; j++) {
                    float sx = cx0 - 34f + i * 17f;
                    float sz = z0 + j * 32f;
                    if (Terrain.heightAt(sx, sz) < Terrain.WATER_LEVEL - 0.25f) {
                        hasWater = true;
                        break;
                    }
                }
            }
            if (hasWater) {
                Obstacle water = new Obstacle(Obstacle.Type.WATER_PLANE,
                        Terrain.riverCenterX(cz0), Terrain.WATER_LEVEL - 0.15f, cz0,
                        68f, 0.3f, CHUNK_SIZE);
                water.solid = false;
                out.add(water);
            }
        }

        // --- named POI structures (once, in the chunk containing the POI) ---
        addPoiStructures(out, x0, z0);

        // --- cell content: 2x2 cells of 32 m per chunk ---
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                float ccx = x0 + 16f + i * 32f;
                float ccz = z0 + 16f + j * 32f;
                generateCell(out, rng, ccx, ccz);
            }
        }
        return out;
    }

    private void addRoadSlab(List<Obstacle> out, float cx, float cz, float w, float d) {
        float h = Terrain.heightAt(cx, cz);
        Obstacle slab = new Obstacle(Obstacle.Type.ROAD_SLAB, cx, h - 0.02f, cz,
                w, 0.24f, d);
        slab.solid = false;
        out.add(slab);
    }

    /** Landmark buildings that must exist exactly where their POI is. */
    private void addPoiStructures(List<Obstacle> out, float x0, float z0) {
        float x1 = x0 + CHUNK_SIZE, z1 = z0 + CHUNK_SIZE;

        // Walkable architectural interiors (Safehouse, Workshop, Garage, Ranger Hut)
        buildingInterior.populateChunkInteriors(out, x0, x1, z0, z1);

        // market
        if (in(120f, -60f, x0, x1, z0, z1)) {
            addBuilding(out, 120f, -60f, 24f, 7f, 18f);
        }
        // watchtower (tall landmark on the hills)
        if (in(120f, -360f, x0, x1, z0, z1)) {
            addBuilding(out, 120f, -360f, 10f, 22f, 10f);
        }
        // relay mast: base + tall antenna
        if (in(240f, -300f, x0, x1, z0, z1)) {
            addBuilding(out, 240f, -300f, 8f, 4f, 8f);
            float h = Terrain.heightAt(240f, -300f);
            Obstacle mast = new Obstacle(Obstacle.Type.ANTENNA, 240f, h + 4f + 11f,
                    -300f, 1.2f, 22f, 1.2f);
            out.add(mast);
        }
        // docks hut on the west bank
        if (in(-170f, 300f, x0, x1, z0, z1)) {
            addBuilding(out, -170f, 300f, 8f, 4f, 8f);
        }
    }

    private boolean in(float px, float pz, float x0, float x1, float z0, float z1) {
        return px >= x0 && px < x1 && pz >= z0 && pz < z1;
    }

    private void addBuilding(List<Obstacle> out, float x, float z, float w, float h, float d) {
        float ground = Terrain.heightAt(x, z);
        out.add(new Obstacle(Obstacle.Type.BUILDING, x, ground + h / 2f, z, w, h, d));
    }

    private void generateCell(List<Obstacle> out, java.util.Random rng, float ccx, float ccz) {
        OpenWorld.Region region = regionAt(ccx, ccz);
        switch (region) {
            case CITY_CORE:
                if (!nearRoad(ccx, ccz) && !nearAnyPoi(ccx, ccz)) {
                    addBuilding(out, ccx, ccz, 14f + rng.nextFloat() * 10f,
                            6f + rng.nextFloat() * 9f, 14f + rng.nextFloat() * 10f);
                    int crates = rng.nextInt(3);
                    for (int c = 0; c < crates; c++) {
                        addCrate(out, ccx + 10f + rng.nextFloat() * 6f,
                                ccz + (rng.nextFloat() * 14f - 7f), rng);
                    }
                } else if (nearRoad(ccx, ccz)) {
                    // Street lamp beside the road (solid thin pole).
                    Obstacle lamp = new Obstacle(Obstacle.Type.LAMP, ccx, 0f, ccz,
                            0.4f, 5f, 0.4f);
                    out.add(lamp);
                }
                break;
            case RESIDENTIAL:
                if (!nearRoad(ccx, ccz) && !nearAnyPoi(ccx, ccz)) {
                    addBuilding(out, ccx, ccz, 9f + rng.nextFloat() * 5f,
                            4f + rng.nextFloat() * 2.5f, 9f + rng.nextFloat() * 5f);
                    for (int b = 0; b < 2; b++) {
                        addBush(out, ccx + (rng.nextFloat() * 20f - 10f),
                                ccz + (rng.nextFloat() * 20f - 10f), rng);
                    }
                }
                if (rng.nextInt(3) == 0) {
                    addTree(out, ccx + (rng.nextFloat() * 24f - 12f),
                            ccz + (rng.nextFloat() * 24f - 12f), rng);
                }
                break;
            case RIVERSIDE_VILLAGE:
                if (!nearRoad(ccx, ccz) && !nearAnyPoi(ccx, ccz) && rng.nextInt(3) != 0) {
                    addBuilding(out, ccx, ccz, 7f + rng.nextFloat() * 5f,
                            3.2f + rng.nextFloat() * 1.8f, 7f + rng.nextFloat() * 5f);
                }
                for (int t = 0; t < 2; t++) {
                    addTree(out, ccx + (rng.nextFloat() * 26f - 13f),
                            ccz + (rng.nextFloat() * 26f - 13f), rng);
                }
                break;
            case WEST_FOREST:
                for (int t = 0; t < 3; t++) {
                    addTree(out, ccx + (rng.nextFloat() * 28f - 14f),
                            ccz + (rng.nextFloat() * 28f - 14f), rng);
                }
                if (rng.nextInt(3) == 0) {
                    addRock(out, ccx + (rng.nextFloat() * 20f - 10f),
                            ccz + (rng.nextFloat() * 20f - 10f), rng);
                }
                break;
            case NORTH_HILLS:
                if (rng.nextInt(2) == 0) {
                    addTree(out, ccx + (rng.nextFloat() * 24f - 12f),
                            ccz + (rng.nextFloat() * 24f - 12f), rng);
                }
                if (rng.nextInt(2) == 0) {
                    addRock(out, ccx + (rng.nextFloat() * 22f - 11f),
                            ccz + (rng.nextFloat() * 22f - 11f), rng);
                }
                break;
            case RIVER:
                if (rng.nextInt(4) == 0) {
                    float rx = ccx + (rng.nextFloat() * 20f - 10f);
                    float rz = ccz + (rng.nextFloat() * 40f - 20f);
                    if (Terrain.heightAt(rx, rz) > 0.5f) {
                        addRock(out, rx, rz, rng);
                    }
                }
                break;
            default: // OUTSKIRTS
                if (rng.nextInt(3) == 0) {
                    addTree(out, ccx + (rng.nextFloat() * 30f - 15f),
                            ccz + (rng.nextFloat() * 30f - 15f), rng);
                }
                if (rng.nextInt(4) == 0) {
                    addRock(out, ccx + (rng.nextFloat() * 24f - 12f),
                            ccz + (rng.nextFloat() * 24f - 12f), rng);
                }
                break;
        }
    }

    private void addTree(List<Obstacle> out, float x, float z, java.util.Random rng) {
        float ground = Terrain.heightAt(x, z);
        if (ground < Terrain.WATER_LEVEL + 0.5f) return; // no trees in water
        float trunkH = 4f + rng.nextFloat() * 2f;
        Obstacle tree = new Obstacle(Obstacle.Type.TREE, x, ground + trunkH / 2f, z,
                0.9f, trunkH, 0.9f);
        out.add(tree);
    }

    private void addRock(List<Obstacle> out, float x, float z, java.util.Random rng) {
        float ground = Terrain.heightAt(x, z);
        if (ground < Terrain.WATER_LEVEL + 0.3f) return;
        float w = 1.6f + rng.nextFloat() * 1.6f;
        float h = 1.0f + rng.nextFloat() * 1.6f;
        Obstacle rock = new Obstacle(Obstacle.Type.ROCK, x, ground + h / 2f, z,
                w, h, w * (0.8f + rng.nextFloat() * 0.4f));
        out.add(rock);
    }

    private void addBush(List<Obstacle> out, float x, float z, java.util.Random rng) {
        float ground = Terrain.heightAt(x, z);
        if (ground < Terrain.WATER_LEVEL + 0.3f) return;
        Obstacle bush = new Obstacle(Obstacle.Type.BUSH, x, ground + 0.4f, z,
                1.4f, 0.8f, 1.4f);
        bush.solid = false;
        out.add(bush);
    }

    private void addCrate(List<Obstacle> out, float x, float z, java.util.Random rng) {
        float ground = Terrain.heightAt(x, z);
        Obstacle crate = new Obstacle(Obstacle.Type.CRATE, x, ground + 0.6f, z,
                1.2f, 1.2f, 1.2f);
        out.add(crate);
    }

    private boolean nearRoad(float x, float z) {
        for (float[] r : ROADS) {
            if (x + 14f > r[0] && x - 14f < r[1] && z + 14f > r[2] && z - 14f < r[3]) {
                return true;
            }
        }
        return false;
    }

    private boolean nearAnyPoi(float x, float z) {
        for (Poi p : pois) {
            if (Math.abs(x - p.x) < 28f && Math.abs(z - p.z) < 28f) return true;
        }
        return false;
    }
}
