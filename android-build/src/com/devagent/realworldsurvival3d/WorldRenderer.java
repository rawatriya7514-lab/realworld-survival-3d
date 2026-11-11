package com.devagent.realworldsurvival3d;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.HashMap;
import java.util.List;
import javax.microedition.khronos.opengles.GL10;

/**
 * WorldRenderer - GLES2 rendering engine for REALWORLD SURVIVAL 3D.
 *
 * Features:
 *   - Per-pixel Lambert lighting with a directional dusk sun
 *   - Hemisphere ambient (sky/ground) light
 *   - Blinn-Phong specular highlights
 *   - Exponential-squared distance fog matched to the sky horizon
 *   - Procedural sky gradient with horizon glow
 *   - Textured surfaces (concrete, asphalt, rusted metal, neon, hazard stripes)
 *   - Soft blob shadows under characters
 *   - Dynamic FOV, camera shake, wall-run Dutch-angle roll
 *   - 2D HUD overlay (bars, joystick, buttons, extraction compass)
 *   - Behind-camera culling and quality presets for mobile performance
 */
public class WorldRenderer {

    // ------------------------------------------------------------------
    // Quality presets
    // ------------------------------------------------------------------
    public static final int QUALITY_PERFORMANCE = 0;
    public static final int QUALITY_HIGH = 1;

    private int quality = QUALITY_HIGH;
    private float fogDensity = 0.0042f;
    private float farPlane = 420f;
    private boolean shadowsEnabled = true;
    private boolean userShadowsEnabled = true;
    private boolean bloomEnabled = true;

    public void setQuality(int q) {
        quality = q;
        if (q == SettingsConfig.QUALITY_LOW) {
            fogDensity = 0.0075f;   // heavier fog hides the shorter draw distance
            farPlane = 160f;
            shadowsEnabled = false;
        } else if (q == SettingsConfig.QUALITY_MEDIUM) {
            fogDensity = 0.0055f;
            farPlane = 260f;
            shadowsEnabled = userShadowsEnabled;
        } else {
            fogDensity = 0.0042f;
            farPlane = 420f;
            shadowsEnabled = userShadowsEnabled;
        }
    }

    public void setShadowsEnabled(boolean enabled) {
        this.userShadowsEnabled = enabled;
        this.shadowsEnabled = enabled;
    }

    public void setBloomEnabled(boolean enabled) {
        this.bloomEnabled = enabled;
    }

    // ------------------------------------------------------------------
    // Light / atmosphere palette (dusk)
    // ------------------------------------------------------------------
    private static final float[] SUN_DIR = normalize(new float[]{-0.42f, -0.62f, -0.30f});
    private static final float[] LIGHT_COLOR = {1.00f, 0.78f, 0.58f};
    private static final float[] AMB_SKY = {0.34f, 0.40f, 0.58f};
    private static final float[] AMB_GROUND = {0.16f, 0.15f, 0.14f};
    private static final float[] SKY_ZENITH = {0.075f, 0.11f, 0.24f};
    private static final float[] SKY_HORIZON = {0.86f, 0.44f, 0.24f};
    private static final float[] FOG_COLOR = {0.62f, 0.36f, 0.27f};

    // ------------------------------------------------------------------
    // Shader programs
    // ------------------------------------------------------------------
    private int worldProgram, hudProgram, skyProgram;

    // World shader attribute/uniform locations
    private int wLocPos, wLocNormal, wLocUV;
    private int wLocMVP, wLocModel, wLocTex, wLocUseTex, wLocTint, wLocUVScale;
    private int wLocSunDir, wLocLightColor, wLocAmbSky, wLocAmbGround;
    private int wLocCamPos, wLocFogColor, wLocFogDensity, wLocSpec, wLocAlpha;

    // HUD shader locations
    private int hLocPos, hLocMVP, hLocColor;

    // Sky shader locations
    private int sLocPos, sLocMVP, sLocZenith, sLocHorizon;

    // ------------------------------------------------------------------
    // Geometry
    // ------------------------------------------------------------------
    private FloatBuffer cubeVbo, quadVbo, quad2DVbo, fanVbo, triVbo, clipQuadVbo;
    private ShortBuffer cubeIbo, quadIbo;
    private static final int FLOATS_PER_VERT = 8; // pos3 normal3 uv2

    // ------------------------------------------------------------------
    // Textures
    // ------------------------------------------------------------------
    private static final String[] TEX_FILES = {
            "wall_concrete.png", "roof_asphalt.png", "rusted_metal.png",
            "hazard_stripes.png", "billboard_neon.png", "chaser_skin.png",
            "runner_suit.png"
    };
    private int[] texIds = new int[TEX_FILES.length];
    public static final int TEX_CONCRETE = 0, TEX_ROOF = 1, TEX_RUST = 2,
            TEX_HAZARD = 3, TEX_NEON = 4, TEX_CHASER = 5, TEX_RUNNER = 6;

    // Procedural 4-tile terrain atlas (grass / rock / sand / asphalt)
    private int atlasTex = 0;

    // Terrain chunk VBO cache (key = col * 10000 + row)
    private static final int MAX_CHUNK_VBOS = 27;
    private static final int VERT_STRIDE = FLOATS_PER_VERT * 4; // 32 bytes
    private static final class ChunkVbo {
        int vbo;
        int vertexCount;
    }
    private final HashMap<Long, ChunkVbo> chunkVbos = new HashMap<>();

    // ------------------------------------------------------------------
    // Camera rig & view state
    // ------------------------------------------------------------------
    public final CameraRig cameraRig = new CameraRig();
    private long lastCamTime = 0L;
    private float camX = 0, camY = 26, camZ = 10;
    private float camTX = 0, camTY = 22, camTZ = 0;
    private float camUpX = 0, camUpY = 1, camUpZ = 0;
    private float screenShake = 0f;
    private float[] projMatrix = new float[16];
    private float[] viewMatrix = new float[16];
    private float[] vpMatrix = new float[16];
    private float[] orthoMatrix = new float[16];

    public int screenWidth = 800;
    public int screenHeight = 600;

    // Pre-allocated scratch buffers to eliminate per-frame allocations (Phase 13)
    private final float[] scratchMvp = new float[16];
    private final float[] scratchModel = new float[16];
    public final CullingOptimizer cullingOptimizer = new CullingOptimizer();

    public boolean isVisible(float x, float y, float z, float radius) {
        float camFX = camTX - camX, camFY = camTY - camY, camFZ = camTZ - camZ;
        boolean vis = CullingOptimizer.isSphereVisible(x, y, z, radius, camX, camY, camZ,
                camFX, camFY, camFZ, farPlane);
        if (vis) cullingOptimizer.recordVisible();
        else cullingOptimizer.recordCulled();
        return vis;
    }

    private Context context;
    private boolean glReady = false;

    public WorldRenderer(Context context) {
        this.context = context;
        setQuality(QUALITY_HIGH);
    }

    // ==================================================================
    // GL lifecycle
    // ==================================================================
    public void init() {
        buildShaders();
        buildGeometry();
        loadAllTextures();
        buildTerrainAtlas();
        glReady = true;
    }

    public boolean isReady() {
        return glReady;
    }

    public void updateViewport(int w, int h) {
        screenWidth = w;
        screenHeight = h;
        Matrix.orthoM(orthoMatrix, 0, 0, w, h, 0, -1, 1);
    }

    // ==================================================================
    // Shaders
    // ==================================================================
    private static int compile(int type, String src, String tag) {
        int sh = GLES20.glCreateShader(type);
        GLES20.glShaderSource(sh, src);
        GLES20.glCompileShader(sh);
        int[] ok = new int[1];
        GLES20.glGetShaderiv(sh, GLES20.GL_COMPILE_STATUS, ok, 0);
        if (ok[0] == 0) {
            String log = GLES20.glGetShaderInfoLog(sh);
            android.util.Log.e("RWS3D", "Shader compile failed (" + tag + "): " + log);
            GLES20.glDeleteShader(sh);
            return 0;
        }
        return sh;
    }

    private static int linkProgram(int vs, int fs, String tag) {
        int p = GLES20.glCreateProgram();
        GLES20.glAttachShader(p, vs);
        GLES20.glAttachShader(p, fs);
        GLES20.glLinkProgram(p);
        int[] ok = new int[1];
        GLES20.glGetProgramiv(p, GLES20.GL_LINK_STATUS, ok, 0);
        if (ok[0] == 0) {
            String log = GLES20.glGetProgramInfoLog(p);
            android.util.Log.e("RWS3D", "Program link failed (" + tag + "): " + log);
            GLES20.glDeleteProgram(p);
            return 0;
        }
        return p;
    }

