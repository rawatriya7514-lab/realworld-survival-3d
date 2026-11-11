package com.devagent.realworldsurvival3d;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Build;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * GameAudioManager - Low-latency audio engine for REALWORLD SURVIVAL 3D.
 * Manages:
 *   - SoundPool for instantaneous sound effects (parkour, vehicle engine/horn/crash/skid, NPC speech, quest stingers, combat)
 *   - MediaPlayer for dynamic studio background music tracks
 *   - Dynamic volume mixing (Master x Channel), pitch modulation, and spatial attenuation via AudioMixer
 */
public class GameAudioManager {
    // Re-export SFX IDs from AudioMixer
    public static final int SFX_FOOTSTEP = AudioMixer.SFX_FOOTSTEP;
    public static final int SFX_JUMP = AudioMixer.SFX_JUMP;
    public static final int SFX_DOUBLE_JUMP = AudioMixer.SFX_DOUBLE_JUMP;
    public static final int SFX_WALLRUN = AudioMixer.SFX_WALLRUN;
    public static final int SFX_VAULT = AudioMixer.SFX_VAULT;
    public static final int SFX_LEDGE_GRAB = AudioMixer.SFX_LEDGE_GRAB;
    public static final int SFX_MANTLE = AudioMixer.SFX_MANTLE;
    public static final int SFX_SLIDE = AudioMixer.SFX_SLIDE;
    public static final int SFX_ROLL = AudioMixer.SFX_ROLL;
    public static final int SFX_HARD_LAND = AudioMixer.SFX_HARD_LAND;
    public static final int SFX_HEAVY_BREATH = AudioMixer.SFX_HEAVY_BREATH;
    public static final int SFX_HEARTBEAT = AudioMixer.SFX_HEARTBEAT;
    public static final int SFX_PICKUP_DRINK = AudioMixer.SFX_PICKUP_DRINK;
    public static final int SFX_PICKUP_MEDKIT = AudioMixer.SFX_PICKUP_MEDKIT;
    public static final int SFX_PICKUP_TOKEN = AudioMixer.SFX_PICKUP_TOKEN;
    public static final int SFX_CHASER_ROAR = AudioMixer.SFX_CHASER_ROAR;
    public static final int SFX_LEVEL_WIN = AudioMixer.SFX_LEVEL_WIN;
    public static final int SFX_GAME_OVER = AudioMixer.SFX_GAME_OVER;
    public static final int SFX_VEHICLE_ENGINE = AudioMixer.SFX_VEHICLE_ENGINE;
    public static final int SFX_VEHICLE_HORN = AudioMixer.SFX_VEHICLE_HORN;
    public static final int SFX_VEHICLE_CRASH = AudioMixer.SFX_VEHICLE_CRASH;
    public static final int SFX_VEHICLE_SKID = AudioMixer.SFX_VEHICLE_SKID;
    public static final int SFX_NPC_GREET = AudioMixer.SFX_NPC_GREET;
    public static final int SFX_QUEST_COMPLETE = AudioMixer.SFX_QUEST_COMPLETE;
    public static final int SFX_COMBAT_HIT = AudioMixer.SFX_COMBAT_HIT;
    public static final int SFX_DATA_SLATE = AudioMixer.SFX_DATA_SLATE;

    private Context context;
    private SoundPool soundPool;
    private final Map<Integer, Integer> sfxMap = new HashMap<>();
    private final AudioMixer audioMixer = new AudioMixer();
    private MediaPlayer musicPlayer;

    private float masterVolume = 1.0f;
    private float sfxVolume = 0.85f;
    private float musicVolume = 0.65f;

    private int breathingStreamId = 0;
    private int heartbeatStreamId = 0;
    private int vehicleEngineStreamId = 0;
    private boolean isMuted = false;

    public GameAudioManager(Context context) {
        this.context = context;
        initSoundPool();
        loadAllSfx();
    }

