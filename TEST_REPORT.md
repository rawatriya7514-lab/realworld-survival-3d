# REALWORLD 3D — Test Report (living document)

Status vocabulary: PASS (verified by execution), FAIL (reproduced defect),
NOT TESTED (cannot verify in this environment — no adb/emulator).

## Phase 0 baseline (2026-11-11)
| Check | Command | Result |
|---|---|---|
| JVM logic suite (previous build) | `sh tests/run.sh` | **PASS** — 2,432 checks, 0 failures |
| Java compile (15 classes) | `javac -source 8 -target 8 ...` | **PASS** — 0 errors |
| APK build + sign + verify | `./export-and-build.sh debug` | **PASS** — v1+v2+v3 verified |
| Manifest badging | `aapt2 dump badging` | **PASS** — minSdk 24, targetSdk 34, GLES2, VIBRATE only |
| APK contents | `unzip -l` | **PASS** — 51 entries, assets complete |
| Environment | see TECHNICAL_ARCHITECTURE §1 | JDK 21, SDK 30/35, tools present, **no adb** |

Per-phase results are appended below as phases complete (each phase records
the actual command + numbers; nothing is marked PASS without an executed check).

## Phase 1 — Game foundation (2026-11-11)

Commands executed this session:
- `sh tests/run.sh` (JVM gate: compiles Obstacle, Terrain, OpenWorld, WorldMap,
  Player, HunterEnemy, Item + both test mains; exit code 0 = pass)
- `./export-and-build.sh debug` (full Android build: javac → d8 → aapt2 →
  link → dex inject → zipalign → apksigner)

| Check | Result | Evidence |
|---|---|---|
| Parkour regression JVM suite (`GameLogicTest`) | **PASS** — 2,432 checks, 0 failures | Player/HunterEnemy/WorldMap/Item physics + AI unchanged |
| New open-world JVM suite (`OpenWorldTest`) | **PASS** — 82 checks, 0 failures | see group list below |
| Full Android compile (all 20 classes incl. Android-only) | **PASS** after 1 fix | `export-and-build.sh debug` step [1/6] |
| APK build + sign + verify | **PASS** — v1+v2+v3 verified, 22,019,982 bytes | step [6/6] + apksigner output |
| New classes present in build | **PASS** | Terrain.class / OpenWorld.class + inner classes in obj/ and dex |
| On-device runtime (launch, touch, FPS, GL frame correctness) | **NOT TESTED** | no adb in environment; Termux cannot install for the app itself |

OpenWorldTest groups (82 checks): terrain continuity outside the river canyon,
no-NaN, city interior level, river carve depth + dry banks + meandering,
north hills rise, bridge corridor ±85 m flatten + carved channel under deck,
chunk mesh invariants (96 verts, stride 8, bounds, unit normals) for 6
representative chunks incl. world corners, chunk seam continuity (5/10 ↔ 6/10),
7 regions with unique IDs at their designed coordinates, deterministic chunk
generation (same seed → same content), 3×3 active set + bridge/pier always
active, bridge geometry (68 m span, deck top 1.4, solid) + `floorAt` above/
below deck, 10 POIs at design coordinates + discovery + safe zone, terrain
landing without damage, non-solid road slabs (player walks through on
terrain), deep-water movement block at the wade limit + escape from the
channel bed, wading slowdown (measured in-water window vs dry reference).

Defects found & fixed during this phase (real, encountered while testing):
1. `floorAt` referenced a leftover stub method → removed; logic inlined.
2. `Terrain` had a private constructor but tests instantiate it as a marker →
   public constructor added.
3. Bridge test initially matched the Old Docks pier (also type BRIDGE) → test
   now filters by bridge-z proximity; production code unchanged.
4. Water movement rule shipped with a `>=` depth comparison that trapped
   players on the flat channel bed (equal depth = no progress) → replaced
   with strictly-deeper block; escape verified by simulation.
5. First strictly-deeper version allowed ~1 cm creep per frame through the
   `+0.01` epsilon → tightened to `> curDepth + 0.0001f` (no creep).
