package com.devagent.realworldsurvival3d;

import java.util.HashMap;
import java.util.Map;

/**
 * AudioMixer - Pure Java audio engine math, sound registry, spatial attenuation,
 * and dynamic pitch/volume modulation.
 * Decoupled from Android SoundPool / MediaPlayer so audio logic can be 100% JVM tested.
 */
public class AudioMixer {
    // Sound FX IDs
    public static final int SFX_FOOTSTEP = 1;
    public static final int SFX_JUMP = 2;
    public static final int SFX_DOUBLE_JUMP = 3;
    public static final int SFX_WALLRUN = 4;
    public static final int SFX_VAULT = 5;
    public static final int SFX_LEDGE_GRAB = 6;
    public static final int SFX_MANTLE = 7;
    public static final int SFX_SLIDE = 8;
    public static final int SFX_ROLL = 9;
    public static final int SFX_HARD_LAND = 10;
    public static final int SFX_HEAVY_BREATH = 11;
    public static final int SFX_HEARTBEAT = 12;
    public static final int SFX_PICKUP_DRINK = 13;
    public static final int SFX_PICKUP_MEDKIT = 14;
    public static final int SFX_PICKUP_TOKEN = 15;
    public static final int SFX_CHASER_ROAR = 16;
    public static final int SFX_LEVEL_WIN = 17;
    public static final int SFX_GAME_OVER = 18;
    // Phase 11 Audio Expansion
    public static final int SFX_VEHICLE_ENGINE = 19;
    public static final int SFX_VEHICLE_HORN = 20;
    public static final int SFX_VEHICLE_CRASH = 21;
    public static final int SFX_VEHICLE_SKID = 22;
    public static final int SFX_NPC_GREET = 23;
    public static final int SFX_QUEST_COMPLETE = 24;
    public static final int SFX_COMBAT_HIT = 25;
    public static final int SFX_DATA_SLATE = 26;

    private static final Map<Integer, String> ASSET_PATHS = new HashMap<>();
    private static final Map<Integer, Long> COOLDOWNS_MS = new HashMap<>();
    private final Map<Integer, Long> lastPlayTimes = new HashMap<>();

    static {
        ASSET_PATHS.put(SFX_FOOTSTEP, "audio/sfx/footstep_concrete.wav");
        ASSET_PATHS.put(SFX_JUMP, "audio/sfx/jump.wav");
        ASSET_PATHS.put(SFX_DOUBLE_JUMP, "audio/sfx/double_jump.wav");
        ASSET_PATHS.put(SFX_WALLRUN, "audio/sfx/wallrun.wav");
        ASSET_PATHS.put(SFX_VAULT, "audio/sfx/vault.wav");
        ASSET_PATHS.put(SFX_LEDGE_GRAB, "audio/sfx/ledge_grab.wav");
        ASSET_PATHS.put(SFX_MANTLE, "audio/sfx/mantle.wav");
        ASSET_PATHS.put(SFX_SLIDE, "audio/sfx/slide.wav");
        ASSET_PATHS.put(SFX_ROLL, "audio/sfx/roll.wav");
        ASSET_PATHS.put(SFX_HARD_LAND, "audio/sfx/hard_land.wav");
        ASSET_PATHS.put(SFX_HEAVY_BREATH, "audio/sfx/heavy_breath.wav");
        ASSET_PATHS.put(SFX_HEARTBEAT, "audio/sfx/heartbeat.wav");
        ASSET_PATHS.put(SFX_PICKUP_DRINK, "audio/sfx/pickup_drink.wav");
        ASSET_PATHS.put(SFX_PICKUP_MEDKIT, "audio/sfx/pickup_medkit.wav");
        ASSET_PATHS.put(SFX_PICKUP_TOKEN, "audio/sfx/pickup_token.wav");
        ASSET_PATHS.put(SFX_CHASER_ROAR, "audio/sfx/chaser_roar.wav");
        ASSET_PATHS.put(SFX_LEVEL_WIN, "audio/sfx/level_win.wav");
        ASSET_PATHS.put(SFX_GAME_OVER, "audio/sfx/game_over.wav");
        ASSET_PATHS.put(SFX_VEHICLE_ENGINE, "audio/sfx/vehicle_engine.wav");
        ASSET_PATHS.put(SFX_VEHICLE_HORN, "audio/sfx/vehicle_horn.wav");
        ASSET_PATHS.put(SFX_VEHICLE_CRASH, "audio/sfx/vehicle_crash.wav");
        ASSET_PATHS.put(SFX_VEHICLE_SKID, "audio/sfx/vehicle_skid.wav");
        ASSET_PATHS.put(SFX_NPC_GREET, "audio/sfx/npc_greet.wav");
        ASSET_PATHS.put(SFX_QUEST_COMPLETE, "audio/sfx/quest_complete.wav");
        ASSET_PATHS.put(SFX_COMBAT_HIT, "audio/sfx/combat_hit.wav");
        ASSET_PATHS.put(SFX_DATA_SLATE, "audio/sfx/data_slate.wav");

        COOLDOWNS_MS.put(SFX_FOOTSTEP, 180L);
        COOLDOWNS_MS.put(SFX_VEHICLE_HORN, 400L);
        COOLDOWNS_MS.put(SFX_VEHICLE_CRASH, 300L);
        COOLDOWNS_MS.put(SFX_VEHICLE_SKID, 350L);
        COOLDOWNS_MS.put(SFX_COMBAT_HIT, 200L);
        COOLDOWNS_MS.put(SFX_NPC_GREET, 500L);
    }

