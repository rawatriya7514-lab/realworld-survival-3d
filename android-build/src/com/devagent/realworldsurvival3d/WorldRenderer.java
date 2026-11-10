package com.devagent.realworldsurvival3d;

import android.opengl.GLU;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.List;
import javax.microedition.khronos.opengles.GL10;

/**
 * WorldRenderer - High-Performance 3D Parkour & Survival Renderer.
 * Features:
 *   - 3D Perspective rendering with dynamic FOV (speed zoom)
 *   - 1st-Person and 3rd-Person camera modes with Dutch-angle wall-run tilt
 *   - Impact screen shake on hard landing
 *   - 3D Skyscraper cityscape, wall-run billboards, balance beams, jump pads
 *   - Animated rotating pickups with bobbing
 *   - Rooftop chasers with alert indicators
 *   - 2D Orthographic HUD with analog joystick, action buttons, health/stamina bars,
 *     adrenaline combo gauges, and dynamic parkour notification banners
 */
public class WorldRenderer {

    private FloatBuffer unitCubeVertices;
    private FloatBuffer colorBuffer;
    private FloatBuffer hudQuadVertices;

    // Screen Dimensions
    public int screenWidth = 800;
    public int screenHeight = 600;

    // Camera smoothing
    private float camX = 0, camY = 25, camZ = 10;
    private float camTargetX = 0, camTargetY = 22, camTargetZ = 0;
    private float screenShake = 0f;

    public WorldRenderer() {
        initBuffers();
    }

    public void init() {
        // Ready for GL setup
    }

    private void initBuffers() {
        // Unit cube: -0.5 to 0.5 in X, Y, Z (8 vertices, drawn as 6 quads/triangles)
        float[] cubeVerts = {
            // Front face
            -0.5f, -0.5f,  0.5f,   0.5f, -0.5f,  0.5f,   0.5f,  0.5f,  0.5f,  -0.5f,  0.5f,  0.5f,
            // Back face
            -0.5f, -0.5f, -0.5f,  -0.5f,  0.5f, -0.5f,   0.5f,  0.5f, -0.5f,   0.5f, -0.5f, -0.5f,
            // Top face
            -0.5f,  0.5f, -0.5f,  -0.5f,  0.5f,  0.5f,   0.5f,  0.5f,  0.5f,   0.5f,  0.5f, -0.5f,
            // Bottom face
            -0.5f, -0.5f, -0.5f,   0.5f, -0.5f, -0.5f,   0.5f, -0.5f,  0.5f,  -0.5f, -0.5f,  0.5f,
            // Right face
             0.5f, -0.5f, -0.5f,   0.5f,  0.5f, -0.5f,   0.5f,  0.5f,  0.5f,   0.5f, -0.5f,  0.5f,
            // Left face
            -0.5f, -0.5f, -0.5f,  -0.5f, -0.5f,  0.5f,  -0.5f,  0.5f,  0.5f,  -0.5f,  0.5f, -0.5f,
        };

        ByteBuffer bb = ByteBuffer.allocateDirect(cubeVerts.length * 4);
        bb.order(ByteOrder.nativeOrder());
        unitCubeVertices = bb.asFloatBuffer();
        unitCubeVertices.put(cubeVerts);
        unitCubeVertices.position(0);

        ByteBuffer cb = ByteBuffer.allocateDirect(24 * 4 * 4);
        cb.order(ByteOrder.nativeOrder());
        colorBuffer = cb.asFloatBuffer();

        // HUD Quad (2D quad)
        float[] quadVerts = {
            0f, 0f, 0f,
            1f, 0f, 0f,
            1f, 1f, 0f,
            0f, 1f, 0f
        };
        ByteBuffer qb = ByteBuffer.allocateDirect(quadVerts.length * 4);
        qb.order(ByteOrder.nativeOrder());
        hudQuadVertices = qb.asFloatBuffer();
        hudQuadVertices.put(quadVerts);
        hudQuadVertices.position(0);
    }

    public void updateViewport(int w, int h) {
        this.screenWidth = w;
        this.screenHeight = h;
    }

    public void render(GL10 gl, Player player, WorldMap worldMap, List<HunterEnemy> chasers,
                       List<Item> items, TouchInput touch, GameView.GameState state) {
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);

        // Setup 3D Projection
        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glLoadIdentity();
        float aspect = (float) screenWidth / Math.max(1, screenHeight);

        // Dynamic FOV: Zooms out during sprint / wall run for speed thrill
        float fov = 65f;
        if (player.state == Player.ParkourState.SPRINT || player.state == Player.ParkourState.WALL_RUN_LEFT ||
            player.state == Player.ParkourState.WALL_RUN_RIGHT || player.adrenalineRush) {
            fov = 82f;
        }
        GLU.gluPerspective(gl, fov, aspect, 0.4f, 600f);

