package com.devagent.realworldsurvival3d;

import android.content.Context;
import android.content.SharedPreferences;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;
import java.util.ArrayList;
import java.util.List;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * GameView - Main loop & event controller for REALWORLD SURVIVAL 3D (Parkour Edition).
 * Coordinates:
 *   - OpenGL ES 3D Renderer (WorldRenderer)
 *   - Multi-Touch Controls (TouchInput)
 *   - Parkour Character Physics (Player)
 *   - High-Altitude World Layout (WorldMap)
 *   - Rooftop Chaser Stalkers (HunterEnemy)
 *   - Survival Item Pickups (Item)
 *   - Low-Latency Audio Engine (GameAudioManager)
 */
public class GameView extends GLSurfaceView implements GLSurfaceView.Renderer {

    public enum GameState { MENU, PLAYING, PAUSED, GAMEOVER, MISSION_COMPLETE }
    private GameState currentState = GameState.PLAYING;

    // Core Systems
    private Player player;
    private WorldMap worldMap;
    private List<HunterEnemy> chasers;
    private List<Item> items;
    private WorldRenderer worldRenderer;
    private TouchInput touchInput;
    private GameAudioManager audioManager;
    private SharedPreferences prefs;

    // Timing
    private long lastFrameTime;
    private float deltaTime;

    // Missions
    public int currentMission = 1;
    public int itemsCollected = 0;
    public int requiredItems = 5;
    public float missionTimer = 0f;
    public String missionTitle = "Rooftop Infiltration";
    public String missionGoal = "Wall-run across the chasm to the extraction tower!";

    public GameView(Context context) {
        super(context);
        setRenderer(this);
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        prefs = context.getSharedPreferences("RWSurvivalParkour", Context.MODE_PRIVATE);

        // Initialize Systems
        player = new Player();
        worldMap = new WorldMap();
        chasers = new ArrayList<>();
        items = new ArrayList<>();
        worldRenderer = new WorldRenderer();
        touchInput = new TouchInput();
        audioManager = new GameAudioManager(context);

        spawnMissions(currentMission);

        lastFrameTime = System.currentTimeMillis();
    }

