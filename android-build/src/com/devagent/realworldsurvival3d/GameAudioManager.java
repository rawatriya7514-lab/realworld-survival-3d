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
 *   - SoundPool for instantaneous parkour sound effects (footsteps, jumps, wall-runs, vaults, mantles, rolls)
 *   - MediaPlayer for dynamic studio background music tracks
 */
public class GameAudioManager {
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

    private Context context;
    private SoundPool soundPool;
    private Map<Integer, Integer> sfxMap;
    private MediaPlayer musicPlayer;
    private float sfxVolume = 0.85f;
    private float musicVolume = 0.65f;
    private int breathingStreamId = 0;
    private int heartbeatStreamId = 0;
    private boolean isMuted = false;

    public GameAudioManager(Context context) {
        this.context = context;
        sfxMap = new HashMap<>();
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
                    .setMaxStreams(12)
                    .setAudioAttributes(attrs)
                    .build();
        } else {
            soundPool = new SoundPool(12, AudioManager.STREAM_MUSIC, 0);
        }
    }

    private void loadAllSfx() {
        loadSfx(SFX_FOOTSTEP, "audio/sfx/footstep_concrete.wav");
        loadSfx(SFX_JUMP, "audio/sfx/jump.wav");
        loadSfx(SFX_DOUBLE_JUMP, "audio/sfx/double_jump.wav");
        loadSfx(SFX_WALLRUN, "audio/sfx/wallrun.wav");
        loadSfx(SFX_VAULT, "audio/sfx/vault.wav");
        loadSfx(SFX_LEDGE_GRAB, "audio/sfx/ledge_grab.wav");
        loadSfx(SFX_MANTLE, "audio/sfx/mantle.wav");
        loadSfx(SFX_SLIDE, "audio/sfx/slide.wav");
        loadSfx(SFX_ROLL, "audio/sfx/roll.wav");
        loadSfx(SFX_HARD_LAND, "audio/sfx/hard_land.wav");
        loadSfx(SFX_HEAVY_BREATH, "audio/sfx/heavy_breath.wav");
        loadSfx(SFX_HEARTBEAT, "audio/sfx/heartbeat.wav");
        loadSfx(SFX_PICKUP_DRINK, "audio/sfx/pickup_drink.wav");
        loadSfx(SFX_PICKUP_MEDKIT, "audio/sfx/pickup_medkit.wav");
        loadSfx(SFX_PICKUP_TOKEN, "audio/sfx/pickup_token.wav");
        loadSfx(SFX_CHASER_ROAR, "audio/sfx/chaser_roar.wav");
        loadSfx(SFX_LEVEL_WIN, "audio/sfx/level_win.wav");
        loadSfx(SFX_GAME_OVER, "audio/sfx/game_over.wav");
    }

    private void loadSfx(int id, String assetPath) {
        try {
            AssetFileDescriptor afd = context.getAssets().openFd(assetPath);
            int soundId = soundPool.load(afd, 1);
            sfxMap.put(id, soundId);
        } catch (Exception e) {
            // Asset might not be packaged yet or fallback
        }
    }

    public void playSfx(int sfxId) {
        if (isMuted || soundPool == null) return;
        Integer soundId = sfxMap.get(sfxId);
        if (soundId != null) {
            soundPool.play(soundId, sfxVolume, sfxVolume, 1, 0, 1.0f);
        }
    }

    public void startHeavyBreathing() {
        if (breathingStreamId != 0 || isMuted || soundPool == null) return;
        Integer soundId = sfxMap.get(SFX_HEAVY_BREATH);
        if (soundId != null) {
            breathingStreamId = soundPool.play(soundId, sfxVolume * 0.7f, sfxVolume * 0.7f, 1, -1, 1.0f);
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
            heartbeatStreamId = soundPool.play(soundId, sfxVolume * 0.9f, sfxVolume * 0.9f, 2, -1, 1.0f);
        }
    }

    public void stopHeartbeat() {
        if (heartbeatStreamId != 0 && soundPool != null) {
            soundPool.stop(heartbeatStreamId);
            heartbeatStreamId = 0;
        }
    }

    public void playMusic(String assetTrack) {
        stopMusic();
        try {
            musicPlayer = new MediaPlayer();
            AssetFileDescriptor afd = context.getAssets().openFd(assetTrack);
            musicPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            musicPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            musicPlayer.setVolume(musicVolume, musicVolume);
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

    /** True when a music track is currently loaded and playable. */
    public boolean musicAvailable() {
        return musicPlayer != null;
    }

    public void setSfxVolume(float vol) {
        this.sfxVolume = vol;
    }

    public void setMusicVolume(float vol) {
        this.musicVolume = vol;
        if (musicPlayer != null) {
            musicPlayer.setVolume(musicVolume, musicVolume);
        }
    }

    public void pause() {
        if (musicPlayer != null && musicPlayer.isPlaying()) {
            musicPlayer.pause();
        }
        stopHeavyBreathing();
        stopHeartbeat();
    }

    public void resume() {
        if (musicPlayer != null) {
            musicPlayer.start();
        }
    }

    public void release() {
        stopMusic();
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
    }
}
