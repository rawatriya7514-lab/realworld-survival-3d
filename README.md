# REALWORLD SURVIVAL 3D — Parkour Edition

A complete, playable 3D rooftop-parkour survival game for Android, written in
Java with a custom GLES2 renderer. No game engine — the renderer, physics,
AI, UI, audio, and build pipeline are all original code in this repository.

## At a Glance

| | |
|---|---|
| **APK** | `RealWorldSurvival3D.apk` at the project root and in `android-build/` |
| **APK size** | 22,007,694 bytes (~21 MB) |
| **Package** | `com.devagent.realworldsurvival3d` · versionCode 2 · versionName 1.1 |
| **Android** | minSdk 24 (Android 7.0), targetSdk 34 · landscape · GLES 2.0 |
| **Permissions** | `VIBRATE` only (no network, no storage access) |
| **Signing** | Debug keystore, verified with APK Signature Schemes v1 + v2 + v3 |
| **Language** | Java (17 classes, ~7,500 lines), compiled with javac 21, dexed with d8 |
| **Testing** | 2,432 JVM logic checks pass / 0 fail (`sh tests/run.sh`) |

## What the Game Is

You are a parkour runner on a lockdown city skyline. Three escalating missions
send you across ten skyscrapers, balance beams, wall-run billboards and fan
boosters while infected rooftop chasers hunt you. Collect survival items,
chain parkour combos to fill your adrenaline meter, and reach (or signal) the
extraction helipad before the chasers get you.

**Missions**

1. *Rooftop Infiltration* — parkour east across the towers to the gold helipad.
2. *Scavenger's High-Rise* — collect 6 survival items, then reach the Apex helipad.
3. *Apex Extraction* — recover 3 data drives, then use the helipad console to
   signal extraction (contextual INTERACT button).

**Systems implemented**

- Parkour movement: walk/run/sprint, jump, double jump, wall-run (left/right)
  with wall-kick, ledge grab + mantle, vault, slide, slide-kick, high-fall
  safety roll, fall damage, stamina, adrenaline combos.
- Hunter chaser AI: roof-bounded patrol, detection, same-roof chase,
  edge-to-edge leaps across short gaps, give-up timer, attacks; defeated by
  drop-stomps and slide tackles (never flies, never teleports).
- Interaction system with on-screen prompts (data drives, extraction console).
- GLES2 renderer: directional sun (Lambert) + hemisphere ambient +
  Blinn-Phong specular, exponential fog, sky gradient, textured surfaces
  (8 POT textures), blob shadows, extraction compass arrow, camera shake/roll,
  dynamic FOV. Performance/High quality presets.
- Audio: SoundPool SFX engine (18 sounds incl. breathing/heartbeat loops) +
  MediaPlayer music (5 seamless loop tracks, per-mission playlists), separate
  music/SFX volume settings.
- Full menu flow: main menu, settings, pause, game over, mission complete,
  about. HUD: health/stamina bars, mission tracker, prompts, minimap-style
  compass (in-GL), damage flash + haptics.
- Persistence: progress save/load (mission, score, currency, items, health,
  best score) and separate settings storage (music/SFX volume, look
  sensitivity, invert-Y, quality, 30/60 FPS limit).

## Controls

| Control | Location | Action |
|---|---|---|
| Analog joystick | left 45% of screen | 360° movement (dynamic base) |
| Camera swipe | right 55% of screen | yaw/pitch look (sensitivity + invert-Y in Settings) |
| JUMP | bottom-right (large) | jump / double jump / wall-kick / slide-kick |
| VAULT | bottom-right | vault waist-high obstacles (while sprinting) |
| SLIDE | bottom-right | knee slide; landing roll (negates fall damage) |
| SPRINT | bottom-right | sprint toggle (stamina) |
| INTERACT | mid-right (contextual) | collect data drives / signal console |
| CAM | top-right | 3rd ↔ 1st person |
| PAUSE | top-right (left of CAM) | pause menu |
| Back button | system | pause/resume in game |

Full details: `controls-guide.md`.

## Build & Install

```bash
cd /data/data/com.termux/files/home/realworld-survival-3d
./export-and-build.sh debug        # produces RealWorldSurvival3D.apk
sh tests/run.sh                    # optional: run the 2,432-check logic suite
```

Install on a device: `adb install -r RealWorldSurvival3D.apk`, or copy the APK
to the device and open it (allow "install unknown apps" when prompted). On this
Termux device, `termux-open RealWorldSurvival3D.apk` hands the APK to the
system package installer.

## Project Layout

```
android-build/            Native Android app (the shipped game)
  src/.../*.java          17 game classes (renderer, physics, AI, UI, audio…)
  AndroidManifest.xml     minSdk 24, targetSdk 34, VIBRATE only
  res/                    theme, strings, icon mipmaps
  assets/                 8 textures, 6 OBJ props, 5 music loops, 17 SFX
export-and-build.sh       javac → d8 → aapt2 → dex-inject → zipalign → apksigner
generate_music.py         Regenerates the 5 music loops (canonical source)
generate_assets.py        Procedural texture/SFX/model generator
tests/GameLogicTest.java  2,432-check JVM suite for physics/AI/world logic
tests/run.sh              Test runner (pure JVM, no device needed)
```

The `project.godot`, `scenes/`, `scripts/`, `shaders/` folders are an unused
Godot 4 skeleton kept for reference; Godot cannot run in this environment
(see `known-limitations.md`). The native Android build is the shipped game.

## Documentation

- `build-instructions.md` — full build pipeline
- `controls-guide.md` — touch layout and button behavior
- `feature-list.md` — feature checklist with verification status
- `testing-results.md` — test results (JVM tests pass; device tests not run)
- `known-limitations.md` — honest current limits
- `QA_AUDIT_REPORT.md` — quality audit history and bug fixes
