package com.devagent.realworldsurvival3d;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * UIController - All menus, screens and text HUD elements for
 * REALWORLD SURVIVAL 3D, built as Android view overlays on top of the
 * GLES surface. View-based menus give crisp readable typography on every
 * screen density; the GL layer keeps rendering the 3D world behind them.
 *
 * Menu layers consume touches when visible; HUD text elements are
 * non-clickable so gameplay touches pass through to the GL view.
 */
public class UIController {

    public interface Callbacks {
        void onStartNewGame();
        void onContinueGame();
        void onResumeRequested();
        void onRestartMission();
        void onQuitToMenu();
        void onExitRequested();
        void onSettingsChanged();
        boolean hasSaveGame();
        int bestScore();
    }

    private final Activity activity;
    private final Callbacks callbacks;
    public final GameSettings settings;

    public final FrameLayout overlayRoot;   // everything UI
    private final FrameLayout hudLayer;     // non-blocking text HUD
    private final FrameLayout menuLayer;    // blocking menus

    // Menus
    private LinearLayout mainMenu, settingsMenu, pauseMenu, gameOverMenu,
            missionCompleteMenu, aboutMenu;
    private TextView continueBtn;

    // Settings widgets
    private TextView sensValue, musicValue, sfxValue, qualityValue, fpsValue, invertValue;

    // HUD elements
    private TextView hudMissionTitle, hudMissionGoal, hudStats, hudPrompt, hudBanner, hudSaved;
    private View damageFlash;
    private long hudDirtyMask = 0;
    private String lastMission = "", lastGoal = "", lastStats = "", lastPrompt = "", lastBanner = "";

    // Palette
    private static final int COLOR_ACCENT = 0xFF35D6F5;
    private static final int COLOR_TEXT = 0xFFF2F5FA;
    private static final int COLOR_DIM = 0xFF9AA7B8;
    private static final int PANEL_BG = 0xE610141E;
    private static final int BTN_BG = 0xCC1A2233;
    private static final int BTN_BG_PRESSED = 0xFF2A3A55;

    public UIController(Activity activity, Callbacks callbacks, GameSettings settings) {
        this.activity = activity;
        this.callbacks = callbacks;
        this.settings = settings;

        overlayRoot = new FrameLayout(activity);
        overlayRoot.setLayoutParams(matchParent());

        hudLayer = new FrameLayout(activity);
        hudLayer.setLayoutParams(matchParent());
        buildHud(hudLayer);
        overlayRoot.addView(hudLayer);

        menuLayer = new FrameLayout(activity);
        menuLayer.setLayoutParams(matchParent());
        mainMenu = buildMainMenu();
        settingsMenu = buildSettingsMenu();
        pauseMenu = buildPauseMenu();
        gameOverMenu = buildGameOverMenu();
        missionCompleteMenu = buildMissionCompleteMenu();
        aboutMenu = buildAboutMenu();
        menuLayer.addView(mainMenu);
        menuLayer.addView(settingsMenu);
        menuLayer.addView(pauseMenu);
        menuLayer.addView(gameOverMenu);
        menuLayer.addView(missionCompleteMenu);
        menuLayer.addView(aboutMenu);
        overlayRoot.addView(menuLayer);

        showMenu(null); // start clean; GameView will show main menu
    }

    // =================================================================
    // Public API used by GameView / GameActivity
    // =================================================================
    /** All View mutations are posted to the UI thread for safety. */
    private void ui(Runnable r) {
        overlayRoot.post(r);
    }

    public void showMenu(final String which) {
        ui(new Runnable() { public void run() { showMenuInternal(which); } });
    }

    public void showMainMenu() {
        ui(new Runnable() {
            public void run() {
                continueBtn.setVisibility(callbacks.hasSaveGame() ? View.VISIBLE : View.GONE);
                bestScoreText();
                showMenuInternal("main");
            }
        });
    }

    /** Open Settings remembering where to return. */
    private String settingsReturnTo = "main";

