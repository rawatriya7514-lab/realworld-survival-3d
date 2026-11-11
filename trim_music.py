#!/usr/bin/env python3
"""
RealWorld Survival 3D - Music Asset Optimizer
Converts the oversized 190s stereo 44.1kHz music WAVs into
90s seamless-looping mono 22.05kHz WAVs (~4 MB each instead of ~33 MB).
Keeps the game audio loopable with a short crossfold so the wrap point
does not click. Run once after generate_assets.py.
"""
import os
import struct
import sys
import wave

try:
    import numpy as np
except ImportError:
    np = None

MUSIC_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                         "android-build", "assets", "audio", "music")

TARGET_SECONDS = 90.0
TARGET_RATE = 22050
CROSSFOLD_SECONDS = 1.0


def load_wav(path):
    with wave.open(path, "rb") as wf:
        channels = wf.getnchannels()
        width = wf.getsampwidth()
        rate = wf.getframerate()
        frames = wf.readframes(wf.getnframes())
    if width != 2:
        raise SystemExit(f"{path}: expected 16-bit samples")
    if np is not None:
        data = np.frombuffer(frames, dtype="<i2").astype(np.float32)
        if channels == 2:
            data = data.reshape(-1, 2).mean(axis=1)
    else:
        n = len(frames) // 2
        ints = struct.unpack(f"<{n}h", frames)
        if channels == 2:
            mono = [(ints[i * 2] + ints[i * 2 + 1]) / 2.0 for i in range(len(ints) // 2)]
        else:
            mono = [float(v) for v in ints]
        data = mono
    return data, rate


def resample_mean(data, rate, target_rate):
    """Downsample by integer decimation with box averaging."""
    if rate % target_rate != 0:
        raise SystemExit("non-integer decimation not supported")
    factor = rate // target_rate
    if np is not None:
        usable = (len(data) // factor) * factor
        return data[:usable].reshape(-1, factor).mean(axis=1)
    out = []
    for i in range(len(data) // factor):
        chunk = data[i * factor:(i + 1) * factor]
        out.append(sum(chunk) / factor)
    return out


def normalize(data, peak=0.85):
    if np is not None:
        current = np.max(np.abs(data)) or 1.0
    else:
        current = max(abs(min(data)), abs(max(data))) or 1.0
    return data * (peak / current)


def make_seamless_loop(data, rate):
    """Fold the tail into the head so the wrap point is continuous."""
    x = int(CROSSFOLD_SECONDS * rate)
    n = len(data)
    if n <= x * 2:
        return data
    body = data[:n - x]
    tail = data[n - x:]
    head = data[:x]
    if np is not None:
        w = np.linspace(0.0, 1.0, x, dtype=np.float32)
        body[:x] = tail * (1.0 - w) + head * w
    else:
        for i in range(x):
            w = i / float(x)
            body[i] = tail[i] * (1.0 - w) + head[i] * w
    return body


def write_wav(path, data, rate):
    if np is not None:
        ints = np.clip(data, -1.0, 1.0)
        ints = (ints * 32767.0).astype("<i2")
        payload = ints.tobytes()
    else:
        payload = bytearray()
        for s in data:
            v = max(-32767, min(32767, int(s * 32767.0)))
            payload += struct.pack("<h", v)
    with wave.open(path, "wb") as wf:
        wf.setnchannels(1)
        wf.setsampwidth(2)
        wf.setframerate(rate)
        wf.writeframes(payload)


def main():
    if not os.path.isdir(MUSIC_DIR):
        raise SystemExit(f"missing {MUSIC_DIR}")
    for name in sorted(os.listdir(MUSIC_DIR)):
        if not name.endswith(".wav"):
            continue
        path = os.path.join(MUSIC_DIR, name)
        old_size = os.path.getsize(path)
        mono, rate = load_wav(path)
        mono = resample_mean(mono, rate, TARGET_RATE)
        # Truncate to the target loop length before folding the seam
        mono = mono[:int(TARGET_SECONDS * TARGET_RATE)]
        loop = make_seamless_loop(mono, TARGET_RATE)
        loop = normalize(loop)
        tmp = path + ".tmp"
        write_wav(tmp, loop, TARGET_RATE)
        os.replace(tmp, path)
        new_size = os.path.getsize(path)
        print(f"{name}: {old_size:,} -> {new_size:,} bytes "
              f"({os.path.getsize(path)/TARGET_RATE/2:.1f}s @ {TARGET_RATE}Hz mono)")
    total = sum(os.path.getsize(os.path.join(MUSIC_DIR, f)) for f in os.listdir(MUSIC_DIR))
    print(f"Total music: {total:,} bytes ({total/1024/1024:.1f} MB)")


if __name__ == "__main__":
    sys.exit(main())
