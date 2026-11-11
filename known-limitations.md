REALWORLD SURVIVAL 3D - Known Limitations
=========================================

DATE: 2026-11-11 (updated for build v2 / versionName 1.1)

TESTING LIMITATIONS (most important)
-----------------------------------
- NO DEVICE TESTING was performed. This Termux environment has no adb, no
  emulator, and cannot self-install APKs. Everything verified so far is:
  (a) JVM logic tests (2,432 checks, all passing), (b) compile/link/sign
  verification of the APK, (c) static manifest badging checks.
  On-device rendering performance, touch feel, audio latency, and lifecycle
  behavior on real hardware remain UNVERIFIED.
- The APK is debug-signed (keystore: standard Android debug key). Play Store
  upload requires a release keystore; `release` signing is not wired up.

ENGINE / RENDERER LIMITATIONS
-----------------------------
- The Godot 4.x skeleton (project.godot, scenes/, scripts/, shaders/) is NOT
  part of the shipped game. The Godot binary cannot execute in the Termux
  sandbox (execve blocked) and no export templates exist, so that path was
  abandoned in favor of the native Java build.
- Renderer is GLES2 with hand-written shaders - no PBR, no shadow mapping
  (blob shadows only), no post-processing, no skeletal animation.
- Characters are box-composite figures with blob shadows, not meshed models.
- HUD text is drawn with Android View overlays on top of the GL surface, so
  text follows the activity UI thread, not the game camera.

PHYSICS LIMITATIONS
-------------------
- Collision is AABB-based (axis-aligned boxes only); no slopes, no rotating
  surfaces. Ballistic leaps use simple clamped integration.
- Wall-run detection probes 1.3m to the player's left/right; walls beyond
  that distance are not detected, and the surface the player is standing on
  never counts as a wall (fixed behavior, see QA_AUDIT_REPORT.md).
- Falling past the map edge (-2m) counts as death by hazard.

AI LIMITATIONS
--------------
- Chasers cannot path around buildings: they either chase on the same roof,
  leap a gap <= 9m in a straight line, or give up after 6 seconds. They can
  not plan multi-roof routes to the player.
- Patrol targets are random within roof bounds (no waypoints).

AUDIO LIMITATIONS
-----------------
- Music is 22.05 kHz mono 16-bit WAV (loop length 90s, ~3.97 MB each) -
  a deliberate size/quality trade-off for a ~21 MB APK. Regenerate at higher
  quality with generate_music.py if storage allows.
- MediaPlayer loop boundaries are used without gapless-playback callbacks;
  a tiny seam may be audible on some devices.
- No 3D spatialization; all SFX play at full pan.

CONTENT LIMITATIONS
-------------------
- 3 missions on one static map (no procedural or additional levels).
- Difficulty comes from item placement + chaser count, not scripted events.
- Menus are functional but plain (standard Android widgets over the game).

BUILD LIMITATIONS
-----------------
- Debug-signed only; no release keystore or Play Store pipeline.
- The 4-byte zipalign is a pure-Python implementation inside the build
  script (no zipalign binary in Termux); it has been verified by apksigner
  but is less battle-tested than the SDK tool.
- `generate_music.py` / `generate_assets.py` regenerate bundled assets; they
  are the canonical source of the audio, so hand-edited WAVs would be
  overwritten on regeneration.
