package com.devagent.realworldsurvival3d;

/**
 * Terrain - Analytic heightfield for the open world (PURE JAVA, JVM-testable).
 *
 * World: 896 m x 896 m, spanning -448..+448 on X and Z, Y up.
 * Height is a closed-form function of (x, z) so collision, rendering, and the
 * world map all sample the exact same surface:
 *
 *   1. rolling base noise (three sine octaves)
 *   2. north hills (z < -240 rise up to ~+28 m)
 *   3. west forest rise (x < -240 rises to +5 m)
 *   4. flat zones blended in with a 24 m margin:
 *        city core -> 0.0, residential -> 0.0, riverside village -> 0.8,
 *        bridge corridor -> 1.2
 *   5. river carve along riverCenterX(z) mixing down to a bed at -4.8 m
 *
 * WATER_LEVEL = -2.2. Depth = WATER_LEVEL - heightAt. Players wade up to
 * WADE_MAX = 1.5 m; deeper water is impassable (Player enforces the block).
 *
 * Chunk meshes: 64 m chunks subdivided on a 16 m grid (4x4 quads = 32
 * triangles, 96 vertices). Vertex layout matches the existing world shader:
 * pos3, normal3, uv2 (stride 8 floats). UVs map each quad into a 4-tile
 * procedural atlas (grass / rock / sand / asphalt) chosen per quad.
 */
public final class Terrain {

    // ------------------------------------------------------------------
    // World layout
    // ------------------------------------------------------------------
    public static final int WORLD_HALF = 448;      // world spans -448..+448
    public static final int CHUNK_SIZE = 64;
    public static final int CHUNKS_PER_SIDE = 14;  // 14 x 14 = 196 chunks
    public static final int MESH_STEP = 16;        // 16 m mesh grid

    // ------------------------------------------------------------------
    // Water
    // ------------------------------------------------------------------
    public static final float WATER_LEVEL = -2.2f;
    public static final float WADE_MAX = 1.5f;

    // ------------------------------------------------------------------
    // Flat zone rectangles (x0, x1, z0, z1) - shared with OpenWorld
    // ------------------------------------------------------------------
    public static final float CITY_X0 = 8f, CITY_X1 = 304f, CITY_Z0 = -144f, CITY_Z1 = 144f;
    public static final float RES_X0 = -112f, RES_X1 = 24f, RES_Z0 = -256f, RES_Z1 = -48f;
    public static final float VIL_X0 = -240f, VIL_X1 = 24f, VIL_Z0 = 192f, VIL_Z1 = 384f;
    public static final float BRIDGE_Z = 240f;
    public static final float BRIDGE_HALF_Z = 28f; // corridor z 212..268
    public static final float BRIDGE_HALF_X = 85f; // covers both approach roads

    private static final float MARGIN = 24f;       // flatten blend margin
    private static final float CITY_TARGET = 0f;
    private static final float RES_TARGET = 0f;
    private static final float VIL_TARGET = 0.8f;
    private static final float BRIDGE_TARGET = 1.2f;

    /** Instance usable as a marker object (all logic is static). */
    public Terrain() {}

    // ==================================================================
    // Height field
    // ==================================================================

    /** River center line: x = -190 + 45*sin(z*0.0016 + 0.5). */
    public static float riverCenterX(float z) {
        return -190f + 45f * (float) Math.sin(z * 0.0016f + 0.5f);
    }