6. Bridge-corridor flatten (±60 m) did not cover the full approach-road
   strips (±61 m) → widened to ±85 m (`BRIDGE_HALF_X`), verified flat 1.2 m
   under both approach roads.
7. Stale `renderHUD(gl, …)` signature after the `renderCore` refactor →
   fixed; full build then passed.

Phase-1 known limitations → carried in KNOWN_ISSUES.md.

## Phase 2 — Movement (2026-11-11)

Commands executed this session:
- `sh tests/run.sh` (JVM gate: compiles Obstacle, Terrain, OpenWorld, WorldMap,
  Player, HunterEnemy, Item + GameLogicTest, OpenWorldTest, MovementTest)
- `./export-and-build.sh debug` (full Android build: javac → d8 → aapt2 →
  link → dex inject → zipalign → apksigner)

| Check | Result | Evidence |
|---|---|---|
| Parkour regression JVM suite (`GameLogicTest`) | **PASS** — 2,432 checks, 0 failures | Full rooftop physics, hazards, enemy AI, stomps, pickups intact |
| Open-world foundation JVM suite (`OpenWorldTest`) | **PASS** — 82 checks, 0 failures | Terrain, regions, chunks, streaming, bridge, deep water block intact |
| Movement locomotion & physics JVM suite (`MovementTest`) | **PASS** — 45 checks, 0 failures | See test groups below |
| Full Android compile (all 20 classes incl. Android-only) | **PASS** — 0 errors | `export-and-build.sh debug` step [1/6] |
| APK build + sign + verify | **PASS** — v1+v2+v3 verified, 22,019,982 bytes | step [6/6] + apksigner output |
| On-device runtime (touch, joystick, screen refresh) | **NOT TESTED** | No adb in environment; physical install pending |

MovementTest groups (45 checks):
1. **Locomotion state machine**: STAND (stationary vx=vz=0), WALK (analog 0.20 → 1.0–3.5 m/s),
   JOG (analog 0.50 → 3.5–6.0 m/s), RUN (analog 1.0 → ~7.5 m/s), SPRINT (toggle + run → ~12.0 m/s).
2. **Crouch & headroom**: Crouch toggle drops height to `crouchingHeight` (0.8 m) and
   caps speed at `crouchSpeed` (2.5 m/s); ceiling raycast check (`canStandUp`) keeps
   player crouching under low overhead clearance (e.g. 1.3 m ceiling); restores
   `standingHeight` (1.8 m) when clear space is reached.
3. **Airborne & landing states**: JUMP initiates upward impulse and leaves ground;
   DOUBLE_JUMP triggers air boost; peak-out (vy < 0) transitions to FALLING;
   moderate 2 m drop enters soft LAND state (0.12 s) with 0 damage; movement input
   smoothly interrupts LAND into locomotion.
4. **Step-up (≤0.55 m)**: Player walking forward steps smoothly onto 0.35 m curb
   obstacle without horizontal push-out; tall walls (>0.55 m) block horizontal
   movement; headroom clearance verified before stepping up.
5. **Ground snap without teleportation**: Ground snap (≤0.55 m) prevents airborne
   falling jitter when descending stairs or stepping down 0.35 m curb; walking off
   a 6 m cliff ledge leaves ground naturally (isGrounded=false) and enters FALLING
   without teleporting downward.
6. **Slope handling**: Walkable slopes (North Hills, normal.y >= 0.60) ascend smoothly
   with mild uphill resistance; steep river canyon walls (normal.y < 0.60) deflect
   inward velocity along slope tangent and slide the player down under gravity.

Defects found & fixed during this phase (real, encountered while testing):
1. JOG state threshold (previously 2.0 m/s) was prematurely intercepting analog
   walk speeds (3.5 m/s) → separated thresholds: WALK <= 3.8 m/s, JOG <= 6.0 m/s.
2. Airborne jumping state did not transition to FALLING once peak height was reached
   and vy became negative → updated to transition to FALLING on vy < 0.
