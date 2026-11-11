# REALWORLD SURVIVAL 3D — Dual-Mode Master Edition

A complete, playable 3D rooftop-parkour & 896m open-world survival game for Android, written in pure
Java with a custom GLES2 renderer. No third-party game engine — the renderer, physics, AI, UI, audio,
spatial culling, road network, Dijkstra navigation, inventory, drivable vehicles, and build pipeline
are 100% original code.

## At a Glance

| | |
|---|---|
| **APK** | `RealWorldSurvival3D.apk` at the project root and in `android-build/` |
| **APK size** | 22,458,892 bytes (~22.4 MB) |
| **SHA-256** | `899782e6ef913597d196fa4622fa6d37cbc69e4a7ce45c247c115824e2900023` |
| **Package** | `com.devagent.realworldsurvival3d` · versionCode 2 · versionName 1.1 |
| **Android** | minSdk 24 (Android 7.0), targetSdk 34 · landscape · GLES 2.0 |
| **Permissions** | `VIBRATE` only (no internet, no intrusive permissions) |
| **Signing** | Signed with v1 + v2 + v3 schemes (4-byte zipaligned) |
| **Architecture** | Java (39 classes, ~16,000 lines), compiled with javac, dexed with d8 |
| **Testing** | **3,362 checks pass / 0 fail** across 16 test suites (`sh tests/run.sh`) |

---

## Dual-Mode Gameplay Architecture

The game features two complete, seamless gameplay modes:

### 1. Rooftop Parkour Campaign
- High-intensity parkour course across multi-tier skyscraper rooftops (Alpha, Beta, Gamma, Delta, Epsilon, Apex).
- Complete locomotion: walk, jog, run, turbo sprint, jump, double jump, wall-run (with Dutch angle roll), vault, slide, high-fall safety roll (negates landing damage).
- Drop stomp combat bouncing atop infected Hunter enemies, slide tackle stuns.
- Lethal toxic hazard sludge on the streets below; extraction helipad console broadcast.

### 2. 896m x 896m Open-World Survival
- Continuous terrain heightfield (-448m to +448m) with 16 streaming chunks and 3x3 active chunk windows.
- 6 Diverse Biomes/Regions: City Core, Central Plaza safe haven, Residential District, Industrial Park, North Hills ridges, West Forest, and Riverside Village.
- Deep river barrier with passable shallow shallows and crossable 68m River Bridge deck.
- Walkable architectural interiors with solid walls, ceilings, doorways, and survival props for key POIs (Safehouse, Workshop, Garage, Ranger Hut).
- Dynamic 8-minute Day/Night diurnal cycle with orbital sun/moon lighting and atmospheric weather presets.
- 33-node road network with Dijkstra shortest path navigation, HUD minimap heading-up projection, and >12m deviation auto-rerouting.
- Civilian NPCs with proximity conversational dialogue and safe haven protections.
- Hostile infected stalkers with aggro detection, pursuit, safe zone deflection (34m plaza buffer), and staggered update rate budgeting.
- "The Long Signal" 6-mission story arc + 5 collectible hidden data slates.
- 12-slot backpack inventory with item stacking, usable consumables (medkit healing, energy drink stamina/adrenaline boosts), and quest item protection.
- Drivable land vehicles (Scout SUV & Trail Bike Motorcycle) with forward/reverse throttle, velocity-proportional steering, handbrake drift, obstacle rebound bounce, and hostile roadkill ramming.

---

## Controls

| Control | Location | Action |
|---|---|---|
| Analog joystick | Left 45% of screen | 360° locomotion / vehicle throttle & steering |
| Camera swipe | Right 55% of screen | Look yaw/pitch (touch sensitivity & invert-Y in Settings) |
| JUMP | Bottom-right | Jump / double jump / wall-kick |
| VAULT | Bottom-right | Vault over waist-high barriers while running |
| SLIDE | Bottom-right | Crouch knee-slide; landing safety roll; vehicle handbrake drift |
| CROUCH | Bottom-right | Low-clearance crouch crawl under obstacles |
| SPRINT | Bottom-right | Turbo sprint toggle (consumes stamina) |
| INTERACT | Mid-right (contextual) | Enter/exit vehicles, talk to NPCs, collect items/data slates, activate consoles |
| MAP | Top-right | Fullscreen 2D interactive topological map overlay |
| CAM | Top-right | 3rd person ↔ 1st person view |
| PAUSE | Top-right | Pause menu (settings, save game, audio controls, graphics presets) |

---

## Performance & Optimization

- **Zero Per-Frame Allocations**: Hot render loops use pre-allocated scratch matrices and analytical in-place TRS calculations (`CullingOptimizer.setTRS`), eliminating runtime GC hitching.
- **Spatial Frustum Culling**: Objects outside the camera FOV cone, behind the camera plane, or beyond max draw distance are culled before rendering.
- **Staggered AI Budget**: Entities within 40m update every frame; mid-range (40-100m) update every 5th frame with 5x dt; distant entities (>100m) are frozen.
- **Physics Delta Clamping**: Frame delta time is clamped between 1ms and 100ms, preventing tunneling or explosive physics during background hitches.
- **Atomic Persistence**: Game state is saved to a temporary file (`.tmp`), synced to disk descriptor, rotated to backup (`.bak`), and verified with CRC32 checksums for 100% corruption resilience.

---

## Build & Test Verification

```bash
cd /data/data/com.termux/files/home/realworld-survival-3d

# Run the complete 16-suite JVM regression test suite (3,362 checks)
sh tests/run.sh

# Build, align, and sign the Android APK
./export-and-build.sh debug
```

Install on device:
```bash
termux-open RealWorldSurvival3D.apk
# Or via adb:
adb install -r RealWorldSurvival3D.apk
```
