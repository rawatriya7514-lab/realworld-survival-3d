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

## Open issues (Phase 1)
1. **No swimming** — deep water (depth > 1.5 m) is a movement wall. Players
   can wade the shallows and cross at the bridge; a swim state comes later
   (environment phase) if design keeps it.
2. **Roads only in flat zones** — the 11 road strips are placed in the city/
   residential/village flats and the bridge corridor; no road surface on
   hills yet (hill driving is off-road terrain).
3. **Buildings have no interiors** — all structures are solid boxes; walkable
   interiors + doors are Phase 7.
4. **Open-world progress is not saved** — pausing/quitting an open-world
   session loses discovered-POI state (parkour autosave unaffected). Save
   phase: 12.
5. **No day/night cycle yet** — open world inherits the parkour dusk lighting;
   cycle lands with Phase 7.
6. **Water is a translucent plane, not a shader** — no waves/reflections;
   fine at dusk, revisit with graphics presets (Phase 7/13).
7. **POI discovery only** — shops/safe zones are discoverable but have no
   shop UI or healing yet (Phases 8–10).
8. **Unverified on-device GL path** — terrain VBO path, atlas texture and
   blended-water draws compile and are logic-tested, but have never rendered
   on a real screen (no adb). First device test is the top priority of the
   next phase.

## Open issues (Phase 2)
1. **Camera height does not yet lower when crouching** — player collision
   bounding box and visual model crouch properly (height drops to 0.8 m), but
   camera height follows standing head offset until Camera Phase 4.
2. **Procedural limb animations not yet speed-matched** — animation state
   machine and blending land in Phase 3.

## Resolved during development
- PHASE 1: flat-channel trap on entering deep water (strictly-deeper rule),
  1 cm/frame creep epsilon, bridge-corridor approach flatten coverage
  (±85 m), leftover `considerFloor` stub, private `Terrain` constructor,
  stale `renderHUD(gl, …)` call. Details: TEST_REPORT.md §Phase 1.
- PHASE 2: analog walk/jog speed threshold boundary separation (walk <= 3.8,
  jog <= 6.0, run <= 8.5), mid-air jump-to-fall transition on vy < 0,
  step-up detection probe margin aligned with collision radius (prevents
  premature wall push-out before step-up), headroom validation on uncrouch
  and step-up (prevents clipping through low ceilings). Details: TEST_REPORT.md §Phase 2.