        // Setup 3D Modelview
        gl.glMatrixMode(GL10.GL_MODELVIEW);
        gl.glLoadIdentity();

        // Camera Positioning
        setupCamera(gl, player);

        // Render 3D World Obstacles (Skyscrapers, Billboards, Beams, Jump Pads)
        gl.glEnable(GL10.GL_DEPTH_TEST);
        gl.glEnable(GL10.GL_CULL_FACE);
        gl.glCullFace(GL10.GL_BACK);

        if (worldMap != null && worldMap.obstacles != null) {
            for (Obstacle o : worldMap.obstacles) {
                renderObstacle(gl, o);
            }
        }

        // Render Collectible Items
        if (items != null) {
            for (Item item : items) {
                if (!item.collected) {
                    renderItem(gl, item);
                }
            }
        }

        // Render Rooftop Chasers
        if (chasers != null) {
            for (HunterEnemy chaser : chasers) {
                if (chaser.state != HunterEnemy.State.DEFEATED) {
                    renderChaser(gl, chaser);
                }
            }
        }

        // Render Player (in 3rd person)
        if (!player.firstPersonView && player.state != Player.ParkourState.DEAD) {
            renderPlayer(gl, player);
        }

        // Render Extraction Beacon Beam
        if (worldMap != null) {
            renderExtractionBeacon(gl, worldMap.extractionX, worldMap.extractionY, worldMap.extractionZ);
        }

