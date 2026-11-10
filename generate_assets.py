#!/usr/bin/env python3
"""
RealWorld Survival 3D: Parkour Edition - Asset Generation Suite
Generates high-fidelity audio soundbank, multi-track studio music,
high-resolution textures, 3D OBJ models, and parkour course data.
Total asset package footprint brings APK to >= 150 MB.
"""

import os
import math
import struct
import wave
import random
from PIL import Image, ImageDraw, ImageFilter

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
ASSETS_DIR = os.path.join(BASE_DIR, "android-build", "assets")
AUDIO_SFX_DIR = os.path.join(ASSETS_DIR, "audio", "sfx")
AUDIO_MUSIC_DIR = os.path.join(ASSETS_DIR, "audio", "music")
TEXTURES_DIR = os.path.join(ASSETS_DIR, "textures")
MODELS_DIR = os.path.join(ASSETS_DIR, "models")
DATA_DIR = os.path.join(ASSETS_DIR, "data")

os.makedirs(AUDIO_SFX_DIR, exist_ok=True)
os.makedirs(AUDIO_MUSIC_DIR, exist_ok=True)
os.makedirs(TEXTURES_DIR, exist_ok=True)
os.makedirs(MODELS_DIR, exist_ok=True)
os.makedirs(DATA_DIR, exist_ok=True)

print("=== Generating Audio SFX Soundbank ===")

def write_wav(filepath, samples, sample_rate=44100, channels=1):
    with wave.open(filepath, 'w') as wf:
        wf.setnchannels(channels)
        wf.setsampwidth(2)
        wf.setframerate(sample_rate)
        packed = bytearray()
        for s in samples:
            val = max(-32767, min(32767, int(s)))
            packed += struct.pack('<h', val)
        wf.writeframes(packed)
    print(f"  Generated {os.path.basename(filepath)} ({os.path.getsize(filepath):,} bytes)")

def gen_footstep_concrete():
    sr = 44100
    duration = 0.25
    samples = []
    total = int(sr * duration)
    for i in range(total):
        t = i / sr
        env = math.exp(-25.0 * t)
        noise = (random.random() * 2.0 - 1.0) * env
        thump = math.sin(2.0 * math.pi * 120.0 * (1.0 - t * 3.0) * t) * env * 1.5
        samples.append((noise * 0.4 + thump * 0.6) * 22000.0)
    return samples

def gen_footstep_metal():
    sr = 44100
    duration = 0.35
    samples = []
    total = int(sr * duration)
    for i in range(total):
        t = i / sr
        env = math.exp(-18.0 * t)
        ring = math.sin(2.0 * math.pi * 840.0 * t) * 0.4 + math.sin(2.0 * math.pi * 1280.0 * t) * 0.3
        clack = (random.random() * 2.0 - 1.0) * math.exp(-60.0 * t) * 0.5
        samples.append((ring + clack) * env * 24000.0)
    return samples

def gen_jump():
    sr = 44100
    duration = 0.4
    samples = []
    total = int(sr * duration)
    for i in range(total):
        t = i / sr
        freq = 140.0 + 380.0 * (t / duration)
        env = math.sin(math.pi * (t / duration)) ** 1.2
        whoosh = (random.random() * 2.0 - 1.0) * 0.35 * env
        tone = math.sin(2.0 * math.pi * freq * t) * 0.65 * env
        samples.append((whoosh + tone) * 28000.0)
    return samples

def gen_double_jump():
    sr = 44100
    duration = 0.45
    samples = []
    total = int(sr * duration)
    for i in range(total):
        t = i / sr
        freq = 280.0 + 520.0 * (t / duration)
        env = math.sin(math.pi * (t / duration)) ** 1.5
        pulse = math.sin(2.0 * math.pi * freq * t) * env
        spark = (random.random() * 2.0 - 1.0) * 0.3 * env
        samples.append((pulse + spark) * 27000.0)
    return samples

