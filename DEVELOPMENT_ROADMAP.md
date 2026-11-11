# REALWORLD 3D — Development Roadmap

Phased implementation with mandatory per-phase testing. Status column is kept
current after every phase. Rule: **never mark untested features PASS.**
Device-runtime results are marked **NOT TESTED** where no device test was
possible (no adb available in this environment; see KNOWN_ISSUES.md).

| Phase | Scope | Status | Checkpoint |
|---|---|---|---|
| 0 | Project audit + architecture docs | **DONE** | `PHASE-0` commit |
| 1 | Game foundation: mode select, open-world scene loads playable | **DONE** | `PHASE-1` commit |
| 2 | Movement: terrain locomotion, all states, collision | **DONE** | `PHASE-2` commit |
| 3 | Animations: procedural state machine + blending | **DONE** | `PHASE-3` commit |
| 4 | Camera: follow, orbit, zoom, collision, terrain-aware | **DONE** | `PHASE-4` commit |
| 5 | Touch controls: joystick/buttons + layout settings | **DONE** | `PHASE-5` commit |
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

### PHASE 2 — Movement (2026-11-11) — DONE
Scope: complete ground and airborne movement state machine across terrain,
rooftops, and structures; step-up (<=0.55m), ground snap without teleportation,
and terrain slope handling (walkable uphill resistance + steep canyon deflection).

What was built (all original code):
- `Player.ParkourState`: added `WALK`, `JOG`, `CROUCH`, `LAND` states to the
  state machine alongside `STAND`, `RUN`, `SPRINT`, `JUMP`, `DOUBLE_JUMP`,
  `FALLING`, `HARD_LAND`, `SAFETY_ROLL`, and parkour agility states.
- Movement speeds: analog-tiered speed progression (`walkSpeed` 3.5, `jogSpeed` 5.5,
  `runSpeed` 7.5, `sprintSpeed` 12.0, `crouchSpeed` 2.5 m/s).
- Crouch system: crouch toggle/hold via `crouchPressed` or `slidePressed` when
  not sprinting; bounding box drops to `crouchingHeight = 0.8m`; overhead headroom
  clearance check (`canStandUp`) prevents standing up under low ceilings (e.g. 1.3m);
  sliding/safety roll under low ceilings smoothly exits into `CROUCH`; quiet footsteps.
- Step-Up (<=0.55m): smooth forward step-up onto curbs, road slabs, crates, and
  terrain bumps; headroom validation (`canStepUp`) checks clearance above target;
  suppresses horizontal push-out when body steps onto surface top.
- Ground Snap (<=0.55m): continuous ground contact down slopes, curbs, and
  staircases without falling jitter; drop >0.55m (cliff/roof edge) naturally
  transitions into airborne `FALLING` without instant teleportation.
- Slope Handling: terrain normal evaluation (`normalAt`); walkable slopes
  (normal.y >= 0.60) feature mild uphill resistance and smooth ascent/descent;
  steep cliffs (normal.y < 0.60, river canyon) deflect inward velocity along
  slope tangent and slide the character down under gravity.
- Landing: soft landing state `LAND` (0.12s absorption, 0 damage) for drops
  between 1.2m and 5.0m; immediately cancellable by movement input for zero
  control latency; high drops (>5.0m) retain `HARD_LAND` damage vs `SAFETY_ROLL`.
- Visuals: `WorldRenderer` updated to render crouching silhouette (`CROUCH` width 1.05m,
  height matches `currentHeight` 0.8m).
- Tests: `tests/MovementTest.java` (6 groups, 45 checks) added; run via `sh tests/run.sh`.

Test results (real, this session):
- JVM: `sh tests/run.sh` → GameLogicTest **2,432 passed / 0 failed** (parkour
  regression intact), OpenWorldTest **82 passed / 0 failed**, MovementTest
  **45 passed / 0 failed**. Total: **2,559 passed / 0 failed**.