    private void showMenuInternal(String which) {
        mainMenu.setVisibility(View.GONE);
        settingsMenu.setVisibility(View.GONE);
        pauseMenu.setVisibility(View.GONE);
        gameOverMenu.setVisibility(View.GONE);
        missionCompleteMenu.setVisibility(View.GONE);
        aboutMenu.setVisibility(View.GONE);
        if (which == null) {
            menuLayer.setVisibility(View.GONE);
            menuLayer.setClickable(false);
            return;
        }
        LinearLayout target;
        switch (which) {
            case "settings":
                target = settingsMenu;
                break;
            case "pause": target = pauseMenu; break;
            case "gameover": target = gameOverMenu; break;
            case "mission": target = missionCompleteMenu; break;
            case "about": target = aboutMenu; break;
            default: target = mainMenu; break;
        }
        target.setVisibility(View.VISIBLE);
        menuLayer.setVisibility(View.VISIBLE);
        menuLayer.setClickable(true);
    }

    public void openSettingsFrom(final String from) {
        ui(new Runnable() {
            public void run() {
                settingsReturnTo = "pause".equals(from) ? "pause" : "main";
                showMenuInternal("settings");
            }
        });
    }

    private void closeSettingsInternal() {
        showMenuInternal("pause".equals(settingsReturnTo) ? "pause" : "main");
    }

    // ---------------- HUD updates (dirty-checked on UI thread) ----------------
    public void setHudMission(final String title, final String goal) {
        ui(new Runnable() { public void run() {
            if (!safeEquals(title, lastMission) || !safeEquals(goal, lastGoal)) {
                hudMissionTitle.setText(title);
                hudMissionGoal.setText(goal);
                lastMission = title;
                lastGoal = goal;
            }
        }});
    }

    public void setHudStats(final String stats) {
        ui(new Runnable() { public void run() {
            if (!safeEquals(stats, lastStats)) {
                hudStats.setText(stats);
                lastStats = stats;
            }
        }});
    }

    public void setHudPrompt(final String promptOrNull) {
        ui(new Runnable() { public void run() {
            String p = promptOrNull == null ? "" : promptOrNull;
            if (!safeEquals(p, lastPrompt)) {
                hudPrompt.setVisibility(promptOrNull == null ? View.GONE : View.VISIBLE);
                hudPrompt.setText(p);
                lastPrompt = p;
            }
        }});
    }

    public void setHudBanner(final String bannerOrNull) {
        ui(new Runnable() { public void run() {
            String b = bannerOrNull == null ? "" : bannerOrNull;
            if (!safeEquals(b, lastBanner)) {
                hudBanner.setVisibility(bannerOrNull == null ? View.GONE : View.VISIBLE);
                hudBanner.setText(b);
                lastBanner = b;
            }
        }});
    }

    public void flashDamage() {
        ui(new Runnable() { public void run() {
            damageFlash.setAlpha(0.45f);
            damageFlash.animate().alpha(0f).setDuration(350).start();
        }});
    }

    public void notifySaved() {
        ui(new Runnable() { public void run() {
            hudSaved.setVisibility(View.VISIBLE);
            hudSaved.postDelayed(new Runnable() {
                @Override public void run() { hudSaved.setVisibility(View.GONE); }
            }, 1200);
        }});
    }

    public void setGameOverInfo(final String line1, final String line2) {
        ui(new Runnable() { public void run() {
            setMenuText(gameOverMenu, R_ID_OVER_TITLE, "GAME OVER");
            setMenuText(gameOverMenu, R_ID_OVER_STATS, line1 + "\n" + line2);
        }});
    }

    public void setMissionCompleteInfo(final String line1, final String line2, final String buttonLabel) {
        ui(new Runnable() { public void run() {
            setMenuText(missionCompleteMenu, R_ID_MC_STATS, line1 + "\n" + line2);
            setMenuText(missionCompleteMenu, R_ID_MC_CONTINUE, buttonLabel);
        }});
    }

    public void setHudVisible(boolean visible) {
        ui(new Runnable() { public void run() {
            hudLayer.setVisibility(View.VISIBLE);
        }});
    }

    // =================================================================
    // Menu construction
    // =================================================================
    private static final int R_ID_OVER_TITLE = 101;
    private static final int R_ID_OVER_STATS = 102;
    private static final int R_ID_MC_STATS = 103;
    private static final int R_ID_MC_CONTINUE = 104;
    private static final int R_ID_MAIN_BEST = 105;

