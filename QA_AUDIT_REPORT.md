REALWORLD SURVIVAL 3D - QA AUDIT REPORT
=======================================
DATE: 2026-11-11 (build v2 / versionName 1.1)

SCOPE
-----
Honest quality audit of the native Android game in android-build/, plus a
correction record for the earlier audit (2026-11-10) whose claims were
partly inflated. Every claim below is tied to a command that was actually
run; device-runtime claims are explicitly marked NOT VERIFIED.

CORRECTIONS TO THE PREVIOUS AUDIT
---------------------------------
The 2026-11-10 report claimed textures, OBJ models, menus, save system and
audio were "built and verified". Reality at that time:
  - Textures/OBJs were bundled in assets/ but the GL10 renderer drew only
    flat colored cubes. FIXED: the new GLES2 renderer (WorldRenderer.java)
    loads and uses all 8 textures.
  - No menus, no HUD text, no pause flow, no save/load existed.
    FIXED: UIController.java (~600 lines, 6 screens + HUD overlays),
    SaveManager + GameSettings persistence, full state machine in
    GameView.java.
  - Audio assets existed but no code played them. FIXED:
    GameAudioManager (SoundPool SFX + MediaPlayer music, separate volumes).
  - "OpenGL ES 1.0" claim was true then; now GLES2 with shaders.
  - Music assets were 33.5 MB per track (169 MB APK). FIXED: regenerated
    with generate_music.py (90s seamless mono loops, 3.97 MB each,
    21 MB APK).

CURRENT VERIFICATION STATE
--------------------------
PASS (verified by execution):
  * Compilation: 17 classes, 0 errors (javac, android-30 jar).
  * JVM logic suite: 2,432 checks / 0 failures (tests/run.sh).
  * Packaging: aapt2 compile/link with full asset bundle; d8 (clean after
    adding --lib); dex injection; 4-byte zipalign; apksigner v1+v2+v3;
    apksigner verify true on all three schemes.
  * Manifest: single VIBRATE permission, minSdk 24 / targetSdk 34, GLES2
    requirement, landscape, correct label + launchable activity.
  * Asset integrity: 51 zip entries; 5 music loops + 17 SFX + 8 textures +
    6 OBJs + level JSON present in the APK.

NOT VERIFIED (no adb/emulator; Termux cannot self-install):
  * On-device rendering (shader compile on vendor GPUs), touch handling,
    audio latency, thermal/FPS behavior, activity lifecycle.

BUGS FOUND AND FIXED THIS CYCLE (found via the JVM test suite)
-------------------------------------------------------------
1. Roof-standing players were shoved by "wall" collisions while walking or
   landing on rooftops - the horizontal push-out did not distinguish walls
   from the floor the player was standing on.
   FIX: two-pass resolveWorldCollisions - vertical landing pass selects the
   floor (landedOn); horizontal push-out only applies when the feet are
   below the obstacle top and the obstacle is not the landing surface.
2. Wall-run triggered while standing on a roof when jumping (probe point
   inside the same building's footprint), giving a bogus upward stick.
   FIX: checkWallRun now skips any obstacle whose XZ footprint contains the
   player and requires the player to be 0.5m below the obstacle top.
3. Hazard street dealt 35 damage per frame (instant death on touch, no
   escape chance). FIX: 1s hazardCooldown between ticks (35 dmg/s).
4. Landing states were silently clobbered: handleMovement's state tail
   overwrote DEAD/HARD_LAND/SAFETY_ROLL set inside collision resolution -
   deaths never registered. FIX: early return on DEAD; the tail no longer
   overrides HARD_LAND/SAFETY_ROLL.
5. Enemy leap used to fly across any distance; now bounded by LEAP_RANGE 9m
   with a landing-roof search, give-up timer, and roof-bounded patrol.
   (Verified: enemy never crosses a 70m chasm; never hovers over the void.)

RISK ASSESSMENT
---------------
- Highest risk: unverified on-device behavior (GLES2 shader variants,
  touch latency, MediaPlayer looping seams).
- Medium risk: pure-Python zipalign inside the build script (verified by
  apksigner each build, but less battle-tested than the SDK tool).
- Low risk: AABB-only collision, single static map, no navmesh AI.

RECOMMENDATIONS (next steps)
----------------------------
1. Install on one physical device (termux-open the APK) and walk the
   device-test checklist in testing-results.md.
2. Capture shader-compile logs on at least one Mali and one Adreno GPU.
3. Wire a release keystore + signed release build before distribution.
4. Optional polish: in-GL text, enemy pathing over multi-roof routes,
   ETC1/ASTC texture compression if APK size matters.