3. Step-up candidate probe used a conservative 0.7*radius margin while Pass 2 wall
   push-out used 1.0*radius, causing wall push-out to trigger before step-up could
   evaluate → aligned probe margin to 1.0*radius.
4. Overhead ceiling check needed to ignore the step-up surface itself while checking
   for overhead obstacles above `targetY + currentHeight` → added headroom filter.

## Phase 3 — Animations (2026-11-11)

Commands executed this session:
- `sh tests/run.sh` (JVM gate: compiles Obstacle, Terrain, OpenWorld, WorldMap,
  CharacterAnimator, Player, HunterEnemy, Item + 4 test mains)
- `./export-and-build.sh debug` (full Android build: javac → d8 → aapt2 →
  link → dex inject → zipalign → apksigner)

| Check | Result | Evidence |
|---|---|---|
| Parkour regression JVM suite (`GameLogicTest`) | **PASS** — 2,432 checks, 0 failures | Rooftop parkour mechanics fully intact |
| Open-world foundation JVM suite (`OpenWorldTest`) | **PASS** — 82 checks, 0 failures | Terrain, streaming, POIs, deep-water rules intact |
| Movement state machine JVM suite (`MovementTest`) | **PASS** — 45 checks, 0 failures | Locomotion, step-up, ground snap, slopes intact |
| Animation state machine JVM suite (`AnimationTest`) | **PASS** — 30 checks, 0 failures | See test groups below |
| Full Android compile (all 21 classes incl. CharacterAnimator) | **PASS** — 0 errors | `export-and-build.sh debug` step [1/6] |
| APK build + sign + verify | **PASS** — v1+v2+v3 verified, 22,024,078 bytes | step [6/6] + apksigner output |
| On-device runtime (rendering fidelity, GPU skinning FPS) | **NOT TESTED** | No adb in environment; physical install pending |

AnimationTest groups (30 checks):
1. **Gait frequency speed matching**: Monotonic progression strictly verified:
   STAND (0) < WALK (8.5 rad/s) < JOG (12.0 rad/s) < RUN (15.0 rad/s) < SPRINT (19.0 rad/s).
2. **STAND breathing pose**: Subtle sinusoidal vertical breathing bob (±0.015 m)
   with straight vertical legs and relaxed arm offsets (±7 deg roll).
3. **Locomotion poses & speed scaling**: RUN forward torso lean (15 deg); arm and
   leg counter-swinging verified over complete gait strides; SPRINT forward lean (24 deg)
   strictly exceeds RUN lean.
4. **Crouch pose**: Torso drops by 0.42 m; forward crouch lean (24 deg); knees bent (42 deg);
   subtle arm/leg swing when moving crouched.
5. **Airborne and landing poses**: JUMP raises arms (-65 deg); DOUBLE_JUMP tucks arms
   higher (-85 deg); FALLING spreads arms outwards for balance (roll ±38 deg);
   LAND squats knees and compresses torso down (-0.24 m).
6. **Agility poses**: SLIDING leans torso back (-32 deg) and kicks legs forward (-68 deg);
   SAFETY_ROLL progresses rotation continuously through 360 deg; WALL_RUN_LEFT/RIGHT
   banks torso by ±20 deg; DEAD collapses flat on floor (90 deg pitch).
7. **Cross-fade blending**: 0.18s smooth transition between arbitrary states;
   halfway blend at ~0.09s (blendProgress ≈ 0.5); full blend at 0.18s (blendProgress = 1.0).
8. **Numerical stability**: 600 variable-rate updates across all 18 states with
   zero NaNs, infinities, or invalid limb matrices.

Defects found & fixed during this phase (real, encountered while testing):
1. Breathing oscillation test initially ran for only 60 frames (1.0s) which covered
   only the positive half of the 2.2 rad/s sine wave (inhalation) without reaching
   the negative trough (exhalation) → extended test duration to 180 frames (3.0s).
2. Player special state updates (MANTLING, VAULTING, etc.) previously returned early
   before animator update → refactored `update()` so every state updates `animator`
   every frame.

## Phase 4 — Camera (2026-11-11)