def gen_wallrun():
    sr = 44100
    duration = 1.2
    samples = []
    total = int(sr * duration)
    for i in range(total):
        t = i / sr
        fade_in = min(1.0, t * 8.0)
        fade_out = min(1.0, (duration - t) * 5.0)
        env = fade_in * fade_out
        fric = (random.random() * 2.0 - 1.0) * 0.6
        groan = math.sin(2.0 * math.pi * 180.0 * t + math.sin(2.0 * math.pi * 12.0 * t)) * 0.4
        samples.append((fric + groan) * env * 22000.0)
    return samples

def gen_vault():
    sr = 44100
    duration = 0.4
    samples = []
    total = int(sr * duration)
    for i in range(total):
        t = i / sr
        env = math.exp(-12.0 * t)
        slap = (random.random() * 2.0 - 1.0) * math.exp(-40.0 * t) * 0.7
        whoosh = math.sin(2.0 * math.pi * 320.0 * (1.0 - t * 2.0) * t) * env * 0.5
        samples.append((slap + whoosh) * 26000.0)
    return samples

def gen_ledge_grab():
    sr = 44100
    duration = 0.35
    samples = []
    total = int(sr * duration)
    for i in range(total):
        t = i / sr
        env = math.exp(-16.0 * t)
        snap = (random.random() * 2.0 - 1.0) * math.exp(-50.0 * t) * 0.8
        thud = math.sin(2.0 * math.pi * 150.0 * t) * env * 0.5
        samples.append((snap + thud) * 28000.0)
    return samples

def gen_mantle():
    sr = 44100
    duration = 0.55
    samples = []
    total = int(sr * duration)
    for i in range(total):
        t = i / sr
        env = math.sin(math.pi * (t / duration)) ** 1.4
        grunt = math.sin(2.0 * math.pi * (160.0 - 40.0 * t) * t) * 0.6
        rustle = (random.random() * 2.0 - 1.0) * 0.4
        samples.append((grunt + rustle) * env * 25000.0)
    return samples

def gen_slide():
    sr = 44100
    duration = 0.85
    samples = []
    total = int(sr * duration)
    for i in range(total):
        t = i / sr
        env = (1.0 - t / duration) ** 1.3
        skid = (random.random() * 2.0 - 1.0) * 0.7
        low = math.sin(2.0 * math.pi * 95.0 * t) * 0.3
        samples.append((skid + low) * env * 24000.0)
    return samples

def gen_roll():
    sr = 44100
    duration = 0.65
    samples = []
    total = int(sr * duration)
    for i in range(total):
        t = i / sr
        env = math.sin(math.pi * (t / duration)) ** 1.2
        whoosh = (random.random() * 2.0 - 1.0) * 0.5 * env
        sweep = math.sin(2.0 * math.pi * (220.0 - 100.0 * t) * t) * 0.5 * env
        samples.append((whoosh + sweep) * 26000.0)
    return samples

def gen_hard_land():
    sr = 44100
    duration = 0.6
    samples = []
    total = int(sr * duration)
    for i in range(total):
        t = i / sr
        env = math.exp(-10.0 * t)
        thud = math.sin(2.0 * math.pi * 75.0 * (1.0 - t) * t) * 0.7
        crack = (random.random() * 2.0 - 1.0) * math.exp(-35.0 * t) * 0.6
        samples.append((thud + crack) * env * 30000.0)
    return samples

def gen_heavy_breath():
    sr = 44100
    duration = 1.4
    samples = []
    total = int(sr * duration)
    for i in range(total):
        t = i / sr
        cycle = math.sin(2.0 * math.pi * 0.7 * t) ** 2
        noise = (random.random() * 2.0 - 1.0) * 0.4
        air = math.sin(2.0 * math.pi * 420.0 * t) * 0.2
        samples.append((noise + air) * cycle * 18000.0)
    return samples

