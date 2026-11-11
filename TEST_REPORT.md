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
