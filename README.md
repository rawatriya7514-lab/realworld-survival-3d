# REALWORLD SURVIVAL 3D - PARKOUR EDITION

An intensive 3D Third-Person & First-Person Rooftop Parkour Survival Game built natively for Android.

## Project Overview
- **Project Path:** `/data/data/com.termux/files/home/realworld-survival-3d/`
- **Native APK:** `RealWorldSurvival3D.apk` (**169,738,058 bytes / ~162 MB**)
- **Package Name:** `com.devagent.realworldsurvival3d`
- **Android Target:** Min SDK 24 (Android 7.0), Target SDK 34 (Android 14)
- **Signature Schemes:** Signed with APK Signature Scheme v2 & v3 (Verified)

---

## 🏃 Deep Research & Parkour Mechanics Breakdown

Inspired by *Mirror's Edge*, *Dying Light*, and *Vector*, RealWorld Survival 3D Parkour Edition implements a comprehensive physics-based acrobatic survival engine:

### 1. Locomotion & Momentum
- **Ground Movement:** Smooth analog walk, run, and turbo sprint with inertia and momentum preservation.
- **Dynamic FOV Expansion:** Perspective field of view zooms from 65° to 82° during sprint, wall-runs, and adrenaline bursts for an authentic sense of speed.
- **Head Bob & Camera Tilt:** Subtle rhythmic bobbing during strides and landing compression dips.

### 2. Full Parkour Actions
- **Wall Running (Left & Right):**
  - When leaping near vertical building faces or billboards, runner attaches horizontally to wall surfaces.
  - 75% gravity reduction while gliding forward along the wall at 12.5 m/s for up to 2.0 seconds.
  - Dutch-angle dynamic camera roll (±15°) immersion.
  - **Wall Kick Jump:** Tapping Jump during a wall run launches the runner off the wall with high outward/forward impulse to bridge massive rooftop gaps.
- **Ledge Grab & Mantling:**
  - Forward ray & box detection detects roof ledges while airborne.
  - Character grips ledges and smoothly mantles up onto the rooftop surface with sine-interpolated biomechanical motion.
- **Hurdle Vaulting:**
  - Sprinting toward waist-high obstacles (AC units, concrete parapets, solar racks) initiates a high-momentum hurdle vault without losing velocity.
- **Knee Slide:**
  - Tapping Slide while sprinting executes a low-clearance baseball slide, dropping the collision height by 60% to slip under low pipes and barriers.
  - Can be canceled into a Slide-Kick jump to tackle pursuing enemies!
- **High-Fall Safety Roll (0 Fall Damage):**
  - Dropping from heights > 5.0m will severely injure or kill the runner unless they execute a safety parkour roll!
  - Tapping Slide/Roll upon landing performs a tuck-and-roll, converting vertical kinetic energy into an explosive forward sprint with **0 damage taken**.
  - Unrolled landings cause severe fall damage (8 HP per meter), camera bone-shake, and hard land stun.

### 3. Survival & Vital Systems
- **Stamina Dynamics:**
  - 100 Stamina pool consumed by sprint (-12/s), wall run (-14/s), vault (-8), mantle (-8), slide (-10).
  - Stamina < 20% triggers heavy breathing audio loop and disables sprinting.
  - Rapid regeneration (+18/s) when walking or resting.
- **Adrenaline Combo Multiplier:**
  - Chaining parkour maneuvers (Vault -> Wall Run -> Leap -> Roll) builds a combo multiplier (x2, x3, x5).
  - 100 Adrenaline triggers **Adrenaline Rush**: 1.25x speed boost, golden motion trail, and temporary infinite stamina!
- **Hostile Rooftop Chasers (Hunter Stalkers):**
  - Intelligent AI that patrols skyscrapers, spots the runner, sounds alarm shrieks, and pursues across rooftops.
  - Can leap across rooftop gaps.
  - Player can stun/eliminate chasers using high-altitude drop kicks or slide tackles!
- **Survival Scavenging:**
  - **Energy Drinks:** Instant max stamina + adrenaline boost.
  - **Survival Medkits:** Restores +45 Health.
  - **Parkour Tokens:** Collectible tokens rewarding agility across narrow balance beams.
  - **Data Drives:** Extraction keys required for high-tier missions.

---

## 🎮 Mobile Controls Guide

| Control | Location | Action |
|---|---|---|
| **Analog Joystick** | Bottom-Left Area | 360° Omnidirectional movement |
| **Camera Swipe** | Right Screen Area | Pitch (-80° to 80°) and Yaw (360°) look |
| **JUMP Button** | Bottom-Right (Blue) | Ground jump, Double-jump, Wall-kick, Mantle |
| **VAULT Button** | Bottom-Right (Yellow) | Contextual vault over waist-high obstacles |
| **SLIDE / ROLL** | Bottom-Right (Orange) | Low knee slide & high-fall landing roll |
| **SPRINT Toggle** | Bottom-Right (Red/Grey) | Turbo sprint toggle (consumes stamina) |
| **CAM Switch** | Top-Right (Grey) | Switch between 3rd-Person & 1st-Person view |

---

## 🎧 High-Fidelity Audio Soundbank & Studio Soundtracks

The package bundles **over 160 MB** of uncompressed 44.1kHz 16-bit studio audio:

### Music Tracks (`assets/audio/music/`):
1. `bgm_parkour_rush.wav` (142 BPM Breakbeat / Synthwave parkour theme)
2. `bgm_chase.wav` (160 BPM Tense dark Phrygian pursuit rhythm)
3. `bgm_survival_ambient.wav` (95 BPM Rooftop wind & atmospheric synth pads)
4. `bgm_apex_tower.wav` (High-octane apex extraction theme)
5. `bgm_free_run.wav` (Endless rooftop parkour free-run soundtrack)

### Sound Effects (`assets/audio/sfx/`):
- `footstep_concrete.wav`, `footstep_metal.wav`
- `jump.wav`, `double_jump.wav`
- `wallrun.wav` (Continuous friction skid loop)
- `vault.wav`, `ledge_grab.wav`, `mantle.wav`
- `slide.wav`, `roll.wav`
- `hard_land.wav`, `heavy_breath.wav`, `heartbeat.wav`
- `pickup_drink.wav`, `pickup_medkit.wav`, `pickup_token.wav`
- `chaser_roar.wav`, `level_win.wav`, `game_over.wav`

---

## 📦 Build & Installation

### Quick Build (Native Android Toolchain)
```bash
cd /data/data/com.termux/files/home/realworld-survival-3d
./export-and-build.sh debug
```

### Install to Android Device
```bash
adb install RealWorldSurvival3D.apk
```
Or open the APK directly on Android via Termux or file manager.