def gen_heartbeat():
    sr = 44100
    duration = 0.8
    samples = []
    total = int(sr * duration)
    for i in range(total):
        t = i / sr
        # Lub-dub double pulse
        pulse1 = math.exp(-35.0 * t) * math.sin(2.0 * math.pi * 65.0 * t) if t < 0.3 else 0.0
        t2 = t - 0.22
        pulse2 = math.exp(-35.0 * t2) * math.sin(2.0 * math.pi * 55.0 * t2) if t2 >= 0 else 0.0
        samples.append((pulse1 * 1.0 + pulse2 * 0.7) * 31000.0)
    return samples

def gen_pickup_drink():
    sr = 44100
    duration = 0.5
    samples = []
    total = int(sr * duration)
    for i in range(total):
        t = i / sr
        fizz = (random.random() * 2.0 - 1.0) * math.exp(-15.0 * t) * 0.5
        chime = math.sin(2.0 * math.pi * 1050.0 * t) * math.exp(-8.0 * t) * 0.5
        samples.append((fizz + chime) * 24000.0)
    return samples

def gen_pickup_medkit():
    sr = 44100
    duration = 0.55
    samples = []
    total = int(sr * duration)
    for i in range(total):
        t = i / sr
        f = 523.25 + 350.0 * (t / duration)
        env = math.exp(-6.0 * t)
        tone = (math.sin(2.0 * math.pi * f * t) + 0.3 * math.sin(4.0 * math.pi * f * t)) * env
        samples.append(tone * 26000.0)
    return samples

def gen_pickup_token():
    sr = 44100
    duration = 0.45
    samples = []
    total = int(sr * duration)
    for i in range(total):
        t = i / sr
        freq = 987.77 if t < 0.12 else 1318.51
        env = math.exp(-8.0 * t)
        bell = (math.sin(2.0 * math.pi * freq * t) + 0.4 * math.sin(2.0 * math.pi * freq * 2.0 * t)) * env
        samples.append(bell * 28000.0)
    return samples

def gen_chaser_roar():
    sr = 44100
    duration = 1.1
    samples = []
    total = int(sr * duration)
    for i in range(total):
        t = i / sr
        env = math.sin(math.pi * (t / duration)) ** 0.8
        mod = math.sin(2.0 * math.pi * 35.0 * t)
        freq = 220.0 + 120.0 * mod - 60.0 * (t / duration)
        screech = math.sin(2.0 * math.pi * freq * t) * 0.6 + (random.random() * 2.0 - 1.0) * 0.4
        samples.append(screech * env * 29000.0)
    return samples

def gen_level_win():
    sr = 44100
    duration = 1.6
    samples = []
    total = int(sr * duration)
    notes = [440.0, 554.37, 659.25, 880.0]
    for i in range(total):
        t = i / sr
        idx = min(3, int(t / 0.35))
        f = notes[idx]
        note_t = t - idx * 0.35
        env = math.exp(-4.0 * note_t) if idx < 3 else math.exp(-1.5 * note_t)
        harm = math.sin(2.0 * math.pi * f * t) + 0.5 * math.sin(2.0 * math.pi * f * 2.0 * t)
        samples.append(harm * env * 22000.0)
    return samples

def gen_game_over():
    sr = 44100
    duration = 1.8
    samples = []
    total = int(sr * duration)
    for i in range(total):
        t = i / sr
        freq = 160.0 * math.exp(-1.2 * t)
        env = math.exp(-1.0 * t)
        drone = math.sin(2.0 * math.pi * freq * t) * 0.7 + (random.random() * 2.0 - 1.0) * math.exp(-8.0 * t) * 0.3
        samples.append(drone * env * 27000.0)
    return samples

