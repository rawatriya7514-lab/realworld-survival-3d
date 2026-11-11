package com.devagent.realworldsurvival3d;

import java.util.ArrayList;
import java.util.List;

/**
 * BuildingInterior - Pure Java generator and collision model for walkable
 * architectural building interiors with doorways, ceilings, and survival props.
 *
 * Implements Phase 7 Environment requirements:
 *  - Walkable building interiors for key POIs (Safehouse, Workshop, Garage, Ranger Hut)
 *  - Accurate door openings allowing seamless player entry and exit
 *  - Enclosed perimeter walls with physical collision (AABB)
 *  - Solid ceiling/roof slabs providing overhead cover and parkour roof surfaces
 *  - Interior furniture & survival props (workbenches, survival cots, supply crates)
 */
public class BuildingInterior {

    public static final class InteriorDefinition {
        public final String name;
        public final float centerX, centerZ;
        public final float width, depth, height;
        public final float doorX, doorZ, doorWidth, doorHeight;
        public final List<Obstacle> obstacles = new ArrayList<>();

        public InteriorDefinition(String name, float centerX, float centerZ,
                                  float width, float depth, float height,
                                  float doorX, float doorZ, float doorWidth, float doorHeight) {
            this.name = name;
            this.centerX = centerX;
            this.centerZ = centerZ;
            this.width = width;
            this.depth = depth;
            this.height = height;
            this.doorX = doorX;
            this.doorZ = doorZ;
            this.doorWidth = doorWidth;
            this.doorHeight = doorHeight;
        }

        public boolean containsPoint(float x, float z) {
            float hw = width * 0.5f;
            float hd = depth * 0.5f;
            return x >= (centerX - hw) && x <= (centerX + hw)
                    && z >= (centerZ - hd) && z <= (centerZ + hd);
        }
    }

    private final List<InteriorDefinition> interiors = new ArrayList<>();

    public BuildingInterior() {
        buildInteriors();
    }

    public List<InteriorDefinition> getInteriors() {
        return interiors;
    }

    private void buildInteriors() {
        // 1. SAFEHOUSE POI at (210, 60): 20m wide x 16m deep x 6.5m high, door on South wall
        buildSafehouse();

        // 2. WORKSHOP POI at (-20, -200): 16m wide x 12m deep x 5.5m high, door on South wall
        buildWorkshop();

        // 3. GARAGE POI at (60, 40): 18m wide x 14m deep x 6.0m high, wide bay door on West wall
        buildGarage();

        // 4. RANGER HUT POI at (-330, 60): 10m wide x 9m deep x 4.5m high, door on East wall
        buildRangerHut();
    }

    private void buildSafehouse() {
        float cx = 210f, cz = 60f;
        float w = 20f, d = 16f, h = 6.0f;
        float ground = Terrain.heightAt(cx, cz);
        float wallThick = 0.6f;
        float doorW = 3.2f, doorH = 3.0f;
        float doorX = cx, doorZ = cz + d * 0.5f;

        InteriorDefinition def = new InteriorDefinition(
                "Safehouse", cx, cz, w, d, h, doorX, doorZ, doorW, doorH);

        // North Wall (solid back wall)
        def.obstacles.add(new Obstacle(Obstacle.Type.BUILDING,
                cx, ground + h * 0.5f, cz - d * 0.5f + wallThick * 0.5f,
                w, h, wallThick));

        // West Wall (solid left wall)
        def.obstacles.add(new Obstacle(Obstacle.Type.BUILDING,
                cx - w * 0.5f + wallThick * 0.5f, ground + h * 0.5f, cz,
                wallThick, h, d));

        // East Wall (solid right wall)
        def.obstacles.add(new Obstacle(Obstacle.Type.BUILDING,
                cx + w * 0.5f - wallThick * 0.5f, ground + h * 0.5f, cz,
                wallThick, h, d));

        // South Wall Left of Door
        float sideW = (w - doorW) * 0.5f;
        def.obstacles.add(new Obstacle(Obstacle.Type.BUILDING,
                cx - w * 0.5f + sideW * 0.5f, ground + h * 0.5f, cz + d * 0.5f - wallThick * 0.5f,
                sideW, h, wallThick));

        // South Wall Right of Door
        def.obstacles.add(new Obstacle(Obstacle.Type.BUILDING,
                cx + w * 0.5f - sideW * 0.5f, ground + h * 0.5f, cz + d * 0.5f - wallThick * 0.5f,
                sideW, h, wallThick));

        // Door Lintel (above door opening)
        float lintelH = h - doorH;
        def.obstacles.add(new Obstacle(Obstacle.Type.BUILDING,
                doorX, ground + doorH + lintelH * 0.5f, cz + d * 0.5f - wallThick * 0.5f,
                doorW, lintelH, wallThick));

        // Solid Roof/Ceiling Slab
        def.obstacles.add(new Obstacle(Obstacle.Type.BUILDING,
                cx, ground + h + 0.3f, cz,
                w + 0.6f, 0.6f, d + 0.6f));

        // Interior Floor Slab
        Obstacle floor = new Obstacle(Obstacle.Type.ROAD_SLAB,
                cx, ground + 0.05f, cz,
                w - 0.8f, 0.1f, d - 0.8f);
        floor.solid = false;
        def.obstacles.add(floor);

        // Interior Props: Survival workbench, supply crates, cot
        // Workbench along west wall
        def.obstacles.add(new Obstacle(Obstacle.Type.CRATE,
                cx - w * 0.5f + 2.0f, ground + 0.55f, cz - 2.0f,
                1.4f, 1.1f, 3.2f));

        // Survival Cot / bunk along north wall
        def.obstacles.add(new Obstacle(Obstacle.Type.CRATE,
                cx - 3.5f, ground + 0.35f, cz - d * 0.5f + 1.8f,
                2.8f, 0.7f, 1.4f));

        // Stacked supply crates in northeast corner
        def.obstacles.add(new Obstacle(Obstacle.Type.CRATE,
                cx + w * 0.5f - 2.2f, ground + 0.6f, cz - d * 0.5f + 2.2f,
                1.6f, 1.2f, 1.6f));
        def.obstacles.add(new Obstacle(Obstacle.Type.CRATE,
                cx + w * 0.5f - 2.2f, ground + 1.6f, cz - d * 0.5f + 2.2f,
                1.1f, 0.8f, 1.1f));

        interiors.add(def);
    }