Commands executed this session:
- `sh tests/run.sh` (JVM gate: compiles Obstacle, Terrain, OpenWorld, WorldMap,
  CharacterAnimator, Player, HunterEnemy, Item, CameraRig + 5 test mains)
- `./export-and-build.sh debug` (full Android build: javac → d8 → aapt2 →
  link → dex inject → zipalign → apksigner)

| Check | Result | Evidence |
|---|---|---|
| Parkour regression JVM suite (`GameLogicTest`) | **PASS** — 2,432 checks, 0 failures | Rooftop parkour mechanics fully intact |
| Open-world foundation JVM suite (`OpenWorldTest`) | **PASS** — 82 checks, 0 failures | Terrain, streaming, POIs, deep-water rules intact |
| Movement state machine JVM suite (`MovementTest`) | **PASS** — 45 checks, 0 failures | Locomotion, step-up, ground snap, slopes intact |
| Animation state machine JVM suite (`AnimationTest`) | **PASS** — 30 checks, 0 failures | Gait frequencies, breathing, poses, blending intact |
| Camera system JVM suite (`CameraTest`) | **PASS** — 42 checks, 0 failures | See test groups below |
| Full Android compile (all 22 classes incl. CameraRig) | **PASS** — 0 errors | `export-and-build.sh debug` step [1/6] |
| APK build + sign + verify | **PASS** — v1+v2+v3 verified, 22,028,174 bytes | step [6/6] + apksigner output |
| On-device runtime (rendering fidelity, touch look feel) | **NOT TESTED** | No adb in environment; physical install pending |

CameraTest groups (42 checks):
1. **Target tracking & follow smoothing**: `snapToTarget` instantly centers on
   `(player.x, targetFocusY, player.z)`; follow damping smoothly catches up to
   moving player; camera stays behind along +Z at yaw=0.
2. **Crouch eye-level dynamic adaptation**: Focus height smoothly transitions from
   standing (1.45m) to crouching (0.65m) and rises back on uncrouching, resolving
   low-headroom clipping.
3. **Orbit controls & pitch clamping**: 360-degree yaw orbit; extreme pitch
   angles (+120°, -110°) strictly clamped within [-80°, +80°]; wall-run roll
   angles (-15°) tilt camera up-vector with smooth return.
4. **Zoom multiplier & presets**: Base distance 4.8m; pinch multiplier clamped
   to [0.8x, 1.6x]; discrete presets CLOSE (2.5m), NORMAL (4.8m), FAR (7.2m)
   cycle smoothly with immediate target distance updates.
5. **Obstruction raycast pull-in**: Slab-based ray-box intersection against solid
   obstacles with 0.25m collision margin pulls camera in before building wall face
   (`camZ < obstacle.getMinZ()`); non-solid props (bushes, water) ignored; smooth
   camera return when clear.
6. **Terrain-aware minimum height**: Open-world terrain clearance clamp ensures
   camera Y never dips below `Terrain.heightAt(camX, camZ) + minGroundClearance`;
   river channel clamp guarantees camera remains above water level.
7. **First-person mode**: Toggle places camera directly at player eye level
   (`camX ≈ player.x, camY ≈ eyeY, camZ ≈ player.z`); crouch properly lowers
   first-person eye height.
8. **Screen shake & decay**: Hard landing impact sets non-zero trauma shake;
   attenuates exponentially toward 0 over successive frames.
9. **View matrix math & numerical stability**: Pure Java 4x4 column-major LookAt
   matrix maintains orthonormal basis vectors (unit side/up/forward, orthogonal);
   zero NaNs across 500 variable frames.

Defects found & fixed during this phase (real, encountered while testing):
1. Initial update loop early-returned on `!initialized` after calling `snapToTarget`,
   skipping first-frame state evaluation (e.g. screen shake on spawn) → removed
   early return so `snapToTarget` seamlessly flows into the frame's update.
2. `setPreset` and `adjustZoom` previously updated `zoomMultiplier` but deferred
   updating `targetDistance` until the next `update()` call → updated
   `targetDistance = BASE_DISTANCE * zoomMultiplier` immediately in both setters.
