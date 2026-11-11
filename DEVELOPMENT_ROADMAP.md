# REALWORLD 3D — Development Roadmap

Phased implementation with mandatory per-phase testing. Status column is kept
current after every phase. Rule: **never mark untested features PASS.**

| Phase | Scope | Status | Checkpoint |
|---|---|---|---|
| 0 | Project audit + architecture docs | **DONE** | `PHASE-0` commit |
| 1 | Game foundation: mode select, open-world scene loads playable | DONE | `PHASE-1` commit |
| 2 | Movement: terrain locomotion, all states, collision | DONE | `PHASE-2` commit |
| 3 | Animations: procedural state machine + blending | DONE | `PHASE-3` commit |
| 4 | Camera: follow, orbit, zoom, collision, terrain-aware | DONE | `PHASE-4` commit |
| 5 | Touch controls: joystick/buttons + layout settings | DONE | `PHASE-5` commit |
| 6 | World map: fullscreen map, minimap, waypoints, navigation | DONE | `PHASE-6` commit |
| 7 | Environment: terrain/roads/buildings-interiors/water/trees/day-night | DONE | `PHASE-7` commit |
| 8 | AI: civilian NPCs + hostiles with staggered updates | DONE | `PHASE-8` commit |
| 9 | Missions: story + side objectives, rewards, failure | DONE | `PHASE-9` commit |
| 10 | Inventory + vehicles | DONE | `PHASE-10` commit |
| 11 | Audio + full settings (graphics/controls/audio) | DONE | `PHASE-11` commit |
| 12 | Save/load with corruption protection | DONE | `PHASE-12` commit |
| 13 | Optimization: culling, AI stagger, presets, draw budget | DONE | `PHASE-13` commit |
| 14 | Final QA: every system PASS/FAIL/NOT TESTED | DONE | `PHASE-14` commit |
| 15 | APK release: build, verify, deliver | DONE | `PHASE-15` commit |

## Phase logs (actual results recorded per phase)

### PHASE 0 — Project audit (2026-11-11)
- Environment verified: JDK 21.0.13, Android SDK platforms 30+35, aapt2/d8/apksigner
  present, 85 GB free, device Android 16 arm64-v8a (8 cores), **no adb**, debug
  keystore present.
- Godot evaluated and rejected (broken 186-byte Python shim, no export templates);
  native Java/GLES2 engine selected and documented (TECHNICAL_ARCHITECTURE.md).
- Existing project audited: 15 classes / 4,473 lines, all compile; JVM suite
  2,432 checks PASS / 0 FAIL; APK v1.1 (22 MB) verified v1+v2+v3.
- Deliverable: this roadmap + TECHNICAL_ARCHITECTURE.md + baseline TEST_REPORT.md
  + KNOWN_ISSUES.md. Test run: `sh tests/run.sh` → PASS (2,432/0).

### PHASE 1 — Game foundation
- Added `GameMode` (PARKOUR / OPEN_WORLD) + main-menu mode selection.
- Open-world foundation: `Terrain` (analytic heightfield + chunk meshes) and
  `OpenWorld` (regions, 64 m chunk grid, streaming set) wired into `GameView`
  through a new `WorldScene` abstraction; renderer draws terrain + region props.
- Test: compile PASS; JVM suite extended with terrain/region checks (see
  TEST_REPORT §P1). Launch path verified by code + build; device launch NOT TESTED.

### PHASE 2 — Movement
- `PlayerController` ground states on terrain: idle/walk/jog/run/sprint/jump/
  fall/land/crouch; terrain ground query replaces flat-floor assumption;
  step-up ≤0.55 m, slope slide >50°, slope speed factor, no wall penetration
  (AABB push-out retained), no float (ground snap), no teleport (clamped dt).
- Test: 26 new JVM movement checks PASS (TEST_REPORT §P2).

