package com.devagent.realworldsurvival3d;

/**
 * Obstacle - Represents 3D world structures in the parkour playground.
 * Types include Skyscrapers, Wall-Run Panels, Vault Barriers, Balance Beams,
 * Jump Springboards, Hazard Pools, and Extraction Helipads.
 */
public class Obstacle {
    public enum Type {
        BUILDING,
        WALLRUN_PANEL,
        VAULT_BARRIER,
        BALANCE_BEAM,
        JUMP_PAD,
        HAZARD_POOL,
        EXTRACTION_HELIPAD,
        // Rooftop & street dressing props (also solid colliders)
        VENT,
        AC_UNIT,
        ANTENNA,
        WATER_TANK,
        CONSOLE,
        // Open-world additions
        TREE,          // collider = trunk; renderer adds the canopy
        ROCK,
        BUSH,          // decorative (non-solid)
        WATER_PLANE,   // river surface (non-solid)
        ROAD_SLAB,     // asphalt/dirt road segment (non-solid, walks on terrain)
        BRIDGE,        // solid walkable deck
        CRATE,         // container (solid)
        LAMP           // street lamp; renderer adds the glow
    }

    public Type type;
    public float x, y, z;          // Center position
    public float width, height, depth; // Dimensions (w=X, h=Y, d=Z)
    public float r, g, b;          // Base color tint
    public boolean climbable;      // Top ledge can be grabbed/mantled
    public boolean wallRunnable;   // Vertical surface can be wall-run
    public boolean vaultable;      // Can be vaulted over seamlessly
    /** false = drawn but ignored by collision (bushes, water, road decals). */
    public boolean solid = true;

    public Obstacle(Type type, float x, float y, float z, float width, float height, float depth) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.z = z;
        this.width = width;
        this.height = height;
        this.depth = depth;

        switch (type) {
            case BUILDING:
                this.r = 0.45f; this.g = 0.48f; this.b = 0.52f;
                this.climbable = true;
                this.wallRunnable = true;
                this.vaultable = false;
                break;
            case WALLRUN_PANEL:
                this.r = 0.85f; this.g = 0.25f; this.b = 0.15f; // Bright red/orange billboard
                this.climbable = false;
                this.wallRunnable = true;
                this.vaultable = false;
                break;
            case VAULT_BARRIER:
                this.r = 0.95f; this.g = 0.80f; this.b = 0.10f; // Yellow hazard barrier
                this.climbable = true;
                this.wallRunnable = false;
                this.vaultable = true;
                break;
            case BALANCE_BEAM:
                this.r = 0.70f; this.g = 0.40f; this.b = 0.20f; // Rust steel girder
                this.climbable = false;
                this.wallRunnable = false;
                this.vaultable = false;
                break;
            case JUMP_PAD:
                this.r = 0.10f; this.g = 0.85f; this.b = 0.95f; // Cyan high-pressure fan
                this.climbable = false;
                this.wallRunnable = false;
                this.vaultable = false;
                break;
            case HAZARD_POOL:
                this.r = 0.20f; this.g = 0.95f; this.b = 0.20f; // Toxic glowing green
                this.climbable = false;
                this.wallRunnable = false;
                this.vaultable = false;
                break;
            case EXTRACTION_HELIPAD:
                this.r = 1.00f; this.g = 0.90f; this.b = 0.00f; // Gold landing circle
                this.climbable = true;
                this.wallRunnable = false;
                this.vaultable = false;
                break;
            case VENT:
                this.r = 0.62f; this.g = 0.60f; this.b = 0.55f; // Brushed aluminum
                this.climbable = true;
                this.wallRunnable = false;
                this.vaultable = true;
                break;
            case AC_UNIT:
                this.r = 0.55f; this.g = 0.58f; this.b = 0.62f; // Industrial gray
                this.climbable = true;
                this.wallRunnable = false;
                this.vaultable = true;
                break;
            case ANTENNA:
                this.r = 0.30f; this.g = 0.32f; this.b = 0.35f; // Dark steel
                this.climbable = false;
                this.wallRunnable = false;
                this.vaultable = false;
                break;
            case WATER_TANK:
                this.r = 0.45f; this.g = 0.38f; this.b = 0.30f; // Weathered tank
                this.climbable = true;
                this.wallRunnable = false;
                this.vaultable = false;
                break;
            case CONSOLE:
                this.r = 0.90f; this.g = 0.75f; this.b = 0.20f; // Signal console
                this.climbable = false;
                this.wallRunnable = false;
                this.vaultable = false;
                break;
            // Open-world prop defaults (renderer may draw extra parts on top)
            case TREE:
                this.r = 0.36f; this.g = 0.25f; this.b = 0.15f; // trunk bark
                break;
            case ROCK:
                this.r = 0.46f; this.g = 0.44f; this.b = 0.42f;
                break;
            case BUSH:
                this.r = 0.20f; this.g = 0.40f; this.b = 0.16f;
                break;
            case WATER_PLANE:
                this.r = 0.16f; this.g = 0.36f; this.b = 0.55f;
                break;
            case ROAD_SLAB:
                this.r = 0.30f; this.g = 0.30f; this.b = 0.32f;
                break;
            case BRIDGE:
                this.r = 0.42f; this.g = 0.30f; this.b = 0.18f; // weathered wood
                break;
            case CRATE:
                this.r = 0.55f; this.g = 0.42f; this.b = 0.25f;
                break;
            case LAMP:
                this.r = 0.30f; this.g = 0.32f; this.b = 0.36f;
                break;
        }
    }

    public float getMinX() { return x - width / 2f; }
    public float getMaxX() { return x + width / 2f; }
    public float getMinY() { return y - height / 2f; }
    public float getMaxY() { return y + height / 2f; }
    public float getMinZ() { return z - depth / 2f; }
    public float getMaxZ() { return z + depth / 2f; }

    /**
     * Checks if a point is horizontally inside this obstacle with margin.
     */
    public boolean containsXZ(float px, float pz, float margin) {
        return px >= (getMinX() - margin) && px <= (getMaxX() + margin) &&
               pz >= (getMinZ() - margin) && pz <= (getMaxZ() + margin);
    }
}
