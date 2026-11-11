package com.devagent.realworldsurvival3d;

/**
 * CullingOptimizer - High-performance spatial culling, zero-allocation math,
 * and frame budget regulation (Phase 13).
 * Pure Java, 100% JVM-testable without Android runtime.
 */
public class CullingOptimizer {

    // Performance metrics counters
    private int visibleCount = 0;
    private int culledCount = 0;

    /**
     * Determines whether a bounding sphere is visible from camera position and orientation.
     * Evaluates:
     *   1. Behind-the-camera plane test with sphere radius margin
     *   2. Far-plane Euclidean distance cutoff with sphere radius margin
     *   3. Horizontal/vertical camera field-of-view cone test (~90 degree half-angle guard)
     */
    public static boolean isSphereVisible(float cx, float cy, float cz, float radius,
                                         float camX, float camY, float camZ,
                                         float camFX, float camFY, float camFZ,
                                         float farPlane) {
        float dx = cx - camX;
        float dy = cy - camY;
        float dz = cz - camZ;

        // 1. Behind-camera plane check
        float dot = dx * camFX + dy * camFY + dz * camFZ;
        if (dot < -radius) {
            return false;
        }

        // 2. Far plane cutoff
        float distSq = dx * dx + dy * dy + dz * dz;
        float cullDist = farPlane + radius;
        if (distSq > cullDist * cullDist) {
            return false;
        }

        // 3. Wide FOV guard (half-angle cone ~80 deg: cos(80) ~= 0.173)
        float dirLenSq = camFX * camFX + camFY * camFY + camFZ * camFZ;
        if (dirLenSq > 0.001f && distSq > radius * radius) {
            float cosAngle = dot / (float) Math.sqrt(distSq * dirLenSq);
            if (cosAngle < 0.173f) {
                return false;
            }
        }

        return true;
    }

    /**
     * Determines whether an axis-aligned bounding box (AABB) is visible.
     */
    public static boolean isAABBVisible(float minX, float minY, float minZ,
                                       float maxX, float maxY, float maxZ,
                                       float camX, float camY, float camZ,
                                       float camFX, float camFY, float camFZ,
                                       float farPlane) {
        float cx = (minX + maxX) * 0.5f;
        float cy = (minY + maxY) * 0.5f;
        float cz = (minZ + maxZ) * 0.5f;

        float hx = (maxX - minX) * 0.5f;
        float hy = (maxY - minY) * 0.5f;
        float hz = (maxZ - minZ) * 0.5f;
        float radius = (float) Math.sqrt(hx * hx + hy * hy + hz * hz);

        return isSphereVisible(cx, cy, cz, radius, camX, camY, camZ,
                camFX, camFY, camFZ, farPlane);
    }

    /**
     * Clamps delta time strictly within [minDt, maxDt] to prevent physics tunneling
     * and instability on frame spikes.
     */
    public static float clampDeltaTime(float dt, float minDt, float maxDt) {
        if (dt < minDt) return minDt;
        if (dt > maxDt) return maxDt;
        return dt;
    }

    /**
     * Computes target nanosecond budget per frame for a target FPS limit.
     */
    public static long computeFrameBudgetNs(int fpsLimit) {
        if (fpsLimit <= 0) return 0L;
        return 1_000_000_000L / fpsLimit;
    }

    /**
     * In-place zero-allocation construction of column-major model matrix = T * R(Y) * S
     * directly into out[16]. Eliminates all heap allocations in render loops.
     */
    public static void setTRS(float[] out, float tx, float ty, float tz, float rotDeg,
                              float sx, float sy, float sz) {
        float rad = (float) Math.toRadians(rotDeg);
        float c = (float) Math.cos(rad);
        float s = (float) Math.sin(rad);

        out[0] = c * sx;
        out[1] = 0f;
        out[2] = -s * sx;
        out[3] = 0f;

        out[4] = 0f;
        out[5] = sy;
        out[6] = 0f;
        out[7] = 0f;

        out[8] = s * sz;
        out[9] = 0f;
        out[10] = c * sz;
        out[11] = 0f;

        out[12] = tx;
        out[13] = ty;
        out[14] = tz;
        out[15] = 1.0f;
    }

    public void recordVisible() {
        visibleCount++;
    }

    public void recordCulled() {
        culledCount++;
    }

    public int getVisibleCount() {
        return visibleCount;
    }

    public int getCulledCount() {
        return culledCount;
    }

    public float getCullRatio() {
        int total = visibleCount + culledCount;
        if (total == 0) return 0f;
        return (float) culledCount / (float) total;
    }

    public void resetMetrics() {
        visibleCount = 0;
        culledCount = 0;
    }
}