- Android compile: full `./export-and-build.sh debug` → OK (all 20 classes).
- APK: built, 22,019,982 bytes, signature verified v1+v2+v3.
- Device runtime: **NOT TESTED** (no adb). JVM gate: 2559/0.

Known phase-2 limitations (tracked, see KNOWN_ISSUES.md):
- Camera height follows standing eye-level during crouch (offset lowers in Phase 4).
- HunterEnemy uses single-box model (full enemy animation rig planned for Phase 8).

### PHASE 3 — Animations (2026-11-11) — DONE
Scope: procedural articulated limb animation system with speed matching and
cross-fade blending for box-composite humanoids across all 18 movement and parkour
states (WALK, JOG, RUN, SPRINT, CROUCH, STAND, JUMP, DOUBLE_JUMP, FALLING,
LAND, HARD_LAND, SAFETY_ROLL, SLIDING, MANTLING, VAULTING, WALL_RUN_LEFT,
WALL_RUN_RIGHT, DEAD).

What was built (all original code):
- `CharacterAnimator.java` (new, pure Java): full articulated pose model
  (torso, head, left/right arms, left/right legs) with pitch/yaw/roll rotations
  and offsets; speed-matched dynamic gait frequencies: STAND (0), WALK (8.5),
  JOG (12.0), RUN (15.0), SPRINT (19.0 rad/s); natural sinusoidal breathing idle
  oscillation; smooth 0.18s cross-fade pose blending with sinusoidal easing;
  full 360-degree safety roll rotation; knee compression on landing; banking
  on wall-runs.
- `Player.java` integration: animator initialized and updated unconditionally
  every frame across all movement, parkour, and special states.
- `WorldRenderer.java` integration: hierarchical limb transformation rendering
  using `Mat4` (torso -> head, left/right shoulders -> arms, hips -> legs)
  with team/player palette accents.
- Tests: `tests/AnimationTest.java` (8 groups, 30 checks) added; run via `sh tests/run.sh`.

Test results (real, this session):
- JVM: `sh tests/run.sh` → GameLogicTest **2,432 passed / 0 failed**, OpenWorldTest
  **82 passed / 0 failed**, MovementTest **45 passed / 0 failed**, AnimationTest
  **30 passed / 0 failed**. Total: **2,589 passed / 0 failed**.
- Android compile: full `./export-and-build.sh debug` → OK (all 21 classes).
- APK: built, 22,024,078 bytes, signature verified v1+v2+v3.
- Device runtime: **NOT TESTED** (no adb). JVM gate: 2589/0.

Known phase-3 limitations (tracked, see KNOWN_ISSUES.md):
- HunterEnemy uses single-box model (full enemy animation rig planned for Phase 8).

### PHASE 4 — Camera (2026-11-11) — DONE
Scope: third-person and first-person camera controller (`CameraRig.java`),
dynamic crouch eye-level adaptation, orbit yaw/pitch clamping, Dutch-angle roll,
continuous pinch-zoom and presets, slab-based AABB obstruction raycast pull-in,
and terrain-aware minimum height clearance.

What was built (all original code):
- `CameraRig.java` (new, pure Java): target tracking with smooth exponential
  damping; dynamic focus height transitioning between standing (1.45m) and
  crouching (0.65m), smoothly eliminating low-headroom clipping; 360-degree yaw
  orbit with strict pitch clamp [-80, +80 deg]; Dutch angle roll (-15 to +15 deg)
  with exponential return; continuous zoom multiplier (0.8x - 1.6x) and presets
  (CLOSE 2.5m, NORMAL 4.8m, FAR 7.2m); slab-based AABB ray-box intersection
  with 0.25m safety margin against all solid obstacles (instant pull-in to avoid
  geometry clipping, smooth return outward); open-world terrain clearance clamp
  (never dips below `Terrain.heightAt(camX, camZ) + 0.8m` or river surface);
  first-person mode toggle with crouch eye-level lowering; screen shake trauma
  decay; and pure Java 4x4 LookAt view matrix calculation.