### PHASE 3 — Animations
- `CharacterAnimator`: procedural limb animation (idle sway, walk/jog/run
  cycle phase, sprint arms, jump/fall/land poses, crouch, damage/death), with
  0.18 s cross-blend; cycle speed matches ground speed. Fallback animations
  clearly identified (no licensed skeletal set available — GDD §7).
- Test: 18 JVM checks on blending + speed matching PASS (TEST_REPORT §P3).

### PHASE 4 — Camera
- `CameraRig`: smoothed follow, orbit yaw/pitch, zoom (0.8–1.6 arm), sensitivity +
  invert-Y settings, obstruction raycast pull-in against building AABBs +
  terrain, terrain-aware minimum height, reset + shoulder offset option.
- Test: 14 JVM camera-math checks PASS (TEST_REPORT §P4).

### PHASE 5 — Touch controls
- Buttons added: CROUCH, ACTION (attack/interact vehicle horn/boost), MAP
  shortcut; button size/opacity/left-handed layout settings; joystick size +
  sensitivity settings; multi-touch IDs retained; touch cancellation handled.
- Test: build PASS; layout math JVM checks PASS (TEST_REPORT §P5).

### PHASE 6 — World map
- `RoadNetwork` graph + BFS routing; `MapScreenView` fullscreen map (pinch
  zoom, drag pan, marker tap, waypoint set/remove, recenter); GL minimap
  rotating with player; route distance + directional guidance + recalc.
- Test: 22 JVM routing/map checks PASS (TEST_REPORT §P6).

### PHASE 7 — Environment
- Full world build-out: city core (roads grid, shops with walkable interiors,
  safe zone), residential blocks, riverside village + bridge, west forest,
  northern hills, river + water shader tint, vegetation, day/night cycle,
  weather-lite fog; graphics LOW/MEDIUM/HIGH presets + resolution scale.
- Test: 30 JVM world checks PASS (TEST_REPORT §P7).

### PHASE 8 — AI
- `CivilianNPC` (idle/walk/patrol/talk/react, road-graph routes, staggered
  updates); hostile stalkers integrated into hostile zones with reset rules.
- Test: 16 JVM AI checks PASS (TEST_REPORT §P8).

### PHASE 9 — Missions
- `MissionManager` + 6-mission story "The Long Signal" + 2 side objectives;
  VISIT/COLLECT/TALK/DRIVE_TO/SURVIVE/RETURN objective types; rewards,
  failure, progression; HUD mission panel updates.
- Test: 24 JVM mission-graph checks PASS (TEST_REPORT §P9).

### PHASE 10 — Inventory + vehicles
- `Inventory` (quantities, descriptions, persistence); `Vehicle` car +
  motorcycle: enter/exit, throttle/steer/brake/reverse, collision damage,
  health-limited top speed, follow camera, map markers for vehicles.
- Test: 20 JVM vehicle/inventory checks PASS (TEST_REPORT §P10).

### PHASE 11 — Audio + settings
- Master/music/SFX volumes wired; day-night ambient mix; full settings screen
  (graphics LOW/MED/HIGH, resolution scale, FPS 30/45/60, button size/opacity,
  left-handed mode, camera inversion, auto-save toggle).
- Test: build PASS; settings persistence JVM checks PASS (TEST_REPORT §P11).

### PHASE 12 — Save/load
- `SaveData` versioned snapshot: position, region, missions, inventory,
  discovered POIs, vehicle; atomic temp-file write + version check + default
  on corruption; new game / continue / autosave.
- Test: 12 JVM save round-trip + corruption-recovery checks PASS (§P12).

### PHASE 13 — Optimization
- Chunk streaming 3×3/2×2, behind-camera + distance culling, AI stagger,
  shared VBOs, draw budget table, FPS counter (debug), quality presets.
- Test: build PASS; static draw-call budget documented (TEST_REPORT §P13);
  on-device FPS NOT TESTED.

### PHASE 14 — Final QA
- Full system matrix PASS/FAIL/NOT TESTED (TEST_REPORT.md).

### PHASE 15 — APK release
- Final APK built, verified (v1+v2+v3), delivered with source + docs.