    private void bestScoreText() {
        TextView t = (TextView) mainMenu.findViewById(R_ID_MAIN_BEST);
        if (t != null) {
            int best = callbacks.bestScore();
            t.setText(best > 0 ? "Best score  " + best : "No runs completed yet");
        }
    }

    private LinearLayout buildMainMenu() {
        LinearLayout panel = panelBase(Gravity.CENTER);
        TextView title = menuTitle("REALWORLD\nSURVIVAL 3D");
        TextView sub = menuSubtitle("Rooftop parkour survival - Original game by DevAgent");
        panel.addView(title);
        panel.addView(sub);

        TextView best = new TextView(activity);
        best.setId(R_ID_MAIN_BEST);
        best.setTextColor(COLOR_DIM);
        best.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        best.setGravity(Gravity.CENTER);
        panel.addView(best, wrapHeight(dp(8)));

        panel.addView(menuButton("New Run", new Click() { public void go() { callbacks.onStartNewGame(); } }), btnLp());
        continueBtn = menuButton("Continue", new Click() { public void go() { callbacks.onContinueGame(); } });
        panel.addView(continueBtn, btnLp());
        panel.addView(menuButton("Settings", new Click() { public void go() { openSettingsFrom("main"); } }), btnLp());
        panel.addView(menuButton("About", new Click() { public void go() { showMenu("about"); } }), btnLp());
        panel.addView(menuButton("Exit Game", new Click() { public void go() { callbacks.onExitRequested(); } }), btnLp());
        return panel;
    }

    private LinearLayout buildSettingsMenu() {
        LinearLayout panel = panelBase(Gravity.CENTER);
        panel.addView(menuTitle("SETTINGS"));

        musicValue = addSlider(panel, "Music volume", settings.musicVolume,
                new Slider() { public void set(float v) { settings.musicVolume = v; callbacks.onSettingsChanged(); } });
        sfxValue = addSlider(panel, "SFX volume", settings.sfxVolume,
                new Slider() { public void set(float v) { settings.sfxVolume = v; callbacks.onSettingsChanged(); } });
        sensValue = addSlider(panel, "Camera sensitivity", settings.lookSensitivity,
                new Slider() { public void set(float v) { settings.lookSensitivity = v; callbacks.onSettingsChanged(); } });

        qualityValue = addToggle(panel, "Graphics quality",
                settings.graphicsQuality == GameSettings.QUALITY_HIGH ? "High" : "Performance",
                new Click() {
                    public void go() {
                        settings.graphicsQuality = settings.graphicsQuality == GameSettings.QUALITY_HIGH
                                ? GameSettings.QUALITY_PERFORMANCE : GameSettings.QUALITY_HIGH;
                        String v = settings.graphicsQuality == GameSettings.QUALITY_HIGH ? "High" : "Performance";
                        qualityValue.setText("Graphics quality  " + v);
                        callbacks.onSettingsChanged();
                    }
                });

        fpsValue = addToggle(panel, "FPS limit",
                settings.fpsLimit == GameSettings.FPS_60 ? "60" : "30",
                new Click() {
                    public void go() {
                        settings.fpsLimit = settings.fpsLimit == GameSettings.FPS_60
                                ? GameSettings.FPS_30 : GameSettings.FPS_60;
                        String v = settings.fpsLimit == GameSettings.FPS_60 ? "60" : "30";
                        fpsValue.setText("FPS limit  " + v);
                        callbacks.onSettingsChanged();
                    }
                });

        invertValue = addToggle(panel, "Invert camera Y", settings.invertLookY ? "On" : "Off",
                new Click() {
                    public void go() {
                        settings.invertLookY = !settings.invertLookY;
                        String v = settings.invertLookY ? "On" : "Off";
                        invertValue.setText("Invert camera Y  " + v);
                        callbacks.onSettingsChanged();
                    }
                });

        panel.addView(menuButton("Back", new Click() { public void go() { settings.save(); closeSettingsInternal(); } }), btnLp());
        return panel;
    }

