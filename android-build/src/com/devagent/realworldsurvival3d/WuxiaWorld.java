package com.devagent.realworldsurvival3d;

import java.util.ArrayList;
import java.util.List;

/**
 * WuxiaWorld - Open World Regions, Modular Streaming, and Fast Travel System (Phase 9).
 * Features:
 *   - 7 Wuxia Regional Zones (Ancient City, Bamboo Grove, Mountain Hamlet, Dragon Temple, Azure River, Training Grounds, Stele Ruins)
 *   - Fast Travel Shrines and Discovery Tracker
 *   - Exploration percentage calculation
 *   - Pure Java (100% JVM testable)
 */
public class WuxiaWorld {

    public enum WuxiaRegion {
        ANCIENT_CITY("Walled Pavilion City", "Imperial trading hub surrounded by high stone ramparts and tiered pagodas.", 150f, 20f),
        BAMBOO_FOREST("Emerald Bamboo Grove", "Dense stalks of towering green bamboo veiled in early morning mist.", -280f, 120f),
        MOUNTAIN_VILLAGE("Mist Peak Hamlet", "Terraced timber dwellings clinging to the foothills beside winding streams.", 260f, -240f),
        ANCIENT_TEMPLE("Dragon Cloud Monastery", "Sacred mountain sanctuary of martial monks perched upon the northern ridge.", 120f, -380f),
        RIVER_VALLEY("Azure Dragon River", "Winding river canyon traversed by arched stone bridges and willow trees.", 0f, 0f),
        TRAINING_GROUNDS("Martial Arts Training Grounds", "Open stone courtyards equipped with wooden dummies and weapon racks.", 180f, 80f),
        DESERT_RUINS("Ancient Stele Ruins", "Weathered stone monuments bearing lost martial arts inscriptions.", -340f, -280f);

        public final String title;
        public final String description;
        public final float centerX, centerZ;

        WuxiaRegion(String title, String description, float centerX, float centerZ) {
            this.title = title;
            this.description = description;
            this.centerX = centerX;
            this.centerZ = centerZ;
        }
    }

    public static class FastTravelShrine {
        public final String id;
        public final String name;
        public final WuxiaRegion region;
        public final float x, y, z;
        public boolean discovered = false;

        public FastTravelShrine(String id, String name, WuxiaRegion region, float x, float y, float z) {
            this.id = id;
            this.name = name;
            this.region = region;
            this.x = x; this.y = y; this.z = z;
        }
    }

    public final List<FastTravelShrine> shrines = new ArrayList<>();
    public final List<WuxiaRegion> visitedRegions = new ArrayList<>();
    public int stelesDiscovered = 0;
    public static final int TOTAL_STELES = 5;

    public WuxiaWorld() {
        shrines.add(new FastTravelShrine("city_shrine", "Pavilion Gate Shrine", WuxiaRegion.ANCIENT_CITY, 150f, 2.0f, 20f));
        shrines.add(new FastTravelShrine("bamboo_shrine", "Bamboo Wind Shrine", WuxiaRegion.BAMBOO_FOREST, -280f, 8.0f, 120f));
        shrines.add(new FastTravelShrine("village_shrine", "Mist Peak Shrine", WuxiaRegion.MOUNTAIN_VILLAGE, 260f, 4.0f, -240f));
        shrines.add(new FastTravelShrine("temple_shrine", "Dragon Monastery Shrine", WuxiaRegion.ANCIENT_TEMPLE, 120f, 18.0f, -380f));
        shrines.add(new FastTravelShrine("river_shrine", "Azure Shore Shrine", WuxiaRegion.RIVER_VALLEY, 0f, 1.5f, 0f));

        // Start with the city gate shrine discovered
        shrines.get(0).discovered = true;
        visitedRegions.add(WuxiaRegion.ANCIENT_CITY);
    }

    /** Returns current region based on player coordinates. */
    public WuxiaRegion getRegionAt(float x, float z) {
        if (z < -300f && Math.abs(x - 120f) < 140f) return WuxiaRegion.ANCIENT_TEMPLE;
        if (x < -200f && z < -180f) return WuxiaRegion.DESERT_RUINS;
        if (x < -180f) return WuxiaRegion.BAMBOO_FOREST;
        if (x > 180f && z < -150f) return WuxiaRegion.MOUNTAIN_VILLAGE;
        if (Math.abs(x - Terrain.riverCenterX(z)) < 36f) return WuxiaRegion.RIVER_VALLEY;
        if (x >= 100f && x <= 220f && z >= 40f && z <= 120f) return WuxiaRegion.TRAINING_GROUNDS;
        if (x >= 40f && x <= 260f && z >= -100f && z <= 100f) return WuxiaRegion.ANCIENT_CITY;
        return WuxiaRegion.RIVER_VALLEY;
    }

    /** Updates player position, discovers nearby shrines, and records visited regions. */
    public void update(float px, float py, float pz) {
        WuxiaRegion cur = getRegionAt(px, pz);
        if (!visitedRegions.contains(cur)) {
            visitedRegions.add(cur);
        }

        for (FastTravelShrine shrine : shrines) {
            if (!shrine.discovered) {
                float dx = px - shrine.x;
                float dz = pz - shrine.z;
                if (dx * dx + dz * dz < 20f * 20f) {
                    shrine.discovered = true;
                }
            }
        }
    }

    /** Teleports player to a discovered shrine. */
    public boolean fastTravelTo(String shrineId, Player player) {
        for (FastTravelShrine s : shrines) {
            if (s.id.equals(shrineId) && s.discovered) {
                player.x = s.x;
                player.y = s.y;
                player.z = s.z;
                player.vx = 0f;
                player.vy = 0f;
                player.vz = 0f;
                return true;
            }
        }
        return false;
    }

    /** Calculates total open-world exploration completion percentage (0 to 100%). */
    public int getExplorationPercentage() {
        int regionPoints = (visitedRegions.size() * 50) / WuxiaRegion.values().length;
        int shrinePoints = 0;
        for (FastTravelShrine s : shrines) {
            if (s.discovered) shrinePoints++;
        }
        shrinePoints = (shrinePoints * 35) / shrines.size();
        int stelePoints = (stelesDiscovered * 15) / Math.max(1, TOTAL_STELES);
        return Math.min(100, regionPoints + shrinePoints + stelePoints);
    }
}
