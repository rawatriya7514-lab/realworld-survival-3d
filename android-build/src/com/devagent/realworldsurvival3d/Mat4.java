package com.devagent.realworldsurvival3d;

/**
 * Mat4 - Minimal column-major 4x4 matrix math for the GLES2 renderer.
 * Layout matches android.opengl.Matrix conventions (column-major, float[16]).
 */
public final class Mat4 {
    private Mat4() {}

    public static float[] identity() {
        float[] m = new float[16];
        m[0] = m[5] = m[10] = m[15] = 1f;
        return m;
    }

    public static void identity(float[] m) {
        java.util.Arrays.fill(m, 0f);
        m[0] = m[5] = m[10] = m[15] = 1f;
    }

    /** m = m * b (multiply, storing into m). Both column-major. */
    public static void mul(float[] m, float[] b) {
        float[] a = new float[16];
        System.arraycopy(m, 0, a, 0, 16);
        for (int c = 0; c < 4; c++) {
            for (int r = 0; r < 4; r++) {
                m[c * 4 + r] = a[r] * b[c * 4]
                        + a[4 + r] * b[c * 4 + 1]
                        + a[8 + r] * b[c * 4 + 2]
                        + a[12 + r] * b[c * 4 + 3];
            }
        }
    }

    public static float[] perspective(float fovyDeg, float aspect, float near, float far) {
        float[] m = new float[16];
        android.opengl.Matrix.perspectiveM(m, 0, fovyDeg, aspect, near, far);
        return m;
    }

    public static float[] lookAt(float ex, float ey, float ez,
                                 float tx, float ty, float tz,
                                 float ux, float uy, float uz) {
        float[] m = new float[16];
        android.opengl.Matrix.setLookAtM(m, 0, ex, ey, ez, tx, ty, tz, ux, uy, uz);
        return m;
    }

    public static float[] translate(float x, float y, float z) {
        float[] m = identity();
        m[12] = x; m[13] = y; m[14] = z;
        return m;
    }

    public static float[] scale(float x, float y, float z) {
        float[] m = identity();
        m[0] = x; m[5] = y; m[10] = z;
        return m;
    }

    public static float[] rotY(float deg) {
        float[] m = identity();
        float r = (float) Math.toRadians(deg);
        float c = (float) Math.cos(r), s = (float) Math.sin(r);
        m[0] = c; m[2] = -s;
        m[8] = s; m[10] = c;
        return m;
    }

    public static float[] rotX(float deg) {
        float[] m = identity();
        float r = (float) Math.toRadians(deg);
        float c = (float) Math.cos(r), s = (float) Math.sin(r);
        m[5] = c; m[6] = s;
        m[9] = -s; m[10] = c;
        return m;
    }

    public static float[] rotZ(float deg) {
        float[] m = identity();
        float r = (float) Math.toRadians(deg);
        float c = (float) Math.cos(r), s = (float) Math.sin(r);
        m[0] = c; m[1] = s;
        m[4] = -s; m[5] = c;
        return m;
    }

    /** Build model = T * R * S in one call. */
    public static float[] trs(float tx, float ty, float tz, float rotDeg,
                              float sx, float sy, float sz) {
        float[] m = translate(tx, ty, tz);
        if (rotDeg != 0f) mul(m, rotY(rotDeg));
        mul(m, scale(sx, sy, sz));
        return m;
    }

    /** result = projection * view (view may be null for identity). */
    public static float[] vp(float[] projection, float[] view) {
        float[] out = new float[16];
        if (view == null) {
            System.arraycopy(projection, 0, out, 0, 16);
        } else {
            android.opengl.Matrix.multiplyMM(out, 0, projection, 0, view, 0);
        }
        return out;
    }
}