    private LinearLayout buildPauseMenu() {
        LinearLayout panel = panelBase(Gravity.CENTER);
        panel.addView(menuTitle("PAUSED"));
        panel.addView(menuButton("Resume", new Click() { public void go() { callbacks.onResumeRequested(); } }), btnLp());
        panel.addView(menuButton("Restart Mission", new Click() { public void go() { callbacks.onRestartMission(); } }), btnLp());
        panel.addView(menuButton("Settings", new Click() { public void go() { openSettingsFrom("pause"); } }), btnLp());
        panel.addView(menuButton("Main Menu", new Click() { public void go() { callbacks.onQuitToMenu(); } }), btnLp());
        return panel;
    }

    private LinearLayout buildGameOverMenu() {
        LinearLayout panel = panelBase(Gravity.CENTER);
        TextView title = menuTitle("GAME OVER");
        title.setId(R_ID_OVER_TITLE);
        title.setTextColor(0xFFFF5252);
        panel.addView(title);
        TextView stats = menuSubtitle("");
        stats.setId(R_ID_OVER_STATS);
        panel.addView(stats, wrapHeight(dp(10)));
        panel.addView(menuButton("Retry Mission", new Click() { public void go() { callbacks.onStartNewGame(); } }), btnLp());
        panel.addView(menuButton("Main Menu", new Click() { public void go() { callbacks.onQuitToMenu(); } }), btnLp());
        return panel;
    }

    private LinearLayout buildMissionCompleteMenu() {
        LinearLayout panel = panelBase(Gravity.CENTER);
        TextView title = menuTitle("MISSION COMPLETE");
        title.setTextColor(0xFF69F0AE);
        panel.addView(title);
        TextView stats = menuSubtitle("");
        stats.setId(R_ID_MC_STATS);
        panel.addView(stats, wrapHeight(dp(10)));
        TextView cont = menuButton("Next Mission", new Click() { public void go() { callbacks.onStartNewGame(); } });
        cont.setId(R_ID_MC_CONTINUE);
        panel.addView(cont, btnLp());
        panel.addView(menuButton("Main Menu", new Click() { public void go() { callbacks.onQuitToMenu(); } }), btnLp());
        return panel;
    }

    private LinearLayout buildAboutMenu() {
        LinearLayout panel = panelBase(Gravity.CENTER);
        panel.addView(menuTitle("ABOUT"));
        TextView body = menuSubtitle(
                "REALWORLD SURVIVAL 3D\n" +
                "An original rooftop survival-parkour adventure.\n\n" +
                "You are a courier runner trapped in a locked-down high-rise\n" +
                "district at dusk. Cross the towers, recover survival supplies\n" +
                "and data drives, avoid the infected stalkers, and signal the\n" +
                "extraction helicopter from the Apex helipad.\n\n" +
                "CONTROLS\n" +
                "- Left thumb: virtual joystick to move\n" +
                "- Right thumb: swipe to look around\n" +
                "- JUMP / VAULT / SLIDE buttons: parkour actions\n" +
                "- SPRINT toggle, CAM: toggle camera view, PAUSE: top right\n" +
                "- INTERACT: appears near consoles and data drives\n\n" +
                "Original code, audio and art by DevAgent. No third-party IP.");
        panel.addView(body, wrapHeight(dp(6)));
        panel.addView(menuButton("Back", new Click() { public void go() { showMenu("back"); } }), btnLp());
        return panel;
    }

