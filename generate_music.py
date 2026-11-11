#!/usr/bin/env python3
"""
RealWorld Survival 3D - Music Generator (numpy edition)
Generates the five background music tracks as 90-second seamless loops in
mono 22.05 kHz 16-bit WAV (~4 MB each). Replaces the original pure-python
generator's music stage (which produced 33 MB stereo files per track).

Every pattern is an integer number of bars long, and note envelopes reset
per note, so each track wraps with no click. Run: python3 generate_music.py
"""
import os
import struct
import wave

import numpy as np

SR = 22050
LOOP_SECONDS = 90.0
MUSIC_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                         "android-build", "assets", "audio", "music")

rng = np.random.default_rng(20261110)


def note(freq, dur, kind="sine", decay=8.0, gain=1.0, harm=[(0.0, 0.0)]):
    n = int(dur * SR)
    t = np.arange(n, dtype=np.float64) / SR
    env = np.exp(-decay * t)
    wavef = np.sin(2.0 * np.pi * freq * t)
    for mult, amp in harm:
        wavef = wavef + amp * np.sin(2.0 * np.pi * freq * mult * t)
    if kind == "noise":
        wavef = rng.uniform(-1.0, 1.0, n)
    return gain * env * wavef


def kick(dur=0.30):
    n = int(dur * SR)
    t = np.arange(n) / SR
    sweep = 140.0 * np.exp(-18.0 * t) + 42.0
    phase = 2.0 * np.pi * np.cumsum(sweep) / SR
    env = np.exp(-11.0 * t)
    return env * np.sin(phase) * 0.9


def snare(dur=0.22):
    n = int(dur * SR)
    t = np.arange(n) / SR
    noise = rng.uniform(-1.0, 1.0, n) * np.exp(-18.0 * t) * 0.7
    ring = np.sin(2.0 * np.pi * 196.0 * t) * np.exp(-22.0 * t) * 0.3
    return (noise + ring) * 0.8


def hihat(dur=0.09):
    n = int(dur * SR)
    t = np.arange(n) / SR
    noise = rng.uniform(-1.0, 1.0, n)
    bright = np.diff(np.concatenate([[0.0], noise]))  # simple highpass
    return bright * np.exp(-55.0 * t) * 0.9


def place(buf, sample, at):
    end = min(len(buf), at + len(sample))
    if at < len(buf):
        buf[at:end] += sample[:end - at]


def synth_track(bpm, bars, compose):
    """compose(track_buf, beat_index, beat_pos_fn) fills a track of `bars` bars."""
    beat = 60.0 / bpm
    total = int(LOOP_SECONDS * SR)
    buf = np.zeros(total, dtype=np.float64)
    frames_per_beat = int(beat * SR)
    n_beats = int(total / frames_per_beat)
    for b in range(n_beats):
        compose(buf, b, b * frames_per_beat, frames_per_beat)
    return buf