    private void buildShaders() {
        // ---------- world shader ----------
        String wvs = "uniform mat4 uMVP;\n" +
                "uniform mat4 uModel;\n" +
                "uniform vec2 uUVScaleHack;\n" +
                "attribute vec4 aPosition;\n" +
                "attribute vec3 aNormal;\n" +
                "attribute vec2 aTexCoord;\n" +
                "varying vec3 vNormal;\n" +
                "varying vec3 vWorldPos;\n" +
                "varying vec2 vUV;\n" +
                "void main() {\n" +
                "  vUV = aTexCoord * uUVScaleHack;\n" +
                "  vec4 wp = uModel * aPosition;\n" +
                "  vWorldPos = wp.xyz;\n" +
                // Axis-aligned normals keep their direction under axis scaling,
                // so a mat3(uModel)*normal followed by normalize is correct here.
                "  vNormal = normalize(mat3(uModel) * aNormal);\n" +
                "  gl_Position = uMVP * aPosition;\n" +
                "}\n";

        String wfs = "precision mediump float;\n" +
                "uniform sampler2D uTex;\n" +
                "uniform bool uUseTex;\n" +
                "uniform vec4 uTint;\n" +
                "uniform vec3 uSunDir;\n" +
                "uniform vec3 uLightColor;\n" +
                "uniform vec3 uAmbSky;\n" +
                "uniform vec3 uAmbGround;\n" +
                "uniform vec3 uCamPos;\n" +
                "uniform vec3 uFogColor;\n" +
                "uniform float uFogDensity;\n" +
                "uniform float uSpec;\n" +
                "uniform float uAlpha;\n" +
                "varying vec3 vNormal;\n" +
                "varying vec3 vWorldPos;\n" +
                "varying vec2 vUV;\n" +
                "void main() {\n" +
                "  vec4 base = uUseTex ? texture2D(uTex, vUV) * uTint : uTint;\n" +
                "  vec3 n = normalize(vNormal);\n" +
                "  float ndl = max(dot(n, -uSunDir), 0.0);\n" +
                "  float halfLambert = ndl * 0.82 + 0.18;\n" +
                "  vec3 amb = mix(uAmbGround, uAmbSky, clamp(n.y * 0.5 + 0.5, 0.0, 1.0));\n" +
                "  vec3 V = normalize(uCamPos - vWorldPos);\n" +
                "  vec3 H = normalize(V + (-uSunDir));\n" +
                "  float spec = pow(max(dot(n, H), 0.0), 28.0) * uSpec;\n" +
                "  vec3 col = base.rgb * (uLightColor * halfLambert + amb) + vec3(spec);\n" +
                "  float d = length(uCamPos - vWorldPos);\n" +
                "  float fog = 1.0 - exp(-uFogDensity * uFogDensity * d * d);\n" +
                "  col = mix(col, uFogColor, clamp(fog, 0.0, 1.0));\n" +
                "  gl_FragColor = vec4(col, base.a * uAlpha);\n" +
                "}\n";

        int vs = compile(GLES20.GL_VERTEX_SHADER, wvs, "world-vs");
        int fs = compile(GLES20.GL_FRAGMENT_SHADER, wfs, "world-fs");
        worldProgram = linkProgram(vs, fs, "world");

        if (worldProgram != 0) {
            wLocPos = GLES20.glGetAttribLocation(worldProgram, "aPosition");
            wLocNormal = GLES20.glGetAttribLocation(worldProgram, "aNormal");
            wLocUV = GLES20.glGetAttribLocation(worldProgram, "aTexCoord");
            wLocMVP = GLES20.glGetUniformLocation(worldProgram, "uMVP");
            wLocModel = GLES20.glGetUniformLocation(worldProgram, "uModel");
            wLocTex = GLES20.glGetUniformLocation(worldProgram, "uTex");
            wLocUseTex = GLES20.glGetUniformLocation(worldProgram, "uUseTex");
            wLocTint = GLES20.glGetUniformLocation(worldProgram, "uTint");
            wLocUVScale = GLES20.glGetUniformLocation(worldProgram, "uUVScaleHack");
            wLocSunDir = GLES20.glGetUniformLocation(worldProgram, "uSunDir");
            wLocLightColor = GLES20.glGetUniformLocation(worldProgram, "uLightColor");
            wLocAmbSky = GLES20.glGetUniformLocation(worldProgram, "uAmbSky");
            wLocAmbGround = GLES20.glGetUniformLocation(worldProgram, "uAmbGround");
            wLocCamPos = GLES20.glGetUniformLocation(worldProgram, "uCamPos");
            wLocFogColor = GLES20.glGetUniformLocation(worldProgram, "uFogColor");
            wLocFogDensity = GLES20.glGetUniformLocation(worldProgram, "uFogDensity");
            wLocSpec = GLES20.glGetUniformLocation(worldProgram, "uSpec");
            wLocAlpha = GLES20.glGetUniformLocation(worldProgram, "uAlpha");
        }

        // ---------- HUD shader ----------
        String hvs = "uniform mat4 uMVP;\n" +
                "attribute vec2 aPosition;\n" +
                "void main() { gl_Position = uMVP * vec4(aPosition, 0.0, 1.0); }\n";
        String hfs = "precision mediump float;\n" +
                "uniform vec4 uColor;\n" +
                "void main() { gl_FragColor = uColor; }\n";
        int hvsId = compile(GLES20.GL_VERTEX_SHADER, hvs, "hud-vs");
        int hfsId = compile(GLES20.GL_FRAGMENT_SHADER, hfs, "hud-fs");
        hudProgram = linkProgram(hvsId, hfsId, "hud");
        if (hudProgram != 0) {
            hLocPos = GLES20.glGetAttribLocation(hudProgram, "aPosition");
            hLocMVP = GLES20.glGetUniformLocation(hudProgram, "uMVP");
            hLocColor = GLES20.glGetUniformLocation(hudProgram, "uColor");
        }

        // ---------- Sky shader ----------
        String svs = "uniform mat4 uMVP;\n" +
                "attribute vec2 aPosition;\n" +
                "varying float vY;\n" +
                "void main() {\n" +
                "  vY = aPosition.y * 0.5 + 0.5;\n" +
                "  gl_Position = uMVP * vec4(aPosition, 0.999, 1.0);\n" +
                "}\n";
        String sfs = "precision mediump float;\n" +
                "uniform vec3 uZenith;\n" +
                "uniform vec3 uHorizon;\n" +
                "varying float vY;\n" +
                "void main() {\n" +
                "  vec3 col = mix(uHorizon, uZenith, pow(clamp(vY, 0.0, 1.0), 0.75));\n" +
                "  col += uHorizon * exp(-vY * 7.0) * 0.55;\n" + // horizon glow band
                "  gl_FragColor = vec4(col, 1.0);\n" +
                "}\n";
        int svsId = compile(GLES20.GL_VERTEX_SHADER, svs, "sky-vs");
        int sfsId = compile(GLES20.GL_FRAGMENT_SHADER, sfs, "sky-fs");
        skyProgram = linkProgram(svsId, sfsId, "sky");
        if (skyProgram != 0) {
            sLocPos = GLES20.glGetAttribLocation(skyProgram, "aPosition");
            sLocMVP = GLES20.glGetUniformLocation(skyProgram, "uMVP");
            sLocZenith = GLES20.glGetUniformLocation(skyProgram, "uZenith");
            sLocHorizon = GLES20.glGetUniformLocation(skyProgram, "uHorizon");
        }
    }

    // ==================================================================
    // Geometry
    // ==================================================================
    private void buildGeometry() {
        // --- Unit cube: 24 vertices, per-face normals & UVs ---
        float[][] faces = {
                // normal, and 4 corner offsets (ccw seen from outside)
                {0, 0, 1, -0.5f, -0.5f, 0.5f, 0.5f, -0.5f, 0.5f, 0.5f, 0.5f, 0.5f, -0.5f, 0.5f, 0.5f},   // front
                {0, 0, -1, 0.5f, -0.5f, -0.5f, -0.5f, -0.5f, -0.5f, -0.5f, 0.5f, -0.5f, 0.5f, 0.5f, -0.5f}, // back
                {0, 1, 0, -0.5f, 0.5f, -0.5f, -0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, -0.5f},   // top
                {0, -1, 0, -0.5f, -0.5f, 0.5f, 0.5f, -0.5f, 0.5f, 0.5f, -0.5f, -0.5f, -0.5f, -0.5f, -0.5f}, // bottom
                {1, 0, 0, 0.5f, -0.5f, 0.5f, 0.5f, -0.5f, -0.5f, 0.5f, 0.5f, -0.5f, 0.5f, 0.5f, 0.5f},   // right
                {-1, 0, 0, -0.5f, -0.5f, -0.5f, -0.5f, -0.5f, 0.5f, -0.5f, 0.5f, 0.5f, -0.5f, 0.5f, -0.5f}, // left
        };
        float[] uvFace = {0, 0, 1, 0, 1, 1, 0, 1};
        float[] cube = new float[24 * FLOATS_PER_VERT];
        int o = 0;
        for (float[] f : faces) {
            for (int v = 0; v < 4; v++) {
                cube[o++] = f[3 + v * 3];     // x
                cube[o++] = f[4 + v * 3];     // y
                cube[o++] = f[5 + v * 3];     // z
                cube[o++] = f[0];             // nx
                cube[o++] = f[1];             // ny
                cube[o++] = f[2];             // nz
                cube[o++] = uvFace[v * 2];    // u
                cube[o++] = uvFace[v * 2 + 1];// v
            }
        }
        short[] cubeIdx = new short[36];
        for (int f = 0; f < 6; f++) {
            cubeIdx[f * 6 + 0] = (short) (f * 4 + 0);
            cubeIdx[f * 6 + 1] = (short) (f * 4 + 1);
            cubeIdx[f * 6 + 2] = (short) (f * 4 + 2);
            cubeIdx[f * 6 + 3] = (short) (f * 4 + 0);
            cubeIdx[f * 6 + 4] = (short) (f * 4 + 2);
            cubeIdx[f * 6 + 5] = (short) (f * 4 + 3);
        }

        cubeVbo = floatBuffer(cube);
        cubeIbo = shortBuffer(cubeIdx);

        // --- Unit quad in XY (z=0) ---
        float[] quad = {
                0, 0, 0, 0, 1, 0, 0, 0,
                1, 0, 0, 0, 1, 0, 1, 0,
                1, 1, 0, 0, 1, 0, 1, 1,
                0, 1, 0, 0, 1, 0, 0, 1
        };
        short[] quadIdx = {0, 1, 2, 0, 2, 3};
        quadVbo = floatBuffer(quad);
        quadIbo = shortBuffer(quadIdx);

        // --- Dedicated 2D Unit Quad (0..1) for HUD panels and thick lines ---
        float[] quad2D = {
                0f, 0f,
                1f, 0f,
                1f, 1f,
                0f, 1f
        };
        quad2DVbo = floatBuffer(quad2D);

        // --- Fullscreen clip-space quad for the sky (-1..1) ---
        float[] clipQuad = {-1, -1, 1, -1, 1, 1, -1, 1};
        clipQuadVbo = floatBuffer(clipQuad);

        // --- Unit circle fan (2D, radius 1, centered origin) ---
        int segs = 40;
        float[] fan = new float[(segs + 2) * 2];
        fan[0] = 0; fan[1] = 0;
        for (int i = 0; i <= segs; i++) {
            double a = 2.0 * Math.PI * i / segs;
            fan[(i + 1) * 2] = (float) Math.cos(a);
            fan[(i + 1) * 2 + 1] = (float) Math.sin(a);
        }
        fanVbo = floatBuffer(fan);

        // --- Unit triangle pointing +X (2D) for compass arrow ---
        float[] tri = {1f, 0f, -0.7f, 0.55f, -0.7f, -0.55f};
        triVbo = floatBuffer(tri);
    }

    private static FloatBuffer floatBuffer(float[] arr) {
        ByteBuffer bb = ByteBuffer.allocateDirect(arr.length * 4);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer fb = bb.asFloatBuffer();
        fb.put(arr);
        fb.position(0);
        return fb;
    }

    private static ShortBuffer shortBuffer(short[] arr) {
        ByteBuffer bb = ByteBuffer.allocateDirect(arr.length * 2);
        bb.order(ByteOrder.nativeOrder());
        ShortBuffer sb = bb.asShortBuffer();
        sb.put(arr);
        sb.position(0);
        return sb;
    }