    private void buildWorkshop() {
        float cx = -20f, cz = -200f;
        float w = 16f, d = 12f, h = 5.5f;
        float ground = Terrain.heightAt(cx, cz);
        float wallThick = 0.6f;
        float doorW = 3.0f, doorH = 2.8f;
        float doorX = cx, doorZ = cz + d * 0.5f;

        InteriorDefinition def = new InteriorDefinition(
                "Workshop", cx, cz, w, d, h, doorX, doorZ, doorW, doorH);

        // North Wall
        def.obstacles.add(new Obstacle(Obstacle.Type.BUILDING,
                cx, ground + h * 0.5f, cz - d * 0.5f + wallThick * 0.5f,
                w, h, wallThick));

        // West Wall
        def.obstacles.add(new Obstacle(Obstacle.Type.BUILDING,
                cx - w * 0.5f + wallThick * 0.5f, ground + h * 0.5f, cz,
                wallThick, h, d));

        // East Wall
        def.obstacles.add(new Obstacle(Obstacle.Type.BUILDING,
                cx + w * 0.5f - wallThick * 0.5f, ground + h * 0.5f, cz,
                wallThick, h, d));

        // South Wall Left & Right of Door
        float sideW = (w - doorW) * 0.5f;
        def.obstacles.add(new Obstacle(Obstacle.Type.BUILDING,
                cx - w * 0.5f + sideW * 0.5f, ground + h * 0.5f, cz + d * 0.5f - wallThick * 0.5f,
                sideW, h, wallThick));
        def.obstacles.add(new Obstacle(Obstacle.Type.BUILDING,
                cx + w * 0.5f - sideW * 0.5f, ground + h * 0.5f, cz + d * 0.5f - wallThick * 0.5f,
                sideW, h, wallThick));

        // Door Lintel
        float lintelH = h - doorH;
        def.obstacles.add(new Obstacle(Obstacle.Type.BUILDING,
                doorX, ground + doorH + lintelH * 0.5f, cz + d * 0.5f - wallThick * 0.5f,
                doorW, lintelH, wallThick));

        // Roof
        def.obstacles.add(new Obstacle(Obstacle.Type.BUILDING,
                cx, ground + h + 0.3f, cz,
                w + 0.6f, 0.6f, d + 0.6f));

        // Interior workbench & tool cabinet
        def.obstacles.add(new Obstacle(Obstacle.Type.CRATE,
                cx, ground + 0.5f, cz - d * 0.5f + 1.8f,
                3.5f, 1.0f, 1.4f));

        interiors.add(def);
    }

