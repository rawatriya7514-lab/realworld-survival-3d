# REALWORLD 3D — Technical Architecture

**Version:** 2.0 · **Date:** 2026-11-11 · **Status:** Phase 0 deliverable

## 1. Engine Decision (Audit Outcome)

| Option | Verdict | Reason |
|---|---|---|
| Godot 4 + GDScript (preferred by brief) | **Rejected** | The `godot` binary in this environment is a broken 186-byte Python shim (`#!/usr/bin/python3.14` → `godot.run`) with no real engine behind it, and no Godot export templates exist. Building/exporting a Godot APK from Termux here is impossible. |
| Unity / Unreal | **Rejected** | Not installed; no licenses; cannot download toolchains of this size reliably. |
| **Native Java + custom GLES2 engine (existing)** | **Selected** | Proven pipeline (javac → d8 → aapt2 → zipalign → apksigner), 4,473 lines of working engine code, all 15 phases implementable, fully testable on a plain JVM for logic, and APK verified end-to-end in this environment. |

Per the brief: *"If the project already uses another suitable engine, evaluate before migrating."*
The existing native engine **is** the suitable engine. Nothing will be migrated; the
engine is **extended** with new modules. Existing functionality (rooftop parkour
campaign, renderer, audio, settings, save) is preserved untouched.

## 2. System Architecture

### 2.1 Process layout
```
GameActivity (Android lifecycle, immersive fullscreen)
 └─ FrameLayout root
     ├─ GameView (GLSurfaceView, GLES2, game loop via onDrawFrame)
     │    ├─ WorldRenderer      — all GPU drawing
     │    ├─ TouchInput         — multi-touch joystick/buttons/look
     │    ├─ (mode) ParkourMode — existing WorldMap/Player/HunterEnemy flow
     │    └─ (mode) OpenWorld   — new open-world simulation (below)
     └─ UIController overlay (Android views: menus, HUD text, map, inventory)
          ├─ GameSettings (SharedPreferences persistence)
          └─ MapScreenView (interactive world map, Canvas 2D)
```

### 2.2 Open-world modules (new code)

| Module | Responsibility | Test strategy |
|---|---|---|
| `Terrain` | Analytic heightfield: hills, river valley, flats; `heightAt/normalAt/slopeAt`; chunk mesh building (triangle strips) | JVM: height continuity, slope bounds, mesh generation |
| `OpenWorld` | World layout: region map + unique region IDs, 64 m modular chunks, streaming set, buildings (with interiors), shops/safe zones, POIs, props (trees, rocks, fences), water areas | JVM: region lookup, chunk load/unload, interior walkability |
| `RoadNetwork` | Road graph (nodes at intersections, edges = segments), nearest-node query, BFS route + distance, blocked/unreachable handling | JVM: routing correctness, unreachable destinations |
| `PlayerController` (extends `Player`) | Ground movement states idle/walk/jog/run/sprint/jump/fall/land/crouch + terrain ground detection, step/ slope handling; reused by parkour mode | JVM: state transitions, gravity, no float/slide-through |
| `CharacterAnimator` | Procedural animation state machine for box-composite characters: per-state limb angles, blend timers, movement-speed matching (fallback animations, identified in GDD §7) | JVM: blending math, state ↔ speed match |
| `CameraRig` | Third-person camera: follow, orbit, zoom, sensitivity, obstruction raycast pull-in, terrain-aware min height | JVM: obstruction clamping math |
| `CivilianNPC` | Idle/walk/patrol/talk/react + route walking; staggered AI updates | JVM: patrol bounds, state flow |
| `HostileAI` (reuses `HunterEnemy` core) | Patrol/detect/chase/attack/damage/death/reset | JVM (existing suite extended) |
| `Vehicle` | Car & motorcycle: throttle/steer/brake/reverse arcade physics, AABB collision, health, enter/exit | JVM: acceleration curve, braking, collision stop, enter/exit |
| `MissionManager` / `Mission` | Data-driven objectives: VISIT / COLLECT / TALK / DRIVE_TO / SURVIVE / RETURN; rewards, failure, progression; linked to world events | JVM: full mission graph progression |
| `Inventory` | Items with quantities + descriptions; add/remove/query; persistence | JVM: stack math |
| `SaveData` | Versioned snapshot: position, region, missions, inventory, discovered POIs, settings; atomic write + default on corruption | JVM: round-trip + corruption recovery |
| `DayNightCycle` | Sun direction + sky/fog/ambient palette over an 8-min cycle; weather-lite (fog variation) | JVM: palette interpolation bounds |
| `UIController` (extended) | Mode select, expanded settings (LOW/MED/HIGH, resolution scale, FPS 30/45/60, button size/opacity, left-handed, master/music/SFX volume, auto-save), map screen, inventory screen | Built + manual; View code |
| `WorldRenderer` (extended) | Terrain chunk VBOs, trees/props, water, character limbs, day/night uniforms, minimap GL overlay | Built; device NOT TESTED |

### 2.2b Implementation status after PHASE 1 (2026-11-11)
- **Built:** `Terrain` (analytic heightfield + 96-vert chunk meshes), `OpenWorld`
  (7 regions w/ unique IDs, 10 POIs, deterministic seed-1337 chunk content,
  3×3 streaming, bridge + pier, safe zone), Player terrain/water integration
  (in-place extension of `Player`, signature unchanged), renderer open-world
  path (atlas terrain VBOs, props, blended water, culling), `GameMode`
  PARKOUR/OPEN_WORLD routing in `GameView`, main-menu mode select.
- **Not yet built (planned phases):** `PlayerController` state machine (Phase 2),
  `CharacterAnimator` (3), `CameraRig` (4), `RoadNetwork` (6), interiors (7),
  NPCs (8), missions (9), vehicles/inventory (10), save (12).
- The planned `PlayerController` subclass was replaced by extending `Player`
  directly with nullable `terrain`/`waterLevel` fields so the parkour campaign
  keeps its exact physics path.

### 2.3 World & coordinate system
- World: **896 m × 896 m** (−448…+448 on X/Z), Y up, origin near the city center.
- Chunk grid: 64 m cells → 14×14 = **196 chunks**, each with stable ID `R<col>_<row>`; regions group chunks (CITY_CORE, RESIDENTIAL, RIVERSIDE_VILLAGE, WEST_FOREST, NORTH_HILLS, RIVER, OUTSKIRTS); each region has a unique integer ID + name.
- Streaming: active set = 3×3 chunks around player (Performance: 2×2); chunks build cached VBOs on load; distant chunks release buffers.
- Road network drives NPC routes, map rendering, and waypoint navigation (recalculated when the player leaves the current route by >12 m). *(Planned — Phase 6; Phase 1 ships 11 hand-placed road strips as non-solid slabs in flat zones.)*

### 2.4 Performance budget (mid-range arm64, 3 GB RAM)
- Target: 60 FPS uncapped / 45 / 30 selectable; LOW preset drops draw distance & vegetation.
- AI updates staggered: <40 m every frame, <100 m every 5th, beyond frozen.
- One draw call per cube via shared VBO; terrain 1 call per chunk (<40 active); characters ≤6 cubes each.
- HUD via cheap 2D shader; text via Android views (GPU cost ~0).

### 2.5 Testing architecture
- **JVM suite** (`tests/run.sh`) — every pure-logic module: physics, terrain, routing, missions, inventory, vehicles, animation math, save round-trip. Gate: exit 0.
- **Build gate** — `./export-and-build.sh debug` must produce a verified APK.
- **Device** — no adb in this environment; on-device behavior is reported as NOT TESTED with a checklist (see TEST_REPORT.md).
