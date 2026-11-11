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

    public void setQuality(int q) {
        quality = q;
        if (q == QUALITY_PERFORMANCE) {
            fogDensity = 0.0075f;   // heavier fog hides the shorter draw distance
            farPlane = 230f;
            shadowsEnabled = false;
        } else {
            fogDensity = 0.0042f;
            farPlane = 420f;
            shadowsEnabled = true;
        }
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
    private FloatBuffer cubeVbo, quadVbo, fanVbo, triVbo, clipQuadVbo;
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

    // ------------------------------------------------------------------
    // Camera state (smoothed)
    // ------------------------------------------------------------------
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
    // Frame render
    // ==================================================================
    public void render(GL10 gl, Player player, WorldMap worldMap, List<HunterEnemy> chasers,
                       List<Item> items, TouchInput touch, GameView.GameState state,
                       float beaconPulse) {
        if (!glReady) {
            init();
        }

        GLES20.glClearColor(FOG_COLOR[0], FOG_COLOR[1], FOG_COLOR[2], 1f);
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
        setupCamera(player);
        Matrix.multiplyMM(vpMatrix, 0, projMatrix, 0, viewMatrix, 0);

        // ---- Sky ----
        drawSky();

        // ---- World pass ----
        useWorldProgram();
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

        if (worldMap != null && worldMap.obstacles != null) {
            float camFX = camTX - camX, camFY = camTY - camY, camFZ = camTZ - camZ;
            for (Obstacle o : worldMap.obstacles) {
                // Cheap behind-camera cull
                float dx = o.x - camX, dy = o.y - camY, dz = o.z - camZ;
                float radius = (o.width + o.height + o.depth) * 0.5f;
                if (dx * camFX + dy * camFY + dz * camFZ < -radius) continue;
                float distSq = dx * dx + dy * dy + dz * dz;
                float cull = (farPlane + radius * 2f);
                if (distSq > cull * cull) continue;
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

        // Items
        if (items != null) {
            for (Item item : items) {
                if (item.collected) continue;
                drawItem(item);
            }
        }

        // Characters
        if (chasers != null) {
            for (HunterEnemy ch : chasers) {
                if (ch.state != HunterEnemy.State.DEFEATED) {
                    float face = (ch.vx != 0f || ch.vz != 0f)
                            ? (float) Math.toDegrees(Math.atan2(ch.vx, -ch.vz)) : 0f;
                    drawCharacter(ch.x, ch.y, ch.z, face, 0.8f, 1.7f, 0.8f, TEX_CHASER,
                            0.85f, 0.3f, 0.3f);
                    // glowing visor
                    drawCube(ch.x, ch.y + 1.35f, ch.z, face, 0.34f, 0.12f, 0.2f, TEX_NEON,
                            1f, 0.15f, 0.05f, 1f, 1f, 1f, 0.5f, 1f);
                }
            }
        }

        if (!player.firstPersonView && player.state != Player.ParkourState.DEAD) {
            float w = (player.state == Player.ParkourState.SLIDING
                    || player.state == Player.ParkourState.SAFETY_ROLL) ? 1.1f : 0.75f;
            float h = player.currentHeight;
            float tintR = player.adrenalineRush ? 1.0f : 0.25f;
            float tintG = player.adrenalineRush ? 0.85f : 0.7f;
            float tintB = player.adrenalineRush ? 0.15f : 1.0f;
            drawCharacter(player.x, player.y, player.z, -player.yaw, w, h, w, TEX_RUNNER,
                    tintR, tintG, tintB);
        }

        // Blob shadows
        if (shadowsEnabled) {
            GLES20.glEnable(GLES20.GL_BLEND);
            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
            GLES20.glDepthMask(false);
            if (player.state != Player.ParkourState.DEAD) {
                drawShadow(worldMap, player.x, player.z, player.y, 0.55f, 0.38f);
            }
            if (chasers != null) {
                for (HunterEnemy ch : chasers) {
                    if (ch.state != HunterEnemy.State.DEFEATED) {
                        drawShadow(worldMap, ch.x, ch.z, ch.y, 0.55f, 0.30f);
                    }
                }
            }
            GLES20.glDepthMask(true);
            GLES20.glDisable(GLES20.GL_BLEND);
        }

        // ---- HUD ----
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glDisable(GLES20.GL_CULL_FACE);
        renderHUD(gl, player, touch, state, worldMap);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_CULL_FACE);
    }

    private void setupCamera(Player player) {
        float radYaw = (float) Math.toRadians(player.yaw);
        float radPitch = (float) Math.toRadians(player.pitch);
        float fwdX = (float) (Math.sin(radYaw) * Math.cos(radPitch));
        float fwdY = (float) -Math.sin(radPitch);
        float fwdZ = (float) (-Math.cos(radYaw) * Math.cos(radPitch));

        if (player.state == Player.ParkourState.HARD_LAND) {
            screenShake = 0.4f;
        } else if (screenShake > 0f) {
            screenShake *= 0.85f;
        }
        float shakeX = (float) (Math.random() * 2 - 1) * screenShake;
        float shakeY = (float) (Math.random() * 2 - 1) * screenShake;

        if (player.firstPersonView) {
            camX = player.x + shakeX;
            camY = player.y + player.currentHeight * 0.85f + shakeY;
            camZ = player.z;
            camTX = camX + fwdX;
            camTY = camY + fwdY;
            camTZ = camZ + fwdZ;
        } else {
            float dist = 4.8f;
            float tX = player.x - fwdX * dist + shakeX;
            float tY = player.y + 2.4f - fwdY * 1.5f + shakeY;
            float tZ = player.z - fwdZ * dist;
            camX += (tX - camX) * 0.35f;
            camY += (tY - camY) * 0.35f;
            camZ += (tZ - camZ) * 0.35f;
            camTX = player.x;
            camTY = player.y + player.currentHeight * 0.7f;
            camTZ = player.z;
        }

        float rollRad = (float) Math.toRadians(player.cameraRoll);
        camUpX = (float) Math.sin(rollRad);
        camUpY = (float) Math.cos(rollRad);
        camUpZ = 0f;

        Matrix.setLookAtM(viewMatrix, 0, camX, camY, camZ, camTX, camTY, camTZ,
                camUpX, camUpY, camUpZ);
    }

    // ==================================================================
    // World drawing helpers
    // ==================================================================
    private void useWorldProgram() {
        GLES20.glUseProgram(worldProgram);
        GLES20.glUniform3f(wLocSunDir, SUN_DIR[0], SUN_DIR[1], SUN_DIR[2]);
        GLES20.glUniform3f(wLocLightColor, LIGHT_COLOR[0], LIGHT_COLOR[1], LIGHT_COLOR[2]);
        GLES20.glUniform3f(wLocAmbSky, AMB_SKY[0], AMB_SKY[1], AMB_SKY[2]);
        GLES20.glUniform3f(wLocAmbGround, AMB_GROUND[0], AMB_GROUND[1], AMB_GROUND[2]);
        GLES20.glUniform3f(wLocCamPos, camX, camY, camZ);
        GLES20.glUniform3f(wLocFogColor, FOG_COLOR[0], FOG_COLOR[1], FOG_COLOR[2]);
        GLES20.glUniform1f(wLocFogDensity, fogDensity);
        GLES20.glUniform1i(wLocTex, 0);
    }

    private void drawObstacle(Obstacle o) {
        int tex;
        float r = o.r, g = o.g, b = o.b;
        float uvx, uvy, spec;
        switch (o.type) {
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

    private void drawItem(Item item) {
        float r, g, b;
        switch (item.type) {
            case ENERGY_DRINK: r = 0.1f; g = 0.9f; b = 0.95f; break;
            case MEDKIT:       r = 0.95f; g = 0.15f; b = 0.2f; break;
            case DATA_DRIVE:   r = 0.85f; g = 0.3f; b = 0.95f; break;
            default:           r = 1.0f; g = 0.85f; b = 0.1f; break;
        }
        float pulse = 0.75f + 0.25f * (float) Math.sin(item.rotation * 2.0);
        drawCube(item.x, item.y + item.bobOffset + 0.6f, item.z, item.rotation,
                0.6f, 0.6f, 0.6f, TEX_NEON, r, g, b, 1f, 1f, 1f, 1.0f, pulse);
    }

    private void drawCharacter(float x, float y, float z, float yawDeg,
                               float w, float h, float d,
                               int tex, float tr, float tg, float tb) {
        drawCube(x, y + h * 0.5f, z, yawDeg, w, h, d, tex, tr, tg, tb,
                1f, 1f, 1f, 0.3f, 1f);
    }

    private void drawCube(float x, float y, float z, float rotDeg,
                          float sx, float sy, float sz, int tex,
                          float r, float g, float b, float tintA,
                          float uvx, float uvy, float spec, float alpha) {
        float[] model = Mat4.trs(x, y, z, rotDeg, sx, sy, sz);
        drawCubeModel(model, tex, r, g, b, tintA, uvx, uvy, spec, alpha);
    }

    private void drawCubeModel(float[] model, int tex, float r, float g, float b, float a,
                               float uvx, float uvy, float spec, float alpha) {
        float[] mvp = new float[16];
        Matrix.multiplyMM(mvp, 0, vpMatrix, 0, model, 0);
        GLES20.glUniformMatrix4fv(wLocMVP, 1, false, mvp, 0);
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
        GLES20.glUniform3f(sLocZenith, SKY_ZENITH[0], SKY_ZENITH[1], SKY_ZENITH[2]);
        GLES20.glUniform3f(sLocHorizon, SKY_HORIZON[0], SKY_HORIZON[1], SKY_HORIZON[2]);
        GLES20.glEnableVertexAttribArray(sLocPos);
        GLES20.glVertexAttribPointer(sLocPos, 2, GLES20.GL_FLOAT, false, 0, clipQuadVbo);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 4);
        GLES20.glDisableVertexAttribArray(sLocPos);
        GLES20.glDepthMask(true);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
    }

    private void drawShadow(WorldMap worldMap, float x, float z, float entityY,
                            float size, float alpha) {
        float floorY = worldMap.getFloorY(x, z, entityY);
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
    // HUD
    // ==================================================================
    private void renderHUD(GL10 gl, Player player, TouchInput touch, GameView.GameState state,
                           WorldMap worldMap) {
        GLES20.glUseProgram(hudProgram);
        float[] mvp = new float[16];
        float[] tmp = new float[16];

        // Health / stamina / adrenaline bars
        drawBar(mvp, tmp, 25f, 25f, 240f, 18f, player.health / player.maxHealth,
                0.2f, 0.85f, 0.25f);
        drawBar(mvp, tmp, 25f, 50f, 240f, 14f, player.stamina / player.maxStamina,
                0.1f, 0.75f, 0.95f);
        drawBar(mvp, tmp, 25f, 70f, 240f, 10f, player.adrenaline / player.maxAdrenaline,
                1.0f, 0.75f, 0.1f);

        if (touch != null) {
            drawCircle(mvp, tmp, touch.joystickBaseX, touch.joystickBaseY,
                    touch.joystickRadius, 0.25f, 0.25f, 0.3f, 0.40f);
            drawCircle(mvp, tmp, touch.joystickCurrentX, touch.joystickCurrentY,
                    touch.joystickRadius * 0.42f, 0.2f, 0.85f, 0.95f,
                    touch.isJoystickActive ? 0.85f : 0.5f);

            drawCircle(mvp, tmp, touch.btnJumpX, touch.btnJumpY, touch.btnJumpRadius,
                    0.10f, 0.75f, 0.95f, 0.65f);
            drawCircle(mvp, tmp, touch.btnVaultX, touch.btnVaultY, touch.btnVaultRadius,
                    0.95f, 0.85f, 0.10f, 0.65f);
            drawCircle(mvp, tmp, touch.btnSlideX, touch.btnSlideY, touch.btnSlideRadius,
                    0.95f, 0.45f, 0.15f, 0.65f);
            drawCircle(mvp, tmp, touch.btnSprintX, touch.btnSprintY, touch.btnSprintRadius,
                    player.isSprinting ? 0.95f : 0.4f, 0.3f,
                    player.isSprinting ? 0.2f : 0.8f, 0.75f);
            if (touch.btnCamRadius > 0) {
                drawCircle(mvp, tmp, touch.btnCamX, touch.btnCamY, touch.btnCamRadius,
                        0.6f, 0.6f, 0.7f, 0.6f);
            }
            if (touch.btnInteractRadius > 0 && touch.interactVisible) {
                drawCircle(mvp, tmp, touch.btnInteractX, touch.btnInteractY,
                        touch.btnInteractRadius, 1.0f, 0.65f, 0.1f, 0.85f);
            }
            if (touch.btnPauseRadius > 0) {
                drawCircle(mvp, tmp, touch.btnPauseX, touch.btnPauseY, touch.btnPauseRadius,
                        0.9f, 0.9f, 0.95f, 0.55f);
            }
        }

        // Extraction compass arrow around the joystick (screen-space direction)
        if (worldMap != null) {
            float dx = worldMap.extractionX - player.x;
            float dz = worldMap.extractionZ - player.z;
            float worldAng = (float) Math.atan2(dx, -dz);          // world heading
            float screenAng = worldAng - (float) Math.toRadians(player.yaw);
            drawArrow(mvp, tmp, screenWidth * 0.5f, screenHeight * 0.16f,
                    26f, screenAng, 1.0f, 0.85f, 0.1f, 0.85f);
        }
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

    private void draw2DQuad(float[] mvp, float[] tmp, float x, float y, float w, float h,
                            float r, float g, float b, float a) {
        if (a <= 0f) return;
        float[] model = Mat4.translate(x, y, 0);
        Mat4.mul(model, Mat4.scale(w, h, 1));
        Matrix.multiplyMM(mvp, 0, orthoMatrix, 0, model, 0);
        GLES20.glUniformMatrix4fv(hLocMVP, 1, false, mvp, 0);
        GLES20.glUniform4f(hLocColor, r, g, b, a);
        GLES20.glEnableVertexAttribArray(hLocPos);
        GLES20.glVertexAttribPointer(hLocPos, 2, GLES20.GL_FLOAT, false, 0, quadVbo);
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