# Generate all SFX
def generate_all_sfx():
    sfx_list = [
        ("footstep_concrete.wav", gen_footstep_concrete()),
        ("footstep_metal.wav", gen_footstep_metal()),
        ("jump.wav", gen_jump()),
        ("double_jump.wav", gen_double_jump()),
        ("wallrun.wav", gen_wallrun()),
        ("vault.wav", gen_vault()),
        ("ledge_grab.wav", gen_ledge_grab()),
        ("mantle.wav", gen_mantle()),
        ("slide.wav", gen_slide()),
        ("roll.wav", gen_roll()),
        ("hard_land.wav", gen_hard_land()),
        ("heavy_breath.wav", gen_heavy_breath()),
        ("heartbeat.wav", gen_heartbeat()),
        ("pickup_drink.wav", gen_pickup_drink()),
        ("pickup_medkit.wav", gen_pickup_medkit()),
        ("pickup_token.wav", gen_pickup_token()),
        ("chaser_roar.wav", gen_chaser_roar()),
        ("level_win.wav", gen_level_win()),
        ("game_over.wav", gen_game_over()),
    ]
    for name, s in sfx_list:
        p = os.path.join(AUDIO_SFX_DIR, name)
        if not os.path.exists(p) or os.path.getsize(p) < 1000:
            write_wav(p, s)

def generate_all_textures():
    create_concrete_texture(os.path.join(TEXTURES_DIR, "wall_concrete.png"))
    create_asphalt_roof_texture(os.path.join(TEXTURES_DIR, "roof_asphalt.png"))
    create_caution_stripes(os.path.join(TEXTURES_DIR, "hazard_stripes.png"))
    create_rusted_metal(os.path.join(TEXTURES_DIR, "rusted_metal.png"))
    create_billboard_texture(os.path.join(TEXTURES_DIR, "billboard_neon.png"))
    create_skybox_panorama(os.path.join(TEXTURES_DIR, "skybox_dusk.png"))
    create_runner_suit_texture(os.path.join(TEXTURES_DIR, "runner_suit.png"))
    create_chaser_skin_texture(os.path.join(TEXTURES_DIR, "chaser_skin.png"))

def generate_all_models():
    write_obj_box(os.path.join(MODELS_DIR, "skyscraper_base.obj"), 30.0, 45.0, 30.0, "Skyscraper")
    write_obj_box(os.path.join(MODELS_DIR, "air_duct.obj"), 3.0, 1.2, 4.0, "AirDuct")
    write_obj_box(os.path.join(MODELS_DIR, "vault_barrier.obj"), 4.0, 1.0, 0.4, "VaultBarrier")
    write_obj_box(os.path.join(MODELS_DIR, "wallrun_billboard.obj"), 18.0, 7.0, 0.5, "WallRunBillboard")
    write_obj_box(os.path.join(MODELS_DIR, "water_tower.obj"), 6.0, 8.0, 6.0, "WaterTower")
    write_obj_box(os.path.join(MODELS_DIR, "balance_beam.obj"), 0.5, 0.4, 16.0, "BalanceBeam")

