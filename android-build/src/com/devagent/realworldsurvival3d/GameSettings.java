package com.devagent.realworldsurvival3d;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * GameSettings - Persisted player preferences (audio, look, graphics).
 * Stored in their own SharedPreferences file, separate from save progress.
 */
public class GameSettings {
    private static final String PREFS = "RWSurvivalSettings";

    private static final String K_MUSIC = "music_volume";
    private static final String K_SFX = "sfx_volume";
    private static final String K_SENS = "look_sensitivity";
    private static final String K_INVERT = "invert_look_y";
    private static final String K_QUALITY = "graphics_quality";
    private static final String K_FPS = "fps_limit";

    // Rendering quality presets (see WorldRenderer)
    public static final int QUALITY_PERFORMANCE = 0;
    public static final int QUALITY_HIGH = 1;

    // FPS limit options
    public static final int FPS_60 = 0;
    public static final int FPS_30 = 1;

    public float musicVolume = 0.7f;      // 0..1
    public float sfxVolume = 0.85f;       // 0..1
    /** 0..1 UI slider; scaled to 0.4..2.0 touch sensitivity multiplier. */
    public float lookSensitivity = 0.5f;
    public boolean invertLookY = false;
    public int graphicsQuality = QUALITY_HIGH;
    public int fpsLimit = FPS_60;

    private final SharedPreferences prefs;

    public GameSettings(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        load();
    }

    public void load() {
        musicVolume = prefs.getFloat(K_MUSIC, 0.7f);
        sfxVolume = prefs.getFloat(K_SFX, 0.85f);
        lookSensitivity = prefs.getFloat(K_SENS, 0.5f);
        invertLookY = prefs.getBoolean(K_INVERT, false);
        graphicsQuality = prefs.getInt(K_QUALITY, QUALITY_HIGH);
        fpsLimit = prefs.getInt(K_FPS, FPS_60);
        clampAll();
    }

    public void save() {
        SharedPreferences.Editor e = prefs.edit();
        e.putFloat(K_MUSIC, musicVolume);
        e.putFloat(K_SFX, sfxVolume);
        e.putFloat(K_SENS, lookSensitivity);
        e.putBoolean(K_INVERT, invertLookY);
        e.putInt(K_QUALITY, graphicsQuality);
        e.putInt(K_FPS, fpsLimit);
        e.apply();
    }

    /** Touch multiplier derived from the 0..1 slider. */
    public float lookScale() {
        return 0.4f + lookSensitivity * 1.6f;
    }

    /** Slider 0..1 to practical volume: keep audible minimum at 0.1. */
    public static float toVolume(float slider) {
        if (slider <= 0.01f) return 0f;
        return 0.1f + slider * 0.9f;
    }

    private void clampAll() {
        musicVolume = Math.max(0f, Math.min(1f, musicVolume));
        sfxVolume = Math.max(0f, Math.min(1f, sfxVolume));
        lookSensitivity = Math.max(0f, Math.min(1f, lookSensitivity));
        if (graphicsQuality != QUALITY_PERFORMANCE && graphicsQuality != QUALITY_HIGH) {
            graphicsQuality = QUALITY_HIGH;
        }
        if (fpsLimit != FPS_30 && fpsLimit != FPS_60) {
            fpsLimit = FPS_60;
        }
    }
}
