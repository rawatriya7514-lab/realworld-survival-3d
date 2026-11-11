package com.devagent.realworldsurvival3d;

import java.util.ArrayList;
import java.util.List;

/**
 * WorldMap - Constructs the multi-tier 3D parkour cityscape.
 * Contains skyscrapers, wall-run billboards, balance beams, springboards,
 * hazard zones, rooftop dressing props, collectible spawns, and the
 * extraction helipad with its signal console.
 *
 * The main parkour course (Tower Alpha -> Beta -> Gamma -> Apex, plus the
 * Delta/Epsilon/Relay/Spire side loop) must stay stable: missions and item
 * spawns are tuned to it.
 */
public class WorldMap {
    public List<Obstacle> obstacles;
    public float extractionX = 65f;
    public float extractionY = 32f;
    public float extractionZ = 60f;

    public WorldMap() {
        obstacles = new ArrayList<>();
        buildCityParkourWorld();
    }

    private void buildCityParkourWorld() {
        obstacles.clear();

        // 1. Base ground plane / street floor (Toxic Hazard Level below rooftops)
        obstacles.add(new Obstacle(Obstacle.Type.HAZARD_POOL, 0, -2f, 0, 300f, 4f, 300f));

        // 1b. Street-level filler buildings for city depth (solid, landable)
        addStreetFillerBuildings();

        // 2. Skyscraper Cluster (Rooftops ranging from Y=12m to Y=45m)
        // Building 1: Starting Hub (Tower Alpha)
        obstacles.add(new Obstacle(Obstacle.Type.BUILDING, 0, 10f, 0, 26f, 20f, 26f)); // Top at y=20
        // Parapets / Vault barriers on Tower Alpha
        obstacles.add(new Obstacle(Obstacle.Type.VAULT_BARRIER, 0, 20.6f, -12.5f, 20f, 1.2f, 0.4f));
        obstacles.add(new Obstacle(Obstacle.Type.VAULT_BARRIER, 0, 20.6f, 12.5f, 20f, 1.2f, 0.4f));
        // AC Air ducts on Tower Alpha
        obstacles.add(new Obstacle(Obstacle.Type.VAULT_BARRIER, -6f, 20.6f, 3f, 3f, 1.2f, 4f));
        // Jump Pad on Tower Alpha to launch player across to Tower Beta
        obstacles.add(new Obstacle(Obstacle.Type.JUMP_PAD, 8f, 20.2f, 0, 4f, 0.4f, 4f));
        // Rooftop dressing on Alpha
        addProp(Obstacle.Type.VENT, 5f, 20f, 5f, 2.0f, 0.8f, 1.4f);
        addProp(Obstacle.Type.AC_UNIT, -8f, 20f, -6f, 2.4f, 1.2f, 2.0f);
        addProp(Obstacle.Type.VENT, 9f, 20f, -7f, 1.6f, 0.7f, 1.2f);

        // Building 2: Tower Beta (East, slightly higher)
        obstacles.add(new Obstacle(Obstacle.Type.BUILDING, 34f, 12.5f, 0, 24f, 25f, 28f)); // Top at y=25
        obstacles.add(new Obstacle(Obstacle.Type.VAULT_BARRIER, 34f, 25.6f, -6f, 16f, 1.2f, 0.5f));
        // Wall-run billboard connecting Tower Alpha and Tower Beta across chasm
        obstacles.add(new Obstacle(Obstacle.Type.WALLRUN_PANEL, 17f, 24f, 14f, 14f, 7f, 0.6f));
        // Rooftop dressing on Beta
        addProp(Obstacle.Type.AC_UNIT, 30f, 25f, 8f, 2.6f, 1.4f, 2.2f);
        addProp(Obstacle.Type.VENT, 38f, 25f, 3f, 2.0f, 0.8f, 1.4f);
        addProp(Obstacle.Type.WATER_TANK, 39f, 25f, -8f, 2.2f, 2.4f, 2.2f);

        // Building 3: Tower Gamma (North-East, High-Rise)
        obstacles.add(new Obstacle(Obstacle.Type.BUILDING, 35f, 15f, 38f, 24f, 30f, 26f)); // Top at y=30
        // Wall-run billboard between Tower Beta and Tower Gamma
        obstacles.add(new Obstacle(Obstacle.Type.WALLRUN_PANEL, 47f, 28f, 19f, 0.6f, 8f, 16f));
        // Dressing on Gamma
        addProp(Obstacle.Type.VENT, 30f, 30f, 33f, 2.0f, 0.8f, 1.6f);
        addProp(Obstacle.Type.AC_UNIT, 40f, 30f, 45f, 2.4f, 1.3f, 2.0f);
        addProp(Obstacle.Type.VENT, 35f, 30f, 47f, 1.8f, 0.7f, 1.2f);

        // Building 4: The Apex Tower (North, Extraction Site)
        obstacles.add(new Obstacle(Obstacle.Type.BUILDING, 65f, 16f, 60f, 28f, 32f, 28f)); // Top at y=32
        // Extraction Helipad on Apex Tower
        obstacles.add(new Obstacle(Obstacle.Type.EXTRACTION_HELIPAD, 65f, 32.2f, 60f, 10f, 0.4f, 10f));
        // Signal console: mission 3 requires interacting with this to call extraction
        addProp(Obstacle.Type.CONSOLE, 61f, 32.4f, 60f, 1.2f, 1.1f, 1.2f);
        addProp(Obstacle.Type.ANTENNA, 70f, 32f, 53f, 0.5f, 5.5f, 0.5f);
        extractionX = 65f;
        extractionY = 32f;
        extractionZ = 60f;

        // Narrow Balance Steel Beam connecting Tower Gamma to Apex Tower
        obstacles.add(new Obstacle(Obstacle.Type.BALANCE_BEAM, 50f, 30.5f, 49f, 0.6f, 0.4f, 22f));

        // Building 5: Tower Delta (South, Lower Industrial)
        obstacles.add(new Obstacle(Obstacle.Type.BUILDING, -4f, 9f, -36f, 26f, 18f, 24f)); // Top at y=18
        // Steel Beam from Alpha to Delta
        obstacles.add(new Obstacle(Obstacle.Type.BALANCE_BEAM, -2f, 18.5f, -18f, 0.6f, 0.4f, 16f));
        // Wall-run panel on West side of Delta chasm
        obstacles.add(new Obstacle(Obstacle.Type.WALLRUN_PANEL, -17f, 21f, -18f, 0.6f, 7f, 16f));
        // Dressing on Delta
        addProp(Obstacle.Type.WATER_TANK, -10f, 18f, -40f, 2.4f, 2.6f, 2.4f);
        addProp(Obstacle.Type.VENT, 2f, 18f, -34f, 2.0f, 0.8f, 1.4f);
        addProp(Obstacle.Type.AC_UNIT, -12f, 18f, -30f, 2.4f, 1.3f, 2.0f);

        // Building 6: Tower Epsilon (West, Stepped Climbing Tower)
        obstacles.add(new Obstacle(Obstacle.Type.BUILDING, -36f, 11f, 0, 24f, 22f, 26f)); // Top at y=22
        // Wall-run billboard Alpha to Epsilon
        obstacles.add(new Obstacle(Obstacle.Type.WALLRUN_PANEL, -18f, 23f, -13f, 14f, 7f, 0.6f));
        // Jump springboard on Epsilon
        obstacles.add(new Obstacle(Obstacle.Type.JUMP_PAD, -36f, 22.2f, 5f, 4f, 0.4f, 4f));
        // Dressing on Epsilon
        addProp(Obstacle.Type.WATER_TANK, -42f, 22f, -6f, 2.4f, 2.6f, 2.4f);
        addProp(Obstacle.Type.VENT, -32f, 22f, 9f, 1.8f, 0.7f, 1.2f);
        addProp(Obstacle.Type.ANTENNA, -44f, 22f, 8f, 0.4f, 4.5f, 0.4f);

        // Building 7: High Observation Spire (North-West)
        obstacles.add(new Obstacle(Obstacle.Type.BUILDING, -38f, 19f, 40f, 20f, 38f, 20f)); // Top at y=38
        obstacles.add(new Obstacle(Obstacle.Type.VAULT_BARRIER, -38f, 38.6f, 40f, 6f, 1.2f, 6f));
        addProp(Obstacle.Type.ANTENNA, -42f, 38f, 44f, 0.5f, 6.0f, 0.5f);
        addProp(Obstacle.Type.AC_UNIT, -33f, 38f, 36f, 2.2f, 1.3f, 2.0f);

        // Building 8: Mid-Way Relay Tower
        obstacles.add(new Obstacle(Obstacle.Type.BUILDING, 0, 14f, 42f, 22f, 28f, 22f)); // Top at y=28
        // Balance beam connecting Tower Alpha to Mid-Way Relay
        obstacles.add(new Obstacle(Obstacle.Type.BALANCE_BEAM, 0, 22f, 21f, 0.6f, 0.4f, 18f));
        // Dressing on Relay
        addProp(Obstacle.Type.VENT, -4f, 28f, 38f, 2.0f, 0.8f, 1.4f);
        addProp(Obstacle.Type.AC_UNIT, 5f, 28f, 46f, 2.4f, 1.3f, 2.0f);
        addProp(Obstacle.Type.VENT, 2f, 28f, 48f, 1.6f, 0.7f, 1.2f);

        // Building 9: South-East Crane Platform
        obstacles.add(new Obstacle(Obstacle.Type.BUILDING, 38f, 13f, -42f, 22f, 26f, 22f)); // Top at y=26
        obstacles.add(new Obstacle(Obstacle.Type.WALLRUN_PANEL, 20f, 24f, -42f, 16f, 6f, 0.6f));
        addProp(Obstacle.Type.WATER_TANK, 33f, 26f, -46f, 2.4f, 2.6f, 2.4f);
        addProp(Obstacle.Type.VENT, 43f, 26f, -38f, 1.8f, 0.7f, 1.2f);

        // Building 10: Far West Outpost
        obstacles.add(new Obstacle(Obstacle.Type.BUILDING, -70f, 12f, 10f, 26f, 24f, 26f)); // Top at y=24
        obstacles.add(new Obstacle(Obstacle.Type.BALANCE_BEAM, -53f, 23f, 5f, 14f, 0.4f, 0.6f));
        addProp(Obstacle.Type.AC_UNIT, -64f, 24f, 4f, 2.4f, 1.3f, 2.0f);
        addProp(Obstacle.Type.VENT, -76f, 24f, 16f, 2.0f, 0.8f, 1.4f);
        addProp(Obstacle.Type.WATER_TANK, -77f, 24f, 4f, 2.2f, 2.4f, 2.2f);
    }

