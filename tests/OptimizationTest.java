import com.devagent.realworldsurvival3d.CullingOptimizer;
import com.devagent.realworldsurvival3d.Obstacle;
import com.devagent.realworldsurvival3d.OpenWorld;
import java.util.List;

/**
 * OptimizationTest - JVM unit test suite for Phase 13 (Optimization & Performance).
 * Tests frustum/sphere/AABB culling, zero-allocation matrix math,
 * frame budget regulation, delta time clamping, and chunk bounds.
 */
public class OptimizationTest {
    private static int passed = 0;
    private static int failed = 0;

    private static void check(boolean condition, String desc) {
        if (condition) {
            System.out.println("  PASS  " + desc);
            passed++;
        } else {
            System.err.println("  FAIL  " + desc);
            failed++;
        }
    }

    private static void approx(float actual, float expected, float eps, String desc) {
        check(Math.abs(actual - expected) <= eps, desc + " (" + actual + " ~ " + expected + ")");
    }

    public static void main(String[] args) {
        System.out.println("=== OptimizationTest (Phase 13) ===");

        testSphereFrustumCulling();
        testAabbFrustumCulling();
        testCullingMetrics();
        testZeroAllocationMatrixMath();
        testFrameBudgetAndDeltaClamping();
        testOpenWorldChunkSpatialBudget();
        testGraphicsPresetDrawDistances();

        System.out.println("\nOptimizationTest: " + passed + " passed, " + failed + " failed\n");
        if (failed > 0) System.exit(1);
    }

    // -----------------------------------------------------------------
    // Group 1: Sphere Frustum Culling
    // -----------------------------------------------------------------
    private static void testSphereFrustumCulling() {
        System.out.println("CullingOptimizer: sphere culling");
        // Camera at origin facing +Z (forward: fx=0, fy=0, fz=1), farPlane = 100m
        float camX = 0f, camY = 0f, camZ = 0f;
        float fx = 0f, fy = 0f, fz = 1f;
        float farPlane = 100f;

        // In front within distance
        boolean v1 = CullingOptimizer.isSphereVisible(0f, 0f, 30f, 2.0f, camX, camY, camZ, fx, fy, fz, farPlane);
        check(v1, "sphere 30m ahead is visible");

        // Directly behind camera
        boolean v2 = CullingOptimizer.isSphereVisible(0f, 0f, -25f, 2.0f, camX, camY, camZ, fx, fy, fz, farPlane);
        check(!v2, "sphere 25m behind camera is culled");

        // Ahead but beyond far plane (150m)
        boolean v3 = CullingOptimizer.isSphereVisible(0f, 0f, 150f, 2.0f, camX, camY, camZ, fx, fy, fz, farPlane);
        check(!v3, "sphere beyond far plane (150m) is culled");

        // Touching far plane boundary (center 102m, radius 5m -> min dist 97m <= 100m)
        boolean v4 = CullingOptimizer.isSphereVisible(0f, 0f, 102f, 5.0f, camX, camY, camZ, fx, fy, fz, farPlane);
        check(v4, "sphere overlapping far plane boundary is visible");

        // Extreme off-axis to the side (x=100m, z=5m)
        boolean v5 = CullingOptimizer.isSphereVisible(100f, 0f, 5f, 2.0f, camX, camY, camZ, fx, fy, fz, farPlane);
        check(!v5, "extreme off-axis sphere is culled by FOV cone");
    }

    // -----------------------------------------------------------------
    // Group 2: AABB Frustum Culling
    // -----------------------------------------------------------------
    private static void testAabbFrustumCulling() {
        System.out.println("CullingOptimizer: AABB culling");
        float camX = 0f, camY = 0f, camZ = 0f;
        float fx = 0f, fy = 0f, fz = 1f;
        float farPlane = 120f;

        // Visible building ahead
        boolean v1 = CullingOptimizer.isAABBVisible(5f, 0f, 40f, 15f, 20f, 50f,
                camX, camY, camZ, fx, fy, fz, farPlane);
        check(v1, "AABB 40m ahead is visible");

        // Building behind camera
        boolean v2 = CullingOptimizer.isAABBVisible(5f, 0f, -60f, 15f, 20f, -40f,
                camX, camY, camZ, fx, fy, fz, farPlane);
        check(!v2, "AABB behind camera is culled");

        // Building far away (z=200m)
        boolean v3 = CullingOptimizer.isAABBVisible(5f, 0f, 200f, 15f, 20f, 220f,
                camX, camY, camZ, fx, fy, fz, farPlane);
        check(!v3, "AABB far beyond draw distance is culled");
    }

    // -----------------------------------------------------------------
    // Group 3: Culling Metrics Tracker
    // -----------------------------------------------------------------
    private static void testCullingMetrics() {
        System.out.println("CullingOptimizer: performance metrics");
        CullingOptimizer opt = new CullingOptimizer();

        check(opt.getVisibleCount() == 0, "initial visible count is 0");
        check(opt.getCulledCount() == 0, "initial culled count is 0");
        approx(opt.getCullRatio(), 0.0f, 0.001f, "initial cull ratio is 0.0");

        // Record 30 visible and 70 culled
        for (int i = 0; i < 30; i++) opt.recordVisible();
        for (int i = 0; i < 70; i++) opt.recordCulled();

        check(opt.getVisibleCount() == 30, "visible count is 30");
        check(opt.getCulledCount() == 70, "culled count is 70");
        approx(opt.getCullRatio(), 0.70f, 0.001f, "cull ratio is 70% (0.70)");

        opt.resetMetrics();
        check(opt.getVisibleCount() == 0 && opt.getCulledCount() == 0, "resetMetrics zeroes counters");
    }

