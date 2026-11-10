package com.devagent.realworldsurvival3d;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * SaveManager - Handles save/load using SharedPreferences (manual key-value).
 */
public class SaveManager {
    private static final String PREFS_NAME = "RWSurvivalSave";
    private SharedPreferences prefs;

    public SaveManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

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

    public boolean hasSave() {
        return prefs.contains("player_health");
    }

    public void clear() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();
    }
}