    /** Ground height at world (x, z). Clamped outside the world bounds. */
    public static float heightAt(float x, float z) {
        if (x > WORLD_HALF) x = WORLD_HALF; else if (x < -WORLD_HALF) x = -WORLD_HALF;
        if (z > WORLD_HALF) z = WORLD_HALF; else if (z < -WORLD_HALF) z = -WORLD_HALF;

        // 1) rolling base
        float h = 1.6f * (float) Math.sin(x * 0.021f) * (float) Math.cos(z * 0.017f)
                + 1.1f * (float) Math.sin(x * 0.043f + 1.7f) * (float) Math.cos(z * 0.031f + 0.6f)
                + 0.55f * (float) Math.sin(x * 0.087f + 4.2f) * (float) Math.sin(z * 0.079f + 2.1f);

        // 2) north hills
        float tN = smooth01(clamp((-240f - z) / 190f));
        h += tN * (22f + 6f * (float) Math.sin(x * 0.045f + 1f) * (float) Math.sin(z * 0.03f));

        // 3) west forest rise
        float tW = smooth01(clamp((-240f - x) / 120f));
        h += tW * 5f;

        // 4) flat zones (before the carve so the river channel survives)
        h = mix(h, CITY_TARGET, rectWeight(x, z, CITY_X0, CITY_X1, CITY_Z0, CITY_Z1));
        h = mix(h, RES_TARGET, rectWeight(x, z, RES_X0, RES_X1, RES_Z0, RES_Z1));
        h = mix(h, VIL_TARGET, rectWeight(x, z, VIL_X0, VIL_X1, VIL_Z0, VIL_Z1));
        float bcx = riverCenterX(BRIDGE_Z);
        h = mix(h, BRIDGE_TARGET,
                rectWeight(x, z, bcx - BRIDGE_HALF_X, bcx + BRIDGE_HALF_X,
                        BRIDGE_Z - BRIDGE_HALF_Z, BRIDGE_Z + BRIDGE_HALF_Z));

        // 5) river carve: full channel inside |dx|<=20, fades out at |dx|=34
        float dx = x - riverCenterX(z);
        float t = 1f - smooth01(clamp((Math.abs(dx) - 20f) / 14f));
        if (t > 0f) {
            float bed = WATER_LEVEL - 2.6f * t;
            h = h * (1f - t) + bed * t;
        }
        return h;
    }

    /** Surface normal at (x, z), written into out[3]. */
    public static void normalAt(float x, float z, float[] out) {
        float e = 0.5f;
        float nx = heightAt(x - e, z) - heightAt(x + e, z);
        float nz = heightAt(x, z - e) - heightAt(x, z + e);
        float ny = 2f * e;
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        out[0] = nx / len;
        out[1] = ny / len;
        out[2] = nz / len;
    }

    /** 0 = flat, 1 = vertical. Cheap steepness measure (1 - normal.y). */
    public static float slope01(float x, float z, float[] scratch) {
        normalAt(x, z, scratch);
        return 1f - scratch[1];
    }

    /** Water depth below WATER_LEVEL at (x, z); 0 on dry ground. */
    public static float waterDepthAt(float x, float z) {
        float d = WATER_LEVEL - heightAt(x, z);
        return d > 0f ? d : 0f;
    }

    // ==================================================================
    // Chunk mesh building
    // ==================================================================

    /** Built vertex array for one chunk: pos3 normal3 uv2 (8 floats/vertex). */
    public static final class MeshData {
        public final float[] verts;
        public final int vertexCount;
        MeshData(float[] verts) {
            this.verts = verts;
            this.vertexCount = verts.length / 8;
        }
    }

    // Atlas tiles: 2x2 grid, GL space (ty 0 = bottom row of the texture).
    public static final int TILE_GRASS = 0, TILE_ROCK = 1, TILE_SAND = 2, TILE_ASPHALT = 3;