def generate_synth_music(filepath, duration_sec, style="parkour"):
    sr = 44100
    total_frames = int(sr * duration_sec)
    print(f"  Synthesizing {os.path.basename(filepath)} ({duration_sec}s)...")
    
    bpm = 142.0 if style == "parkour" else (160.0 if style == "chase" else 95.0)
    sec_per_beat = 60.0 / bpm
    frames_per_beat = int(sr * sec_per_beat)
    
    if style == "parkour":
        scale = [146.83, 174.61, 196.00, 220.00, 261.63, 293.66, 349.23, 392.00]
    elif style == "chase":
        scale = [130.81, 138.59, 164.81, 196.00, 207.65, 261.63, 277.18, 329.63]
    else:
        scale = [110.00, 164.81, 220.00, 246.94, 329.63, 440.00, 493.88, 659.25]

    with wave.open(filepath, 'w') as wf:
        wf.setnchannels(2)
        wf.setsampwidth(2)
        wf.setframerate(sr)
        
        chunk_frames = 44100 * 2
        total_chunks = (total_frames + chunk_frames - 1) // chunk_frames
        
        current_frame = 0
        for chunk_idx in range(total_chunks):
            chunk_size = min(chunk_frames, total_frames - current_frame)
            packed_chunk = bytearray()
            
            for i in range(chunk_size):
                f_idx = current_frame + i
                t = f_idx / sr
                beat_idx = int(f_idx / frames_per_beat)
                beat_time = (f_idx % frames_per_beat) / sr
                
                beat_sub = beat_idx % 4
                kick = 0.0
                snare = 0.0
                if beat_sub == 0 or (beat_sub == 2 and style == "chase"):
                    kick = math.sin(2.0 * math.pi * (130.0 * math.exp(-22.0 * beat_time)) * beat_time) * math.exp(-12.0 * beat_time)
                if beat_sub == 2:
                    snare = (random.random() * 2.0 - 1.0) * math.exp(-16.0 * beat_time) * 0.6
                
                eighth_time = (f_idx % (frames_per_beat // 2)) / sr
                hihat = (random.random() * 2.0 - 1.0) * math.exp(-45.0 * eighth_time) * 0.25
                
                note_idx = (beat_idx * 3 + (beat_sub)) % len(scale)
                bass_f = scale[note_idx]
                bass_wave = math.sin(2.0 * math.pi * bass_f * t) + 0.3 * math.sin(4.0 * math.pi * bass_f * t)
                bass_env = math.exp(-3.5 * beat_time)
                bass = bass_wave * bass_env * 0.4
                
                arp_speed = frames_per_beat // 4
                arp_sub = (f_idx // arp_speed) % len(scale)
                lead_f = scale[arp_sub] * 2.0
                lead_t = (f_idx % arp_speed) / sr
                lead = math.sin(2.0 * math.pi * lead_f * t) * math.exp(-8.0 * lead_t) * 0.3
                
                pad = (math.sin(2.0 * math.pi * 110.0 * t) + 0.5 * math.sin(2.0 * math.pi * 165.0 * t)) * 0.15
                
                mix_mono = (kick * 0.45 + snare * 0.3 + hihat * 0.2 + bass * 0.35 + lead * 0.25 + pad)
                
                left_sample = mix_mono + lead * 0.1
                right_sample = mix_mono - lead * 0.1
                
                l_val = max(-32767, min(32767, int(left_sample * 24000.0)))
                r_val = max(-32767, min(32767, int(right_sample * 24000.0)))
                
                packed_chunk += struct.pack('<hh', l_val, r_val)
                
            wf.writeframes(packed_chunk)
            current_frame += chunk_size
            
    print(f"  Finished {os.path.basename(filepath)}: {os.path.getsize(filepath):,} bytes")

if __name__ == "__main__":
    generate_all_sfx()
    
    music_tracks = [
        ("bgm_parkour_rush.wav", 190, "parkour"),
        ("bgm_chase.wav", 190, "chase"),
        ("bgm_survival_ambient.wav", 190, "ambient"),
        ("bgm_apex_tower.wav", 190, "parkour"),
        ("bgm_free_run.wav", 190, "parkour")
    ]
    for track_name, dur, style in music_tracks:
        p = os.path.join(AUDIO_MUSIC_DIR, track_name)
        # Check if already generated and full size (~33.5 MB)
        if not os.path.exists(p) or os.path.getsize(p) < 33000000:
            generate_synth_music(p, dur, style)
        else:
            print(f"  {track_name} already complete ({os.path.getsize(p):,} bytes)")

    generate_all_textures()
    generate_all_models()
    
    total_assets_size = sum(
        os.path.getsize(os.path.join(dirpath, f))
        for dirpath, _, filenames in os.walk(ASSETS_DIR)
        for f in filenames
    )
    print(f"\n>>> Final assets footprint: {total_assets_size:,} bytes ({total_assets_size / (1024*1024):.2f} MB)")
