package com.devagent.realworldsurvival3d;

import java.util.ArrayList;
import java.util.List;

/**
 * WorldMap - Constructs the multi-tier 3D parkour cityscape.
 * Contains skyscrapers, wall-run billboards, balance beams, springboards,
 * hazard zones, collectible spawns, and extraction helipads.
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

        // Building 2: Tower Beta (East, slightly higher)
        obstacles.add(new Obstacle(Obstacle.Type.BUILDING, 34f, 12.5f, 0, 24f, 25f, 28f)); // Top at y=25
        obstacles.add(new Obstacle(Obstacle.Type.VAULT_BARRIER, 34f, 25.6f, -6f, 16f, 1.2f, 0.5f));
        // Wall-run billboard connecting Tower Alpha and Tower Beta across chasm
        obstacles.add(new Obstacle(Obstacle.Type.WALLRUN_PANEL, 17f, 24f, 14f, 14f, 7f, 0.6f));

        // Building 3: Tower Gamma (North-East, High-Rise)
        obstacles.add(new Obstacle(Obstacle.Type.BUILDING, 35f, 15f, 38f, 24f, 30f, 26f)); // Top at y=30
        // Wall-run billboard between Tower Beta and Tower Gamma
        obstacles.add(new Obstacle(Obstacle.Type.WALLRUN_PANEL, 47f, 28f, 19f, 0.6f, 8f, 16f));

        // Building 4: The Apex Tower (North, Extraction Site)
        obstacles.add(new Obstacle(Obstacle.Type.BUILDING, 65f, 16f, 60f, 28f, 32f, 28f)); // Top at y=32
        // Extraction Helipad on Apex Tower
        obstacles.add(new Obstacle(Obstacle.Type.EXTRACTION_HELIPAD, 65f, 32.2f, 60f, 10f, 0.4f, 10f));
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

        // Building 6: Tower Epsilon (West, Stepped Climbing Tower)
        obstacles.add(new Obstacle(Obstacle.Type.BUILDING, -36f, 11f, 0, 24f, 22f, 26f)); // Top at y=22
        // Wall-run billboard Alpha to Epsilon
        obstacles.add(new Obstacle(Obstacle.Type.WALLRUN_PANEL, -18f, 23f, -13f, 14f, 7f, 0.6f));
        // Jump springboard on Epsilon
        obstacles.add(new Obstacle(Obstacle.Type.JUMP_PAD, -36f, 22.2f, 5f, 4f, 0.4f, 4f));

        // Building 7: High Observation Spire (North-West)
        obstacles.add(new Obstacle(Obstacle.Type.BUILDING, -38f, 19f, 40f, 20f, 38f, 20f)); // Top at y=38
        obstacles.add(new Obstacle(Obstacle.Type.VAULT_BARRIER, -38f, 38.6f, 40f, 6f, 1.2f, 6f));

        // Building 8: Mid-Way Relay Tower
        obstacles.add(new Obstacle(Obstacle.Type.BUILDING, 0, 14f, 42f, 22f, 28f, 22f)); // Top at y=28
        // Balance beam connecting Tower Alpha to Mid-Way Relay
        obstacles.add(new Obstacle(Obstacle.Type.BALANCE_BEAM, 0, 22f, 21f, 0.6f, 0.4f, 18f));

        // Building 9: South-East Crane Platform
        obstacles.add(new Obstacle(Obstacle.Type.BUILDING, 38f, 13f, -42f, 22f, 26f, 22f)); // Top at y=26
        obstacles.add(new Obstacle(Obstacle.Type.WALLRUN_PANEL, 20f, 24f, -42f, 16f, 6f, 0.6f));

        // Building 10: Far West Outpost
        obstacles.add(new Obstacle(Obstacle.Type.BUILDING, -70f, 12f, 10f, 26f, 24f, 26f)); // Top at y=24
        obstacles.add(new Obstacle(Obstacle.Type.BALANCE_BEAM, -53f, 23f, 5f, 14f, 0.4f, 0.6f));
    }
}
