package com.devagent.realworldsurvival3d;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * GameSettings - Persisted player preferences (audio, graphics, controls).
 * Extends pure Java SettingsConfig and persists values to Android SharedPreferences.
 */
public class GameSettings extends SettingsConfig {
    private static final String PREFS = "RWSurvivalSettings";

    // Backward-compatible quality aliases
    public static final int QUALITY_PERFORMANCE = QUALITY_LOW;

    private final SharedPreferences prefs;

    public GameSettings(Context context) {
        super();
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        load();
    }

    public void load() {
        masterVolume = prefs.getFloat(KEY_MASTER_VOL, 1.0f);
        musicVolume = prefs.getFloat(KEY_MUSIC_VOL, 0.70f);
        sfxVolume = prefs.getFloat(KEY_SFX_VOL, 0.85f);
        lookSensitivity = prefs.getFloat(KEY_LOOK_SENS, 0.50f);
        invertLookY = prefs.getBoolean(KEY_INVERT_LOOK_Y, false);
        graphicsQuality = prefs.getInt(KEY_GRAPHICS_QUALITY, QUALITY_HIGH);
        fpsLimit = prefs.getInt(KEY_FPS_LIMIT, FPS_60);
        shadowsEnabled = prefs.getBoolean(KEY_SHADOWS, true);
        bloomEnabled = prefs.getBoolean(KEY_BLOOM, true);
        buttonScale = prefs.getFloat(KEY_BTN_SCALE, 1.0f);
        buttonOpacity = prefs.getFloat(KEY_BTN_OPACITY, 0.65f);
        leftHanded = prefs.getBoolean(KEY_LEFT_HANDED, false);
        joystickSensitivity = prefs.getFloat(KEY_JOY_SENS, 1.0f);
        clampAll();
    }

    public void save() {
        clampAll();
        SharedPreferences.Editor e = prefs.edit();
        e.putFloat(KEY_MASTER_VOL, masterVolume);
        e.putFloat(KEY_MUSIC_VOL, musicVolume);
        e.putFloat(KEY_SFX_VOL, sfxVolume);
        e.putFloat(KEY_LOOK_SENS, lookSensitivity);
        e.putBoolean(KEY_INVERT_LOOK_Y, invertLookY);
        e.putInt(KEY_GRAPHICS_QUALITY, graphicsQuality);
        e.putInt(KEY_FPS_LIMIT, fpsLimit);
        e.putBoolean(KEY_SHADOWS, shadowsEnabled);
        e.putBoolean(KEY_BLOOM, bloomEnabled);
        e.putFloat(KEY_BTN_SCALE, buttonScale);
        e.putFloat(KEY_BTN_OPACITY, buttonOpacity);
        e.putBoolean(KEY_LEFT_HANDED, leftHanded);
        e.putFloat(KEY_JOY_SENS, joystickSensitivity);
        e.apply();
    }

    /** Backward-compatible alias for sliderToVolume. */
    public static float toVolume(float slider) {
        return sliderToVolume(slider);
    }
}