    // -----------------------------------------------------------------
    // Group 4: Zero-Allocation Matrix Math
    // -----------------------------------------------------------------
    private static void testZeroAllocationMatrixMath() {
        System.out.println("CullingOptimizer: zero-allocation setTRS verification");
        float[] dest = new float[16];
        float tx = 14.5f, ty = 8.2f, tz = -33.1f;
        float yaw = 45.0f;
        float sx = 2.0f, sy = 3.5f, sz = 1.8f;

        CullingOptimizer.setTRS(dest, tx, ty, tz, yaw, sx, sy, sz);

        float rad = (float) Math.toRadians(yaw);
        float c = (float) Math.cos(rad);
        float s = (float) Math.sin(rad);

        approx(dest[0], c * sx, 0.001f, "m[0] is cos * sx");
        approx(dest[2], -s * sx, 0.001f, "m[2] is -sin * sx");
        approx(dest[5], sy, 0.001f, "m[5] is sy");
        approx(dest[8], s * sz, 0.001f, "m[8] is sin * sz");
        approx(dest[10], c * sz, 0.001f, "m[10] is cos * sz");

        // Verify translation column
        approx(dest[12], tx, 0.001f, "m[12] is tx");
        approx(dest[13], ty, 0.001f, "m[13] is ty");
        approx(dest[14], tz, 0.001f, "m[14] is tz");
        approx(dest[15], 1.0f, 0.001f, "m[15] is 1.0");

        // Verify unused off-diagonals are 0
        check(dest[1] == 0f && dest[3] == 0f && dest[4] == 0f && dest[6] == 0f
                && dest[7] == 0f && dest[9] == 0f && dest[11] == 0f,
                "off-diagonals are 0.0");
    }

    // -----------------------------------------------------------------
    // Group 5: Frame Budget & Delta Time Limiting
    // -----------------------------------------------------------------
    private static void testFrameBudgetAndDeltaClamping() {
        System.out.println("CullingOptimizer: frame budget & delta time clamping");
        // Normal 60 FPS delta
        approx(CullingOptimizer.clampDeltaTime(0.0166f, 0.001f, 0.100f), 0.0166f, 0.0001f,
                "normal 60 FPS dt (16.6ms) passes through");

        // Huge frame hitch (e.g. 450ms GC spike) clamped to 100ms
        approx(CullingOptimizer.clampDeltaTime(0.450f, 0.001f, 0.100f), 0.100f, 0.0001f,
                "450ms hitch clamped to 100ms max");

        // Zero or negative delta clamped to 1ms
        approx(CullingOptimizer.clampDeltaTime(-0.02f, 0.001f, 0.100f), 0.001f, 0.0001f,
                "negative dt clamped to 1ms min");
        approx(CullingOptimizer.clampDeltaTime(0.0f, 0.001f, 0.100f), 0.001f, 0.0001f,
                "zero dt clamped to 1ms min");

        // Frame budgets
        check(CullingOptimizer.computeFrameBudgetNs(60) == 16_666_666L, "60 FPS budget is 16.66ms");
        check(CullingOptimizer.computeFrameBudgetNs(30) == 33_333_333L, "30 FPS budget is 33.33ms");
        check(CullingOptimizer.computeFrameBudgetNs(0) == 0L, "uncapped FPS budget is 0");
    }

    // -----------------------------------------------------------------
    // Group 6: OpenWorld Chunk Spatial Budget
    // -----------------------------------------------------------------
    private static void testOpenWorldChunkSpatialBudget() {
        System.out.println("OpenWorld: active chunk streaming budget");
        OpenWorld world = new OpenWorld();

        // Player at Central Plaza (150, 20)
        world.recomputeActive(150f, 20f);
        List<Obstacle> active = world.activeObstacles();

        check(!active.isEmpty(), "active obstacles list is non-empty");

        // Verify obstacles in far-away North Hills (z < -300) are excluded
        boolean farObstacleFound = false;
        for (Obstacle o : active) {
            if (o.z < -250f) {
                farObstacleFound = true;
                break;
            }
        }
        check(!farObstacleFound, "far North Hills obstacles (>250m) excluded from active 3x3 window");
    }

    // -----------------------------------------------------------------
    // Group 7: Preset Draw Distance Scaling
    // -----------------------------------------------------------------
    private static void testGraphicsPresetDrawDistances() {
        System.out.println("CullingOptimizer: draw distance preset scaling");
        float camX = 0f, camY = 0f, camZ = 0f;
        float fx = 0f, fy = 0f, fz = 1f;

        // Object placed 200m ahead
        float objZ = 200f;
        float radius = 3.0f;

        // Low quality farPlane = 160m
        boolean visLow = CullingOptimizer.isSphereVisible(0f, 0f, objZ, radius, camX, camY, camZ, fx, fy, fz, 160f);
        check(!visLow, "object at 200m is culled on Low quality (160m)");

        // High quality farPlane = 420m
        boolean visHigh = CullingOptimizer.isSphereVisible(0f, 0f, objZ, radius, camX, camY, camZ, fx, fy, fz, 420f);
        check(visHigh, "object at 200m is visible on High quality (420m)");
    }
}