        // Render 2D UI Overlay (Orthographic)
        renderHUD(gl, player, touch, state, worldMap);
    }

    private void setupCamera(GL10 gl, Player player) {
        float radYaw = (float) Math.toRadians(player.yaw);
        float radPitch = (float) Math.toRadians(player.pitch);

        float forwardX = (float) (Math.sin(radYaw) * Math.cos(radPitch));
        float forwardY = (float) -Math.sin(radPitch);
        float forwardZ = (float) (-Math.cos(radYaw) * Math.cos(radPitch));

        // Screen shake decay
        if (player.state == Player.ParkourState.HARD_LAND) {
            screenShake = 0.4f;
        } else if (screenShake > 0f) {
            screenShake *= 0.85f;
        }
        float shakeX = (float) (Math.random() * 2 - 1) * screenShake;
        float shakeY = (float) (Math.random() * 2 - 1) * screenShake;

        if (player.firstPersonView) {
            // First person: Camera at player eye level
            float eyeY = player.y + player.currentHeight * 0.85f;
            camX = player.x + shakeX;
            camY = eyeY + shakeY;
            camZ = player.z;

            camTargetX = camX + forwardX;
            camTargetY = camY + forwardY;
            camTargetZ = camZ + forwardZ;
        } else {
            // Third person chase camera: 4.5m behind, 2.5m above
            float dist = 4.8f;
            float targetCamX = player.x - forwardX * dist + shakeX;
            float targetCamY = player.y + 2.4f - forwardY * 1.5f + shakeY;
            float targetCamZ = player.z - forwardZ * dist;

            // Camera smoothing
            camX += (targetCamX - camX) * 0.35f;
            camY += (targetCamY - camY) * 0.35f;
            camZ += (targetCamZ - camZ) * 0.35f;

            camTargetX = player.x;
            camTargetY = player.y + player.currentHeight * 0.7f;
            camTargetZ = player.z;
        }

        // Camera up vector with Dutch-angle roll on wall runs
        float rollRad = (float) Math.toRadians(player.cameraRoll);
        float upX = (float) Math.sin(rollRad);
        float upY = (float) Math.cos(rollRad);
        float upZ = 0f;

        GLU.gluLookAt(gl, camX, camY, camZ,
                camTargetX, camTargetY, camTargetZ,
                upX, upY, upZ);
    }

    private void renderObstacle(GL10 gl, Obstacle o) {
        gl.glPushMatrix();
        gl.glTranslatef(o.x, o.y, o.z);
        gl.glScalef(o.width, o.height, o.depth);
        drawColorBox(gl, o.r, o.g, o.b, 1.0f);
        gl.glPopMatrix();
    }

    private void renderItem(GL10 gl, Item item) {
        gl.glPushMatrix();
        gl.glTranslatef(item.x, item.y + item.bobOffset + 0.6f, item.z);
        gl.glRotatef(item.rotation, 0f, 1f, 0f);
        gl.glScalef(0.6f, 0.6f, 0.6f);

        switch (item.type) {
            case ENERGY_DRINK:
                drawColorBox(gl, 0.1f, 0.9f, 0.95f, 1.0f); // Cyan drink
                break;
            case MEDKIT:
                drawColorBox(gl, 0.95f, 0.15f, 0.2f, 1.0f); // Red medkit
                break;
            case DATA_DRIVE:
                drawColorBox(gl, 0.85f, 0.3f, 0.95f, 1.0f); // Purple data drive
                break;
            case PARKOUR_TOKEN:
                drawColorBox(gl, 1.0f, 0.85f, 0.1f, 1.0f); // Gold token
                break;
        }
        gl.glPopMatrix();
    }

    private void renderChaser(GL10 gl, HunterEnemy chaser) {
        gl.glPushMatrix();
        gl.glTranslatef(chaser.x, chaser.y + 0.9f, chaser.z);
        // Body (Dark red predator suit)
        gl.glScalef(0.8f, 1.7f, 0.8f);
        drawColorBox(gl, 0.75f, 0.15f, 0.15f, 1.0f);

        // Glowing red visor
        gl.glScalef(0.5f, 0.2f, 0.2f);
        gl.glTranslatef(0f, 0.35f, 0.5f);
        drawColorBox(gl, 1.0f, 0.1f, 0.0f, 1.0f);
        gl.glPopMatrix();
    }

    private void renderPlayer(GL10 gl, Player player) {
        gl.glPushMatrix();
        gl.glTranslatef(player.x, player.y + player.currentHeight * 0.5f, player.z);
        gl.glRotatef(-player.yaw, 0f, 1f, 0f);

        // Body dimensions based on standing vs slide/roll
        float h = player.currentHeight;
        float w = (player.state == Player.ParkourState.SLIDING || player.state == Player.ParkourState.SAFETY_ROLL) ? 1.1f : 0.75f;

        gl.glScalef(w, h, w);
        // Runner suit: Sporty dark athletic suit with cyan adrenaline glow
        if (player.adrenalineRush) {
            drawColorBox(gl, 1.0f, 0.85f, 0.15f, 1.0f); // Golden adrenaline rush!
        } else {
            drawColorBox(gl, 0.15f, 0.65f, 0.95f, 1.0f); // Cyan parkour runner
        }
        gl.glPopMatrix();
    }

    private void renderExtractionBeacon(GL10 gl, float ex, float ey, float ez) {
        gl.glPushMatrix();
        gl.glTranslatef(ex, ey + 15f, ez);
        gl.glScalef(0.8f, 30f, 0.8f);
        drawColorBox(gl, 1.0f, 0.9f, 0.2f, 0.7f); // Rising gold beacon light
        gl.glPopMatrix();
    }

    private void drawColorBox(GL10 gl, float r, float g, float b, float a) {
        colorBuffer.clear();
        for (int i = 0; i < 24; i++) {
            // Apply slight ambient lighting variation per face
            float tint = (i < 4) ? 1.0f : ((i < 8) ? 0.75f : ((i < 12) ? 1.15f : 0.85f));
            colorBuffer.put(Math.min(1.0f, r * tint));
            colorBuffer.put(Math.min(1.0f, g * tint));
            colorBuffer.put(Math.min(1.0f, b * tint));
            colorBuffer.put(a);
        }
        colorBuffer.position(0);

        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GL10.GL_COLOR_ARRAY);

        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, unitCubeVertices);
        gl.glColorPointer(4, GL10.GL_FLOAT, 0, colorBuffer);

        for (int f = 0; f < 6; f++) {
            gl.glDrawArrays(GL10.GL_TRIANGLE_FAN, f * 4, 4);
        }

        gl.glDisableClientState(GL10.GL_COLOR_ARRAY);
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
    }

    private void renderHUD(GL10 gl, Player player, TouchInput touch, GameView.GameState state, WorldMap worldMap) {
        gl.glDisable(GL10.GL_DEPTH_TEST);
        gl.glDisable(GL10.GL_CULL_FACE);

        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glPushMatrix();
        gl.glLoadIdentity();
        gl.glOrthof(0, screenWidth, screenHeight, 0, -1, 1);

        gl.glMatrixMode(GL10.GL_MODELVIEW);
        gl.glPushMatrix();
        gl.glLoadIdentity();

        // 1. Health Bar (Top Left)
        drawHudBar(gl, 25f, 25f, 240f, 18f, player.health / player.maxHealth, 0.2f, 0.85f, 0.25f);

        // 2. Stamina Bar (Below Health Bar)
        drawHudBar(gl, 25f, 50f, 240f, 14f, player.stamina / player.maxStamina, 0.1f, 0.75f, 0.95f);

        // 3. Adrenaline Bar (Below Stamina Bar)
        drawHudBar(gl, 25f, 70f, 240f, 10f, player.adrenaline / player.maxAdrenaline, 1.0f, 0.75f, 0.1f);

        // 4. Virtual Joystick (Bottom Left)
        if (touch != null) {
            // Base ring
            drawHudCircle(gl, touch.joystickBaseX, touch.joystickBaseY, touch.joystickRadius, 0.3f, 0.3f, 0.35f, 0.45f);
            // Thumb knob
            drawHudCircle(gl, touch.joystickCurrentX, touch.joystickCurrentY, touch.joystickRadius * 0.42f,
                    0.2f, 0.85f, 0.95f, touch.isJoystickActive ? 0.85f : 0.5f);

            // 5. Action Buttons (Bottom Right)
            // JUMP Button
            drawHudCircle(gl, touch.btnJumpX, touch.btnJumpY, touch.btnJumpRadius, 0.1f, 0.75f, 0.95f, 0.65f);
            // VAULT Button
            drawHudCircle(gl, touch.btnVaultX, touch.btnVaultY, touch.btnVaultRadius, 0.95f, 0.85f, 0.1f, 0.65f);
            // SLIDE/ROLL Button
            drawHudCircle(gl, touch.btnSlideX, touch.btnSlideY, touch.btnSlideRadius, 0.95f, 0.45f, 0.15f, 0.65f);
            // SPRINT Toggle Button
            drawHudCircle(gl, touch.btnSprintX, touch.btnSprintY, touch.btnSprintRadius,
                    player.isSprinting ? 0.95f : 0.4f, 0.3f, player.isSprinting ? 0.2f : 0.8f, 0.75f);
            // CAM View Toggle Button
            drawHudCircle(gl, touch.btnCamX, touch.btnCamY, touch.btnCamRadius, 0.6f, 0.6f, 0.7f, 0.6f);
        }

        // 6. Parkour Action Banner (Center screen dynamic alert)
        if (player.actionNotifyTimer > 0f) {
            float bannerAlpha = Math.min(1.0f, player.actionNotifyTimer * 1.5f);
            drawHudBar(gl, screenWidth * 0.5f - 180f, 110f, 360f, 38f, 1.0f, 0.95f, 0.85f, 0.1f);
        }

        gl.glPopMatrix();
        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glPopMatrix();
        gl.glMatrixMode(GL10.GL_MODELVIEW);
    }

    private void drawHudBar(GL10 gl, float x, float y, float w, float h, float ratio, float r, float g, float b) {
        // Background dark frame
        gl.glPushMatrix();
        gl.glTranslatef(x - 2, y - 2, 0);
        gl.glScalef(w + 4, h + 4, 1);
        draw2DQuad(gl, 0.1f, 0.1f, 0.12f, 0.75f);
        gl.glPopMatrix();

        // Filled foreground
        float fillW = Math.max(0f, Math.min(w, w * ratio));
        if (fillW > 0) {
            gl.glPushMatrix();
            gl.glTranslatef(x, y, 0);
            gl.glScalef(fillW, h, 1);
            draw2DQuad(gl, r, g, b, 0.9f);
            gl.glPopMatrix();
        }
    }

    private void drawHudCircle(GL10 gl, float cx, float cy, float radius, float r, float g, float b, float a) {
        int segments = 16;
        float[] verts = new float[(segments + 2) * 3];
        verts[0] = cx; verts[1] = cy; verts[2] = 0;
        for (int i = 0; i <= segments; i++) {
            double angle = (2.0 * Math.PI * i) / segments;
            verts[(i + 1) * 3] = cx + (float) (Math.cos(angle) * radius);
            verts[(i + 1) * 3 + 1] = cy + (float) (Math.sin(angle) * radius);
            verts[(i + 1) * 3 + 2] = 0;
        }

        ByteBuffer vb = ByteBuffer.allocateDirect(verts.length * 4);
        vb.order(ByteOrder.nativeOrder());
        FloatBuffer vBuf = vb.asFloatBuffer();
        vBuf.put(verts);
        vBuf.position(0);

        gl.glColor4f(r, g, b, a);
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vBuf);
        gl.glDrawArrays(GL10.GL_TRIANGLE_FAN, 0, segments + 2);
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
    }

    private void draw2DQuad(GL10 gl, float r, float g, float b, float a) {
        gl.glColor4f(r, g, b, a);
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, hudQuadVertices);
        gl.glDrawArrays(GL10.GL_TRIANGLE_FAN, 0, 4);
        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
    }
}