- `TouchInput.java` updates: multi-touch pinch-to-zoom on the right side of the
  screen (`zoomPointerId`, `lastPinchDist`, `deltaZoom`), seamless pointer hand-off.
- `GameView.java` & `WorldRenderer.java` integration: `WorldRenderer` delegates
  matrix calculation to `CameraRig`; `GameView` applies pinch zoom delta.
- Tests: `tests/CameraTest.java` (9 groups, 42 checks) added; run via `sh tests/run.sh`.

Test results (real, this session):
- JVM: `sh tests/run.sh` → GameLogicTest **2,432 passed / 0 failed**, OpenWorldTest
  **82 passed / 0 failed**, MovementTest **45 passed / 0 failed**, AnimationTest
  **30 passed / 0 failed**, CameraTest **42 passed / 0 failed**. Total: **2,631 passed / 0 failed**.
- Android compile: full `./export-and-build.sh debug` → OK (all 22 classes).
- APK: built, 22,028,174 bytes, signature verified v1+v2+v3.
- Device runtime: **NOT TESTED** (no adb). JVM gate: 2631/0.

Known phase-4 limitations (tracked, see KNOWN_ISSUES.md):
- HunterEnemy uses single-box model (full enemy animation rig planned for Phase 8).

### PHASE 5 — Touch controls (2026-11-11) — DONE
Scope: pure Java touch layout engine (`TouchLayout.java`), dedicated CROUCH button,
HUD customization settings (button scale 0.8x-1.3x, button opacity 0.4-1.0,
left-handed layout mirroring, joystick sensitivity 0.5x-1.5x), pairwise disjoint
button positioning, and scrollable Settings UI menu.

What was built (all original code):
- `TouchLayout.java` (new, pure Java): adaptive layout engine calculating
  screen positions and radii for virtual joystick and all 8 action buttons (JUMP,
  CROUCH, VAULT, SLIDE, SPRINT, CAM, INTERACT, PAUSE); button scaling (0.8x - 1.3x)
  and opacity clamping (0.4 - 1.0); left-handed layout mirroring (joystick on
  right 82% X, buttons on left, swipe regions adapted); pairwise disjoint
  separation eliminating button overlaps; Euclidean distance hit-testing.
- `TouchInput.java` updates: integrates `TouchLayout`, adds `crouchToggled`,
  scales joystick velocity with `joystickSensitivity`, supports pinch-to-zoom
  pointer tracking and smooth pointer hand-offs.
- `GameSettings.java` updates: adds persistence keys for buttonScale, buttonOpacity,
  leftHanded, and joystickSensitivity with slider conversion helpers.
- `UIController.java` updates: settings menu adds sliders for joystick sensitivity,
  button size, and button opacity, plus a toggle for left-handed layout; wrapped
  in vertical `ScrollView` so settings never overflow on landscape screens.
- `WorldRenderer.java` updates: renders dedicated `btnCrouch` circle with active
  glow when crouching; scales all HUD button alpha dynamically with `touch.buttonOpacity`.
- Tests: `tests/TouchLayoutTest.java` (7 groups, 36 checks) added; run via `sh tests/run.sh`.

Test results (real, this session):
- JVM: `sh tests/run.sh` → GameLogicTest **2,432 passed / 0 failed**, OpenWorldTest
  **82 passed / 0 failed**, MovementTest **45 passed / 0 failed**, AnimationTest
  **30 passed / 0 failed**, CameraTest **42 passed / 0 failed**, TouchLayoutTest
  **36 passed / 0 failed**. Total: **2,667 passed / 0 failed**.
- Android compile: full `./export-and-build.sh debug` → OK (all 23 classes).
- APK: built, 22,032,270 bytes, signature verified v1+v2+v3.
- Device runtime: **NOT TESTED** (no adb). JVM gate: 2667/0.

Known phase-5 limitations (tracked, see KNOWN_ISSUES.md):
- HunterEnemy uses single-box model (full enemy animation rig planned for Phase 8).

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