    public void spawnMissions(int missionIndex) {
        currentMission = missionIndex;
        itemsCollected = 0;
        missionTimer = 0f;

        // Reset player on starting roof (Tower Alpha)
        player.x = 0f;
        player.y = 22f;
        player.z = 0f;
        player.vx = 0f;
        player.vy = 0f;
        player.vz = 0f;
        player.health = 100f;
        player.stamina = 100f;
        player.adrenaline = 0f;
        player.state = Player.ParkourState.STAND;

        // Spawn chasers on different rooftops
        chasers.clear();
        chasers.add(new HunterEnemy(34f, 25f, 0f));    // Tower Beta
        chasers.add(new HunterEnemy(35f, 30f, 38f));   // Tower Gamma
        chasers.add(new HunterEnemy(-36f, 22f, 0f));   // Tower Epsilon
        if (missionIndex >= 2) {
            chasers.add(new HunterEnemy(0f, 28f, 42f)); // Mid-Way Relay
            chasers.add(new HunterEnemy(65f, 32f, 60f)); // Apex Tower
        }

        // Spawn items along parkour lines
        items.clear();
        // Energy drinks on wall-run approaches
        items.add(new Item(Item.Type.ENERGY_DRINK, 10f, 21f, 10f));
        items.add(new Item(Item.Type.ENERGY_DRINK, 34f, 26f, -10f));
        items.add(new Item(Item.Type.ENERGY_DRINK, -10f, 21f, -10f));
        // Medkits
        items.add(new Item(Item.Type.MEDKIT, 35f, 31f, 35f));
        items.add(new Item(Item.Type.MEDKIT, -36f, 23f, -5f));
        // Parkour Tokens across balance beams
        items.add(new Item(Item.Type.PARKOUR_TOKEN, 50f, 31.5f, 49f)); // On steel beam!
        items.add(new Item(Item.Type.PARKOUR_TOKEN, -2f, 19.5f, -18f)); // On Delta beam!
        items.add(new Item(Item.Type.PARKOUR_TOKEN, 0f, 23f, 21f));   // On Relay beam!
        // Data Drives
        items.add(new Item(Item.Type.DATA_DRIVE, 38f, 27f, -42f));
        items.add(new Item(Item.Type.DATA_DRIVE, -70f, 25f, 10f));

        switch (missionIndex) {
            case 1:
                missionTitle = "Mission 1: Rooftop Infiltration";
                missionGoal = "Wall-run across the chasm and reach the Apex Tower extraction helipad!";
                requiredItems = 0;
                break;
            case 2:
                missionTitle = "Mission 2: Scavenger's High-Rise";
                missionGoal = "Collect 6 survival items across high-altitude balance beams!";
                requiredItems = 6;
                break;
            case 3:
                missionTitle = "Mission 3: Apex Extraction";
                missionGoal = "Evade all rooftop stalkers and activate the gold helipad beacon!";
                requiredItems = 3;
                break;
        }

        currentState = GameState.PLAYING;
        audioManager.playMusic("audio/music/bgm_parkour_rush.wav");
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        touchInput.handleTouchEvent(event);
        return true;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        gl.glClearColor(0.12f, 0.18f, 0.32f, 1.0f); // Dark atmospheric dusk sky
        gl.glEnable(GL10.GL_DEPTH_TEST);
        gl.glDepthFunc(GL10.GL_LEQUAL);
        worldRenderer.init();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        gl.glViewport(0, 0, width, height);
        worldRenderer.updateViewport(width, height);
        touchInput.updateScreenSize(width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        long now = System.currentTimeMillis();
        deltaTime = (now - lastFrameTime) / 1000.0f;
        lastFrameTime = now;
        if (deltaTime > 0.1f) deltaTime = 0.1f;

        if (currentState == GameState.PLAYING) {
            updateGame(deltaTime);
        }

        // Render 3D World & HUD
        worldRenderer.render(gl, player, worldMap, chasers, items, touchInput, currentState);
    }

    private void updateGame(float dt) {
        missionTimer += dt;

        // Apply camera swipe rotation
        player.yaw += touchInput.deltaYaw;
        player.pitch = Math.max(-80f, Math.min(80f, player.pitch + touchInput.deltaPitch));

        // Camera view toggle (1st person / 3rd person)
        if (touchInput.camTogglePressed) {
            player.firstPersonView = !player.firstPersonView;
        }

        // Update Player Parkour Kinematics
        player.update(dt, touchInput.moveX, touchInput.moveY, touchInput.jumpPressed,
                touchInput.slidePressed, touchInput.sprintToggled, worldMap.obstacles);

        // Sound Effects Triggers
        if (player.playJumpSfx) {
            audioManager.playSfx(GameAudioManager.SFX_JUMP);
            player.playJumpSfx = false;
        }
        if (player.playDoubleJumpSfx) {
            audioManager.playSfx(GameAudioManager.SFX_DOUBLE_JUMP);
            player.playDoubleJumpSfx = false;
        }
        if (player.playWallRunSfx) {
            audioManager.playSfx(GameAudioManager.SFX_WALLRUN);
            player.playWallRunSfx = false;
        }
        if (player.playVaultSfx) {
            audioManager.playSfx(GameAudioManager.SFX_VAULT);
            player.playVaultSfx = false;
        }
        if (player.playLedgeGrabSfx) {
            audioManager.playSfx(GameAudioManager.SFX_LEDGE_GRAB);
            player.playLedgeGrabSfx = false;
        }
        if (player.playMantleSfx) {
            audioManager.playSfx(GameAudioManager.SFX_MANTLE);
            player.playMantleSfx = false;
        }
        if (player.playSlideSfx) {
            audioManager.playSfx(GameAudioManager.SFX_SLIDE);
            player.playSlideSfx = false;
        }
        if (player.playRollSfx) {
            audioManager.playSfx(GameAudioManager.SFX_ROLL);
            player.playRollSfx = false;
        }
        if (player.playHardLandSfx) {
            audioManager.playSfx(GameAudioManager.SFX_HARD_LAND);
            player.playHardLandSfx = false;
        }
        if (player.playFootstepSfx) {
            audioManager.playSfx(GameAudioManager.SFX_FOOTSTEP);
            player.playFootstepSfx = false;
        }

        // Breathing & Heartbeat Audio loops
        if (player.stamina < 20f && player.isGrounded) {
            audioManager.startHeavyBreathing();
        } else {
            audioManager.stopHeavyBreathing();
        }

        if (player.health < 30f) {
            audioManager.startHeartbeat();
        } else {
            audioManager.stopHeartbeat();
        }

        // Update Chasers
        boolean anyChasing = false;
        for (HunterEnemy chaser : chasers) {
            chaser.update(dt, player, worldMap.obstacles);
            if (chaser.alertSoundPlayed) {
                audioManager.playSfx(GameAudioManager.SFX_CHASER_ROAR);
                chaser.alertSoundPlayed = false;
            }
            if (chaser.state == HunterEnemy.State.CHASING) {
                anyChasing = true;
            }
        }

        // Update Items & Check Collection
        for (Item item : items) {
            if (!item.collected) {
                item.update(dt);
                float dx = item.x - player.x;
                float dy = item.y - player.y;
                float dz = item.z - player.z;
                float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
                if (dist < 1.8f) {
                    item.onCollected(player);
                    itemsCollected++;
                    switch (item.type) {
                        case ENERGY_DRINK:
                            audioManager.playSfx(GameAudioManager.SFX_PICKUP_DRINK);
                            break;
                        case MEDKIT:
                            audioManager.playSfx(GameAudioManager.SFX_PICKUP_MEDKIT);
                            break;
                        default:
                            audioManager.playSfx(GameAudioManager.SFX_PICKUP_TOKEN);
                            break;
                    }
                }
            }
        }

        // Check Mission Completion at Extraction Helipad
        float edx = worldMap.extractionX - player.x;
        float edz = worldMap.extractionZ - player.z;
        float distToExtraction = (float) Math.sqrt(edx * edx + edz * edz);

        if (distToExtraction < 5.0f && Math.abs(worldMap.extractionY - player.y) < 3.0f) {
            if (itemsCollected >= requiredItems) {
                // Victory!
                currentState = GameState.MISSION_COMPLETE;
                audioManager.playSfx(GameAudioManager.SFX_LEVEL_WIN);
                player.score += 500;
            }
        }

        // Check Game Over
        if (player.state == Player.ParkourState.DEAD || player.y < 0f) {
            currentState = GameState.GAMEOVER;
            audioManager.playSfx(GameAudioManager.SFX_GAME_OVER);
        }

        // Reset per-frame button presses
        touchInput.resetFrameButtons();
    }

    public void onResume() {
        super.onResume();
        if (audioManager != null) {
            audioManager.resume();
        }
    }

    public void onPause() {
        super.onPause();
        if (audioManager != null) {
            audioManager.pause();
        }
    }

    public void destroy() {
        if (audioManager != null) {
            audioManager.release();
        }
    }

    public boolean onBackPressed() {
        if (currentState == GameState.PLAYING) {
            currentState = GameState.PAUSED;
            if (audioManager != null) audioManager.pause();
            return true;
        } else if (currentState == GameState.PAUSED) {
            currentState = GameState.PLAYING;
            if (audioManager != null) audioManager.resume();
            return true;
        } else if (currentState == GameState.GAMEOVER || currentState == GameState.MISSION_COMPLETE) {
            spawnMissions(currentMission == 3 ? 1 : currentMission + 1);
            return true;
        }
        return false;
    }
}
