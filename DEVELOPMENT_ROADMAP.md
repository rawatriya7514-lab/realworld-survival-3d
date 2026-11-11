# REALWORLD 3D — Development Roadmap

Phased implementation with mandatory per-phase testing. Status column is kept
current after every phase. Rule: **never mark untested features PASS.**
Device-runtime results are marked **NOT TESTED** where no device test was
possible (no adb available in this environment; see KNOWN_ISSUES.md).

| Phase | Scope | Status | Checkpoint |
|---|---|---|---|
| 0 | Project audit + architecture docs | **DONE** | `PHASE-0` commit |
| 1 | Game foundation: mode select, open-world scene loads playable | **DONE** | `PHASE-1` commit |
| 2 | Movement: terrain locomotion, all states, collision | PLANNED | — |
| 3 | Animations: procedural state machine + blending | PLANNED | — |
| 4 | Camera: follow, orbit, zoom, collision, terrain-aware | PLANNED | — |
| 5 | Touch controls: joystick/buttons + layout settings | PLANNED | — |
| 6 | World map: fullscreen map, minimap, waypoints, navigation | PLANNED | — |
| 7 | Environment: terrain/roads/buildings-interiors/water/trees/day-night | PLANNED | — |
| 8 | AI: civilian NPCs + hostiles with staggered updates | PLANNED | — |
| 9 | Missions: story + side objectives, rewards, failure | PLANNED | — |
| 10 | Inventory + vehicles | PLANNED | — |
| 11 | Audio + full settings (graphics/controls/audio) | PLANNED | — |
| 12 | Save/load with corruption protection | PLANNED | — |
| 13 | Optimization: culling, AI stagger, presets, draw budget | PLANNED | — |
| 14 | Final QA: every system PASS/FAIL/NOT TESTED | PLANNED | — |
| 15 | APK release: build, verify, deliver | PLANNED | — |

## Phase logs (actual results recorded per phase)

### PHASE 0 — Project audit (2026-11-11) — DONE
- Environment verified: JDK 21.0.13, Android SDK platforms 30+35, aapt2/d8/apksigner
  present, 85 GB free, device Android 16 arm64-v8a (8 cores), **no adb**, debug
  keystore present.
- Godot evaluated and rejected (broken 186-byte Python shim, no export templates);
  native Java/GLES2 engine selected and documented (TECHNICAL_ARCHITECTURE.md).
- Existing project audited: 15 classes / 4,473 lines, all compile; JVM suite
  2,432 checks PASS / 0 FAIL; APK v1.1 (22 MB) verified v1+v2+v3.
- Deliverable: this roadmap + TECHNICAL_ARCHITECTURE.md + baseline TEST_REPORT.md
  + KNOWN_ISSUES.md. Test run: `sh tests/run.sh` → PASS (2,432/0).

### PHASE 1 — Game foundation (2026-11-11) — DONE
Scope: mode select on the main menu + a playable open-world foundation with
terrain, streaming chunks, regions/POIs, roads, bridge, river and player
terrain/water integration. Parkour campaign left functionally untouched.

What was built (all original code):
- `Terrain.java` (new, pure Java): analytic 896 m × 896 m heightfield
  (`heightAt` / `normalAt` / `slope01` / `waterDepthAt`), river carve along a
  meandering center line, flat zones (city core, residential, riverside
  village, bridge corridor) blended with 24 m margins, north hills, west
  forest rise, `WATER_LEVEL = -2.2`, `WADE_MAX = 1.5`; 64 m chunk mesh builder
  (96 verts / chunk, stride 8: pos3+normal3+uv2) with a 4-tile procedural
  atlas mapping (grass/rock/sand/asphalt, 6 % inset).
- `OpenWorld.java` (new, pure Java): 7 named regions with unique IDs
  (CITY_CORE … OUTSKIRTS), 10 POIs with discovery radii, Central Plaza safe
  zone, 11 road strips rendered as non-solid 16 m ROAD_SLAB segments,
  deterministic seed-1337 chunk content (buildings/trees/rocks/crates/lamps
  per region), 3×3 streaming window with cache, global river bridge (solid
  deck, top y = 1.4) + Old Docks pier, `floorAt` terrain+structure query.
- `Player.java`: terrain-aware extension (`terrain` / `waterLevel` fields,
  null = classic parkour behaviour unchanged): heightfield landing, wading
  slowdown ×0.55, strict deeper-water movement block (equal-depth flat beds
  cannot trap; escape always possible). Public update signature unchanged.