    private void addProp(Obstacle.Type type, float x, float y, float z,
                         float w, float h, float d) {
        // Prop y is the TOP surface height; convert to center for the collider
        obstacles.add(new Obstacle(type, x, y + h / 2f, z, w, h, d));
    }

    private void addStreetFillerBuildings() {
        // Solid low buildings between the main towers; add skyline depth and
        // give falling players survivable intermediate landings.
        float[][] fillers = {
                {-18f, 5f, 24f},   {14f, 6f, -20f},  {-26f, 7f, -22f},
                {22f, 8f, 20f},    {-24f, 6f, 26f},  {52f, 7f, -18f},
                {-58f, 6f, -16f},  {-14f, 5f, 14f},  {18f, 5f, 34f},
                {-46f, 8f, -34f},  {44f, 6f, 12f},   {-56f, 7f, 30f},
                {56f, 6f, 34f},    {8f, 7f, -44f},   {-8f, 6f, -50f},
        };
        for (float[] f : fillers) {
            float top = 5f + (f[1] - 5f); // top height from row (5..8m)
            obstacles.add(new Obstacle(Obstacle.Type.BUILDING,
                    f[0], top / 2f, f[2], 12f, top, 12f));
        }
    }

    /**
     * Highest solid surface at or just below (nearY + 0.6) under a world XZ
     * point. Returns -999 when there is nothing (open sky / off map).
     */
    public float getFloorY(float x, float z, float nearY) {
        float best = -999f;
        for (Obstacle o : obstacles) {
            if (o.type == Obstacle.Type.WALLRUN_PANEL) continue; // thin airborne panels
            if (o.containsXZ(x, z, 0.0f)) {
                float top = o.getMaxY();
                if (top <= nearY + 0.6f && top > best) best = top;
            }
        }
        return best;
    }

    /**
     * The building obstacle whose roof contains (x,z), or null.
     */
    public Obstacle buildingAt(float x, float z) {
        for (Obstacle o : obstacles) {
            if (o.type == Obstacle.Type.BUILDING && o.containsXZ(x, z, 0.0f)) {
                return o;
            }
        }
        return null;
    }
}