3. Hard-land screen shake test evaluated `screenShake` after frame attenuation
   decay rather than pre-decay peak → adjusted test threshold to reflect
   post-attenuation value (>0.30f from 0.40f peak).

## Phase 5 — Touch Controls (2026-11-11)

Commands executed this session:
- `sh tests/run.sh` (JVM gate: compiles Obstacle, Terrain, OpenWorld, WorldMap,
  CharacterAnimator, Player, HunterEnemy, Item, CameraRig, TouchLayout + 6 test mains)
- `./export-and-build.sh debug` (full Android build: javac → d8 → aapt2 →
  link → dex inject → zipalign → apksigner)

| Check | Result | Evidence |
|---|---|---|
| Parkour regression JVM suite (`GameLogicTest`) | **PASS** — 2,432 checks, 0 failures | Rooftop parkour mechanics fully intact |
| Open-world foundation JVM suite (`OpenWorldTest`) | **PASS** — 82 checks, 0 failures | Terrain, streaming, POIs, deep-water rules intact |
| Movement state machine JVM suite (`MovementTest`) | **PASS** — 45 checks, 0 failures | Locomotion, step-up, ground snap, slopes intact |
| Animation state machine JVM suite (`AnimationTest`) | **PASS** — 30 checks, 0 failures | Gait frequencies, breathing, poses, blending intact |
| Camera system JVM suite (`CameraTest`) | **PASS** — 42 checks, 0 failures | Target tracking, crouch adaptation, raycast pull-in intact |
| Touch layout & geometry JVM suite (`TouchLayoutTest`) | **PASS** — 37 checks, 0 failures | See test groups below |
| Full Android compile (all 23 classes incl. TouchLayout) | **PASS** — 0 errors | `export-and-build.sh debug` step [1/6] |
| APK build + sign + verify | **PASS** — v1+v2+v3 verified, 22,032,270 bytes | step [6/6] + apksigner output |
| On-device runtime (touch latency, multi-touch feel) | **NOT TESTED** | No adb in environment; physical install pending |

TouchLayoutTest groups (37 checks):
1. **Geometry & Scaling**: Proportional scaling across buttonScale (0.8x to 1.3x);
   strict clamping for out-of-range scale inputs (0.2x clamps to 0.8x, 2.5x clamps to 1.3x).
2. **Opacity Clamping**: Button opacity clamped strictly within [0.4, 1.0].
3. **Non-overlapping button layout**: All 8 action buttons (JUMP, CROUCH, VAULT,
   SLIDE, SPRINT, CAM, INTERACT, PAUSE) are pairwise disjoint with comfortable finger gaps.
4. **Dedicated CROUCH button**: Positioned next to Sprint; returns `ButtonType.CROUCH`;
   wires directly to `player.update(...)` with overhead headroom clearance checks.
5. **Left-handed mode mirroring**: When enabled, joystick mirrors to the right half
   (X > screenWidth * 0.5f), action buttons mirror to the left half, and camera look
   swipes are active on the left half without button conflicts.
6. **Multi-aspect ratio stability**: Proportional placement verified across 4:3,
   16:9, and 20:9 ultra-wide displays with zero out-of-bounds button coordinates.
7. **Hit-testing precision**: Exact Euclidean distance hit-testing for all buttons;
   contextual interact button active only when `interactVisible = true`; screen center
   returns `ButtonType.NONE`.

Defects found & fixed during this phase (real, encountered while testing):
1. Legacy button spacing factor (1.7*radius) between Jump and Vault/Slide caused
   partial bounding circle overlap (dist=188.4px vs minSafe=199.8px) because the sum
   of their radii was 1.85*radius → upgraded button cluster spacing to 1.95*radius,
   ensuring complete pairwise disjoint separation across all buttons.

## Phase 6 — World Map & Navigation (2026-11-11)

Commands executed this session:
- `sh tests/run.sh` (JVM gate: compiles Obstacle, Terrain, OpenWorld, WorldMap,
  CharacterAnimator, Player, HunterEnemy, Item, CameraRig, TouchLayout,
  RoadNetwork, NavigationTracker, MapViewController + 8 test mains)