- `WorldRenderer.java`: procedural terrain atlas texture, chunk VBO cache
  (3×3 active), identity-model VBO draw path, behind-camera + distance cull,
  `renderOpenWorld` path with trees / lamps / water (blended) / rocks /
  bushes / roads / bridge draws, terrain-aware camera min height,
  open-world blob shadows. Untextured-tint draw helper added.
- `GameView.java`: `GameMode {PARKOUR, OPEN_WORLD}`; `startOpenWorld()` spawn
  (210, 44) facing the city; `updateOpenWorld()` with ±442 border clamp, POI
  discovery toasts, fatal-drop guard; shared health-feedback and SFX
  processing for both modes; pause/autosave only in PARKOUR (open-world save
  arrives in Phase 12); retry re-enters the active mode.
- `UIController.java`: "Open World" / "Rooftop Parkour" main-menu buttons,
  updated About text, updated game-over retry routing.
- `Obstacle.java`: new prop types (TREE, ROCK, BUSH, WATER_PLANE, ROAD_SLAB,
  BRIDGE, CRATE, LAMP) with solid flag + default colors.
- Tests: `tests/OpenWorldTest.java` (15 groups, 82 checks) added; run via
  `sh tests/run.sh`.

Test results (real, this session):
- JVM: `sh tests/run.sh` → GameLogicTest **2432 passed / 0 failed** (parkour
  regression suite intact), OpenWorldTest **82 passed / 0 failed** (terrain
  continuity + seams, flat zones, river carve/banks, bridge corridor ±85 m,
  chunk mesh invariants, determinism, 3×3 streaming, bridge/pier, POIs,
  terrain landing, non-solid roads, deep-water block + escape, wading
  slowdown).
- Android compile: full `./export-and-build.sh debug` → OK (all sources,
  including Android-only classes). One compile error found and fixed during
  the build (stale `renderHUD(gl, …)` call after the renderer refactor).
- APK: built, 22,019,982 bytes, signature verified v1+v2+v3; Terrain/OpenWorld
  classes confirmed present in the dex output.
- Device runtime (touch, FPS, GL correctness on screen): **NOT TESTED** —
  no adb in this environment; Termux cannot install APKs for the app itself.
  Next phase must include a device check before further renderer work.

Known phase-1 limitations (tracked, see KNOWN_ISSUES.md):
- No swimming: deep water is a movement wall (wading ≤1.5 m only).
- Roads exist only in flat zones; no road surface on slopes yet.
- Buildings are solid boxes without walkable interiors (Phase 7).
- Open-world progress is not saved yet (Phase 12).
- No day/night cycle yet (Phase 7); dusk lighting inherited from parkour.

### PHASE 2 — Movement — PLANNED
- Full ground state machine on terrain: idle/walk/jog/run/sprint/jump/fall/
  land/crouch; step-up ≤0.55 m, slope handling, ground snap without teleport.
- Gate: new JVM movement checks + parkour regression must stay green before
  any renderer change.

### PHASE 3 — Animations — PLANNED
- Procedural limb animation with blending + speed matching (fallback approach,
  disclosed in GDD §7). Gate: JVM blending checks.

### PHASE 4 — Camera — PLANNED
- Orbit/zoom/collision pull-in + terrain-aware min height settings.
- Gate: JVM camera-math checks.

### PHASE 5 — Touch controls — PLANNED
- Full HUD layout settings (size/opacity/left-handed), extra action buttons.
- Gate: build PASS + layout math checks.

### PHASE 6 — World map — PLANNED
- Fullscreen map (pinch/pan/markers/waypoints) + rotating minimap + road
  routing. Gate: JVM routing checks.

### PHASE 7 — Environment — PLANNED
- Building interiors, day/night cycle, weather-lite, graphics presets.
- Gate: JVM world checks.

### PHASE 8 — AI — PLANNED
- Civilian NPCs + hostile zones with staggered updates.
### PHASE 9 — Missions — PLANNED
- "The Long Signal" 6-mission story + side objectives, rewards, failure.
### PHASE 10 — Inventory + vehicles — PLANNED
### PHASE 11 — Audio + settings — PLANNED
### PHASE 12 — Save/load — PLANNED
### PHASE 13 — Optimization — PLANNED
### PHASE 14 — Final QA — PLANNED
### PHASE 15 — APK release — PLANNED
