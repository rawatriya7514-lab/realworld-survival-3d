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
