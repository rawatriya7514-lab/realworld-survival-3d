package com.devagent.realworldsurvival3d;

import java.util.List;

/**
 * MapViewController - Pure Java viewport controller and projection engine
 * for both the Fullscreen Interactive World Map and the Rotating Minimap HUD.
 *
 * Implements Phase 6 requirements:
 *  - 896m x 896m world bounds (-448m to +448m)
 *  - Continuous pan & zoom with anchor-focus stability
 *  - Bijective world <-> screen coordinate projections
 *  - Rotating HUD minimap projection (player heading always points UP)
 *  - Compass cardinal angle calculations (N, E, S, W)
 *  - POI snapping and tap-to-set waypoint routing
 *  - Ergonomic touch hit-testing for map controls (Close, Center, Clear, Zoom +/-)
 */
public class MapViewController {

    public static final float MIN_WORLD_X = -448.0f;
    public static final float MAX_WORLD_X = 448.0f;
    public static final float MIN_WORLD_Z = -448.0f;
    public static final float MAX_WORLD_Z = 448.0f;
    public static final float WORLD_SIZE = 896.0f;

    public static final float MIN_ZOOM = 0.22f; // pixels per meter (fits entire 896m world)
    public static final float MAX_ZOOM = 3.50f; // zoomed in street view
    public static final float DEFAULT_ZOOM = 0.75f;

    public static final float POI_SNAP_RADIUS = 28.0f;

    public enum ButtonAction {
        NONE,
        CLOSE,
        CENTER_PLAYER,
        CLEAR_ROUTE,
        ZOOM_IN,
        ZOOM_OUT
    }

    // Viewport geometry
    public float screenWidth = 800f;
    public float screenHeight = 600f;

    // Fullscreen map center & zoom (in world meters)
    public float centerX = 0f;
    public float centerZ = 0f;
    public float zoom = DEFAULT_ZOOM;

    // Rotating HUD Minimap geometry
    public float minimapCenterX = 700f;
    public float minimapCenterY = 175f;
    public float minimapRadius = 72f;
    public float minimapRange = 90f; // meters visible around player

    // Map control button positions and radii
    public float btnCloseX, btnCloseY, btnCloseRadius;
    public float btnCenterX, btnCenterY, btnCenterRadius;
    public float btnClearX, btnClearY, btnClearRadius;
    public float btnZoomInX, btnZoomInY, btnZoomInRadius;
    public float btnZoomOutX, btnZoomOutY, btnZoomOutRadius;

    public MapViewController() {
        setScreenSize(800f, 600f);
    }

    public void setScreenSize(float width, float height) {
        this.screenWidth = Math.max(100f, width);
        this.screenHeight = Math.max(100f, height);

        float minDim = Math.min(this.screenWidth, this.screenHeight);
        float baseBtnRadius = Math.max(22f, minDim * 0.045f);

        // Top right close button
        btnCloseRadius = baseBtnRadius;
        btnCloseX = this.screenWidth - btnCloseRadius * 1.8f;
        btnCloseY = btnCloseRadius * 1.8f;

        // Bottom left controls: Center on player and Clear route
        btnCenterRadius = baseBtnRadius;
        btnCenterX = btnCenterRadius * 2.0f;
        btnCenterY = this.screenHeight - btnCenterRadius * 2.0f;

        btnClearRadius = baseBtnRadius;
        btnClearX = btnCenterX + btnCenterRadius * 2.6f;
        btnClearY = btnCenterY;

        // Bottom right zoom controls: (+) and (-)
        btnZoomInRadius = baseBtnRadius;
        btnZoomInX = this.screenWidth - btnZoomInRadius * 1.8f;
        btnZoomInY = this.screenHeight - btnZoomInRadius * 4.6f;

        btnZoomOutRadius = baseBtnRadius;
        btnZoomOutX = btnZoomInX;
        btnZoomOutY = this.screenHeight - btnZoomOutRadius * 2.0f;

        // Minimap position: top right corner (below HUD buttons)
        minimapRadius = Math.max(55f, minDim * 0.12f);
        minimapCenterX = this.screenWidth - minimapRadius - 20f;
        minimapCenterY = minimapRadius + 75f;
    }