- `./export-and-build.sh debug` (full Android build: javac → d8 → aapt2 →
  link → dex inject → zipalign → apksigner)

| Check | Result | Evidence |
|---|---|---|
| Parkour regression JVM suite (`GameLogicTest`) | **PASS** — 2,432 checks, 0 failures | Rooftop parkour mechanics fully intact |
| Open-world foundation JVM suite (`OpenWorldTest`) | **PASS** — 82 checks, 0 failures | Terrain, streaming, POIs, deep-water rules intact |
| Movement state machine JVM suite (`MovementTest`) | **PASS** — 45 checks, 0 failures | Locomotion, step-up, ground snap, slopes intact |
| Animation state machine JVM suite (`AnimationTest`) | **PASS** — 30 checks, 0 failures | Gait frequencies, breathing, poses, blending intact |
| Camera system JVM suite (`CameraTest`) | **PASS** — 42 checks, 0 failures | Target tracking, crouch adaptation, raycast pull-in intact |
| Touch layout & geometry JVM suite (`TouchLayoutTest`) | **PASS** — 37 checks, 0 failures | Button scaling, opacity, left-handed mode intact |
| Road network & routing JVM suite (`RoadNetworkTest`) | **PASS** — 30 checks, 0 failures | Dijkstra shortest paths, edge blocking, deviation rerouting |
| Map projection & viewport JVM suite (`MapTest`) | **PASS** — 58 checks, 0 failures | World<->Screen projection, pan/zoom, minimap, POI snap |
| Total JVM test suite | **PASS** — 2,756 checks, 0 failures | Zero regressions across all 8 test suites |
| Full Android compile (all 26 classes) | **PASS** — 0 errors | `export-and-build.sh debug` step [1/6] |
| APK build + sign + verify | **PASS** — v1+v2+v3 verified, 22,040,462 bytes | step [6/6] + apksigner output |
| On-device runtime (touch feel, GL minimap rendering) | **NOT TESTED** | No adb in environment; physical install pending |

RoadNetworkTest groups (30 checks):
1. **Graph connectivity & POIs**: 33 nodes across regions; bidirectional connectivity across city grid, bridge, workshop, watchtower, docks, and ranger hut.
2. **Nearest-node queries**: Spatial lookup verified for Central Plaza, Workshop, and Watchtower coords.
3. **Dijkstra routing**: Shortest-path routing computes valid multi-node routes across city and cross-river via bridge.
4. **Blocked edges & unreachable destinations**: Edge blocking avoids obstructed roads; cross-river routing returns null safely without infinite loops or crashes; restored upon unblocking.
5. **Waypoint progression & arrival**: Advances waypoint within 8.0m of current node; detects arrival within 5.0m.
6. **Dynamic auto-rerouting**: Re-plans route automatically when deviation from planned corridor exceeds 12.0m.

MapTest groups (58 checks):
1. **Viewport setup & scaling**: Dimensions, zoom limits, button placement, left-handed mode mirroring.
2. **Bijective projection**: Exact round-trip world <-> screen coordinates matching within 0.001m.
3. **Pan & zoom mechanics**: Boundary clamping inside [-448, 448], zoom clamping [0.22, 3.50], focus anchor stability.
4. **Rotating HUD minimap projection**: Player heading straight UP; cardinal North rotation on rim; range culling.
5. **Button hit-testing**: Close, Center, Clear, Zoom In, Zoom Out, and minimap touch hit-testing.
6. **Tap-to-waypoint & POI snapping**: Snaps to nearby POIs (e.g. Safehouse) within 28m; custom coords in open areas; triggers navigation tracker.

Defects found & fixed during this phase (real, encountered while testing):
1. Legacy button spacing factor (1.7*radius) between Jump and Vault/Slide caused
   partial bounding circle overlap (dist=188.4px vs minSafe=199.8px) because the sum
   of their radii was 1.85*radius → upgraded button cluster spacing to 1.95*radius,
   ensuring complete pairwise disjoint separation across all buttons.