    /**
     * Builds the triangle mesh for chunk (col, row). Deterministic.
     * Winding: CCW seen from above (front faces are up-facing).
     */
    public static MeshData buildChunkMesh(int col, int row) {
        int quads = CHUNK_SIZE / MESH_STEP; // 4
        float x0 = col * CHUNK_SIZE - WORLD_HALF;
        float z0 = row * CHUNK_SIZE - WORLD_HALF;
        float[] out = new float[quads * quads * 6 * 8];
        int o = 0;
        float[] nA = new float[3], nB = new float[3], nC = new float[3], nD = new float[3];
        for (int qz = 0; qz < quads; qz++) {
            for (int qx = 0; qx < quads; qx++) {
                float ax = x0 + qx * MESH_STEP, az = z0 + qz * MESH_STEP;
                float bx = ax + MESH_STEP, bz = az + MESH_STEP;
                int tile = tileFor((ax + bx) * 0.5f, (az + bz) * 0.5f);
                float u0 = tileU0(tile), u1 = tileU1(tile);
                float v0 = tileV0(tile), v1 = tileV1(tile);
                float ha = heightAt(ax, az);
                float hb = heightAt(bx, az);
                float hc = heightAt(bx, bz);
                float hd = heightAt(ax, bz);
                normalAt(ax, az, nA);
                normalAt(bx, az, nB);
                normalAt(bx, bz, nC);
                normalAt(ax, bz, nD);
                // Tri 1: a -> c -> b ; Tri 2: a -> d -> c (both face +Y)
                o = emit(out, o, ax, ha, az, nA, u0, v0);
                o = emit(out, o, bx, hc, bz, nC, u1, v1);
                o = emit(out, o, bx, hb, az, nB, u1, v0);
                o = emit(out, o, ax, ha, az, nA, u0, v0);
                o = emit(out, o, ax, hd, bz, nD, u0, v1);
                o = emit(out, o, bx, hc, bz, nC, u1, v1);
            }
        }
        return new MeshData(out);
    }

    private static int emit(float[] out, int o, float x, float y, float z,
                            float[] n, float u, float v) {
        out[o] = x; out[o + 1] = y; out[o + 2] = z;
        out[o + 3] = n[0]; out[o + 4] = n[1]; out[o + 5] = n[2];
        out[o + 6] = u; out[o + 7] = v;
        return o + 8;
    }

    /**
     * Texture tile for a mesh quad, chosen by height, slope and zone:
     * sand near/below water, rock on steep or high ground, asphalt inside the
     * city core / bridge corridor, grass everywhere else.
     */
    public static int tileFor(float x, float z) {
        float h = heightAt(x, z);
        if (h < WATER_LEVEL + 0.6f) return TILE_SAND;
        float[] scratch = new float[3];
        if (slope01(x, z, scratch) > 0.5f || h > 18f) return TILE_ROCK;
        if (x >= CITY_X0 && x <= CITY_X1 && z >= CITY_Z0 && z <= CITY_Z1) return TILE_ASPHALT;
        float bcx = riverCenterX(BRIDGE_Z);
        if (Math.abs(x - bcx) <= BRIDGE_HALF_X && Math.abs(z - BRIDGE_Z) <= BRIDGE_HALF_Z) {
            return TILE_ASPHALT;
        }
        return TILE_GRASS;
    }

    // Atlas UV window per tile (6% inset keeps atlas bleed away).
    private static final float INSET = 0.06f;
    private static final float TILE = 0.5f;

    public static float tileU0(int tile) {
        float u = (tile % 2) * TILE;
        return u + INSET * TILE;
    }

    public static float tileU1(int tile) {
        float u = (tile % 2) * TILE + TILE;
        return u - INSET * TILE;
    }

    public static float tileV0(int tile) {
        float v = (tile / 2) * TILE; // ty 0 bottom row (GL space)
        return v + INSET * TILE;
    }

    public static float tileV1(int tile) {
        float v = (tile / 2) * TILE + TILE;
        return v - INSET * TILE;
    }

    // ==================================================================
    // Math helpers
    // ==================================================================
    private static float clamp(float v) {
        return v < 0f ? 0f : (v > 1f ? 1f : v);
    }

    private static float smooth01(float t) {
        return t * t * (3f - 2f * t);
    }

    private static float mix(float a, float b, float t) {
        return a * (1f - t) + b * t;
    }

    /** 1 inside the rect interior, fading to 0 across MARGIN outside it. */
    public static float rectWeight(float x, float z, float x0, float x1, float z0, float z1) {
        float wx = smooth01(clamp((x - x0) / MARGIN)) * smooth01(clamp((x1 - x) / MARGIN));
        float wz = smooth01(clamp((z - z0) / MARGIN)) * smooth01(clamp((z1 - z) / MARGIN));
        return wx * wz;
    }
}