    public void setLeftHanded(boolean leftHanded) {
        if (leftHanded) {
            // In left-handed layout, move minimap to top-left to avoid button cluster
            minimapCenterX = minimapRadius + 20f;
        } else {
            minimapCenterX = screenWidth - minimapRadius - 20f;
        }
    }

    // ==================================================================
    // Projections: World <-> Screen (Fullscreen Map)
    // ==================================================================

    public float worldToScreenX(float worldX) {
        return screenWidth * 0.5f + (worldX - centerX) * zoom;
    }

    public float worldToScreenY(float worldZ) {
        return screenHeight * 0.5f + (worldZ - centerZ) * zoom;
    }

    public float screenToWorldX(float screenX) {
        return centerX + (screenX - screenWidth * 0.5f) / zoom;
    }

    public float screenToWorldZ(float screenY) {
        return centerZ + (screenY - screenHeight * 0.5f) / zoom;
    }

    // ==================================================================
    // Pan & Zoom Controls
    // ==================================================================

    public void pan(float dxScreen, float dyScreen) {
        centerX -= dxScreen / zoom;
        centerZ -= dyScreen / zoom;
        clampCenter();
    }

    public void zoomBy(float factor, float focusScreenX, float focusScreenY) {
        if (factor <= 0f) return;
        float oldZoom = zoom;
        float newZoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoom * factor));
        if (Math.abs(newZoom - oldZoom) < 0.0001f) return;

        // Keep the world point under the focus point stationary
        float focusWorldX = screenToWorldX(focusScreenX);
        float focusWorldZ = screenToWorldZ(focusScreenY);

        this.zoom = newZoom;
        this.centerX = focusWorldX - (focusScreenX - screenWidth * 0.5f) / newZoom;
        this.centerZ = focusWorldZ - (focusScreenY - screenHeight * 0.5f) / newZoom;
        clampCenter();
    }

    public void zoomIn() {
        zoomBy(1.35f, screenWidth * 0.5f, screenHeight * 0.5f);
    }

    public void zoomOut() {
        zoomBy(0.74f, screenWidth * 0.5f, screenHeight * 0.5f);
    }

    public void centerOn(float worldX, float worldZ) {
        this.centerX = worldX;
        this.centerZ = worldZ;
        clampCenter();
    }

    public void centerOnPlayer(float playerX, float playerZ) {
        centerOn(playerX, playerZ);
    }

    public void resetOverview() {
        this.centerX = 0f;
        this.centerZ = 0f;
        this.zoom = DEFAULT_ZOOM;
    }

    private void clampCenter() {
        centerX = Math.max(MIN_WORLD_X, Math.min(MAX_WORLD_X, centerX));
        centerZ = Math.max(MIN_WORLD_Z, Math.min(MAX_WORLD_Z, centerZ));
    }

    // ==================================================================
    // Button Hit-Testing
    // ==================================================================

    public ButtonAction testButtonClick(float sx, float sy) {
        if (isInside(sx, sy, btnCloseX, btnCloseY, btnCloseRadius)) {
            return ButtonAction.CLOSE;
        }
        if (isInside(sx, sy, btnCenterX, btnCenterY, btnCenterRadius)) {
            return ButtonAction.CENTER_PLAYER;
        }
        if (isInside(sx, sy, btnClearX, btnClearY, btnClearRadius)) {
            return ButtonAction.CLEAR_ROUTE;
        }
        if (isInside(sx, sy, btnZoomInX, btnZoomInY, btnZoomInRadius)) {
            return ButtonAction.ZOOM_IN;
        }
        if (isInside(sx, sy, btnZoomOutX, btnZoomOutY, btnZoomOutRadius)) {
            return ButtonAction.ZOOM_OUT;
        }
        return ButtonAction.NONE;
    }

    public boolean isInsideMinimap(float sx, float sy) {
        return isInside(sx, sy, minimapCenterX, minimapCenterY, minimapRadius);
    }

    private static boolean isInside(float px, float py, float cx, float cy, float radius) {
        float dx = px - cx;
        float dy = py - cy;
        return (dx * dx + dy * dy) <= (radius * radius);
    }

    // ==================================================================
    // Waypoint Tap Handling (with POI snapping)
    // ==================================================================

    public static class WaypointResult {
        public final float targetX;
        public final float targetZ;
        public final String name;
        public final boolean snappedToPoi;

        public WaypointResult(float targetX, float targetZ, String name, boolean snappedToPoi) {
            this.targetX = targetX;
            this.targetZ = targetZ;
            this.name = name;
            this.snappedToPoi = snappedToPoi;
        }
    }

    /**
     * Converts screen touch to world coordinates, snaps to nearest POI if within
     * POI_SNAP_RADIUS, sets the navigation destination, and initiates route calculation.
     */
    public WaypointResult handleMapTap(float screenX, float screenY,
                                       List<OpenWorld.Poi> pois,
                                       NavigationTracker tracker,
                                       float playerX, float playerZ) {
        float tapWorldX = screenToWorldX(screenX);
        float tapWorldZ = screenToWorldZ(screenY);

        // Clamp to open-world boundary
        tapWorldX = Math.max(MIN_WORLD_X, Math.min(MAX_WORLD_X, tapWorldX));
        tapWorldZ = Math.max(MIN_WORLD_Z, Math.min(MAX_WORLD_Z, tapWorldZ));

        float targetX = tapWorldX;
        float targetZ = tapWorldZ;
        String targetName = "Waypoint (" + (int) tapWorldX + ", " + (int) tapWorldZ + ")";
        boolean snapped = false;

        // Check for snapping to POIs
        if (pois != null) {
            float bestDistSq = POI_SNAP_RADIUS * POI_SNAP_RADIUS;
            for (OpenWorld.Poi poi : pois) {
                float dx = poi.x - tapWorldX;
                float dz = poi.z - tapWorldZ;
                float distSq = dx * dx + dz * dz;
                if (distSq <= bestDistSq) {
                    bestDistSq = distSq;
                    targetX = poi.x;
                    targetZ = poi.z;
                    targetName = poi.name;
                    snapped = true;
                }
            }
        }

        if (tracker != null) {
            tracker.setDestination(targetX, targetZ, targetName, playerX, playerZ);
        }

        return new WaypointResult(targetX, targetZ, targetName, snapped);
    }

    // ==================================================================
    // Rotating Minimap Projection
    // ==================================================================

    /**
     * Converts a world coordinate (wx, wz) to relative screen coordinates (out[0], out[1])
     * on the rotating HUD minimap centered at (minimapCenterX, minimapCenterY).
     *
     * The player heading (playerYaw) always points straight UP (out[1] < 0).
     * out[2] receives the radial distance from minimap center in pixels.
     *
     * @return true if the point falls within minimapRadius
     */
    public boolean worldToMinimapLocal(float wx, float wz, float playerX, float playerZ,
                                       float playerYaw, float[] out) {
        float dx = wx - playerX;
        float dz = wz - playerZ;

        double rad = Math.toRadians(playerYaw);
        float sin = (float) Math.sin(rad);
        float cos = (float) Math.cos(rad);

        // In 3D world: North is -Z, East is +X.
        // When playerYaw = 0: forward is -dz, right is +dx.
        float localForward = -dz * cos + dx * sin;
        float localRight = dx * cos + dz * sin;

        float scale = minimapRadius / minimapRange;
        float relX = localRight * scale;
        float relY = -localForward * scale; // screen Y grows downward

        out[0] = relX;
        out[1] = relY;
        float dist = (float) Math.sqrt(relX * relX + relY * relY);
        out[2] = dist;

        return dist <= minimapRadius;
    }

    /**
     * Returns screen coordinates on the minimap border ring for Cardinal direction North.
     * North corresponds to world (0, -1).
     */
    public void getMinimapNorthPosition(float playerYaw, float[] out) {
        // North angle relative to player heading
        double rad = Math.toRadians(-playerYaw);
        float borderDist = minimapRadius - 7.0f;
        out[0] = minimapCenterX + borderDist * (float) Math.sin(rad);
        out[1] = minimapCenterY - borderDist * (float) Math.cos(rad);
    }
}