    private void initSoundPool() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AudioAttributes attrs = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            soundPool = new SoundPool.Builder()
                    .setMaxStreams(16)
                    .setAudioAttributes(attrs)
                    .build();
        } else {
            soundPool = new SoundPool(16, AudioManager.STREAM_MUSIC, 0);
        }
    }

    private void loadAllSfx() {
        for (int id = 1; id <= 26; id++) {
            String path = AudioMixer.getAssetPath(id);
            if (path != null) {
                loadSfx(id, path);
            }
        }
    }

    private void loadSfx(int id, String assetPath) {
        try {
            AssetFileDescriptor afd = context.getAssets().openFd(assetPath);
            int soundId = soundPool.load(afd, 1);
            sfxMap.put(id, soundId);
        } catch (Exception e) {
            // Silently fallback if asset not yet bundled
        }
    }

    public float getEffectiveSfxVolume() {
        return AudioMixer.effectiveVolume(masterVolume, sfxVolume);
    }

    public float getEffectiveMusicVolume() {
        return AudioMixer.effectiveVolume(masterVolume, musicVolume);
    }

    public void setMasterVolume(float vol) {
        this.masterVolume = Math.max(0f, Math.min(1f, vol));
        updateActiveStreamVolumes();
    }

    public void setSfxVolume(float vol) {
        this.sfxVolume = Math.max(0f, Math.min(1f, vol));
        updateActiveStreamVolumes();
    }

    public void setMusicVolume(float vol) {
        this.musicVolume = Math.max(0f, Math.min(1f, vol));
        if (musicPlayer != null) {
            float eff = getEffectiveMusicVolume();
            musicPlayer.setVolume(eff, eff);
        }
    }

    private void updateActiveStreamVolumes() {
        if (soundPool == null) return;
        float effSfx = getEffectiveSfxVolume();
        if (breathingStreamId != 0) {
            soundPool.setVolume(breathingStreamId, effSfx * 0.7f, effSfx * 0.7f);
        }
        if (heartbeatStreamId != 0) {
            soundPool.setVolume(heartbeatStreamId, effSfx * 0.9f, effSfx * 0.9f);
        }
    }

    public void playSfx(int sfxId) {
        playSfx(sfxId, 1.0f, 1.0f);
    }

    public void playSfx(int sfxId, float volumeScale, float rate) {
        if (isMuted || soundPool == null) return;
        long now = System.currentTimeMillis();
        if (!audioMixer.canPlay(sfxId, now)) return;

        Integer soundId = sfxMap.get(sfxId);
        if (soundId != null) {
            float vol = getEffectiveSfxVolume() * Math.max(0f, Math.min(1f, volumeScale));
            float r = Math.max(0.5f, Math.min(2.0f, rate));
            soundPool.play(soundId, vol, vol, 1, 0, r);
            audioMixer.recordPlay(sfxId, now);
        }
    }

    /** 3D Spatial Audio playback with distance attenuation and stereo pan. */
    public void playSpatialSfx(int sfxId, float listenerX, float listenerZ, float listenerYaw,
                               float srcX, float srcZ, float minAudible, float maxAudible) {
        if (isMuted || soundPool == null) return;
        long now = System.currentTimeMillis();
        if (!audioMixer.canPlay(sfxId, now)) return;

        float distGain = AudioMixer.calculateDistanceGain(listenerX, listenerZ, srcX, srcZ, minAudible, maxAudible);
        if (distGain <= 0.01f) return;

        float pan = AudioMixer.calculateStereoPan(listenerYaw, listenerX, listenerZ, srcX, srcZ);
        float eff = getEffectiveSfxVolume() * distGain;
        float[] ch = AudioMixer.calculateChannelVolumes(eff, pan);

        Integer soundId = sfxMap.get(sfxId);
        if (soundId != null) {
            soundPool.play(soundId, ch[0], ch[1], 1, 0, 1.0f);
            audioMixer.recordPlay(sfxId, now);
        }
    }

    // ------------------------------------------------------------------
    // Continuous audio loops (Breathing, Heartbeat, Vehicle Engine)
    // ------------------------------------------------------------------
    public void startHeavyBreathing() {
        if (breathingStreamId != 0 || isMuted || soundPool == null) return;
        Integer soundId = sfxMap.get(SFX_HEAVY_BREATH);
        if (soundId != null) {
            float eff = getEffectiveSfxVolume() * 0.7f;
            breathingStreamId = soundPool.play(soundId, eff, eff, 1, -1, 1.0f);
        }
    }

    public void stopHeavyBreathing() {
        if (breathingStreamId != 0 && soundPool != null) {
            soundPool.stop(breathingStreamId);
            breathingStreamId = 0;
        }
    }

    public void startHeartbeat() {
        if (heartbeatStreamId != 0 || isMuted || soundPool == null) return;
        Integer soundId = sfxMap.get(SFX_HEARTBEAT);
        if (soundId != null) {
            float eff = getEffectiveSfxVolume() * 0.9f;
            heartbeatStreamId = soundPool.play(soundId, eff, eff, 2, -1, 1.0f);
        }
    }

    public void stopHeartbeat() {
        if (heartbeatStreamId != 0 && soundPool != null) {
            soundPool.stop(heartbeatStreamId);
            heartbeatStreamId = 0;
        }
    }

    public void startVehicleEngine() {
        if (vehicleEngineStreamId != 0 || isMuted || soundPool == null) return;
        Integer soundId = sfxMap.get(SFX_VEHICLE_ENGINE);
        if (soundId != null) {
            float eff = getEffectiveSfxVolume() * 0.40f;
            vehicleEngineStreamId = soundPool.play(soundId, eff, eff, 3, -1, 0.70f);
        }
    }

    public void updateVehicleEngine(float currentSpeed, float maxSpeed, boolean isOccupied) {
        if (vehicleEngineStreamId == 0 || soundPool == null) {
            if (isOccupied) startVehicleEngine();
            return;
        }
        if (!isOccupied) {
            stopVehicleEngine();
            return;
        }
        float pitch = AudioMixer.calculateEnginePitch(currentSpeed, maxSpeed);
        float vol = getEffectiveSfxVolume() * AudioMixer.calculateEngineVolume(currentSpeed, maxSpeed, isOccupied);
        soundPool.setRate(vehicleEngineStreamId, pitch);
        soundPool.setVolume(vehicleEngineStreamId, vol, vol);
    }

    public void stopVehicleEngine() {
        if (vehicleEngineStreamId != 0 && soundPool != null) {
            soundPool.stop(vehicleEngineStreamId);
            vehicleEngineStreamId = 0;
        }
    }

    // ------------------------------------------------------------------
    // Background Music (MediaPlayer)
    // ------------------------------------------------------------------
    public void playMusic(String assetTrack) {
        stopMusic();
        try {
            musicPlayer = new MediaPlayer();
            AssetFileDescriptor afd = context.getAssets().openFd(assetTrack);
            musicPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            musicPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            float eff = getEffectiveMusicVolume();
            musicPlayer.setVolume(eff, eff);
            musicPlayer.setLooping(true);
            musicPlayer.prepare();
            musicPlayer.start();
        } catch (Exception e) {
            // Silently fallback if music not found
        }
    }

    public void stopMusic() {
        if (musicPlayer != null) {
            try {
                if (musicPlayer.isPlaying()) {
                    musicPlayer.stop();
                }
                musicPlayer.release();
            } catch (Exception ignored) {}
            musicPlayer = null;
        }
    }

    public boolean musicAvailable() {
        return musicPlayer != null;
    }

    public void pause() {
        if (musicPlayer != null && musicPlayer.isPlaying()) {
            musicPlayer.pause();
        }
        stopHeavyBreathing();
        stopHeartbeat();
        stopVehicleEngine();
    }

    public void resume() {
        if (musicPlayer != null) {
            musicPlayer.start();
        }
    }

    public void release() {
        stopMusic();
        stopHeavyBreathing();
        stopHeartbeat();
        stopVehicleEngine();
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
    }
}
