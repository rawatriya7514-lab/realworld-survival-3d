import com.devagent.realworldsurvival3d.MapViewController;
import com.devagent.realworldsurvival3d.NavigationTracker;
import com.devagent.realworldsurvival3d.OpenWorld;
import com.devagent.realworldsurvival3d.RoadNetwork;

import java.util.List;

/**
 * JVM test suite for MapViewController (Phase 6 World Map & Minimap).
 */
public class MapTest {

    private static int passed = 0;
    private static int failed = 0;

    private static void check(String desc, boolean cond) {
        if (cond) {
            System.out.println("  PASS  " + desc);
            passed++;
        } else {
            System.out.println("  FAIL  " + desc);
            failed++;
        }
    }

    private static void checkNear(String desc, float actual, float expected, float eps) {
        if (Math.abs(actual - expected) <= eps) {
            System.out.println("  PASS  " + desc + " (" + actual + " ~ " + expected + ")");
            passed++;
        } else {
            System.out.println("  FAIL  " + desc + " (" + actual + " != " + expected + ", eps=" + eps + ")");
            failed++;
        }
    }

    public static void main(String[] args) {
        System.out.println("MapViewController: viewport setup & scaling");
        testViewport();

        System.out.println("MapViewController: bijective world <-> screen projection");
        testBijectiveProjection();

        System.out.println("MapViewController: pan & zoom mechanics");
        testPanAndZoom();

        System.out.println("MapViewController: rotating HUD minimap projection");
        testMinimapProjection();

        System.out.println("MapViewController: button hit-testing");
        testButtonHitTesting();

        System.out.println("MapViewController: tap-to-waypoint & POI snapping");
        testWaypointAndPoiSnapping();

        System.out.println();
        System.out.println("MapTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }

    private static void testViewport() {
        MapViewController map = new MapViewController();
        map.setScreenSize(1920f, 1080f);

        checkNear("screen width initialized", map.screenWidth, 1920f, 0.1f);
        checkNear("screen height initialized", map.screenHeight, 1080f, 0.1f);
        check("zoom initialized to DEFAULT_ZOOM", map.zoom == MapViewController.DEFAULT_ZOOM);
        check("minimap radius is positive", map.minimapRadius > 0);
        check("minimap center inside screen width", map.minimapCenterX > 0 && map.minimapCenterX < 1920f);
        check("close button placed at top right", map.btnCloseX > 1700f && map.btnCloseY < 200f);
        check("center button placed at bottom left", map.btnCenterX < 200f && map.btnCenterY > 900f);

        // Left handed mode test
        map.setLeftHanded(true);
        check("left-handed minimap moved to left side", map.minimapCenterX < 200f);
        map.setLeftHanded(false);
        check("right-handed minimap restored to right side", map.minimapCenterX > 1700f);
    }

    private static void testBijectiveProjection() {
        MapViewController map = new MapViewController();
        map.setScreenSize(1280f, 720f);
        map.centerOn(50f, -100f);
        map.zoom = 1.25f;

        // Test 1: Center coordinates project to screen center
        float scx = map.worldToScreenX(50f);
        float scy = map.worldToScreenY(-100f);
        checkNear("center world projects to screen center X", scx, 640f, 0.001f);
        checkNear("center world projects to screen center Y", scy, 360f, 0.001f);

        // Test 2: Arbitrary points round-trip exactly
        float[] testPoints = {0f, 0f, 150f, 20f, -200f, 180f, 440f, -440f, -320f, -150f};
        boolean allRoundTripsPassed = true;
        for (int i = 0; i < testPoints.length; i += 2) {
            float wx = testPoints[i];
            float wz = testPoints[i + 1];

            float sx = map.worldToScreenX(wx);
            float sy = map.worldToScreenY(wz);

            float roundX = map.screenToWorldX(sx);
            float roundZ = map.screenToWorldZ(sy);

            if (Math.abs(roundX - wx) > 0.001f || Math.abs(roundZ - wz) > 0.001f) {
                allRoundTripsPassed = false;
                break;
            }
        }
        check("all arbitrary world points round-trip bijectively within 0.001m", allRoundTripsPassed);
    }

    private static void testPanAndZoom() {
        MapViewController map = new MapViewController();
        map.setScreenSize(1000f, 800f);
        map.centerOn(0f, 0f);
        map.zoom = 1.0f;

        // Drag 100 pixels right (moves world left)
        map.pan(100f, 50f);
        checkNear("pan updates centerX", map.centerX, -100f, 0.001f);
        checkNear("pan updates centerZ", map.centerZ, -50f, 0.001f);

        // Clamping to world bounds [-448, 448]
        map.pan(10000f, 0f);
        checkNear("centerX clamped at MIN_WORLD_X", map.centerX, MapViewController.MIN_WORLD_X, 0.001f);
        map.pan(-20000f, 0f);
        checkNear("centerX clamped at MAX_WORLD_X", map.centerX, MapViewController.MAX_WORLD_X, 0.001f);

        // Reset and test Zoom
        map.resetOverview();
        checkNear("resetOverview centers at origin", map.centerX, 0f, 0.001f);
        float initZoom = map.zoom;
        map.zoomIn();
        check("zoomIn increases zoom level", map.zoom > initZoom);
        map.zoomOut();
        checkNear("zoomOut restores zoom level", map.zoom, initZoom, 0.05f);

        // Zoom clamping
        for (int i = 0; i < 20; i++) map.zoomIn();
        checkNear("zoom clamped at MAX_ZOOM", map.zoom, MapViewController.MAX_ZOOM, 0.001f);
        for (int i = 0; i < 20; i++) map.zoomOut();
        checkNear("zoom clamped at MIN_ZOOM", map.zoom, MapViewController.MIN_ZOOM, 0.001f);

        // Focus anchor stability: point under screen focus remains at focus
        map.resetOverview();
        map.zoom = 1.0f;
        float focusSx = 300f;
        float focusSy = 250f;
        float anchorWorldX = map.screenToWorldX(focusSx);
        float anchorWorldZ = map.screenToWorldZ(focusSy);

        map.zoomBy(1.5f, focusSx, focusSy);
        float postZoomSx = map.worldToScreenX(anchorWorldX);
        float postZoomSy = map.worldToScreenY(anchorWorldZ);
        checkNear("focus anchor X unchanged across zoom", postZoomSx, focusSx, 0.01f);
        checkNear("focus anchor Y unchanged across zoom", postZoomSy, focusSy, 0.01f);
    }

    private static void testMinimapProjection() {
        MapViewController map = new MapViewController();
        map.setScreenSize(1000f, 800f);

        float playerX = 100f;
        float playerZ = 50f;
        float[] out = new float[3];

        // Case 1: Player position itself is at center of minimap
        map.worldToMinimapLocal(playerX, playerZ, playerX, playerZ, 0f, out);
        checkNear("player at minimap center X", out[0], 0f, 0.001f);
        checkNear("player at minimap center Y", out[1], 0f, 0.001f);
        checkNear("player distance is 0", out[2], 0f, 0.001f);

        // Case 2: Facing North (yaw = 0). Target 45m North (Z = playerZ - 45)
        // Should project straight UP on screen (out[1] < 0, out[0] ~ 0)
        boolean inside = map.worldToMinimapLocal(playerX, playerZ - 45f, playerX, playerZ, 0f, out);
        check("point 45m North is inside minimap range", inside);
        checkNear("North target relX is ~0", out[0], 0f, 0.01f);
        check("North target relY is negative (straight UP)", out[1] < -20f);

        // Case 3: Facing North (yaw = 0). Target 45m East (X = playerX + 45)
        // Should project straight RIGHT on screen (out[0] > 0, out[1] ~ 0)
        map.worldToMinimapLocal(playerX + 45f, playerZ, playerX, playerZ, 0f, out);
        check("East target relX is positive (straight RIGHT)", out[0] > 20f);
        checkNear("East target relY is ~0", out[1], 0f, 0.01f);

        // Case 4: Facing East (yaw = +90 deg).
        // The North target should now be to player's LEFT (out[0] < 0, out[1] ~ 0)
        map.worldToMinimapLocal(playerX, playerZ - 45f, playerX, playerZ, 90f, out);
        check("facing East, North target is to the LEFT (out[0] < 0)", out[0] < -20f);
        checkNear("facing East, North target forward offset is ~0", out[1], 0f, 0.01f);

        // Case 5: Target outside minimap range (150m > 90m range)
        boolean farInside = map.worldToMinimapLocal(playerX + 150f, playerZ, playerX, playerZ, 0f, out);
        check("target beyond minimap range returns false", !farInside);
        check("target beyond range radial dist > minimapRadius", out[2] > map.minimapRadius);

        // Case 6: Cardinal North needle position on rim
        float[] northPos = new float[2];
        map.getMinimapNorthPosition(0f, northPos);
        checkNear("at yaw=0 North needle is on top rim X", northPos[0], map.minimapCenterX, 0.01f);
        check("at yaw=0 North needle is on top rim Y", northPos[1] < map.minimapCenterY);

        map.getMinimapNorthPosition(90f, northPos);
        check("at yaw=90 North needle rotates to left rim X", northPos[0] < map.minimapCenterX);
        checkNear("at yaw=90 North needle is centered vertically", northPos[1], map.minimapCenterY, 0.01f);
    }

    private static void testButtonHitTesting() {
        MapViewController map = new MapViewController();
        map.setScreenSize(1200f, 700f);

        check("click close button", map.testButtonClick(map.btnCloseX, map.btnCloseY) == MapViewController.ButtonAction.CLOSE);
        check("click center button", map.testButtonClick(map.btnCenterX, map.btnCenterY) == MapViewController.ButtonAction.CENTER_PLAYER);
        check("click clear button", map.testButtonClick(map.btnClearX, map.btnClearY) == MapViewController.ButtonAction.CLEAR_ROUTE);
        check("click zoom in button", map.testButtonClick(map.btnZoomInX, map.btnZoomInY) == MapViewController.ButtonAction.ZOOM_IN);
        check("click zoom out button", map.testButtonClick(map.btnZoomOutX, map.btnZoomOutY) == MapViewController.ButtonAction.ZOOM_OUT);
        check("click empty area returns NONE", map.testButtonClick(600f, 350f) == MapViewController.ButtonAction.NONE);

        check("click inside minimap disc", map.isInsideMinimap(map.minimapCenterX, map.minimapCenterY));
        check("click outside minimap disc", !map.isInsideMinimap(100f, 100f));
    }

    private static void testWaypointAndPoiSnapping() {
        MapViewController map = new MapViewController();
        map.setScreenSize(1000f, 800f);
        map.centerOn(0f, 0f);
        map.zoom = 1.0f;

        OpenWorld openWorld = new OpenWorld();
        RoadNetwork roadNetwork = new RoadNetwork();
        NavigationTracker tracker = new NavigationTracker(roadNetwork);

        float playerX = 150f;
        float playerZ = 20f; // at Central Plaza

        // Case 1: Tap near Safehouse (210, 60)
        // Convert Safehouse world coords to screen
        float safeSx = map.worldToScreenX(215f);
        float safeSy = map.worldToScreenY(62f);

        MapViewController.WaypointResult res1 = map.handleMapTap(
                safeSx, safeSy, openWorld.pois, tracker, playerX, playerZ);

        check("tap near Safehouse snaps to POI", res1.snappedToPoi);
        check("snapped destination name is Safehouse", "Safehouse".equals(res1.name));
        checkNear("snapped target X is 210", res1.targetX, 210f, 0.01f);
        checkNear("snapped target Z is 60", res1.targetZ, 60f, 0.01f);
        check("tracker activated active waypoint", tracker.hasActiveWaypoint);
        check("tracker target matches Safehouse", tracker.targetX == 210f && tracker.targetZ == 60f);

        // Case 2: Tap in open countryside (no POI within 28m)
        float openSx = map.worldToScreenX(-80f);
        float openSy = map.worldToScreenY(100f);
        MapViewController.WaypointResult res2 = map.handleMapTap(
                openSx, openSy, openWorld.pois, tracker, playerX, playerZ);

        check("tap far from POIs does not snap", !res2.snappedToPoi);
        checkNear("custom waypoint X matches tap", res2.targetX, -80f, 1.0f);
        checkNear("custom waypoint Z matches tap", res2.targetZ, 100f, 1.0f);
        check("tracker active with custom waypoint", tracker.hasActiveWaypoint);

        // Test clear route action
        tracker.clearDestination();
        check("clearDestination resets tracker", !tracker.hasActiveWaypoint);
    }
}
