# REALWORLD 3D — Known Issues (living document)

Maintained per phase; nothing is hidden.

## Open issues (Phase 0)
1. **No device testing possible in this environment** — no adb, no emulator,
   Termux cannot self-install APKs. All device-runtime claims stay NOT TESTED
   until a physical install happens.
2. **Godot unavailable** — the environment's `godot` binary is a broken Python
   shim and no export templates exist; the game is therefore built on the
   existing native Java/GLES2 engine (documented decision).
3. **Debug-signed APK only** — no release keystore yet; Play Store upload is
   out of scope until a release key is provided.
4. **Procedural fallback animations** — no skeletal animation set is available
   offline; characters use procedural limb animation (GDD §7). Visual quality
   ceiling acknowledged.
5. **Music is 22.05 kHz mono WAV** — deliberate size trade-off; regenerable
   at higher quality via `generate_music.py`.

## Resolved during development
(appended as issues are fixed, with the phase that fixed them)
