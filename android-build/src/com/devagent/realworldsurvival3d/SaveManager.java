package com.devagent.realworldsurvival3d;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.File;

/**
 * SaveManager - Handles robust game persistence.
 * Combines atomic file writes with CRC32 integrity verification and backup rotation (via SaveFileManager),
 * while preserving backward-compatible SharedPreferences access for high scores and legacy saves.
 */
public class SaveManager {
    private static final String PREFS_NAME = "RWSurvivalSave";
    private static final String SAVE_FILENAME = "rwsurvival_save.dat";

    private final SharedPreferences prefs;
    private final File saveFile;

    public SaveManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        saveFile = new File(context.getFilesDir(), SAVE_FILENAME);
    }

    /** Atomically saves the complete game snapshot to disk. */
    public boolean saveGameState(SaveState state) {
        if (state == null) return false;
        // Also update high score in SharedPreferences
        if (state.bestScore > loadInt("best_score", 0)) {
            saveInt("best_score", state.bestScore);
        }
        return SaveFileManager.saveAtomic(saveFile, state);
    }

    /** Loads and verifies game state from primary file or automatic .bak recovery. */
    public SaveState loadGameState() {
        return SaveFileManager.loadSafe(saveFile);
    }

    public boolean hasSave() {
        return SaveFileManager.hasValidSave(saveFile) || prefs.contains("player_health")
                || prefs.getInt("current_mission", -1) > 0;
    }

    public void clear() {
        SaveFileManager.deleteSave(saveFile);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();
    }

    // -----------------------------------------------------------------
    // Legacy SharedPreferences operations (backward-compatible)
    // -----------------------------------------------------------------
    public void saveFloat(String key, float value) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putFloat(key, value);
        editor.apply();
    }

    public void saveInt(String key, int value) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(key, value);
        editor.apply();
    }

    public void saveString(String key, String value) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(key, value);
        editor.apply();
    }

    public float loadFloat(String key, float defaultValue) {
        return prefs.getFloat(key, defaultValue);
    }

    public int loadInt(String key, int defaultValue) {
        return prefs.getInt(key, defaultValue);
    }

    public String loadString(String key, String defaultValue) {
        return prefs.getString(key, defaultValue);
    }
}