    public static String getAssetPath(int sfxId) {
        return ASSET_PATHS.get(sfxId);
    }

    public static boolean isLooping(int sfxId) {
        return sfxId == SFX_HEAVY_BREATH || sfxId == SFX_HEARTBEAT || sfxId == SFX_VEHICLE_ENGINE;
    }

    /** Master-channel volume composition clamped to [0, 1]. */
    public static float effectiveVolume(float master, float channelVolume) {
        float m = Math.max(0f, Math.min(1f, master));
        float c = Math.max(0f, Math.min(1f, channelVolume));
        return m * c;
    }

    /** Converts linear gain (0..1) to decibels (-60 dB minimum to 0 dB max). */
    public static float linearGainToDb(float gain) {
        if (gain <= 0.001f) return -60.0f;
        float db = 20.0f * (float) Math.log10(gain);
        return Math.max(-60.0f, Math.min(0.0f, db));
    }

    /** Converts decibels (-60..0 dB) to linear gain (0..1). */
    public static float dbToLinearGain(float db) {
        if (db <= -60.0f) return 0f;
        if (db >= 0.0f) return 1.0f;
        return (float) Math.pow(10.0, db / 20.0);
    }

    /**
     * Calculates spatial distance attenuation gain [0..1] based on Euclidean distance.
     * Full volume within minAudibleDist, linear falloff to 0 at maxAudibleDist.
     */
    public static float calculateDistanceGain(float lx, float lz, float sx, float sz,
                                              float minAudibleDist, float maxAudibleDist) {
        float dx = sx - lx;
        float dz = sz - lz;
        float dist = (float) Math.sqrt(dx * dx + dz * dz);
        if (dist <= minAudibleDist) return 1.0f;
        if (dist >= maxAudibleDist) return 0.0f;
        return 1.0f - (dist - minAudibleDist) / (maxAudibleDist - minAudibleDist);
    }

    /**
     * Calculates stereo panning factor [-1.0 (full left) to +1.0 (full right)]
     * given listener yaw orientation in degrees.
     */
    public static float calculateStereoPan(float listenerYawDeg, float lx, float lz, float sx, float sz) {
        float dx = sx - lx;
        float dz = sz - lz;
        float dist = (float) Math.sqrt(dx * dx + dz * dz);
        if (dist < 0.1f) return 0.0f;

        // Angle in radians
        float rad = (float) Math.toRadians(-listenerYawDeg);
        float cos = (float) Math.cos(rad);
        float sin = (float) Math.sin(rad);

        // Rotate offset into listener's local coordinate frame
        // localX is lateral (right is positive)
        float localX = dx * cos - dz * sin;
        float pan = localX / dist;
        return Math.max(-1.0f, Math.min(1.0f, pan));
    }

    /**
     * Decomposes effective volume and stereo pan into left and right channel volumes.
     * pan: -1.0 (left), 0.0 (center), +1.0 (right).
     */
    public static float[] calculateChannelVolumes(float effectiveVol, float pan) {
        float p = Math.max(-1.0f, Math.min(1.0f, pan));
        float left = effectiveVol * (1.0f - Math.max(0f, p));
        float right = effectiveVol * (1.0f + Math.min(0f, p));
        return new float[] { left, right };
    }

    /**
     * Pitch playback rate modulation for vehicle engine audio.
     * Idle starts at 0.70x, revs up to 1.60x at top speed.
     */
    public static float calculateEnginePitch(float currentSpeed, float maxSpeed) {
        float safeMax = Math.max(1.0f, maxSpeed);
        float ratio = Math.min(1.0f, Math.abs(currentSpeed) / safeMax);
        return 0.70f + ratio * 0.90f;
    }

    /**
     * Volume modulation for vehicle engine audio.
     * 0 if unoccupied; 0.35 base idle volume up to 1.0 at maximum velocity.
     */
    public static float calculateEngineVolume(float currentSpeed, float maxSpeed, boolean isOccupied) {
        if (!isOccupied) return 0.0f;
        float safeMax = Math.max(1.0f, maxSpeed);
        float ratio = Math.min(1.0f, Math.abs(currentSpeed) / safeMax);
        return 0.35f + ratio * 0.65f;
    }

    /**
     * Cooldown gate to prevent repetitive audio flanging / distortion.
     */
    public boolean canPlay(int sfxId, long nowMs) {
        Long cd = COOLDOWNS_MS.get(sfxId);
        if (cd == null) return true;
        Long last = lastPlayTimes.get(sfxId);
        if (last == null) return true;
        return (nowMs - last) >= cd;
    }

    public void recordPlay(int sfxId, long nowMs) {
        lastPlayTimes.put(sfxId, nowMs);
    }

    public void resetCooldowns() {
        lastPlayTimes.clear();
    }
}