    private void buildGarage() {
        float cx = 60f, cz = 40f;
        float w = 18f, d = 14f, h = 6.0f;
        float ground = Terrain.heightAt(cx, cz);
        float wallThick = 0.6f;
        float doorW = 5.0f, doorH = 3.4f; // wide vehicle roll-up door
        float doorX = cx - w * 0.5f, doorZ = cz;

        InteriorDefinition def = new InteriorDefinition(
                "Garage", cx, cz, w, d, h, doorX, doorZ, doorW, doorH);

        // East Wall (back wall)
        def.obstacles.add(new Obstacle(Obstacle.Type.BUILDING,
                cx + w * 0.5f - wallThick * 0.5f, ground + h * 0.5f, cz,
                wallThick, h, d));

        // North Wall
        def.obstacles.add(new Obstacle(Obstacle.Type.BUILDING,
                cx, ground + h * 0.5f, cz - d * 0.5f + wallThick * 0.5f,
                w, h, wallThick));

        // South Wall
        def.obstacles.add(new Obstacle(Obstacle.Type.BUILDING,
                cx, ground + h * 0.5f, cz + d * 0.5f - wallThick * 0.5f,
                w, h, wallThick));

        // West Wall Left & Right of roll-up door
        float sideD = (d - doorW) * 0.5f;
        def.obstacles.add(new Obstacle(Obstacle.Type.BUILDING,
                cx - w * 0.5f + wallThick * 0.5f, ground + h * 0.5f, cz - d * 0.5f + sideD * 0.5f,
                wallThick, h, sideD));
        def.obstacles.add(new Obstacle(Obstacle.Type.BUILDING,
                cx - w * 0.5f + wallThick * 0.5f, ground + h * 0.5f, cz + d * 0.5f - sideD * 0.5f,
                wallThick, h, sideD));

        // Roll-up Door Lintel
        float lintelH = h - doorH;
        def.obstacles.add(new Obstacle(Obstacle.Type.BUILDING,
                doorX + wallThick * 0.5f, ground + doorH + lintelH * 0.5f, cz,
                wallThick, lintelH, doorW));

        // Roof
        def.obstacles.add(new Obstacle(Obstacle.Type.BUILDING,
                cx, ground + h + 0.3f, cz,
                w + 0.6f, 0.6f, d + 0.6f));

        interiors.add(def);
    }

    private void buildRangerHut() {
        float cx = -330f, cz = 60f;
        float w = 10f, d = 9f, h = 4.5f;
        float ground = Terrain.heightAt(cx, cz);
        float wallThick = 0.5f;
        float doorW = 2.4f, doorH = 2.4f;
        float doorX = cx + w * 0.5f, doorZ = cz; // door facing east toward city

        InteriorDefinition def = new InteriorDefinition(
                "Ranger Hut", cx, cz, w, d, h, doorX, doorZ, doorW, doorH);

        // West Wall
        def.obstacles.add(new Obstacle(Obstacle.Type.BUILDING,
                cx - w * 0.5f + wallThick * 0.5f, ground + h * 0.5f, cz,
                wallThick, h, d));

        // North Wall
        def.obstacles.add(new Obstacle(Obstacle.Type.BUILDING,
                cx, ground + h * 0.5f, cz - d * 0.5f + wallThick * 0.5f,
                w, h, wallThick));

        // South Wall
        def.obstacles.add(new Obstacle(Obstacle.Type.BUILDING,
                cx, ground + h * 0.5f, cz + d * 0.5f - wallThick * 0.5f,
                w, h, wallThick));

        // East Wall Left & Right of door
        float sideD = (d - doorW) * 0.5f;
        def.obstacles.add(new Obstacle(Obstacle.Type.BUILDING,
                cx + w * 0.5f - wallThick * 0.5f, ground + h * 0.5f, cz - d * 0.5f + sideD * 0.5f,
                wallThick, h, sideD));
        def.obstacles.add(new Obstacle(Obstacle.Type.BUILDING,
                cx + w * 0.5f - wallThick * 0.5f, ground + h * 0.5f, cz + d * 0.5f - sideD * 0.5f,
                wallThick, h, sideD));

        // Door Lintel
        float lintelH = h - doorH;
        def.obstacles.add(new Obstacle(Obstacle.Type.BUILDING,
                doorX - wallThick * 0.5f, ground + doorH + lintelH * 0.5f, cz,
                wallThick, lintelH, doorW));

        // Roof
        def.obstacles.add(new Obstacle(Obstacle.Type.BUILDING,
                cx, ground + h + 0.3f, cz,
                w + 0.5f, 0.5f, d + 0.5f));

        // Desk
        def.obstacles.add(new Obstacle(Obstacle.Type.CRATE,
                cx - 2.5f, ground + 0.45f, cz,
                1.4f, 0.9f, 2.2f));

        interiors.add(def);
    }

    public InteriorDefinition findInterior(String name) {
        for (InteriorDefinition def : interiors) {
            if (def.name.equalsIgnoreCase(name)) return def;
        }
        return null;
    }

    public boolean isInsideAnyInterior(float x, float z) {
        for (InteriorDefinition def : interiors) {
            if (def.containsPoint(x, z)) return true;
        }
        return false;
    }

    /**
     * Appends all architectural obstacles for the given chunk boundary.
     */
    public void populateChunkInteriors(List<Obstacle> out, float x0, float x1, float z0, float z1) {
        for (InteriorDefinition def : interiors) {
            if (def.centerX >= x0 && def.centerX < x1 && def.centerZ >= z0 && def.centerZ < z1) {
                out.addAll(def.obstacles);
            }
        }
    }
}