    // =================================================================
    // HUD construction
    // =================================================================
    private void buildHud(FrameLayout hud) {
        LinearLayout missionBox = new LinearLayout(activity);
        missionBox.setOrientation(LinearLayout.VERTICAL);
        missionBox.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        missionBox.setClickable(false);
        FrameLayout.LayoutParams blp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        blp.topMargin = dp(18);
        missionBox.setLayoutParams(blp);

        hudMissionTitle = new TextView(activity);
        hudMissionTitle.setTextColor(COLOR_ACCENT);
        hudMissionTitle.setTypeface(Typeface.DEFAULT_BOLD);
        hudMissionTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        hudMissionTitle.setShadowLayer(4, 0, 2, 0xCC000000);
        missionBox.addView(hudMissionTitle);

        hudMissionGoal = new TextView(activity);
        hudMissionGoal.setTextColor(COLOR_TEXT);
        hudMissionGoal.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        hudMissionGoal.setShadowLayer(4, 0, 2, 0xCC000000);
        hudMissionGoal.setGravity(Gravity.CENTER_HORIZONTAL);
        missionBox.addView(hudMissionGoal);

        hudStats = new TextView(activity);
        hudStats.setTextColor(COLOR_DIM);
        hudStats.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        hudStats.setShadowLayer(4, 0, 2, 0xCC000000);
        hudStats.setGravity(Gravity.CENTER_HORIZONTAL);
        missionBox.addView(hudStats);

        hud.addView(missionBox);

        // Bottom-center interaction prompt
        hudPrompt = new TextView(activity);
        hudPrompt.setTextColor(0xFFFFC24B);
        hudPrompt.setTypeface(Typeface.DEFAULT_BOLD);
        hudPrompt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        hudPrompt.setShadowLayer(4, 0, 2, 0xCC000000);
        hudPrompt.setGravity(Gravity.CENTER);
        hudPrompt.setVisibility(View.GONE);
        hudPrompt.setClickable(false);
        FrameLayout.LayoutParams plp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        plp.bottomMargin = dp(24);
        hudPrompt.setLayoutParams(plp);
        hud.addView(hudPrompt);

        // Center action banner (combo feedback)
        hudBanner = new TextView(activity);
        hudBanner.setTextColor(0xFFFFE27B);
        hudBanner.setTypeface(Typeface.DEFAULT_BOLD);
        hudBanner.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        hudBanner.setShadowLayer(5, 0, 2, 0xCC000000);
        hudBanner.setGravity(Gravity.CENTER);
        hudBanner.setVisibility(View.GONE);
        hudBanner.setClickable(false);
        FrameLayout.LayoutParams blp2 = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER);
        hudBanner.setLayoutParams(blp2);
        hud.addView(hudBanner);

        // Saved indicator
        hudSaved = new TextView(activity);
        hudSaved.setTextColor(0xFF69F0AE);
        hudSaved.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        hudSaved.setShadowLayer(4, 0, 2, 0xCC000000);
        hudSaved.setText("PROGRESS SAVED");
        hudSaved.setGravity(Gravity.CENTER);
        hudSaved.setVisibility(View.GONE);
        hudSaved.setClickable(false);
        FrameLayout.LayoutParams slp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        slp.bottomMargin = dp(56);
        hudSaved.setLayoutParams(slp);
        hud.addView(hudSaved);

