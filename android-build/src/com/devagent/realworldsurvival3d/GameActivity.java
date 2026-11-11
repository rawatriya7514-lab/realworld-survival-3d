package com.devagent.realworldsurvival3d;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

/**
 * GameActivity - Main entry point for REALWORLD SURVIVAL 3D.
 * Layers the GLES2 game surface with the view-based UI overlay
 * (menus + text HUD) and manages the activity lifecycle.
 */
public class GameActivity extends Activity {

    private GameView gameView;
    private UIController ui;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        enterImmersiveMode(getWindow().getDecorView());

        // Layered root: GL game surface below, view UI overlay above
        FrameLayout root = new FrameLayout(this);
        root.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        GameSettings settings = new GameSettings(this);
        gameView = new GameView(this, settings);
        root.addView(gameView);

        ui = new UIController(this, gameView, settings);
        root.addView(ui.overlayRoot);
        gameView.attachUi(ui);

        setContentView(root);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            enterImmersiveMode(getWindow().getDecorView());
        }
    }

    private static void enterImmersiveMode(View decor) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            decor.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (gameView != null) {
            gameView.onResume();
            gameView.activityResumed();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (gameView != null) {
            gameView.activityPaused();
            gameView.onPause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (gameView != null) {
            gameView.destroy();
        }
    }

    @Override
    public void onBackPressed() {
        if (gameView != null && gameView.onBackPressed()) {
            return; // Handled by game (pause/menus)
        }
        super.onBackPressed(); // from the main menu: exit the app
    }
}
