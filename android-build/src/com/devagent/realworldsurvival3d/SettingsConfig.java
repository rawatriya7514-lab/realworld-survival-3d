package com.devagent.realworldsurvival3d;

import java.util.HashMap;
import java.util.Map;

/**
 * SettingsConfig - Pure Java configuration schema, presets, clamping,
 * and mathematical conversions for game preferences (audio, graphics, controls).
 * Decoupled from Android SharedPreferences for 100% JVM testability.
 */
public class SettingsConfig {
    public static final String KEY_MASTER_VOL = "master_volume";
    public static final String KEY_MUSIC_VOL = "music_volume";
    public static final String KEY_SFX_VOL = "sfx_volume";
    public static final String KEY_LOOK_SENS = "look_sensitivity";
    public static final String KEY_INVERT_LOOK_Y = "invert_look_y";
    public static final String KEY_GRAPHICS_QUALITY = "graphics_quality";
    public static final String KEY_FPS_LIMIT = "fps_limit";
    public static final String KEY_SHADOWS = "shadows_enabled";
    public static final String KEY_BLOOM = "bloom_enabled";
    public static final String KEY_BTN_SCALE = "btn_scale";
    public static final String KEY_BTN_OPACITY = "btn_opacity";
    public static final String KEY_LEFT_HANDED = "left_handed";
    public static final String KEY_JOY_SENS = "joy_sensitivity";

    // Graphics Quality Presets
    public static final int QUALITY_LOW = 0;
    public static final int QUALITY_MEDIUM = 1;
    public static final int QUALITY_HIGH = 2;

    // FPS limits
    public static final int FPS_30 = 30;
    public static final int FPS_60 = 60;

    // Configuration Fields with Default Values
    public float masterVolume = 1.0f;          // 0.0 .. 1.0
    public float musicVolume = 0.70f;          // 0.0 .. 1.0
    public float sfxVolume = 0.85f;            // 0.0 .. 1.0
    public float lookSensitivity = 0.50f;      // 0.0 .. 1.0
    public boolean invertLookY = false;
    public int graphicsQuality = QUALITY_HIGH;
    public int fpsLimit = FPS_60;
    public boolean shadowsEnabled = true;
    public boolean bloomEnabled = true;

    // Touch controls HUD (Phase 5)
    public float buttonScale = 1.0f;           // 0.8 .. 1.3
    public float buttonOpacity = 0.65f;        // 0.4 .. 1.0
    public boolean leftHanded = false;
    public float joystickSensitivity = 1.0f;   // 0.5 .. 1.5

    public SettingsConfig() {
        clampAll();
    }

    public void clampAll() {
        masterVolume = clamp(masterVolume, 0.0f, 1.0f);
        musicVolume = clamp(musicVolume, 0.0f, 1.0f);
        sfxVolume = clamp(sfxVolume, 0.0f, 1.0f);
        lookSensitivity = clamp(lookSensitivity, 0.0f, 1.0f);
        buttonScale = clamp(buttonScale, 0.8f, 1.3f);
        buttonOpacity = clamp(buttonOpacity, 0.4f, 1.0f);
        joystickSensitivity = clamp(joystickSensitivity, 0.5f, 1.5f);

        if (graphicsQuality < QUALITY_LOW || graphicsQuality > QUALITY_HIGH) {
            graphicsQuality = QUALITY_HIGH;
        }
        if (fpsLimit != FPS_30 && fpsLimit != FPS_60) {
            fpsLimit = FPS_60;
        }
    }

    public float getEffectiveMusicVolume() {
        return AudioMixer.effectiveVolume(masterVolume, musicVolume);
    }

    public float getEffectiveSfxVolume() {
        return AudioMixer.effectiveVolume(masterVolume, sfxVolume);
    }

    public float lookScale() {
        return 0.4f + lookSensitivity * 1.6f;
    }

    public static float scaleFromSlider(float slider) {
        return 0.8f + clamp(slider, 0f, 1f) * 0.5f;
    }

    public static float sliderFromScale(float scale) {
        return (clamp(scale, 0.8f, 1.3f) - 0.8f) / 0.5f;
    }

    public static float opacityFromSlider(float slider) {
        return 0.4f + clamp(slider, 0f, 1f) * 0.6f;
    }

    public static float sliderFromOpacity(float opacity) {
        return (clamp(opacity, 0.4f, 1.0f) - 0.4f) / 0.6f;
    }

    public static float joySensFromSlider(float slider) {
        return 0.5f + clamp(slider, 0f, 1f) * 1.0f;
    }

    public static float sliderFromJoySens(float sens) {
        return (clamp(sens, 0.5f, 1.5f) - 0.5f) / 1.0f;
    }

    public static float sliderToVolume(float slider) {
        if (slider <= 0.01f) return 0f;
        return 0.1f + clamp(slider, 0f, 1f) * 0.9f;
    }

