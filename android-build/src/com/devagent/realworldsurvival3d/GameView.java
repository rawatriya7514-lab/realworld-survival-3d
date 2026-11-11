package com.devagent.realworldsurvival3d;

import android.content.Context;
import android.content.SharedPreferences;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import java.util.ArrayList;
import java.util.List;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * GameView - Main loop & event controller for REALWORLD SURVIVAL 3D.
 * Coordinates:
 *   - GLES2 Renderer (WorldRenderer)
 *   - Multi-Touch Controls (TouchInput)
 *   - Parkour Character Physics (Player)
 *   - High-Altitude World Layout (WorldMap)
 *   - Rooftop Chaser Stalkers (HunterEnemy)
 *   - Survival Item Pickups (Item)
 *   - Low-Latency Audio Engine (GameAudioManager)
 *   - Menus / HUD (UIController) and persistence (SaveManager, GameSettings)
 *
 * Gameplay loop: menu -> mission briefing -> explore & collect -> evade or
 * stomp chasers -> reach (and on mission 3, signal) the extraction helipad ->
 * mission complete -> next mission / save.
 */
public class GameView extends GLSurfaceView implements GLSurfaceView.Renderer,
        UIController.Callbacks {

    public enum GameState { MENU, PLAYING, PAUSED, GAMEOVER, MISSION_COMPLETE }
    private GameState currentState = GameState.MENU;

    /** Game selected from the main menu; drives update/render routing. */
    public enum GameMode { PARKOUR, OPEN_WORLD }
    private GameMode activeMode = GameMode.PARKOUR;

    // Core Systems
    private Player player;
    private WorldMap worldMap;
    private OpenWorld openWorld;   // non-null only in OPEN_WORLD mode
    private Terrain terrain;       // non-null only in OPEN_WORLD mode
    private List<HunterEnemy> chasers;
    private List<Item> items;
    private WorldRenderer worldRenderer;
    private TouchInput touchInput;
    private GameAudioManager audioManager;
    private SaveManager saveManager;
    private GameSettings settings;
    private UIController ui;
    private Vibrator vibrator;
    private Context context;

    // Timing
    private long lastFrameTime;
    private float deltaTime;
    private long frameBudgetNs = 0;

    // Missions
    public int currentMission = 1;
    public int itemsCollected = 0;
    public int requiredItems = 0;
    public float missionTimer = 0f;
    public String missionTitle = "";
    public String missionGoal = "";
    private boolean beaconActivated = false;
    private boolean finalMissionCompleted = false;
    private int completedMissions = 0;

    // Interaction state
    private Item interactTargetItem = null;
    private boolean interactTargetConsole = false;
    private String interactPrompt = null;
    private String toastMessage = null;
    private float toastTimer = 0f;

    // Player feedback bookkeeping
    private float lastPlayerHealth = 100f;
    private float regenDelay = 0f;
    private float beaconPulse = 0f;

    // Music per mission
    private static final String MUSIC_MENU = "audio/music/bgm_survival_ambient.wav";
    private static final String MUSIC_M1 = "audio/music/bgm_parkour_rush.wav";
    private static final String MUSIC_M2 = "audio/music/bgm_apex_tower.wav";
    private static final String MUSIC_M3 = "audio/music/bgm_chase.wav";

    // Settings change flag from UI thread
    private volatile boolean settingsDirty = false;

    public GameView(Context context, GameSettings settings) {
        super(context);
        this.context = context;
        this.settings = settings;
        setEGLContextClientVersion(2);
        setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        setPreserveEGLContextOnPause(true);
        setRenderer(this);
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        saveManager = new SaveManager(context);
        audioManager = new GameAudioManager(context);
        applyAudioSettings();
        applyFrameBudget();

        player = new Player();
        worldMap = new WorldMap();
        chasers = new ArrayList<>();
        items = new ArrayList<>();

        completedMissions = saveManager.loadInt("completed_missions", 0);
        currentMission = saveManager.loadInt("current_mission", 1);
        if (currentMission < 1 || currentMission > 3) currentMission = 1;
    }

    /** Called by GameActivity after UIController exists. */
    public void attachUi(UIController ui) {
        this.ui = ui;
        showMenuState();
    }

    private void applyAudioSettings() {
        audioManager.setMusicVolume(GameSettings.toVolume(settings.musicVolume));
        audioManager.setSfxVolume(GameSettings.toVolume(settings.sfxVolume));
    }

    private void applyFrameBudget() {
        frameBudgetNs = settings.fpsLimit == GameSettings.FPS_30
                ? 33_000_000L : 0L;
    }

    // =================================================================
    // Missions
    // =================================================================
    public void spawnMissions(int missionIndex) {
        currentMission = missionIndex;
        activeMode = GameMode.PARKOUR;
        openWorld = null;         // release open-world memory
        terrain = null;
        player.terrain = null;    // restore classic parkour collision
        itemsCollected = 0;
        missionTimer = 0f;
        beaconActivated = false;
        interactPrompt = null;
        toastMessage = null;
        toastTimer = 0f;

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
        player.score = missionIndex == 1 ? 0 : player.score;
        player.currency = missionIndex == 1 ? 0 : player.currency;
        player.state = Player.ParkourState.STAND;
        player.isGrounded = true;
        player.firstPersonView = false;
        player.currentHeight = player.standingHeight;
        lastPlayerHealth = player.health;

        // Spawn chasers on different rooftops
        chasers.clear();
        chasers.add(new HunterEnemy(34f, 25f, 0f));    // Tower Beta
        chasers.add(new HunterEnemy(35f, 30f, 38f));   // Tower Gamma
        chasers.add(new HunterEnemy(-36f, 22f, 0f));   // Tower Epsilon
        if (missionIndex >= 2) {
            chasers.add(new HunterEnemy(0f, 28f, 42f));  // Mid-Way Relay
            chasers.add(new HunterEnemy(65f, 32f, 60f)); // Apex Tower
        }

        // Spawn items along parkour lines
        items.clear();
        switch (missionIndex) {
            case 1:
                items.add(new Item(Item.Type.ENERGY_DRINK, 10f, 21f, 10f));
                items.add(new Item(Item.Type.ENERGY_DRINK, 34f, 26f, -10f));
                items.add(new Item(Item.Type.MEDKIT, -36f, 23f, -5f));
                items.add(new Item(Item.Type.PARKOUR_TOKEN, 50f, 31.5f, 49f));
                items.add(new Item(Item.Type.PARKOUR_TOKEN, -2f, 19.5f, -18f));
                items.add(new Item(Item.Type.PARKOUR_TOKEN, 0f, 23f, 21f));
                requiredItems = 0;
                break;
            case 2:
                items.add(new Item(Item.Type.ENERGY_DRINK, 10f, 21f, 10f));
                items.add(new Item(Item.Type.MEDKIT, 35f, 31f, 35f));
                items.add(new Item(Item.Type.PARKOUR_TOKEN, 50f, 31.5f, 49f));
                items.add(new Item(Item.Type.PARKOUR_TOKEN, -2f, 19.5f, -18f));
                items.add(new Item(Item.Type.PARKOUR_TOKEN, 0f, 23f, 21f));
                items.add(new Item(Item.Type.ENERGY_DRINK, -36f, 23f, -5f));
                items.add(new Item(Item.Type.MEDKIT, -76f, 25f, 8f));
                items.add(new Item(Item.Type.ENERGY_DRINK, 43f, 27f, -42f));
                requiredItems = 6;
                break;
            default:
                items.add(new Item(Item.Type.DATA_DRIVE, 34f, 26f, -10f));
                items.add(new Item(Item.Type.DATA_DRIVE, -2f, 19.5f, -18f));
                items.add(new Item(Item.Type.DATA_DRIVE, -76f, 25f, 8f));
                items.add(new Item(Item.Type.ENERGY_DRINK, 10f, 21f, 10f));
                items.add(new Item(Item.Type.MEDKIT, 35f, 31f, 35f));
                requiredItems = 3;
                break;
        }

        switch (missionIndex) {
            case 1:
                missionTitle = "Mission 1: Rooftop Infiltration";
                missionGoal = "Parkour east across the towers and reach the gold helipad beacon.";
                break;
            case 2:
                missionTitle = "Mission 2: Scavenger's High-Rise";
                missionGoal = "Collect 6 survival items, then reach the Apex helipad.";
                break;
            case 3:
                missionTitle = "Mission 3: Apex Extraction";
                missionGoal = "Recover 3 data drives, then use the console at the helipad.";
                break;
        }

        currentState = GameState.PLAYING;
        audioManager.playMusic(missionMusic(missionIndex));
    }

    private String missionMusic(int mission) {
        switch (mission) {
            case 2: return MUSIC_M2;
            case 3: return MUSIC_M3;
            default: return MUSIC_M1;
        }
    }

    // =================================================================
    // Open world mode
    // =================================================================
    /** Starts a free-roam session in the open world (GL thread). */
    public void startOpenWorld() {
        activeMode = GameMode.OPEN_WORLD;
        openWorld = new OpenWorld();
        terrain = new Terrain();

        itemsCollected = 0;
        requiredItems = 0;
        missionTimer = 0f;
        beaconActivated = false;
        interactPrompt = null;
        toastMessage = null;
        toastTimer = 0f;

        // Spawn on the street just south of the Safehouse, facing the city
        float ground = Terrain.heightAt(210f, 44f);
        player.x = 210f;
        player.y = ground + 0.1f;
        player.z = 44f;
        player.vx = 0f;
        player.vy = 0f;
        player.vz = 0f;
        player.yaw = -90f;    // look west toward Market Street
        player.pitch = 0f;
        player.health = 100f;
        player.stamina = 100f;
        player.adrenaline = 0f;
        player.score = 0;
        player.currency = 0;
        player.state = Player.ParkourState.STAND;
        player.isGrounded = true;
        player.firstPersonView = false;
        player.currentHeight = player.standingHeight;
        player.peakFallY = player.y;
        player.canDoubleJump = true;
        player.terrain = terrain;
        player.waterLevel = Terrain.WATER_LEVEL;
        lastPlayerHealth = player.health;

        chasers.clear();
        items.clear();

        missionTitle = "Free Roam: The Long Signal";
        missionGoal = "Explore the city and countryside. Landmarks get discovered.";

        currentState = GameState.PLAYING;
        openWorld.recomputeActive(player.x, player.z);
        audioManager.playMusic(MUSIC_MENU);
    }

    private void updateOpenWorld(float dt) {
        openWorld.recomputeActive(player.x, player.z);
        player.update(dt, touchInput.moveX, touchInput.moveY, touchInput.jumpPressed,
                touchInput.slidePressed, touchInput.sprintToggled, openWorld.activeObstacles());

        // Invisible border: keep the player inside the 896 m world
        if (player.x > 442f) { player.x = 442f; player.vx = 0f; }
        if (player.x < -442f) { player.x = -442f; player.vx = 0f; }
        if (player.z > 442f) { player.z = 442f; player.vz = 0f; }
        if (player.z < -442f) { player.z = -442f; player.vz = 0f; }

        processHealthFeedback(dt);
        processPlayerSfx();

        // Landmark discovery feedback
        openWorld.discoverNear(player.x, player.z);
        if (openWorld.lastDiscovered != null) {
            toastMessage = "Discovered: " + openWorld.lastDiscovered;
            toastTimer = 2.5f;
            openWorld.lastDiscovered = null;
            vibrate(30);
        }

        // Fatal-drop safety net (deep water is blocked, so this rarely fires)
        if (player.state == Player.ParkourState.DEAD || player.y < -9f) {
            gameOver();
            return;
        }

        if (ui != null) {
            ui.setHudMission(missionTitle, missionGoal);
            ui.setHudStats("Region: " + OpenWorld.regionAt(player.x, player.z).display
                    + "   Score " + player.score);
            String prompt = toastTimer > 0f ? toastMessage : null;
            if (prompt == null && player.actionNotifyTimer > 0f) {
                prompt = player.lastParkourAction;
            }
            ui.setHudPrompt(prompt);
        }
        touchInput.resetFrameButtons();
    }

    /** Shared by both modes: damage flash, regen, breath/heartbeat audio. */
    private void processHealthFeedback(float dt) {
        if (player.health < lastPlayerHealth) {
            if (ui != null) ui.flashDamage();
            vibrate(35);
            regenDelay = 6f;
        }
        lastPlayerHealth = player.health;
        if (player.health > 0 && player.health < player.maxHealth) {
            if (regenDelay > 0f) regenDelay -= dt;
            else player.health = Math.min(player.maxHealth, player.health + 6f * dt);
        }

        // Stamina / health breathing & heartbeat loops
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
    }

    /** Shared by both modes: consumes the Player SFX trigger flags. */
    private void processPlayerSfx() {
        if (player.playJumpSfx) { audioManager.playSfx(GameAudioManager.SFX_JUMP); player.playJumpSfx = false; }
        if (player.playDoubleJumpSfx) { audioManager.playSfx(GameAudioManager.SFX_DOUBLE_JUMP); player.playDoubleJumpSfx = false; }
        if (player.playWallRunSfx) { audioManager.playSfx(GameAudioManager.SFX_WALLRUN); player.playWallRunSfx = false; }
        if (player.playVaultSfx) { audioManager.playSfx(GameAudioManager.SFX_VAULT); player.playVaultSfx = false; }
        if (player.playLedgeGrabSfx) { audioManager.playSfx(GameAudioManager.SFX_LEDGE_GRAB); player.playLedgeGrabSfx = false; }
        if (player.playMantleSfx) { audioManager.playSfx(GameAudioManager.SFX_MANTLE); player.playMantleSfx = false; }
        if (player.playSlideSfx) { audioManager.playSfx(GameAudioManager.SFX_SLIDE); player.playSlideSfx = false; }
        if (player.playRollSfx) { audioManager.playSfx(GameAudioManager.SFX_ROLL); player.playRollSfx = false; }
        if (player.playHardLandSfx) { audioManager.playSfx(GameAudioManager.SFX_HARD_LAND); player.playHardLandSfx = false; }
        if (player.playFootstepSfx) { audioManager.playSfx(GameAudioManager.SFX_FOOTSTEP); player.playFootstepSfx = false; }
    }

    // =================================================================
    // GL lifecycle
    // =================================================================
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        worldRenderer = new WorldRenderer(context);
        worldRenderer.setQuality(settings.graphicsQuality);
        worldRenderer.init();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        if (worldRenderer != null) {
            worldRenderer.updateViewport(width, height);
        }
        if (touchInput == null) {
            touchInput = new TouchInput();
            touchInput.sensitivityScale = settings.lookScale();
            touchInput.invertLookY = settings.invertLookY;
        } else {
            touchInput.updateScreenSize(width, height);
        }
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        long now = System.nanoTime();
        float dt = (now - lastFrameTime) / 1_000_000_000f;
        lastFrameTime = now;
        if (dt > 0.1f) dt = 0.1f;
        if (dt <= 0f) dt = 0.016f;
        deltaTime = dt;

        if (settingsDirty) {
            settingsDirty = false;
            applyAudioSettings();
            applyFrameBudget();
            if (worldRenderer != null) worldRenderer.setQuality(settings.graphicsQuality);
            if (touchInput != null) {
                touchInput.sensitivityScale = settings.lookScale();
                touchInput.invertLookY = settings.invertLookY;
            }
        }

        if (currentState == GameState.PLAYING) {
            updateGame(deltaTime);
        } else if (touchInput != null) {
            touchInput.resetFrameButtons();
            touchInput.interactVisible = false;
        }

        if (worldRenderer != null && touchInput != null) {
            if (activeMode == GameMode.OPEN_WORLD) {
                worldRenderer.renderOpenWorld(gl, player, openWorld, touchInput, currentState);
            } else {
                worldRenderer.render(gl, player, worldMap, chasers, items, touchInput,
                        currentState, beaconPulse);
            }
        }

        // Optional FPS limit (30): block the GL thread past the frame budget
        if (frameBudgetNs > 0) {
            long elapsed = System.nanoTime() - now;
            long remain = frameBudgetNs - elapsed;
            if (remain > 1_000_000L) {
                try {
                    Thread.sleep(remain / 1_000_000L, (int) (remain % 1_000_000L));
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    // =================================================================
    // Game update
    // =================================================================
    private void updateGame(float dt) {
        missionTimer += dt;
        beaconPulse += dt * 2.0f;
        if (beaconPulse > 1f) beaconPulse -= 1f;
        if (toastTimer > 0f) toastTimer -= dt;

        // Apply camera swipe rotation
        player.yaw += touchInput.deltaYaw;
        player.pitch = Math.max(-80f, Math.min(80f, player.pitch + touchInput.deltaPitch));

        if (touchInput.pausePressed) {
            pauseGame();
            return;
        }
        if (touchInput.camTogglePressed) {
            player.firstPersonView = !player.firstPersonView;
        }
        if (worldRenderer != null && Math.abs(touchInput.deltaZoom) > 0.0001f) {
            worldRenderer.cameraRig.adjustZoom(touchInput.deltaZoom);
        }

        // Open-world mode has its own loop (parkour flow continues below)
        if (activeMode == GameMode.OPEN_WORLD) {
            updateOpenWorld(dt);
            return;
        }

        // Player physics
        player.update(dt, touchInput.moveX, touchInput.moveY, touchInput.jumpPressed,
                touchInput.slidePressed, touchInput.sprintToggled, worldMap.obstacles);

        // Health feedback & survival audio (shared with open world)
        processHealthFeedback(dt);

        // SFX triggers
        processPlayerSfx();

        // Chasers
        for (HunterEnemy chaser : chasers) {
            chaser.update(dt, player, worldMap);
            if (chaser.alertSoundPlayed) {
                audioManager.playSfx(GameAudioManager.SFX_CHASER_ROAR);
                chaser.alertSoundPlayed = false;
            }
        }

        // Items: auto-collect or interact-collect (data drives)
        interactTargetItem = null;
        interactTargetConsole = false;
        for (Item item : items) {
            if (item.collected) continue;
            item.update(dt);
            float dx = item.x - player.x;
            float dy = item.y - player.y;
            float dz = item.z - player.z;
            float distSq = dx * dx + dy * dy + dz * dz;

            if (item.type == Item.Type.DATA_DRIVE) {
                if (distSq < 3.2f * 3.2f) {
                    if (interactTargetItem == null) interactTargetItem = item;
                }
            } else if (distSq < 1.8f * 1.8f) {
                item.onCollected(player);
                itemsCollected++;
                switch (item.type) {
                    case ENERGY_DRINK: audioManager.playSfx(GameAudioManager.SFX_PICKUP_DRINK); break;
                    case MEDKIT: audioManager.playSfx(GameAudioManager.SFX_PICKUP_MEDKIT); break;
                    default: audioManager.playSfx(GameAudioManager.SFX_PICKUP_TOKEN); break;
                }
            }
        }

        // Extraction console proximity (mission 3)
        boolean consoleNear = false;
        if (currentMission == 3) {
            float cdx = 61f - player.x;
            float cdz = 60f - player.z;
            if (cdx * cdx + cdz * cdz < 3.0f * 3.0f && Math.abs(32.4f - player.y) < 3f) {
                consoleNear = true;
                interactTargetConsole = true;
            }
        }

        // Interaction prompt
        if (interactTargetItem != null) {
            interactPrompt = "INTERACT to collect the data drive";
        } else if (interactTargetConsole) {
            if (itemsCollected >= requiredItems) {
                interactPrompt = beaconActivated ? null : "INTERACT to signal the helicopter";
            } else {
                interactPrompt = "Data drives: " + itemsCollected + "/" + requiredItems;
            }
        } else {
            interactPrompt = null;
        }
        touchInput.interactVisible = interactTargetItem != null
                || (interactTargetConsole && itemsCollected >= requiredItems && !beaconActivated);

        // Interact press
        if (touchInput.interactPressed) {
            if (interactTargetItem != null) {
                interactTargetItem.onCollected(player);
                itemsCollected++;
                audioManager.playSfx(GameAudioManager.SFX_PICKUP_TOKEN);
                interactTargetItem = null;
            } else if (interactTargetConsole) {
                if (itemsCollected >= requiredItems && !beaconActivated) {
                    beaconActivated = true;
                    audioManager.playSfx(GameAudioManager.SFX_LEVEL_WIN);
                    toastMessage = "EXTRACTION SIGNAL SENT - HOLD THE HELIPAD";
                    toastTimer = 3.5f;
                    vibrate(60);
                } else if (itemsCollected < requiredItems) {
                    toastMessage = "Need " + (requiredItems - itemsCollected) + " more data drives";
                    toastTimer = 2.0f;
                }
            }
        }

        // Extraction / completion rules
        float edx = worldMap.extractionX - player.x;
        float edz = worldMap.extractionZ - player.z;
        float distToExtraction = (float) Math.sqrt(edx * edx + edz * edz);
        boolean onHelipad = distToExtraction < 5.0f
                && Math.abs(worldMap.extractionY - player.y) < 3.0f;

        boolean extractionReady;
        if (currentMission == 3) {
            extractionReady = onHelipad && beaconActivated;
        } else {
            extractionReady = onHelipad && itemsCollected >= requiredItems;
        }
        if (extractionReady) {
            completeMission();
            return;
        }

        // Death
        if (player.state == Player.ParkourState.DEAD || player.y < -2f) {
            gameOver();
            return;
        }

        // HUD updates (dirty-checked)
        if (ui != null) {
            ui.setHudMission(missionTitle, missionGoal);
            String stats = (requiredItems > 0
                    ? "Items " + itemsCollected + "/" + requiredItems + "   " : "")
                    + "Score " + player.score;
            ui.setHudStats(stats);
            String prompt = toastTimer > 0f ? toastMessage : interactPrompt;
            if (prompt == null && player.actionNotifyTimer > 0f) {
                prompt = player.lastParkourAction;
            }
            ui.setHudPrompt(prompt);
        }

        touchInput.resetFrameButtons();
    }

    // =================================================================
    // State transitions
    // =================================================================
    private void completeMission() {
        currentState = GameState.MISSION_COMPLETE;
        audioManager.playSfx(GameAudioManager.SFX_LEVEL_WIN);
        audioManager.stopHeavyBreathing();
        audioManager.stopHeartbeat();
        player.score += 500;
        if (currentMission > completedMissions) completedMissions = currentMission;
        if (player.score > saveManager.loadInt("best_score", 0)) {
            saveManager.saveInt("best_score", player.score);
        }
        saveProgress();

        if (ui != null) {
            if (currentMission >= 3) {
                finalMissionCompleted = true;
                ui.setMissionCompleteInfo(
                        "Extraction complete! You survived the lockdown.",
                        "Final score  " + player.score,
                        "Play Again");
            } else {
                ui.setMissionCompleteInfo(
                        missionTitle + " objectives met",
                        "Score  " + player.score + "   Best  "
                                + saveManager.loadInt("best_score", 0),
                        "Next Mission");
            }
            ui.showMenu("mission");
        }
    }

    private void gameOver() {
        currentState = GameState.GAMEOVER;
        audioManager.playSfx(GameAudioManager.SFX_GAME_OVER);
        audioManager.stopHeavyBreathing();
        audioManager.stopHeartbeat();
        if (player.score > saveManager.loadInt("best_score", 0)) {
            saveManager.saveInt("best_score", player.score);
        }
        vibrate(120);
        if (ui != null) {
            ui.setGameOverInfo(
                    "Score  " + player.score + "   Items  " + itemsCollected,
                    missionTitle);
            ui.showMenu("gameover");
        }
    }

    public void pauseGame() {
        if (currentState != GameState.PLAYING) return;
        currentState = GameState.PAUSED;
        audioManager.pause();
        if (activeMode == GameMode.PARKOUR) {
            saveProgress(); // open-world persistence arrives with the save phase
        }
        if (ui != null) {
            ui.showMenu("pause");
            if (ui != null) ui.notifySaved();
        }
    }

    public void resumeGame() {
        if (currentState != GameState.PAUSED) return;
        currentState = GameState.PLAYING;
        audioManager.resume();
        lastFrameTime = System.nanoTime();
        if (ui != null) ui.showMenu(null);
    }

    private void showMenuState() {
        currentState = GameState.MENU;
        audioManager.stopMusic();
        audioManager.playMusic(MUSIC_MENU);
        if (ui != null) ui.showMainMenu();
    }

    /** Save the current run for Continue. */
    private void saveProgress() {
        saveManager.saveInt("current_mission", currentMission);
        saveManager.saveInt("completed_missions", completedMissions);
        saveManager.saveInt("score", player.score);
        saveManager.saveInt("currency", player.currency);
        saveManager.saveInt("items_collected", itemsCollected);
        saveManager.saveFloat("player_health", Math.max(1f, player.health));
        saveManager.saveString("mission_title", missionTitle);
        saveManager.saveString("mission_goal", missionGoal);
        saveManager.saveInt("best_score", saveManager.loadInt("best_score", 0));
    }

    // =================================================================
    // UIController callbacks (called on the UI thread)
    // =================================================================
    @Override public void onStartParkour() {
        queue(new Runnable() { public void run() { spawnMissions(currentMission); } });
    }

    @Override public void onStartOpenWorld() {
        queue(new Runnable() { public void run() { startOpenWorld(); } });
    }

    @Override public void onRetryActiveMode() {
        queue(new Runnable() {
            public void run() {
                if (activeMode == GameMode.OPEN_WORLD) startOpenWorld();
                else spawnMissions(currentMission);
            }
        });
    }

    @Override public void onContinueGame() {
        queue(new Runnable() {
            public void run() {
                player.score = saveManager.loadInt("score", 0);
                player.currency = saveManager.loadInt("currency", 0);
                completedMissions = saveManager.loadInt("completed_missions", 0);
                spawnMissions(saveManager.loadInt("current_mission", 1));
            }
        });
    }

    @Override public void onResumeRequested() {
        queue(new Runnable() { public void run() { resumeGame(); } });
    }

    @Override public void onRestartMission() {
        queue(new Runnable() { public void run() { spawnMissions(currentMission); } });
    }

    @Override public void onQuitToMenu() {
        queue(new Runnable() {
            public void run() {
                saveProgress();
                showMenuState();
            }
        });
    }

    @Override public void onExitRequested() {
        // Exit is a UI-thread operation
        if (context instanceof android.app.Activity) {
            ((android.app.Activity) context).runOnUiThread(new Runnable() {
                public void run() {
                    saveProgress();
                    audioManager.release();
                    ((android.app.Activity) context).finishAffinity();
                }
            });
        }
    }

    @Override public void onSettingsChanged() {
        settingsDirty = true;
    }

    @Override public boolean hasSaveGame() {
        return saveManager.hasSave() || saveManager.loadInt("current_mission", -1) > 0;
    }

    @Override public int bestScore() {
        return saveManager.loadInt("best_score", 0);
    }

    /** Run a runnable on the GL thread (queueEvent) for state changes. */
    private void queue(Runnable r) {
        queueEvent(r);
    }

    // =================================================================
    // Helpers
    // =================================================================
    private void vibrate(int ms) {
        if (vibrator == null) return;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(ms,
                        VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(ms);
            }
        } catch (Exception ignored) {}
    }

    // =================================================================
    // Activity lifecycle passthroughs
    // =================================================================
    public void activityPaused() {
        audioManager.pause();
        if (currentState == GameState.PLAYING && activeMode == GameMode.PARKOUR) {
            saveProgress();
        }
    }

    public void activityResumed() {
        lastFrameTime = System.nanoTime();
        if (currentState == GameState.PLAYING) {
            audioManager.resume();
        } else if (currentState == GameState.MENU && audioManager.musicAvailable()) {
            audioManager.resume();
        }
    }

    public void destroy() {
        audioManager.release();
    }

    /**
     * Back button: pause/resume during play; menu navigation elsewhere.
     * Returns true when handled.
     */
    public boolean onBackPressed() {
        switch (currentState) {
            case PLAYING:
                pauseGame();
                return true;
            case PAUSED:
                resumeGame();
                if (ui != null) ui.showMenu(null);
                return true;
            case MENU:
                return false; // let the activity exit
            case GAMEOVER:
            case MISSION_COMPLETE:
                return true; // require explicit button choice
            default:
                return false;
        }
    }

    // Mission title used by HUD when a fresh spawn resets things
    public String currentMissionTitle() {
        return missionTitle;
    }
}