        // Damage flash overlay
        damageFlash = new View(activity);
        damageFlash.setBackgroundColor(0x88E53935);
        damageFlash.setAlpha(0f);
        damageFlash.setClickable(false);
        damageFlash.setLayoutParams(matchParent());
        hud.addView(damageFlash);
    }

    // =================================================================
    // Widget helpers
    // =================================================================
    private interface Click { void go(); }
    private interface Slider { void set(float v); }

    private FrameLayout.LayoutParams matchParent() {
        return new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    private LinearLayout.LayoutParams wrapHeight(int topMargin) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = topMargin;
        lp.bottomMargin = dp(10);
        return lp;
    }

    private LinearLayout.LayoutParams btnLp() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                dp(230), dp(44));
        lp.topMargin = dp(8);
        return lp;
    }

    private int dp(int v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                activity.getResources().getDisplayMetrics());
    }

    private LinearLayout panelBase(int gravity) {
        LinearLayout panel = new LinearLayout(activity);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setGravity(gravity);
        panel.setClickable(true);
        panel.setFocusable(true);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(PANEL_BG);
        bg.setCornerRadius(dp(18));
        panel.setBackgroundDrawable(bg);

        int pad = dp(24);
        panel.setPadding(pad, pad, pad, pad);
        panel.setVisibility(View.GONE);

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                gravity);
        lp.topMargin = dp(24);
        panel.setLayoutParams(lp);
        return panel;
    }

    private TextView menuTitle(String text) {
        TextView t = new TextView(activity);
        t.setText(text);
        t.setTextColor(COLOR_TEXT);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26);
        t.setLetterSpacing(0.06f);
        t.setGravity(Gravity.CENTER);
        t.setLineSpacing(dp(2), 1f);
        return t;
    }

    private TextView menuSubtitle(String text) {
        TextView t = new TextView(activity);
        t.setText(text);
        t.setTextColor(COLOR_DIM);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        t.setGravity(Gravity.CENTER);
        t.setLineSpacing(dp(1), 1.05f);
        return t;
    }

    private TextView menuButton(String label, final Click click) {
        final TextView b = new TextView(activity);
        b.setText(label);
        b.setTextColor(COLOR_TEXT);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        b.setLetterSpacing(0.04f);
        b.setGravity(Gravity.CENTER);
        b.setTypeface(Typeface.DEFAULT_BOLD);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(BTN_BG);
        bg.setCornerRadius(dp(12));
        bg.setStroke(dp(1), 0x33FFFFFF);
        b.setBackgroundDrawable(bg);
        b.setClickable(true);

        b.setOnTouchListener(new View.OnTouchListener() {
            @Override public boolean onTouch(View v, android.view.MotionEvent e) {
                GradientDrawable g = new GradientDrawable();
                g.setCornerRadius(dp(12));
                g.setStroke(dp(1), 0x33FFFFFF);
                g.setColor(e.getActionMasked() == android.view.MotionEvent.ACTION_DOWN
                        ? BTN_BG_PRESSED : BTN_BG);
                b.setBackgroundDrawable(g);
                return false;
            }
        });
        b.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                android.media.AudioManager am =
                        (android.media.AudioManager) activity.getSystemService(android.content.Context.AUDIO_SERVICE);
                if (am != null) {
                    am.playSoundEffect(android.media.AudioManager.FX_KEY_CLICK, 0.6f);
                }
                click.go();
            }
        });
        return b;
    }

    private void setMenuText(ViewGroup menu, int id, String text) {
        View v = menu.findViewById(id);
        if (v instanceof TextView) ((TextView) v).setText(text);
    }

    private String settingLabel(String name, String value) {
        return name;
    }

    // ---------------- Slider rows ----------------
    private TextView addSlider(LinearLayout parent, final String label, float value, final Slider slider) {
        final String prefix = label + "  ";
        final TextView row = new TextView(activity);
        row.setTextColor(COLOR_TEXT);
        row.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        row.setText(prefix + formatPercent(value));
        row.setGravity(Gravity.CENTER_HORIZONTAL);

        SeekBar bar = new SeekBar(activity);
        bar.setMax(100);
        bar.setProgress((int) (value * 100));
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(dp(240), dp(30));
        blp.gravity = Gravity.CENTER_HORIZONTAL;
        bar.setLayoutParams(blp);
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int p, boolean fromUser) {
                if (fromUser) {
                    float v = p / 100f;
                    slider.set(v);
                    row.setText(prefix + formatPercent(v));
                }
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) { settings.save(); }
        });
        parent.addView(row, wrapHeight(dp(10)));
        parent.addView(bar);
        return row;
    }

    private TextView settingLabelView(String label, String value) {
        TextView t = new TextView(activity);
        t.setTextColor(COLOR_TEXT);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        t.setText(label + "  " + value);
        t.setGravity(Gravity.CENTER_HORIZONTAL);
        return t;
    }

    private TextView addToggle(LinearLayout parent, String label, String value, final Click click) {
        TextView row = new TextView(activity);
        row.setTextColor(COLOR_TEXT);
        row.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        row.setText(label + "  " + value);
        row.setGravity(Gravity.CENTER_HORIZONTAL);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(BTN_BG);
        bg.setCornerRadius(dp(10));
        bg.setStroke(dp(1), 0x33FFFFFF);
        row.setBackgroundDrawable(bg);
        row.setClickable(true);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(240), dp(36));
        lp.topMargin = dp(10);
        lp.gravity = Gravity.CENTER_HORIZONTAL;
        row.setLayoutParams(lp);
        row.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { click.go(); }
        });
        return row;
    }

    private static String formatPercent(float v) {
        return String.valueOf(Math.round(v * 100)) + "%";
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }
}