    public static int cycleQuality(int current) {
        switch (current) {
            case QUALITY_LOW: return QUALITY_MEDIUM;
            case QUALITY_MEDIUM: return QUALITY_HIGH;
            case QUALITY_HIGH:
            default: return QUALITY_LOW;
        }
    }

    public static String qualityName(int quality) {
        switch (quality) {
            case QUALITY_LOW: return "Low";
            case QUALITY_MEDIUM: return "Medium";
            case QUALITY_HIGH: return "High";
            default: return "Custom";
        }
    }

    public static int cycleFps(int current) {
        return current == FPS_60 ? FPS_30 : FPS_60;
    }

    public static String fpsName(int fps) {
        return String.valueOf(fps);
    }

    public DayNightCycle.GraphicsPreset getGraphicsPreset() {
        switch (graphicsQuality) {
            case QUALITY_LOW: return DayNightCycle.GraphicsPreset.LOW;
            case QUALITY_MEDIUM: return DayNightCycle.GraphicsPreset.MEDIUM;
            case QUALITY_HIGH:
            default: return DayNightCycle.GraphicsPreset.HIGH;
        }
    }

    /** Exports configuration as key-value string map for persistence. */
    public Map<String, String> exportToStringMap() {
        Map<String, String> map = new HashMap<>();
        map.put(KEY_MASTER_VOL, String.valueOf(masterVolume));
        map.put(KEY_MUSIC_VOL, String.valueOf(musicVolume));
        map.put(KEY_SFX_VOL, String.valueOf(sfxVolume));
        map.put(KEY_LOOK_SENS, String.valueOf(lookSensitivity));
        map.put(KEY_INVERT_LOOK_Y, String.valueOf(invertLookY));
        map.put(KEY_GRAPHICS_QUALITY, String.valueOf(graphicsQuality));
        map.put(KEY_FPS_LIMIT, String.valueOf(fpsLimit));
        map.put(KEY_SHADOWS, String.valueOf(shadowsEnabled));
        map.put(KEY_BLOOM, String.valueOf(bloomEnabled));
        map.put(KEY_BTN_SCALE, String.valueOf(buttonScale));
        map.put(KEY_BTN_OPACITY, String.valueOf(buttonOpacity));
        map.put(KEY_LEFT_HANDED, String.valueOf(leftHanded));
        map.put(KEY_JOY_SENS, String.valueOf(joystickSensitivity));
        return map;
    }

    /** Imports configuration from key-value string map with safe parsing and clamping. */
    public void importFromStringMap(Map<String, String> map) {
        if (map == null) return;
        if (map.containsKey(KEY_MASTER_VOL)) masterVolume = parseFloat(map.get(KEY_MASTER_VOL), masterVolume);
        if (map.containsKey(KEY_MUSIC_VOL)) musicVolume = parseFloat(map.get(KEY_MUSIC_VOL), musicVolume);
        if (map.containsKey(KEY_SFX_VOL)) sfxVolume = parseFloat(map.get(KEY_SFX_VOL), sfxVolume);
        if (map.containsKey(KEY_LOOK_SENS)) lookSensitivity = parseFloat(map.get(KEY_LOOK_SENS), lookSensitivity);
        if (map.containsKey(KEY_INVERT_LOOK_Y)) invertLookY = Boolean.parseBoolean(map.get(KEY_INVERT_LOOK_Y));
        if (map.containsKey(KEY_GRAPHICS_QUALITY)) graphicsQuality = parseInt(map.get(KEY_GRAPHICS_QUALITY), graphicsQuality);
        if (map.containsKey(KEY_FPS_LIMIT)) fpsLimit = parseInt(map.get(KEY_FPS_LIMIT), fpsLimit);
        if (map.containsKey(KEY_SHADOWS)) shadowsEnabled = Boolean.parseBoolean(map.get(KEY_SHADOWS));
        if (map.containsKey(KEY_BLOOM)) bloomEnabled = Boolean.parseBoolean(map.get(KEY_BLOOM));
        if (map.containsKey(KEY_BTN_SCALE)) buttonScale = parseFloat(map.get(KEY_BTN_SCALE), buttonScale);
        if (map.containsKey(KEY_BTN_OPACITY)) buttonOpacity = parseFloat(map.get(KEY_BTN_OPACITY), buttonOpacity);
        if (map.containsKey(KEY_LEFT_HANDED)) leftHanded = Boolean.parseBoolean(map.get(KEY_LEFT_HANDED));
        if (map.containsKey(KEY_JOY_SENS)) joystickSensitivity = parseFloat(map.get(KEY_JOY_SENS), joystickSensitivity);
        clampAll();
    }

    private static float parseFloat(String val, float def) {
        try { return Float.parseFloat(val); } catch (Exception e) { return def; }
    }

    private static int parseInt(String val, int def) {
        try { return Integer.parseInt(val); } catch (Exception e) { return def; }
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }
}
