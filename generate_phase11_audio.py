#!/usr/bin/env python3
"""
Generates new Phase 11 sound effects:
- vehicle_engine.wav
- vehicle_horn.wav
- vehicle_crash.wav
- vehicle_skid.wav
- npc_greet.wav
- quest_complete.wav
- combat_hit.wav
- data_slate.wav
"""
import os
import math
import struct
import wave
import random

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
SFX_DIR = os.path.join(BASE_DIR, "android-build", "assets", "audio", "sfx")
os.makedirs(SFX_DIR, exist_ok=True)

def write_wav(filename, samples, sample_rate=44100):
    filepath = os.path.join(SFX_DIR, filename)
    with wave.open(filepath, 'w') as wf:
        wf.setnchannels(1)
        wf.setsampwidth(2)
        wf.setframerate(sample_rate)
        packed = bytearray()
        for s in samples:
            val = max(-32767, min(32767, int(s)))
            packed += struct.pack('<h', val)
        wf.writeframes(packed)
    print(f"Generated {filename} ({len(samples)} samples, {os.path.getsize(filepath):,} bytes)")

def gen_vehicle_engine(sample_rate=44100, duration=0.8):
    # Seamless looping cylinder combustion pulses (75 Hz fundamental + harmonics + exhaust noise)
    total = int(sample_rate * duration)
    samples = []
    base_f = 75.0
    for i in range(total):
        t = i / sample_rate
        # Fundamental + 2nd & 3rd harmonics
        h1 = math.sin(2.0 * math.pi * base_f * t)
        h2 = math.sin(2.0 * math.pi * (base_f * 2.0) * t) * 0.55
        h3 = math.sin(2.0 * math.pi * (base_f * 3.0) * t) * 0.35
        # Low-frequency rumble noise
        noise = (random.random() * 2.0 - 1.0) * 0.25
        val = (h1 + h2 + h3 + noise) * 18000.0
        samples.append(val)
    return samples

def gen_vehicle_horn(sample_rate=44100, duration=0.6):
    # Classic European dual-tone automotive horn (435 Hz + 520 Hz)
    total = int(sample_rate * duration)
    samples = []
    for i in range(total):
        t = i / sample_rate
        # Envelope: fast 0.05s attack, sustain, 0.08s release
        if t < 0.05:
            env = t / 0.05
        elif t > duration - 0.08:
            env = (duration - t) / 0.08
        else:
            env = 1.0
        tone1 = math.sin(2.0 * math.pi * 435.0 * t)
        tone2 = math.sin(2.0 * math.pi * 520.0 * t) * 0.8
        tone3 = math.sin(2.0 * math.pi * 870.0 * t) * 0.2
        val = (tone1 + tone2 + tone3) * env * 24000.0
        samples.append(val)
    return samples

def gen_vehicle_crash(sample_rate=44100, duration=0.55):
    # Metal collision crunch + low frequency chassis thud
    total = int(sample_rate * duration)
    samples = []
    for i in range(total):
        t = i / sample_rate
        env = math.exp(-12.0 * t)
        crunch = (random.random() * 2.0 - 1.0) * env * 0.7
        thud = math.sin(2.0 * math.pi * 65.0 * (1.0 - t * 1.5) * t) * math.exp(-8.0 * t) * 0.8
        metal_ring = math.sin(2.0 * math.pi * 780.0 * t) * math.exp(-22.0 * t) * 0.4
        val = (crunch + thud + metal_ring) * 28000.0
        samples.append(val)
    return samples

def gen_vehicle_skid(sample_rate=44100, duration=0.5):
    # Rubber tire friction screech on asphalt
    total = int(sample_rate * duration)
    samples = []
    for i in range(total):
        t = i / sample_rate
        env = math.sin(math.pi * (t / duration)) ** 0.8
        screech1 = math.sin(2.0 * math.pi * (820.0 + 150.0 * math.sin(40.0 * t)) * t) * 0.5
        screech2 = math.sin(2.0 * math.pi * (1350.0 + 200.0 * math.cos(35.0 * t)) * t) * 0.35
        friction = (random.random() * 2.0 - 1.0) * 0.45
        val = (screech1 + screech2 + friction) * env * 22000.0
        samples.append(val)
    return samples

def gen_npc_greet(sample_rate=44100, duration=0.3):
    # Friendly electronic radio chirp
    total = int(sample_rate * duration)
    samples = []
    for i in range(total):
        t = i / sample_rate
        freq = 600.0 + 600.0 * (t / duration)
        env = math.sin(math.pi * (t / duration)) ** 1.3
        val = math.sin(2.0 * math.pi * freq * t) * env * 22000.0
        samples.append(val)
    return samples

def gen_quest_complete(sample_rate=44100, duration=0.8):
    # Victorious ascending 4-note chime fanfare (C5, E5, G5, C6)
    total = int(sample_rate * duration)
    samples = []
    notes = [523.25, 659.25, 783.99, 1046.50]
    note_dur = duration / len(notes)
    for i in range(total):
        t = i / sample_rate
        idx = min(len(notes) - 1, int(t / note_dur))
        freq = notes[idx]
        note_t = t - idx * note_dur
        env = math.exp(-4.5 * note_t) * (1.0 - math.exp(-40.0 * note_t))
        val = math.sin(2.0 * math.pi * freq * t) * env * 24000.0
        samples.append(val)
    return samples

def gen_combat_hit(sample_rate=44100, duration=0.3):
    # Punchy melee tackle / drop-stomp impact
    total = int(sample_rate * duration)
    samples = []
    for i in range(total):
        t = i / sample_rate
        env = math.exp(-20.0 * t)
        freq = 160.0 - 100.0 * (t / duration)
        thump = math.sin(2.0 * math.pi * freq * t) * env * 0.8
        snap = (random.random() * 2.0 - 1.0) * math.exp(-45.0 * t) * 0.6
        val = (thump + snap) * 27000.0
        samples.append(val)
    return samples

def gen_data_slate(sample_rate=44100, duration=0.5):
    # High-tech collectible chime
    total = int(sample_rate * duration)
    samples = []
    for i in range(total):
        t = i / sample_rate
        env = math.exp(-7.0 * t)
        tone1 = math.sin(2.0 * math.pi * 1174.66 * t) * 0.7
        tone2 = math.sin(2.0 * math.pi * 1760.00 * t) * 0.4
        spark = math.sin(2.0 * math.pi * 2349.32 * t) * 0.2
        val = (tone1 + tone2 + spark) * env * 23000.0
        samples.append(val)
    return samples

if __name__ == "__main__":
    print("=== Generating Phase 11 Audio Assets ===")
    write_wav("vehicle_engine.wav", gen_vehicle_engine())
    write_wav("vehicle_horn.wav", gen_vehicle_horn())
    write_wav("vehicle_crash.wav", gen_vehicle_crash())
    write_wav("vehicle_skid.wav", gen_vehicle_skid())
    write_wav("npc_greet.wav", gen_npc_greet())
    write_wav("quest_complete.wav", gen_quest_complete())
    write_wav("combat_hit.wav", gen_combat_hit())
    write_wav("data_slate.wav", gen_data_slate())
    print("Done!")
