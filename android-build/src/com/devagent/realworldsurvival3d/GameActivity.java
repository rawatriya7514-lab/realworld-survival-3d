package com.devagent.realworldsurvival3d;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

/**
 * GameActivity - Main entry point for REALWORLD SURVIVAL 3D
 * Initializes the game view and manages activity lifecycle.
 */
public class GameActivity extends Activity {

    private GameView gameView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Fullscreen, no title
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        gameView = new GameView(this);
        setContentView(gameView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (gameView != null) {
            gameView.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (gameView != null) {
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
            return; // Handled by game (menu/pause)
        }
        super.onBackPressed();
    }
}
