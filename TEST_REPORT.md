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