def style_parkour(buf, b, at, fpb):
    sub = b % 4
    if sub in (0, 2):
        place(buf, kick(), at)
    if sub == 2:
        place(buf, snare(), at)
    place(buf, hihat() * (0.6 if sub % 2 else 1.0), at + int(fpb * 0.5))
    scale = [146.83, 174.61, 196.00, 220.00, 261.63, 293.66, 349.23, 392.00]
    root = scale[(b // 4) % len(scale)]
    place(buf, note(root, 0.9, decay=3.5, gain=0.34,
                    harm=[(2.0, 0.35)], kind="sine"), at)
    for e in range(4):
        f = scale[(b * 3 + e * 2) % len(scale)] * 2.0
        place(buf, note(f, 0.16, decay=14.0, gain=0.16, harm=[(2.0, 0.3)]),
              at + int(fpb * (e + 0.5)) if e > 0 else at + int(fpb * 0.5))
    pad = np.sin(2.0 * np.pi * 110.0 * np.arange(fpb) / SR) * 0.05
    pad += np.sin(2.0 * np.pi * 165.0 * np.arange(fpb) / SR) * 0.035
    place(buf, pad, at)


def style_chase(buf, b, at, fpb):
    sub = b % 4
    place(buf, kick(), at)
    if sub in (1, 3):
        place(buf, snare(), at)
    place(buf, hihat(0.07), at + int(fpb * 0.25))
    place(buf, hihat(0.07), at + int(fpb * 0.75))
    scale = [130.81, 138.59, 164.81, 196.00, 207.65, 261.63, 277.18, 329.63]
    root = scale[(b // 2) % len(scale)]
    place(buf, note(root, 0.5, decay=6.0, gain=0.36, harm=[(2.0, 0.3)]), at)
    for e in range(2):
        f = scale[(b * 5 + e) % len(scale)] * 2.0
        place(buf, note(f, 0.20, decay=11.0, gain=0.18, harm=[(3.0, 0.2)]),
              at + int(fpb * e))
    drone = np.sin(2.0 * np.pi * 65.41 * np.arange(fpb) / SR) * 0.10
    place(buf, drone, at)


def style_ambient(buf, b, at, fpb):
    sub = b % 8
    if sub == 0:
        place(buf, kick(0.5) * 0.35, at)
    scale = [110.00, 164.81, 220.00, 246.94, 329.63, 440.00, 493.88, 659.25]
    root = scale[(b // 8) % 4]
    n = fpb * 4
    t = np.arange(n) / SR
    pad = np.sin(2.0 * np.pi * root * t) * 0.12
    pad += np.sin(2.0 * np.pi * root * 1.5 * t) * 0.09
    pad += np.sin(2.0 * np.pi * root * 2.0 * t + 1.3) * 0.05
    lfo = 1.0 + 0.25 * np.sin(2.0 * np.pi * t / 6.0)
    place(buf, pad * lfo * np.exp(-0.06 * t) * 0.5, at)
    if sub in (4, 6):
        f = scale[(b * 2 + 5) % len(scale)]
        place(buf, note(f, 2.5, decay=1.8, gain=0.10, harm=[(2.0, 0.4)]), at)


def style_apex(buf, b, at, fpb):
    sub = b % 4
    if sub in (0, 2):
        place(buf, kick(), at)
    if sub == 3:
        place(buf, snare() * 0.8, at)
    place(buf, hihat() * 0.8, at + int(fpb * 0.5))
    scale = [146.83, 164.81, 196.00, 220.00, 246.94, 293.66, 329.63, 392.00]
    root = scale[(b // 8) % len(scale)]
    place(buf, note(root, 0.9, decay=3.0, gain=0.34, harm=[(2.0, 0.3)]), at)
    if b % 4 == 0:
        place(buf, note(root * 2.0, 1.8, decay=2.2, gain=0.12,
                        harm=[(2.0, 0.5)]), at)
    for e in range(4):
        f = scale[(b * 7 + e * 3) % len(scale)] * 2.0
        place(buf, note(f, 0.15, decay=15.0, gain=0.13), at + int(fpb * e))


def style_freerun(buf, b, at, fpb):
    sub = b % 4
    if sub == 0:
        place(buf, kick(), at)
    if sub == 2:
        place(buf, kick() * 0.7, at)
        place(buf, snare() * 0.6, at)
    if sub % 2 == 1:
        place(buf, hihat(), at + int(fpb * 0.5))
    scale = [174.61, 196.00, 220.00, 246.94, 293.66, 329.63, 392.00, 440.00]
    root = scale[(b // 4) % len(scale)]
    place(buf, note(root, 0.9, decay=3.2, gain=0.30, harm=[(2.0, 0.3)]), at)
    for e in range(4):
        f = scale[(b * 4 + e) % len(scale)] * 2.0
        place(buf, note(f, 0.18, decay=12.0, gain=0.14, harm=[(2.0, 0.25)]),
              at + int(fpb * 0.5 * e))


TRACKS = [
    ("bgm_parkour_rush.wav", 144.0, style_parkour),
    ("bgm_chase.wav", 160.0, style_chase),
    ("bgm_survival_ambient.wav", 96.0, style_ambient),
    ("bgm_apex_tower.wav", 128.0, style_apex),
    ("bgm_free_run.wav", 120.0, style_freerun),
]


def main():
    os.makedirs(MUSIC_DIR, exist_ok=True)
    for name, bpm, style in TRACKS:
        buf = synth_track(bpm, 0, style)
        peak = np.max(np.abs(buf)) or 1.0
        buf = buf / peak * 0.82
        ints = (buf * 32767.0).astype("<i2")
        path = os.path.join(MUSIC_DIR, name)
        with wave.open(path, "wb") as wf:
            wf.setnchannels(1)
            wf.setsampwidth(2)
            wf.setframerate(SR)
            wf.writeframes(ints.tobytes())
        print(f"{name}: {os.path.getsize(path):,} bytes "
              f"({LOOP_SECONDS:.0f}s loop @ {SR} Hz mono, {bpm:.0f} BPM)")


if __name__ == "__main__":
    main()