    // ==================================================================
    // Textures
    // ==================================================================
    private void loadAllTextures() {
        int[] ids = new int[TEX_FILES.length];
        GLES20.glGenTextures(TEX_FILES.length, ids, 0);
        for (int i = 0; i < TEX_FILES.length; i++) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, ids[i]);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
                    GLES20.GL_LINEAR_MIPMAP_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,
                    GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
                    GLES20.GL_REPEAT);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
                    GLES20.GL_REPEAT);
            InputStream is = null;
            try {
                is = context.getAssets().open("textures/" + TEX_FILES[i]);
                Bitmap bmp = BitmapFactory.decodeStream(is);
                if (bmp != null) {
                    GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0);
                    GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
                    bmp.recycle();
                }
            } catch (Exception e) {
                android.util.Log.e("RWS3D", "texture load failed: " + TEX_FILES[i], e);
            } finally {
                if (is != null) try { is.close(); } catch (Exception ignored) {}
            }
        }
        texIds = ids;
    }

    private static float[] normalize(float[] v) {
        float len = (float) Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        return new float[]{v[0] / len, v[1] / len, v[2] / len};
    }

    // ==================================================================
    // Terrain: procedural atlas + chunk VBOs
    // ==================================================================

    /** Deterministic 2D hash noise in [0,1). */
    private static float hash2(int x, int y) {
        int h = x * 374761393 + y * 668265263;
        h = (h ^ (h >> 13)) * 1274126177;
        return ((h ^ (h >> 16)) & 0xFFFF) / 65535f;
    }

    /**
     * Builds a 512x512 atlas of four 256px tiles:
     *   GL space (v=0 bottom): tile 0 grass (tx0,ty0), 1 rock (tx1,ty0),
     *   2 sand (tx0,ty1), 3 asphalt (tx1,ty1).
     * Bitmap row 0 is the TOP of the texture, so bitmap bands flip: ty0
     * occupies bitmap y 256..511, ty1 occupies 0..255.
     */
    private void buildTerrainAtlas() {
        final int S = 512, T = 256;
        int[] px = new int[S * S];
        for (int y = 0; y < S; y++) {
            int ty = (y < T) ? 1 : 0;   // bitmap band -> GL tile row
            for (int x = 0; x < S; x++) {
                int tx = x / T;
                int tile = tx + ty * 2; // matches Terrain.TILE_* ordering
                float n = hash2(x, y);
                float n2 = hash2(x * 3 + 7, y * 5 + 11);
                float r, g, b;
                switch (tile) {
                    case Terrain.TILE_ROCK:
                        r = 0.40f + 0.16f * n; g = 0.38f + 0.14f * n; b = 0.36f + 0.12f * n;
                        if (n2 > 0.8f) { r += 0.06f; g += 0.06f; b += 0.05f; }
                        break;
                    case Terrain.TILE_SAND:
                        r = 0.72f + 0.10f * n; g = 0.64f + 0.10f * n; b = 0.44f + 0.08f * n;
                        if (n2 > 0.9f) { r += 0.05f; g += 0.05f; }
                        break;
                    case Terrain.TILE_ASPHALT:
                        r = 0.13f + 0.05f * n; g = 0.13f + 0.05f * n; b = 0.15f + 0.05f * n;
                        if (n2 > 0.92f) { r += 0.10f; g += 0.10f; b += 0.10f; }
                        break;
                    default: // grass
                        r = 0.26f + 0.09f * n; g = 0.40f + 0.13f * n; b = 0.17f + 0.07f * n;
                        if (n2 > 0.82f) { r *= 0.8f; g *= 0.85f; }
                        break;
                }
                int ir = (int) (Math.min(1f, r) * 255f);
                int ig = (int) (Math.min(1f, g) * 255f);
                int ib = (int) (Math.min(1f, b) * 255f);
                px[y * S + x] = 0xFF000000 | (ir << 16) | (ig << 8) | ib;
            }
        }
        Bitmap bmp = Bitmap.createBitmap(px, S, S, Bitmap.Config.ARGB_8888);
        int[] ids = new int[1];
        GLES20.glGenTextures(1, ids, 0);
        atlasTex = ids[0];
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, atlasTex);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_LINEAR_MIPMAP_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_REPEAT);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_REPEAT);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0);
        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
        bmp.recycle();
    }

    private void deleteChunkVbos() {
        for (ChunkVbo c : chunkVbos.values()) {
            GLES20.glDeleteBuffers(1, new int[]{c.vbo}, 0);
        }
        chunkVbos.clear();
    }

    /** Uploads (or reuses) the static VBO for one terrain chunk. */
    private ChunkVbo ensureChunkVbo(int col, int row) {
        Long key = col * 10000L + row;
        ChunkVbo c = chunkVbos.get(key);
        if (c != null) return c;
        if (chunkVbos.size() >= MAX_CHUNK_VBOS) deleteChunkVbos();
        Terrain.MeshData mesh = Terrain.buildChunkMesh(col, row);
        FloatBuffer fb = floatBuffer(mesh.verts);
        int[] ids = new int[1];
        GLES20.glGenBuffers(1, ids, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, ids[0]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, mesh.verts.length * 4, fb,
                GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        c = new ChunkVbo();
        c.vbo = ids[0];
        c.vertexCount = mesh.vertexCount;
        chunkVbos.put(key, c);
        return c;
    }

    private void bindWorldAttribsVbo(int vboId) {
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId);
        GLES20.glEnableVertexAttribArray(wLocPos);
        GLES20.glVertexAttribPointer(wLocPos, 3, GLES20.GL_FLOAT, false, VERT_STRIDE, 0);
        GLES20.glEnableVertexAttribArray(wLocNormal);
        GLES20.glVertexAttribPointer(wLocNormal, 3, GLES20.GL_FLOAT, false, VERT_STRIDE, 12);
        GLES20.glEnableVertexAttribArray(wLocUV);
        GLES20.glVertexAttribPointer(wLocUV, 2, GLES20.GL_FLOAT, false, VERT_STRIDE, 24);
    }

    /** Draws the 3x3 active terrain chunks around the player. */
    private void drawTerrain(Player player, OpenWorld openWorld) {
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, atlasTex);
        GLES20.glUniform1i(wLocUseTex, 1);
        GLES20.glUniform2f(wLocUVScale, 1f, 1f);
        GLES20.glUniform4f(wLocTint, 0.98f, 0.98f, 0.98f, 1f);
        GLES20.glUniform1f(wLocSpec, 0.03f);
        GLES20.glUniform1f(wLocAlpha, 1f);
        int pc = OpenWorld.chunkIndex(player.x), pr = OpenWorld.chunkIndex(player.z);
        // Mesh vertices are already in world space: identity model matrix.
        Mat4.identity(scratchModel);
        Matrix.multiplyMM(scratchMvp, 0, vpMatrix, 0, scratchModel, 0);
        GLES20.glUniformMatrix4fv(wLocMVP, 1, false, scratchMvp, 0);
        GLES20.glUniformMatrix4fv(wLocModel, 1, false, scratchModel, 0);
        float camFX = camTX - camX, camFY = camTY - camY, camFZ = camTZ - camZ;
        for (int dc = -1; dc <= 1; dc++) {
            for (int dr = -1; dr <= 1; dr++) {
                int c = pc + dc, r = pr + dr;
                if (c < 0 || c >= OpenWorld.CHUNKS_PER_SIDE
                        || r < 0 || r >= OpenWorld.CHUNKS_PER_SIDE) continue;
                ChunkVbo chunk = ensureChunkVbo(c, r);
                float chunkX = c * 64f - 448f + 32f;
                float chunkZ = r * 64f - 448f + 32f;
                // Behind-camera cull on chunk centers
                float dx = chunkX - camX, dy = 0f, dz = chunkZ - camZ;
                float radius = 60f;
                if (dx * camFX + dy * camFY + dz * camFZ < -radius) continue;
                float distSq = dx * dx + dz * dz;
                if (distSq > (farPlane + 90f) * (farPlane + 90f)) continue;

                bindWorldAttribsVbo(chunk.vbo);
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, chunk.vertexCount);
                GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
                unbindWorldAttribs();
            }
        }
    }

    private DayNightCycle activeDayNight = null;

    // ==================================================================
    // Frame render
    // ==================================================================
    public void render(GL10 gl, Player player, WorldMap worldMap, List<HunterEnemy> chasers,
                       List<Item> items, TouchInput touch, GameView.GameState state,
                       float beaconPulse) {
        renderCore(player, worldMap, chasers, items, touch, state, beaconPulse, null,
                null, null, null, false, null);
    }

    /** Open-world frame: terrain chunks + streamed obstacles, no parkour beacon. */
    public void renderOpenWorld(GL10 gl, Player player, OpenWorld openWorld,
                                TouchInput touch, GameView.GameState state) {
        renderOpenWorld(gl, player, openWorld, touch, state, null, null, null, false, null);
    }

    public void renderOpenWorld(GL10 gl, Player player, OpenWorld openWorld,
                                TouchInput touch, GameView.GameState state,
                                RoadNetwork roadNetwork, NavigationTracker navTracker,
                                MapViewController mapController, boolean mapOverlayVisible) {
        renderOpenWorld(gl, player, openWorld, touch, state, roadNetwork, navTracker, mapController, mapOverlayVisible, null);
    }

    public void renderOpenWorld(GL10 gl, Player player, OpenWorld openWorld,
                                TouchInput touch, GameView.GameState state,
                                RoadNetwork roadNetwork, NavigationTracker navTracker,
                                MapViewController mapController, boolean mapOverlayVisible,
                                DayNightCycle dayNight) {
        renderOpenWorld(gl, player, openWorld, touch, state, roadNetwork, navTracker,
                mapController, mapOverlayVisible, dayNight, null);
    }

    public void renderOpenWorld(GL10 gl, Player player, OpenWorld openWorld,
                                TouchInput touch, GameView.GameState state,
                                RoadNetwork roadNetwork, NavigationTracker navTracker,
                                MapViewController mapController, boolean mapOverlayVisible,
                                DayNightCycle dayNight, List<Item> items) {
        renderCore(player, null, null, items, touch, state, 0f, openWorld,
                roadNetwork, navTracker, mapController, mapOverlayVisible, dayNight);
    }

    private void renderCore(Player player, WorldMap worldMap, List<HunterEnemy> chasers,
                            List<Item> items, TouchInput touch, GameView.GameState state,
                            float beaconPulse, OpenWorld openWorld,
                            RoadNetwork roadNetwork, NavigationTracker navTracker,
                            MapViewController mapController, boolean mapOverlayVisible,
                            DayNightCycle dayNight) {
        if (!glReady) {
            init();
        }

        this.activeDayNight = dayNight;
        if (dayNight != null) {
            GLES20.glClearColor(dayNight.fogColor[0], dayNight.fogColor[1], dayNight.fogColor[2], 1f);
            this.farPlane = dayNight.currentPreset.drawDistance;
            this.shadowsEnabled = userShadowsEnabled && dayNight.currentPreset.shadowsEnabled;
        } else {
            GLES20.glClearColor(FOG_COLOR[0], FOG_COLOR[1], FOG_COLOR[2], 1f);
            this.farPlane = 160f;
            this.shadowsEnabled = userShadowsEnabled;
        }
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glDepthFunc(GLES20.GL_LEQUAL);
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glCullFace(GLES20.GL_BACK);

        // ---- Projection & camera ----
        float aspect = (float) screenWidth / Math.max(1, screenHeight);
        boolean speedBoost = player.state == Player.ParkourState.SPRINT
                || player.state == Player.ParkourState.WALL_RUN_LEFT
                || player.state == Player.ParkourState.WALL_RUN_RIGHT
                || player.adrenalineRush;
        float fov = speedBoost ? 82f : 65f;
        Matrix.perspectiveM(projMatrix, 0, fov, aspect, 0.4f, farPlane);
        setupCamera(player, worldMap, openWorld);
        Matrix.multiplyMM(vpMatrix, 0, projMatrix, 0, viewMatrix, 0);

        // ---- Sky ----
        drawSky();

        // ---- World pass ----
        useWorldProgram();
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

        if (openWorld != null) {
            drawTerrain(player, openWorld);
        }

        List<Obstacle> worldObstacles = null;
        if (openWorld != null) {
            worldObstacles = openWorld.activeObstacles();
        } else if (worldMap != null && worldMap.obstacles != null) {
            worldObstacles = worldMap.obstacles;
        }
        if (worldObstacles != null) {
            for (Obstacle o : worldObstacles) {
                float radius = (o.width + o.height + o.depth) * 0.5f;
                if (!isVisible(o.x, o.y, o.z, radius)) continue;
                drawObstacle(o);
            }
        }

        // Extraction beacon (translucent, no depth write)
        if (worldMap != null) {
            GLES20.glEnable(GLES20.GL_BLEND);
            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
            GLES20.glDepthMask(false);
            float pulse = 0.45f + 0.25f * beaconPulse;
            drawCube(worldMap.extractionX, worldMap.extractionY + 15f, worldMap.extractionZ,
                    0, 0.9f, 30f, 0.9f, TEX_NEON,
                    1.0f, 0.85f, 0.15f, 1f, 1f, 1f, 0f, pulse);
            GLES20.glDepthMask(true);
            GLES20.glDisable(GLES20.GL_BLEND);
        }

        // Open-world navigation waypoint beacon (translucent cyan beam)
        if (openWorld != null && navTracker != null && navTracker.hasActiveWaypoint) {
            GLES20.glEnable(GLES20.GL_BLEND);
            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
            GLES20.glDepthMask(false);
            float groundY = Terrain.heightAt(navTracker.targetX, navTracker.targetZ);
            float pulse = 0.55f + 0.35f * (float) Math.sin(System.currentTimeMillis() * 0.005);
            drawCube(navTracker.targetX, groundY + 18f, navTracker.targetZ,
                    0, 1.4f, 36f, 1.4f, TEX_NEON,
                    0.15f, 0.85f, 1.0f, 1f, 1f, 1f, 0f, pulse);
            GLES20.glDepthMask(true);
            GLES20.glDisable(GLES20.GL_BLEND);
        }

        // Items
        if (items != null) {
            for (Item item : items) {
                if (item.collected || !isVisible(item.x, item.y, item.z, 2.0f)) continue;
                drawItem(item);
            }
        }

        // Characters
        if (chasers != null) {
            for (HunterEnemy ch : chasers) {
                if (ch.state == HunterEnemy.State.DEFEATED || !isVisible(ch.x, ch.y, ch.z, 2.5f)) continue;
                float face = (ch.vx != 0f || ch.vz != 0f)
                        ? (float) Math.toDegrees(Math.atan2(ch.vx, -ch.vz)) : 0f;
                if (ch.animator != null && ch.animator.currentPose != null) {
                    drawAnimatedCharacter(ch.x, ch.y, ch.z, face, ch.animator.currentPose,
                            TEX_CHASER, 0.85f, 0.25f, 0.25f);
                } else {
                    drawCharacter(ch.x, ch.y, ch.z, face, 0.8f, 1.7f, 0.8f, TEX_CHASER,
                            0.85f, 0.3f, 0.3f);
                }
                // glowing visor
                drawCube(ch.x, ch.y + 1.35f, ch.z, face, 0.34f, 0.12f, 0.2f, TEX_NEON,
                        1f, 0.15f, 0.05f, 1f, 1f, 1f, 0.5f, 1f);
            }
        }

        // Characters: Open-World Civilian NPCs & Hostiles (Phase 8)
        if (openWorld != null) {
            if (openWorld.civilians != null) {
                for (CivilianNPC npc : openWorld.civilians) {
                    if (npc.wasFrozen || !isVisible(npc.x, npc.y, npc.z, 2.5f)) continue;
                    float tr = 0.35f, tg = 0.65f, tb = 0.85f;
                    if ("Maya".equals(npc.name)) { tr = 0.25f; tg = 0.70f; tb = 0.90f; }
                    else if ("Goran".equals(npc.name)) { tr = 0.85f; tg = 0.55f; tb = 0.20f; }
                    else if ("Elena".equals(npc.name)) { tr = 0.90f; tg = 0.40f; tb = 0.60f; }
                    else if ("Jack".equals(npc.name)) { tr = 0.30f; tg = 0.80f; tb = 0.50f; }
                    else if ("Old Miller".equals(npc.name)) { tr = 0.75f; tg = 0.65f; tb = 0.45f; }
                    else if ("Ranger Thorne".equals(npc.name)) { tr = 0.25f; tg = 0.60f; tb = 0.30f; }

                    if (npc.animator != null && npc.animator.currentPose != null) {
                        drawAnimatedCharacter(npc.x, npc.y, npc.z, npc.yaw,
                                npc.animator.currentPose, TEX_RUNNER, tr, tg, tb);
                    } else {
                        drawCharacter(npc.x, npc.y, npc.z, npc.yaw, 0.75f, 1.7f, 0.75f,
                                TEX_RUNNER, tr, tg, tb);
                    }
                }
            }
            if (openWorld.hostiles != null) {
                for (HostileAI h : openWorld.hostiles) {
                    if (h.wasFrozen || h.state == HostileAI.State.DEFEATED || !isVisible(h.x, h.y, h.z, 2.5f)) continue;
                    if (h.animator != null && h.animator.currentPose != null) {
                        drawAnimatedCharacter(h.x, h.y, h.z, h.yaw,
                                h.animator.currentPose, TEX_CHASER, 0.80f, 0.15f, 0.15f);
                    } else {
                        drawCharacter(h.x, h.y, h.z, h.yaw, 0.8f, 1.7f, 0.8f,
                                TEX_CHASER, 0.80f, 0.15f, 0.15f);
                    }
                    // glowing red stalker visor/eyes
                    drawCube(h.x, h.y + 1.35f, h.z, h.yaw, 0.32f, 0.12f, 0.2f, TEX_NEON,
                            1f, 0.05f, 0.02f, 1f, 1f, 1f, 0.5f, 1f);
                }
            }
            if (openWorld.vehicles != null) {
                for (Vehicle v : openWorld.vehicles) {
                    if (!isVisible(v.x, v.y, v.z, 5.0f)) continue;
                    drawVehicle(v);
                }
            }
        }

        if (!player.firstPersonView && player.state != Player.ParkourState.DEAD) {
            float tintR = player.adrenalineRush ? 1.0f : 0.25f;
            float tintG = player.adrenalineRush ? 0.85f : 0.7f;
            float tintB = player.adrenalineRush ? 0.15f : 1.0f;
            if (player.animator != null && player.animator.currentPose != null) {
                drawAnimatedCharacter(player.x, player.y, player.z, -player.yaw,
                        player.animator.currentPose, TEX_RUNNER, tintR, tintG, tintB);
            } else {
                float w = (player.state == Player.ParkourState.SLIDING
                        || player.state == Player.ParkourState.SAFETY_ROLL
                        || player.state == Player.ParkourState.CROUCH) ? 1.05f : 0.75f;
                float h = player.currentHeight;
                drawCharacter(player.x, player.y, player.z, -player.yaw, w, h, w, TEX_RUNNER,
                        tintR, tintG, tintB);
            }
        }

        // Blob shadows
        if (shadowsEnabled) {
            GLES20.glEnable(GLES20.GL_BLEND);
            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
            GLES20.glDepthMask(false);
            if (player.state != Player.ParkourState.DEAD) {
                if (openWorld != null) {
                    drawShadowOpen(openWorld, player.x, player.z, player.y, 0.55f, 0.38f);
                } else {
                    drawShadow(worldMap, player.x, player.z, player.y, 0.55f, 0.38f);
                }
            }
            if (chasers != null) {
                for (HunterEnemy ch : chasers) {
                    if (ch.state != HunterEnemy.State.DEFEATED) {
                        drawShadow(worldMap, ch.x, ch.z, ch.y, 0.55f, 0.30f);
                    }
                }
            }
            if (openWorld != null) {
                if (openWorld.civilians != null) {
                    for (CivilianNPC npc : openWorld.civilians) {
                        if (!npc.wasFrozen) {
                            drawShadowOpen(openWorld, npc.x, npc.z, npc.y, 0.50f, 0.25f);
                        }
                    }
                }
                if (openWorld.hostiles != null) {
                    for (HostileAI h : openWorld.hostiles) {
                        if (!h.wasFrozen && h.state != HostileAI.State.DEFEATED) {
                            drawShadowOpen(openWorld, h.x, h.z, h.y, 0.55f, 0.30f);
                        }
                    }
                }
                if (openWorld.vehicles != null) {
                    for (Vehicle v : openWorld.vehicles) {
                        float vr = (v.type == Vehicle.Type.SUV) ? 1.5f : 0.85f;
                        drawShadowOpen(openWorld, v.x, v.z, v.y, vr, 0.35f);
                    }
                }
            }
            GLES20.glDepthMask(true);
            GLES20.glDisable(GLES20.GL_BLEND);
        }

        // ---- HUD ----
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glDisable(GLES20.GL_CULL_FACE);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        renderHUD(player, touch, state, worldMap, openWorld, roadNetwork, navTracker, mapController, mapOverlayVisible);
        GLES20.glDisable(GLES20.GL_BLEND);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_CULL_FACE);
    }

    private void setupCamera(Player player, WorldMap worldMap, OpenWorld openWorld) {
        long now = System.nanoTime();
        float dt = (lastCamTime == 0L) ? 0.016f : (now - lastCamTime) / 1_000_000_000f;
        lastCamTime = now;
        if (dt <= 0f || dt > 0.1f) dt = 0.016f;

        List<Obstacle> obstacles = (openWorld != null) ? openWorld.activeObstacles()
                : (worldMap != null ? worldMap.obstacles : null);
        boolean terrainAware = (openWorld != null);

        cameraRig.update(dt, player, obstacles, terrainAware);

        camX = cameraRig.camX;
        camY = cameraRig.camY;
        camZ = cameraRig.camZ;
        camTX = cameraRig.camTX;
        camTY = cameraRig.camTY;
        camTZ = cameraRig.camTZ;
        camUpX = cameraRig.camUpX;
        camUpY = cameraRig.camUpY;
        camUpZ = cameraRig.camUpZ;

        Matrix.setLookAtM(viewMatrix, 0, camX, camY, camZ, camTX, camTY, camTZ,
                camUpX, camUpY, camUpZ);
    }

    // ==================================================================
    // World drawing helpers
    // ==================================================================
    private void useWorldProgram() {
        GLES20.glUseProgram(worldProgram);
        float[] sun = (activeDayNight != null) ? activeDayNight.sunDirection : SUN_DIR;
        float[] light = (activeDayNight != null) ? activeDayNight.lightColor : LIGHT_COLOR;
        float[] aSky = (activeDayNight != null) ? activeDayNight.ambientSky : AMB_SKY;
        float[] aGnd = (activeDayNight != null) ? activeDayNight.ambientGround : AMB_GROUND;
        float[] fog = (activeDayNight != null) ? activeDayNight.fogColor : FOG_COLOR;
        float fDensity = (activeDayNight != null) ? activeDayNight.fogDensity : fogDensity;

        GLES20.glUniform3f(wLocSunDir, sun[0], sun[1], sun[2]);
        GLES20.glUniform3f(wLocLightColor, light[0], light[1], light[2]);
        GLES20.glUniform3f(wLocAmbSky, aSky[0], aSky[1], aSky[2]);
        GLES20.glUniform3f(wLocAmbGround, aGnd[0], aGnd[1], aGnd[2]);
        GLES20.glUniform3f(wLocCamPos, camX, camY, camZ);
        GLES20.glUniform3f(wLocFogColor, fog[0], fog[1], fog[2]);
        GLES20.glUniform1f(wLocFogDensity, fDensity);
        GLES20.glUniform1i(wLocTex, 0);
    }

    private void drawObstacle(Obstacle o) {
        // --- Open-world types with custom multi-part or translucent draws ---
        if (o.type == Obstacle.Type.TREE) { drawTree(o); return; }
        if (o.type == Obstacle.Type.LAMP) { drawLamp(o); return; }
        if (o.type == Obstacle.Type.WATER_PLANE) { drawWater(o); return; }
        if (o.type == Obstacle.Type.BUSH) {
            drawBoxNoTex(o, o.r, o.g, o.b, 0.95f, 0.1f);
            return;
        }
        if (o.type == Obstacle.Type.ROCK) {
            drawBoxNoTex(o, o.r, o.g, o.b, 1f, 0.15f);
            return;
        }

        int tex;
        float r = o.r, g = o.g, b = o.b;
        float uvx, uvy, spec;
        switch (o.type) {
            case ROAD_SLAB:
                tex = TEX_ROOF;
                uvx = Math.max(1f, o.width / 8f);
                uvy = Math.max(1f, o.depth / 8f);
                spec = 0.08f;
                break;
            case BRIDGE:
                tex = TEX_RUST;
                uvx = Math.max(1f, Math.max(o.width, o.depth) / 4f);
                uvy = Math.max(1f, o.height / 4f);
                spec = 0.2f;
                break;
            case CRATE:
                tex = TEX_RUST;
                uvx = 1.5f;
                uvy = 1.5f;
                spec = 0.15f;
                break;
            case BUILDING:
            default:
                tex = TEX_CONCRETE;
                uvx = Math.max(1f, o.width / 5f);
                uvy = Math.max(1f, o.height / 5f);
                spec = 0.05f;
                break;
            case WALLRUN_PANEL:
                tex = TEX_NEON;
                uvx = Math.max(1f, o.width / 6f);
                uvy = Math.max(1f, o.height / 6f);
                spec = 0.35f;
                break;
            case VAULT_BARRIER:
                tex = TEX_HAZARD;
                uvx = Math.max(1f, o.width / 3f);
                uvy = Math.max(1f, o.height / 3f);
                spec = 0.10f;
                break;
            case BALANCE_BEAM:
                tex = TEX_RUST;
                uvx = Math.max(1f, o.depth / 3f);
                uvy = 1f;
                spec = 0.25f;
                break;
            case JUMP_PAD:
                tex = TEX_NEON;
                uvx = 2f;
                uvy = 2f;
                spec = 0.5f;
                break;
            case HAZARD_POOL:
                tex = TEX_HAZARD;
                uvx = o.width / 8f;
                uvy = o.depth / 8f;
                spec = 0.6f;
                break;
            case EXTRACTION_HELIPAD:
                tex = TEX_ROOF;
                uvx = 3f;
                uvy = 3f;
                spec = 0.15f;
                break;
            case VENT:
            case AC_UNIT:
            case ANTENNA:
            case WATER_TANK:
            case CONSOLE:
                tex = TEX_RUST;
                uvx = 1.5f;
                uvy = 1.5f;
                spec = 0.2f;
                break;
        }
        float[] model = Mat4.trs(o.x, o.y, o.z, 0f, o.width, o.height, o.depth);
        drawCubeModel(model, tex, r, g, b, 1f, uvx, uvy, spec, 1f);
    }

    // ==================================================================
    // Open-world prop draws (trees, lamps, water, untextured boxes)
    // ==================================================================
    private void drawTree(Obstacle o) {
        float[] trunk = Mat4.trs(o.x, o.y, o.z, 0f, o.width, o.height, o.depth);
        drawCubeModel(trunk, TEX_RUST, 0.36f, 0.25f, 0.15f, 1f, 1f, 1f, 0.1f, 1f);
        float topY = o.getMaxY();
        float[] c1 = Mat4.trs(o.x, topY + 1.1f, o.z, 0f, 3.4f, 2.2f, 3.4f);
        drawCubeModelNoTex(c1, 0.17f, 0.35f, 0.13f, 1f, 0.05f);
        float[] c2 = Mat4.trs(o.x, topY + 2.4f, o.z, 0f, 2.2f, 1.6f, 2.2f);
        drawCubeModelNoTex(c2, 0.20f, 0.40f, 0.15f, 1f, 0.05f);
    }

    private void drawLamp(Obstacle o) {
        float[] pole = Mat4.trs(o.x, o.y, o.z, 0f, o.width, o.height, o.depth);
        drawCubeModel(pole, TEX_RUST, 0.30f, 0.32f, 0.36f, 1f, 1f, 1f, 0.2f, 1f);
        float topY = o.getMaxY();
        float[] glow = Mat4.trs(o.x, topY + 0.3f, o.z, 0f, 0.8f, 0.5f, 0.8f);
        drawCubeModelNoTex(glow, 1.0f, 0.85f, 0.45f, 0.9f, 0.8f);
    }

    private void drawWater(Obstacle o) {
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glDepthMask(false);
        drawBoxNoTex(o, 0.16f, 0.36f, 0.55f, 0.62f, 0.5f);
        GLES20.glDepthMask(true);
        GLES20.glDisable(GLES20.GL_BLEND);
    }

    private void drawBoxNoTex(Obstacle o, float r, float g, float b,
                              float alpha, float spec) {
        Mat4.setTRS(scratchModel, o.x, o.y, o.z, 0f, o.width, o.height, o.depth);
        drawCubeModelNoTex(scratchModel, r, g, b, alpha, spec);
    }

    /** Cube drawn with the world shader but sampling disabled (plain tint). */
    private void drawCubeModelNoTex(float[] model, float r, float g, float b,
                                    float alpha, float spec) {
        Matrix.multiplyMM(scratchMvp, 0, vpMatrix, 0, model, 0);
        GLES20.glUniformMatrix4fv(wLocMVP, 1, false, scratchMvp, 0);
        GLES20.glUniformMatrix4fv(wLocModel, 1, false, model, 0);
        GLES20.glUniform2f(wLocUVScale, 1f, 1f);
        GLES20.glUniform4f(wLocTint, r, g, b, 1f);
        GLES20.glUniform1f(wLocSpec, spec);
        GLES20.glUniform1f(wLocAlpha, alpha);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, atlasTex);
        GLES20.glUniform1i(wLocUseTex, 0);
        bindWorldAttribs(cubeVbo);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, 36, GLES20.GL_UNSIGNED_SHORT, cubeIbo);
        unbindWorldAttribs();
    }

    private void drawItem(Item item) {
        float r, g, b;
        switch (item.type) {
            case ENERGY_DRINK: r = 0.1f; g = 0.9f; b = 0.95f; break;
            case MEDKIT:       r = 0.95f; g = 0.15f; b = 0.2f; break;
            case DATA_DRIVE:   r = 0.85f; g = 0.3f; b = 0.95f; break;
            case RADIO_PART:   r = 0.15f; g = 0.85f; b = 1.0f; break;
            case DATA_SLATE:   r = 1.0f; g = 0.82f; b = 0.15f; break;
            default:           r = 1.0f; g = 0.85f; b = 0.1f; break;
        }
        float pulse = 0.75f + 0.25f * (float) Math.sin(item.rotation * 2.0);
        drawCube(item.x, item.y + item.bobOffset + 0.6f, item.z, item.rotation,
                0.6f, 0.6f, 0.6f, TEX_NEON, r, g, b, 1f, 1f, 1f, 1.0f, pulse);
    }

    private void drawVehicle(Vehicle v) {
        float rad = (float) Math.toRadians(v.yaw);
        float fwdX = (float) Math.sin(rad), fwdZ = -(float) Math.cos(rad);
        float rightX = (float) Math.cos(rad), rightZ = (float) Math.sin(rad);

        if (v.type == Vehicle.Type.SUV) {
            // Main SUV Body
            drawCube(v.x, v.y + 0.60f, v.z, v.yaw, 2.0f, 0.65f, 4.0f,
                    TEX_CONCRETE, 0.22f, 0.38f, 0.65f, 1f, 1f, 1f, 0.3f, 1f);
            // Cabin / windows
            drawCube(v.x, v.y + 1.25f, v.z - 0.2f, v.yaw, 1.7f, 0.65f, 2.2f,
                    TEX_ROOF, 0.15f, 0.22f, 0.30f, 1f, 1f, 1f, 0.6f, 1f);
            // Wheels
            float[][] wheelOffsets = {
                    {0.95f, 1.3f},   // front-right
                    {-0.95f, 1.3f},  // front-left
                    {0.95f, -1.3f},  // rear-right
                    {-0.95f, -1.3f}  // rear-left
            };
            for (float[] off : wheelOffsets) {
                float wx = v.x + rightX * off[0] + fwdX * off[1];
                float wz = v.z + rightZ * off[0] + fwdZ * off[1];
                drawCube(wx, v.y + 0.32f, wz, v.yaw, 0.38f, 0.64f, 0.64f,
                        TEX_CONCRETE, 0.12f, 0.12f, 0.12f, 1f, 1f, 1f, 0.2f, 1f);
            }
            // Headlights
            if (v.headlightsOn) {
                float h1x = v.x + rightX * 0.7f + fwdX * 2.05f;
                float h1z = v.z + rightZ * 0.7f + fwdZ * 2.05f;
                float h2x = v.x - rightX * 0.7f + fwdX * 2.05f;
                float h2z = v.z - rightZ * 0.7f + fwdZ * 2.05f;
                drawCube(h1x, v.y + 0.65f, h1z, v.yaw, 0.35f, 0.16f, 0.12f,
                        TEX_NEON, 1.0f, 1.0f, 0.85f, 1f, 1f, 1f, 0.9f, 1f);
                drawCube(h2x, v.y + 0.65f, h2z, v.yaw, 0.35f, 0.16f, 0.12f,
                        TEX_NEON, 1.0f, 1.0f, 0.85f, 1f, 1f, 1f, 0.9f, 1f);
            }
        } else {
            // Motorcycle Frame
            drawCube(v.x, v.y + 0.55f, v.z, v.yaw, 0.55f, 0.50f, 1.8f,
                    TEX_CONCRETE, 0.90f, 0.38f, 0.12f, 1f, 1f, 1f, 0.4f, 1f);
            // Handlebars
            drawCube(v.x, v.y + 0.95f, v.z + 0.45f, v.yaw, 0.80f, 0.12f, 0.15f,
                    TEX_RUST, 0.20f, 0.20f, 0.20f, 1f, 1f, 1f, 0.2f, 1f);
            // Wheels
            float rad2 = (float) Math.toRadians(v.yaw);
            float fwdX2 = (float) Math.sin(rad2), fwdZ2 = -(float) Math.cos(rad2);
            drawCube(v.x + fwdX2 * 0.85f, v.y + 0.32f, v.z + fwdZ2 * 0.85f, v.yaw,
                    0.25f, 0.64f, 0.64f, TEX_CONCRETE, 0.12f, 0.12f, 0.12f, 1f, 1f, 1f, 0.2f, 1f);
            drawCube(v.x - fwdX2 * 0.85f, v.y + 0.32f, v.z - fwdZ2 * 0.85f, v.yaw,
                    0.25f, 0.64f, 0.64f, TEX_CONCRETE, 0.12f, 0.12f, 0.12f, 1f, 1f, 1f, 0.2f, 1f);
            // Headlight
            if (v.headlightsOn) {
                drawCube(v.x + fwdX2 * 1.0f, v.y + 0.75f, v.z + fwdZ2 * 1.0f, v.yaw,
                        0.25f, 0.20f, 0.12f, TEX_NEON, 1.0f, 1.0f, 0.85f, 1f, 1f, 1f, 0.9f, 1f);
            }
        }
    }

    private void drawCharacter(float x, float y, float z, float yawDeg,
                               float w, float h, float d,
                               int tex, float tr, float tg, float tb) {
        drawCube(x, y + h * 0.5f, z, yawDeg, w, h, d, tex, tr, tg, tb,
                1f, 1f, 1f, 0.3f, 1f);
    }

    private void drawAnimatedCharacter(float x, float y, float z, float yawDeg,
                                      CharacterAnimator.Pose pose,
                                      int tex, float tr, float tg, float tb) {
        float[] base = Mat4.trs(x, y, z, yawDeg, 1f, 1f, 1f);

        // 1. Torso
        float torsoCenterY = 1.05f + pose.torsoOffsetY;
        float[] mTorso = Mat4.identity();
        System.arraycopy(base, 0, mTorso, 0, 16);
        Mat4.mul(mTorso, Mat4.translate(0f, torsoCenterY, 0f));
        if (pose.torsoRoll != 0f) Mat4.mul(mTorso, Mat4.rotZ(pose.torsoRoll));
        if (pose.torsoPitch != 0f) Mat4.mul(mTorso, Mat4.rotX(pose.torsoPitch));
        if (pose.torsoYaw != 0f) Mat4.mul(mTorso, Mat4.rotY(pose.torsoYaw));

        float[] mTorsoCube = Mat4.identity();
        System.arraycopy(mTorso, 0, mTorsoCube, 0, 16);
        Mat4.mul(mTorsoCube, Mat4.scale(0.46f, 0.60f, 0.28f));
        drawCubeModel(mTorsoCube, tex, tr, tg, tb, 1f, 1f, 1f, 0.3f, 1f);

        // 2. Head
        float[] mHead = Mat4.identity();
        System.arraycopy(mTorso, 0, mHead, 0, 16);
        Mat4.mul(mHead, Mat4.translate(0f, 0.45f, 0.02f));
        if (pose.headRoll != 0f) Mat4.mul(mHead, Mat4.rotZ(pose.headRoll));
        if (pose.headPitch != 0f) Mat4.mul(mHead, Mat4.rotX(pose.headPitch));
        if (pose.headYaw != 0f) Mat4.mul(mHead, Mat4.rotY(pose.headYaw));
        Mat4.mul(mHead, Mat4.scale(0.28f, 0.28f, 0.28f));
        drawCubeModel(mHead, tex, tr * 1.15f, tg * 1.15f, tb * 1.15f, 1f, 1f, 1f, 0.4f, 1f);

        // 3. Left Arm
        float[] mLArm = Mat4.identity();
        System.arraycopy(mTorso, 0, mLArm, 0, 16);
        Mat4.mul(mLArm, Mat4.translate(-0.30f, 0.22f, 0f));
        if (pose.leftArmRoll != 0f) Mat4.mul(mLArm, Mat4.rotZ(pose.leftArmRoll));
        if (pose.leftArmPitch != 0f) Mat4.mul(mLArm, Mat4.rotX(pose.leftArmPitch));
        if (pose.leftArmYaw != 0f) Mat4.mul(mLArm, Mat4.rotY(pose.leftArmYaw));
        Mat4.mul(mLArm, Mat4.translate(0f, -0.26f, 0f));
        Mat4.mul(mLArm, Mat4.scale(0.14f, 0.54f, 0.14f));
        drawCubeModel(mLArm, tex, tr * 0.9f, tg * 0.9f, tb * 0.9f, 1f, 1f, 1f, 0.25f, 1f);

        // 4. Right Arm
        float[] mRArm = Mat4.identity();
        System.arraycopy(mTorso, 0, mRArm, 0, 16);
        Mat4.mul(mRArm, Mat4.translate(0.30f, 0.22f, 0f));
        if (pose.rightArmRoll != 0f) Mat4.mul(mRArm, Mat4.rotZ(pose.rightArmRoll));
        if (pose.rightArmPitch != 0f) Mat4.mul(mRArm, Mat4.rotX(pose.rightArmPitch));
        if (pose.rightArmYaw != 0f) Mat4.mul(mRArm, Mat4.rotY(pose.rightArmYaw));
        Mat4.mul(mRArm, Mat4.translate(0f, -0.26f, 0f));
        Mat4.mul(mRArm, Mat4.scale(0.14f, 0.54f, 0.14f));
        drawCubeModel(mRArm, tex, tr * 0.9f, tg * 0.9f, tb * 0.9f, 1f, 1f, 1f, 0.25f, 1f);

        // 5. Left Leg
        float[] mLLeg = Mat4.identity();
        System.arraycopy(base, 0, mLLeg, 0, 16);
        Mat4.mul(mLLeg, Mat4.translate(-0.13f, 0.72f + pose.torsoOffsetY * 0.5f, 0f));
        if (pose.leftLegRoll != 0f) Mat4.mul(mLLeg, Mat4.rotZ(pose.leftLegRoll));
        if (pose.leftLegPitch != 0f) Mat4.mul(mLLeg, Mat4.rotX(pose.leftLegPitch));
        if (pose.leftLegYaw != 0f) Mat4.mul(mLLeg, Mat4.rotY(pose.leftLegYaw));
        Mat4.mul(mLLeg, Mat4.translate(0f, -0.34f, 0f));
        Mat4.mul(mLLeg, Mat4.scale(0.18f, 0.70f, 0.18f));
        drawCubeModel(mLLeg, tex, tr * 0.75f, tg * 0.75f, tb * 0.75f, 1f, 1f, 1f, 0.2f, 1f);

        // 6. Right Leg
        float[] mRLeg = Mat4.identity();
        System.arraycopy(base, 0, mRLeg, 0, 16);
        Mat4.mul(mRLeg, Mat4.translate(0.13f, 0.72f + pose.torsoOffsetY * 0.5f, 0f));
        if (pose.rightLegRoll != 0f) Mat4.mul(mRLeg, Mat4.rotZ(pose.rightLegRoll));
        if (pose.rightLegPitch != 0f) Mat4.mul(mRLeg, Mat4.rotX(pose.rightLegPitch));
        if (pose.rightLegYaw != 0f) Mat4.mul(mRLeg, Mat4.rotY(pose.rightLegYaw));
        Mat4.mul(mRLeg, Mat4.translate(0f, -0.34f, 0f));
        Mat4.mul(mRLeg, Mat4.scale(0.18f, 0.70f, 0.18f));
        drawCubeModel(mRLeg, tex, tr * 0.75f, tg * 0.75f, tb * 0.75f, 1f, 1f, 1f, 0.2f, 1f);
    }

    private void drawCube(float x, float y, float z, float rotDeg,
                          float sx, float sy, float sz, int tex,
                          float r, float g, float b, float tintA,
                          float uvx, float uvy, float spec, float alpha) {
        Mat4.setTRS(scratchModel, x, y, z, rotDeg, sx, sy, sz);
        drawCubeModel(scratchModel, tex, r, g, b, tintA, uvx, uvy, spec, alpha);
    }

    private void drawCubeModel(float[] model, int tex, float r, float g, float b, float a,
                               float uvx, float uvy, float spec, float alpha) {
        Matrix.multiplyMM(scratchMvp, 0, vpMatrix, 0, model, 0);
        GLES20.glUniformMatrix4fv(wLocMVP, 1, false, scratchMvp, 0);
        GLES20.glUniformMatrix4fv(wLocModel, 1, false, model, 0);
        GLES20.glUniform2f(wLocUVScale, uvx, uvy);
        GLES20.glUniform4f(wLocTint, r, g, b, a);
        GLES20.glUniform1f(wLocSpec, spec);
        GLES20.glUniform1f(wLocAlpha, alpha);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texIds[tex]);
        GLES20.glUniform1i(wLocUseTex, 1);

        bindWorldAttribs(cubeVbo);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, 36, GLES20.GL_UNSIGNED_SHORT, cubeIbo);
        unbindWorldAttribs();
    }

    private void bindWorldAttribs(FloatBuffer vbo) {
        vbo.position(0);
        GLES20.glEnableVertexAttribArray(wLocPos);
        GLES20.glVertexAttribPointer(wLocPos, 3, GLES20.GL_FLOAT, false,
                FLOATS_PER_VERT * 4, vbo);
        FloatBuffer n = vbo.duplicate();
        n.position(3);
        GLES20.glEnableVertexAttribArray(wLocNormal);
        GLES20.glVertexAttribPointer(wLocNormal, 3, GLES20.GL_FLOAT, false,
                FLOATS_PER_VERT * 4, n);
        FloatBuffer uv = vbo.duplicate();
        uv.position(6);
        GLES20.glEnableVertexAttribArray(wLocUV);
        GLES20.glVertexAttribPointer(wLocUV, 2, GLES20.GL_FLOAT, false,
                FLOATS_PER_VERT * 4, uv);
    }

    private void unbindWorldAttribs() {
        GLES20.glDisableVertexAttribArray(wLocPos);
        GLES20.glDisableVertexAttribArray(wLocNormal);
        GLES20.glDisableVertexAttribArray(wLocUV);
    }

    private void drawSky() {
        if (skyProgram == 0) return;
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glDepthMask(false);
        GLES20.glUseProgram(skyProgram);
        float[] identity = new float[16];
        Matrix.setIdentityM(identity, 0);
        GLES20.glUniformMatrix4fv(sLocMVP, 1, false, identity, 0);
        float[] zenith = (activeDayNight != null) ? activeDayNight.skyZenith : SKY_ZENITH;
        float[] horizon = (activeDayNight != null) ? activeDayNight.skyHorizon : SKY_HORIZON;
        GLES20.glUniform3f(sLocZenith, zenith[0], zenith[1], zenith[2]);
        GLES20.glUniform3f(sLocHorizon, horizon[0], horizon[1], horizon[2]);
        GLES20.glEnableVertexAttribArray(sLocPos);
        GLES20.glVertexAttribPointer(sLocPos, 2, GLES20.GL_FLOAT, false, 0, clipQuadVbo);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 4);
        GLES20.glDisableVertexAttribArray(sLocPos);
        GLES20.glDepthMask(true);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
    }

    private void drawShadow(WorldMap worldMap, float x, float z, float entityY,
                            float size, float alpha) {
        drawShadowAt(worldMap.getFloorY(x, z, entityY), x, z, entityY, size, alpha);
    }

    private void drawShadowOpen(OpenWorld openWorld, float x, float z, float entityY,
                                float size, float alpha) {
        drawShadowAt(openWorld.floorAt(x, z, entityY), x, z, entityY, size, alpha);
    }

    private void drawShadowAt(float floorY, float x, float z, float entityY,
                              float size, float alpha) {
        if (floorY < -900f) return;
        float height = entityY - floorY;
        float fade = (float) Math.max(0.15, 1.0 - height / 14.0);
        GLES20.glUseProgram(hudProgram);
        float[] m = Mat4.translate(x - size, floorY + 0.03f, z - size);
        // Rotate flat onto XZ plane
        Mat4.mul(m, Mat4.rotX(-90f));
        Mat4.mul(m, Mat4.scale(size * 2f, size * 2f, 1f));
        float[] mvp = new float[16];
        Matrix.multiplyMM(mvp, 0, vpMatrix, 0, m, 0);
        GLES20.glUniformMatrix4fv(hLocMVP, 1, false, mvp, 0);
        GLES20.glUniform4f(hLocColor, 0f, 0f, 0f, alpha * fade);
        GLES20.glEnableVertexAttribArray(hLocPos);
        GLES20.glVertexAttribPointer(hLocPos, 2, GLES20.GL_FLOAT, false, 0, quadVbo);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 4);
        GLES20.glDisableVertexAttribArray(hLocPos);
    }

    // ==================================================================
    // HUD & Mini/World Map (Phase 6)
    // ==================================================================
    private void renderHUD(Player player, TouchInput touch, GameView.GameState state,
                           WorldMap worldMap, OpenWorld openWorld, RoadNetwork roadNetwork,
                           NavigationTracker navTracker, MapViewController mapController,
                           boolean mapOverlayVisible) {
        GLES20.glUseProgram(hudProgram);
        float[] mvp = new float[16];
        float[] tmp = new float[16];

        if (mapOverlayVisible && mapController != null) {
            renderFullscreenMap(player, touch, openWorld, roadNetwork, navTracker, mapController, mvp, tmp);
            return;
        }

        // Health / stamina / adrenaline bars
        drawBar(mvp, tmp, 25f, 25f, 240f, 18f, player.health / player.maxHealth,
                0.2f, 0.85f, 0.25f);
        drawBar(mvp, tmp, 25f, 50f, 240f, 14f, player.stamina / player.maxStamina,
                0.1f, 0.75f, 0.95f);
        drawBar(mvp, tmp, 25f, 70f, 240f, 10f, player.adrenaline / player.maxAdrenaline,
                1.0f, 0.75f, 0.1f);

        if (touch != null) {
            float op = touch.buttonOpacity;
            drawCircle(mvp, tmp, touch.joystickBaseX, touch.joystickBaseY,
                    touch.joystickRadius, 0.25f, 0.25f, 0.3f, op * 0.60f);
            drawCircle(mvp, tmp, touch.joystickCurrentX, touch.joystickCurrentY,
                    touch.joystickRadius * 0.42f, 0.2f, 0.85f, 0.95f,
                    touch.isJoystickActive ? Math.min(1.0f, op * 1.3f) : op * 0.75f);

            drawCircle(mvp, tmp, touch.btnJumpX, touch.btnJumpY, touch.btnJumpRadius,
                    0.10f, 0.75f, 0.95f, op);
            drawCircle(mvp, tmp, touch.btnVaultX, touch.btnVaultY, touch.btnVaultRadius,
                    0.95f, 0.85f, 0.10f, op);
            drawCircle(mvp, tmp, touch.btnSlideX, touch.btnSlideY, touch.btnSlideRadius,
                    0.95f, 0.45f, 0.15f, op);
            drawCircle(mvp, tmp, touch.btnSprintX, touch.btnSprintY, touch.btnSprintRadius,
                    player.isSprinting ? 0.95f : 0.4f, 0.3f,
                    player.isSprinting ? 0.2f : 0.8f,
                    player.isSprinting ? Math.min(1.0f, op * 1.3f) : op);

            // Dedicated Crouch button (Phase 5)
            if (touch.btnCrouchRadius > 0) {
                boolean crouched = player.isCrouching || player.state == Player.ParkourState.CROUCH;
                drawCircle(mvp, tmp, touch.btnCrouchX, touch.btnCrouchY, touch.btnCrouchRadius,
                        crouched ? 0.25f : 0.45f,
                        crouched ? 0.90f : 0.60f,
                        crouched ? 0.80f : 0.70f,
                        crouched ? Math.min(1.0f, op * 1.3f) : op);
            }

            if (touch.btnCamRadius > 0) {
                drawCircle(mvp, tmp, touch.btnCamX, touch.btnCamY, touch.btnCamRadius,
                        0.6f, 0.6f, 0.7f, op * 0.9f);
            }
            if (touch.btnMapRadius > 0) {
                drawCircle(mvp, tmp, touch.btnMapX, touch.btnMapY, touch.btnMapRadius,
                        0.20f, 0.75f, 0.90f, op * 0.85f);
            }
            if (touch.btnInteractRadius > 0 && touch.interactVisible) {
                drawCircle(mvp, tmp, touch.btnInteractX, touch.btnInteractY,
                        touch.btnInteractRadius, 1.0f, 0.65f, 0.1f, Math.min(1.0f, op * 1.3f));
            }
            if (touch.btnPauseRadius > 0) {
                drawCircle(mvp, tmp, touch.btnPauseX, touch.btnPauseY, touch.btnPauseRadius,
                        0.9f, 0.9f, 0.95f, op * 0.85f);
            }
        }

        // Rotating HUD Minimap (Phase 6)
        if (openWorld != null && mapController != null) {
            renderMinimap(player, openWorld, roadNetwork, navTracker, mapController, mvp, tmp);
        }

        // Compass arrow: Parkour extraction or Open World active waypoint
        if (worldMap != null) {
            float dx = worldMap.extractionX - player.x;
            float dz = worldMap.extractionZ - player.z;
            float worldAng = (float) Math.toDegrees(Math.atan2(dx, -dz));
            float screenAng = worldAng - player.yaw;
            drawArrow(mvp, tmp, screenWidth * 0.5f, screenHeight * 0.16f,
                    26f, screenAng, 1.0f, 0.85f, 0.1f, 0.85f);
        } else if (openWorld != null && navTracker != null && navTracker.hasActiveWaypoint) {
            float[] nextWp = new float[2];
            navTracker.getNextWaypoint(nextWp);
            float dx = nextWp[0] - player.x;
            float dz = nextWp[1] - player.z;
            float worldAng = (float) Math.toDegrees(Math.atan2(dx, -dz));
            float screenAng = worldAng - player.yaw;
            drawArrow(mvp, tmp, screenWidth * 0.5f, screenHeight * 0.16f,
                    26f, screenAng, 0.15f, 0.90f, 1.0f, 0.90f);
        }
    }

    private void renderMinimap(Player player, OpenWorld openWorld, RoadNetwork roadNetwork,
                               NavigationTracker navTracker, MapViewController mapController,
                               float[] mvp, float[] tmp) {
        float cx = mapController.minimapCenterX;
        float cy = mapController.minimapCenterY;
        float r = mapController.minimapRadius;

        // Minimap background disc
        drawCircle(mvp, tmp, cx, cy, r, 0.08f, 0.12f, 0.16f, 0.84f);
        // Outer cyan border ring
        drawCircle(mvp, tmp, cx, cy, r + 2.0f, 0.25f, 0.70f, 0.90f, 0.50f);
        drawCircle(mvp, tmp, cx, cy, r - 1.0f, 0.08f, 0.12f, 0.16f, 0.90f);

        // River corridor line if within range
        if (Math.abs(player.z - 240f) < 140f) {
            float[] r1 = new float[3];
            float[] r2 = new float[3];
            mapController.worldToMinimapLocal(-240f, 240f, player.x, player.z, player.yaw, r1);
            mapController.worldToMinimapLocal(240f, 240f, player.x, player.z, player.yaw, r2);
            draw2DLine(mvp, tmp, cx + r1[0], cy + r1[1], cx + r2[0], cy + r2[1],
                    10f, 0.12f, 0.38f, 0.60f, 0.65f);
        }

        // Local road network segments
        if (roadNetwork != null) {
            float[] p1 = new float[3];
            float[] p2 = new float[3];
            List<RoadNetwork.Node> nodes = roadNetwork.getNodes();
            for (int i = 0; i < nodes.size(); i++) {
                RoadNetwork.Node n = nodes.get(i);
                for (int j = 0; j < n.edges.size(); j++) {
                    RoadNetwork.Edge e = n.edges.get(j);
                    if (e.target.id > n.id) {
                        boolean in1 = mapController.worldToMinimapLocal(n.x, n.z, player.x, player.z, player.yaw, p1);
                        boolean in2 = mapController.worldToMinimapLocal(e.target.x, e.target.z, player.x, player.z, player.yaw, p2);
                        if (in1 || in2 || (p1[2] < r * 1.3f && p2[2] < r * 1.3f)) {
                            draw2DLine(mvp, tmp, cx + p1[0], cy + p1[1], cx + p2[0], cy + p2[1],
                                    2.4f, 0.45f, 0.50f, 0.55f, 0.65f);
                        }
                    }
                }
            }
        }

        // Active navigation route on minimap
        if (navTracker != null && navTracker.hasActiveWaypoint && navTracker.currentRoute != null) {
            List<RoadNetwork.Node> rNodes = navTracker.currentRoute.nodes;
            float[] wp1 = new float[3];
            float[] wp2 = new float[3];

            // Player to next waypoint
            float[] nxt = new float[2];
            navTracker.getNextWaypoint(nxt);
            mapController.worldToMinimapLocal(nxt[0], nxt[1], player.x, player.z, player.yaw, wp2);
            draw2DLine(mvp, tmp, cx, cy, cx + wp2[0], cy + wp2[1], 3.6f, 0.15f, 0.90f, 1.0f, 0.95f);

            for (int i = navTracker.currentWaypointIndex; i < rNodes.size() - 1; i++) {
                RoadNetwork.Node na = rNodes.get(i);
                RoadNetwork.Node nb = rNodes.get(i + 1);
                mapController.worldToMinimapLocal(na.x, na.z, player.x, player.z, player.yaw, wp1);
                mapController.worldToMinimapLocal(nb.x, nb.z, player.x, player.z, player.yaw, wp2);
                draw2DLine(mvp, tmp, cx + wp1[0], cy + wp1[1], cx + wp2[0], cy + wp2[1],
                        3.6f, 0.15f, 0.90f, 1.0f, 0.95f);
            }

            // Target marker
            float[] targetPt = new float[3];
            if (mapController.worldToMinimapLocal(navTracker.targetX, navTracker.targetZ, player.x, player.z, player.yaw, targetPt)) {
                drawCircle(mvp, tmp, cx + targetPt[0], cy + targetPt[1], 5.0f, 1.0f, 0.85f, 0.15f, 0.95f);
            }
        }

        // POIs within range
        if (openWorld != null && openWorld.pois != null) {
            float[] poiPt = new float[3];
            for (OpenWorld.Poi p : openWorld.pois) {
                if (mapController.worldToMinimapLocal(p.x, p.z, player.x, player.z, player.yaw, poiPt)) {
                    float pr = 0.95f, pg = 0.85f, pb = 0.20f;
                    if (p.type == OpenWorld.TYPE_SHELTER) { pr = 0.20f; pg = 0.90f; pb = 0.35f; }
                    else if (p.type == OpenWorld.TYPE_SHOP) { pr = 1.0f; pg = 0.60f; pb = 0.15f; }
                    drawCircle(mvp, tmp, cx + poiPt[0], cy + poiPt[1], 4.2f, pr, pg, pb, 0.95f);
                }
            }
        }

        // North Cardinal indicator on rim
        float[] north = new float[2];
        mapController.getMinimapNorthPosition(player.yaw, north);
        drawCircle(mvp, tmp, north[0], north[1], 4.5f, 0.95f, 0.25f, 0.20f, 0.95f);

        // Player heading needle at center
        drawArrow(mvp, tmp, cx, cy, 18f, 0f, 1.0f, 1.0f, 1.0f, 0.95f);
    }

    private void renderFullscreenMap(Player player, TouchInput touch, OpenWorld openWorld,
                                     RoadNetwork roadNetwork, NavigationTracker navTracker,
                                     MapViewController mapController, float[] mvp, float[] tmp) {
        // Dimming backdrop
        draw2DQuad(mvp, tmp, 0f, 0f, screenWidth, screenHeight, 0.05f, 0.08f, 0.12f, 0.94f);

        // River corridor
        float rvy = mapController.worldToScreenY(210f);
        float rvh = (260f - 210f) * mapController.zoom;
        float rvx = mapController.worldToScreenX(MapViewController.MIN_WORLD_X);
        float rvw = MapViewController.WORLD_SIZE * mapController.zoom;
        draw2DQuad(mvp, tmp, rvx, rvy, rvw, rvh, 0.10f, 0.35f, 0.55f, 0.65f);

        // City Core region
        float ccx = mapController.worldToScreenX(32f);
        float ccy = mapController.worldToScreenY(-120f);
        float ccw = (280f - 32f) * mapController.zoom;
        float cch = (120f - (-120f)) * mapController.zoom;
        draw2DQuad(mvp, tmp, ccx, ccy, ccw, cch, 0.18f, 0.22f, 0.28f, 0.50f);

        // Central Plaza safe zone circle
        float plx = mapController.worldToScreenX(OpenWorld.PLAZA_X);
        float ply = mapController.worldToScreenY(OpenWorld.PLAZA_Z);
        drawCircle(mvp, tmp, plx, ply, OpenWorld.PLAZA_RADIUS * mapController.zoom, 0.15f, 0.55f, 0.25f, 0.35f);

        // River Bridge
        float brX = mapController.worldToScreenX(-40f);
        float brY = mapController.worldToScreenY(210f);
        float brW = 30f * mapController.zoom;
        float brH = 50f * mapController.zoom;
        draw2DQuad(mvp, tmp, brX, brY, brW, brH, 0.35f, 0.40f, 0.45f, 0.75f);

        // Entire Road Network
        if (roadNetwork != null) {
            List<RoadNetwork.Node> allNodes = roadNetwork.getNodes();
            for (int i = 0; i < allNodes.size(); i++) {
                RoadNetwork.Node n = allNodes.get(i);
                float nx = mapController.worldToScreenX(n.x);
                float ny = mapController.worldToScreenY(n.z);
                drawCircle(mvp, tmp, nx, ny, 3.2f, 0.40f, 0.45f, 0.50f, 0.85f);
                for (int j = 0; j < n.edges.size(); j++) {
                    RoadNetwork.Edge e = n.edges.get(j);
                    if (e.target.id > n.id) {
                        float ex = mapController.worldToScreenX(e.target.x);
                        float ey = mapController.worldToScreenY(e.target.z);
                        draw2DLine(mvp, tmp, nx, ny, ex, ey, 2.5f, 0.45f, 0.50f, 0.55f, 0.80f);
                    }
                }
            }
        }

        // Active Navigation Route
        if (navTracker != null && navTracker.hasActiveWaypoint && navTracker.currentRoute != null) {
            float psx = mapController.worldToScreenX(player.x);
            float psy = mapController.worldToScreenY(player.z);
            List<RoadNetwork.Node> rNodes = navTracker.currentRoute.nodes;
            float prevX = psx, prevY = psy;
            for (int i = navTracker.currentWaypointIndex; i < rNodes.size(); i++) {
                RoadNetwork.Node rn = rNodes.get(i);
                float nsx = mapController.worldToScreenX(rn.x);
                float nsy = mapController.worldToScreenY(rn.z);
                draw2DLine(mvp, tmp, prevX, prevY, nsx, nsy, 4.5f, 0.15f, 0.90f, 1.0f, 0.95f);
                drawCircle(mvp, tmp, nsx, nsy, 4.0f, 0.20f, 0.95f, 1.0f, 0.95f);
                prevX = nsx; prevY = nsy;
            }
            float tsx = mapController.worldToScreenX(navTracker.targetX);
            float tsy = mapController.worldToScreenY(navTracker.targetZ);
            draw2DLine(mvp, tmp, prevX, prevY, tsx, tsy, 4.5f, 0.15f, 0.90f, 1.0f, 0.95f);
            drawCircle(mvp, tmp, tsx, tsy, 8.5f, 1.0f, 0.85f, 0.15f, 0.95f);
            drawCircle(mvp, tmp, tsx, tsy, 4.5f, 0.15f, 0.90f, 1.0f, 0.95f);
        }

        // POI Markers
        if (openWorld != null && openWorld.pois != null) {
            for (OpenWorld.Poi p : openWorld.pois) {
                float sx = mapController.worldToScreenX(p.x);
                float sy = mapController.worldToScreenY(p.z);
                if (sx >= -25 && sx <= screenWidth + 25 && sy >= -25 && sy <= screenHeight + 25) {
                    float r = 0.95f, g = 0.85f, b = 0.20f;
                    if (p.type == OpenWorld.TYPE_SHELTER) { r = 0.20f; g = 0.90f; b = 0.35f; }
                    else if (p.type == OpenWorld.TYPE_SHOP) { r = 1.0f; g = 0.65f; b = 0.15f; }
                    drawCircle(mvp, tmp, sx, sy, 7.5f, r, g, b, 0.95f);
                    drawCircle(mvp, tmp, sx, sy, 3.5f, 0.08f, 0.12f, 0.16f, 0.95f);
                }
            }
        }

        // Player marker
        float px = mapController.worldToScreenX(player.x);
        float py = mapController.worldToScreenY(player.z);
        drawCircle(mvp, tmp, px, py, 7.5f, 1.0f, 1.0f, 1.0f, 0.95f);
        drawArrow(mvp, tmp, px, py, 16f, -player.yaw, 0.20f, 0.85f, 1.0f, 0.95f);

        // Header and Footer bars
        draw2DQuad(mvp, tmp, 0f, 0f, screenWidth, 42f, 0.08f, 0.12f, 0.16f, 0.88f);
        draw2DQuad(mvp, tmp, 0f, screenHeight - 34f, screenWidth, 34f, 0.08f, 0.12f, 0.16f, 0.88f);

        // Control buttons
        drawCircle(mvp, tmp, mapController.btnCloseX, mapController.btnCloseY,
                mapController.btnCloseRadius, 0.85f, 0.25f, 0.20f, 0.90f);
        drawCircle(mvp, tmp, mapController.btnCenterX, mapController.btnCenterY,
                mapController.btnCenterRadius, 0.20f, 0.70f, 0.90f, 0.90f);
        drawCircle(mvp, tmp, mapController.btnCenterX, mapController.btnCenterY,
                mapController.btnCenterRadius * 0.45f, 1.0f, 1.0f, 1.0f, 0.95f);
        drawCircle(mvp, tmp, mapController.btnClearX, mapController.btnClearY,
                mapController.btnClearRadius, 0.75f, 0.65f, 0.25f, 0.90f);
        drawCircle(mvp, tmp, mapController.btnZoomInX, mapController.btnZoomInY,
                mapController.btnZoomInRadius, 0.25f, 0.75f, 0.85f, 0.90f);
        drawCircle(mvp, tmp, mapController.btnZoomOutX, mapController.btnZoomOutY,
                mapController.btnZoomOutRadius, 0.25f, 0.75f, 0.85f, 0.90f);
    }

    private void drawBar(float[] mvp, float[] tmp, float x, float y, float w, float h,
                         float ratio, float r, float g, float b) {
        // Frame
        draw2DQuad(mvp, tmp, x - 2, y - 2, w + 4, h + 4, 0.08f, 0.08f, 0.10f, 0.75f);
        float fillW = Math.max(0f, Math.min(w, w * ratio));
        if (fillW > 0) {
            draw2DQuad(mvp, tmp, x, y, fillW, h, r, g, b, 0.92f);
        }
    }

    private void draw2DLine(float[] mvp, float[] tmp, float x1, float y1, float x2, float y2,
                            float thickness, float r, float g, float b, float a) {
        if (a <= 0f) return;
        float dx = x2 - x1;
        float dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 0.5f) return;
        float angleDeg = (float) Math.toDegrees(Math.atan2(dy, dx));
        float[] model = Mat4.translate(x1, y1, 0);
        Mat4.mul(model, Mat4.rotZ(angleDeg));
        Mat4.mul(model, Mat4.translate(0, -thickness * 0.5f, 0));
        Mat4.mul(model, Mat4.scale(len, thickness, 1));
        Matrix.multiplyMM(mvp, 0, orthoMatrix, 0, model, 0);
        GLES20.glUniformMatrix4fv(hLocMVP, 1, false, mvp, 0);
        GLES20.glUniform4f(hLocColor, r, g, b, a);
        GLES20.glEnableVertexAttribArray(hLocPos);
        GLES20.glVertexAttribPointer(hLocPos, 2, GLES20.GL_FLOAT, false, 0, quad2DVbo);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 4);
        GLES20.glDisableVertexAttribArray(hLocPos);
    }

    private void draw2DQuad(float[] mvp, float[] tmp, float x, float y, float w, float h,
                            float r, float g, float b, float a) {
        if (a <= 0f) return;
        float[] model = Mat4.translate(x, y, 0);
        Mat4.mul(model, Mat4.scale(w, h, 1));
        Matrix.multiplyMM(mvp, 0, orthoMatrix, 0, model, 0);
        GLES20.glUniformMatrix4fv(hLocMVP, 1, false, mvp, 0);
        GLES20.glUniform4f(hLocColor, r, g, b, a);
        GLES20.glEnableVertexAttribArray(hLocPos);
        GLES20.glVertexAttribPointer(hLocPos, 2, GLES20.GL_FLOAT, false, 0, quad2DVbo);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 4);
        GLES20.glDisableVertexAttribArray(hLocPos);
    }

    private void drawCircle(float[] mvp, float[] tmp, float cx, float cy, float radius,
                            float r, float g, float b, float a) {
        if (a <= 0f) return;
        float[] model = Mat4.translate(cx, cy, 0);
        Mat4.mul(model, Mat4.scale(radius, radius, 1));
        Matrix.multiplyMM(mvp, 0, orthoMatrix, 0, model, 0);
        GLES20.glUniformMatrix4fv(hLocMVP, 1, false, mvp, 0);
        GLES20.glUniform4f(hLocColor, r, g, b, a);
        GLES20.glEnableVertexAttribArray(hLocPos);
        GLES20.glVertexAttribPointer(hLocPos, 2, GLES20.GL_FLOAT, false, 0, fanVbo);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 42);
        GLES20.glDisableVertexAttribArray(hLocPos);
    }

    private void drawArrow(float[] mvp, float[] tmp, float cx, float cy, float size,
                           float angleDeg, float r, float g, float b, float a) {
        float[] model = Mat4.translate(cx, cy, 0);
        Mat4.mul(model, Mat4.rotZ(angleDeg));
        Mat4.mul(model, Mat4.scale(size, size, 1));
        Matrix.multiplyMM(mvp, 0, orthoMatrix, 0, model, 0);
        GLES20.glUniformMatrix4fv(hLocMVP, 1, false, mvp, 0);
        GLES20.glUniform4f(hLocColor, r, g, b, a);
        GLES20.glEnableVertexAttribArray(hLocPos);
        GLES20.glVertexAttribPointer(hLocPos, 2, GLES20.GL_FLOAT, false, 0, triVbo);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 3);
        GLES20.glDisableVertexAttribArray(hLocPos);
    }
}
