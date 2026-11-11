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

## Open issues (Phase 13)
1. **Final QA & Dual-Mode End-to-End Audit** — Comprehensive cross-system verification covering parkour campaign, 896m open world, vehicle driving, combat, mission progression, inventory, and save/load planned for Phase 14.

## Resolved during development
- PHASE 13: Spatial frustum and sphere culling engine (`CullingOptimizer.java`), in-place zero-allocation
  TRS matrix generation, elimination of per-frame heap matrix allocations in `WorldRenderer.java`,
  delta-time hitch clamping [1ms, 100ms] and frame budget regulation (60 FPS/30 FPS) in `GameView.java`.
- PHASE 12: Full versioned game save/load persistence (`SaveState.java`, `SaveFileManager.java`),
  CRC32 checksum validation, atomic staging with `.tmp` and `.bak` rotation, automatic recovery
  from corrupted saves, and full state restoration for Open-World and Parkour campaign modes.
- PHASE 11: Audio engine (`AudioMixer.java`), Master/Music/SFX volume controls, vehicle engine
  speed-pitch modulation and skid/crash SFX, NPC greeting chirps, quest fanfare, combat hits,
  data slate chimes, 3D spatial attenuation and stereo panning, pure Java `SettingsConfig.java`,
  and dynamic graphics settings (quality, shadows, bloom) in `UIController.java` and `WorldRenderer.java`.
- PHASE 10: 12-slot backpack inventory (`Inventory.java`) with item stacking and consumable
  survival effects (medkit healing, energy drink stamina/adrenaline boosts), drivable land
  vehicles (`Vehicle.java`) for Scout SUV and Trail Bike Motorcycle with acceleration, braking,
  reverse, handbrake drift, obstacle collision rebound, hostile roadkill ramming, and 3D
  composite vehicle rendering in `WorldRenderer.java`.
- PHASE 9: "The Long Signal" 6-mission story arc with interactive objectives (locations,
  radio part salvage, NPC dialogue, elevation check, wave defense, transmission finale),
  5 hidden data slates across world landmarks, auto-waypoint synchronization, and
  retry resilience on death.
- PHASE 8: HunterEnemy and HostileAI articulated limb animation rigs, eliminating
  the legacy single-box enemy placeholder; Civilian NPCs with conversational
  dialogue cycles and proximity prompts; Central Plaza safe haven repulsion boundary
  (34m buffer prevents infected from invading safe zones); performance budget
  staggered AI updates (<40m every frame, 40-100m 5th-frame 5x dt, >100m frozen).
- PHASE 1: flat-channel trap on entering deep water (strictly-deeper rule),
  1 cm/frame creep epsilon, bridge-corridor approach flatten coverage
  (±85 m), leftover `considerFloor` stub, private `Terrain` constructor,
  stale `renderHUD(gl, …)` call. Details: TEST_REPORT.md §Phase 1.
- PHASE 2: analog walk/jog speed threshold boundary separation (walk <= 3.8,
  jog <= 6.0, run <= 8.5), mid-air jump-to-fall transition on vy < 0,
  step-up detection probe margin aligned with collision radius (prevents
  premature wall push-out before step-up), headroom validation on uncrouch
  and step-up (prevents clipping through low ceilings). Details: TEST_REPORT.md §Phase 2.
- PHASE 3: procedural limb animation state machine for box-composite humanoid
  (torso, head, arms, legs), speed-matched dynamic gait frequencies (WALK 8.5,
  JOG 12.0, RUN 15.0, SPRINT 19.0 rad/s), 0.18s cross-fade pose blending,
  breathing idle cycle, full 360-deg safety roll rotation, and renderer
  articulated limb transformation matrix hierarchy. Details: TEST_REPORT.md §Phase 3.
- PHASE 4: camera crouch eye-level dynamic adaptation (standing 1.45m to crouch 0.65m),
  building/obstacle raycast obstruction pull-in with 0.25m collision margin (camera
  cannot clip inside geometry), open-world terrain height and river surface minimum
  clearance clamp, pitch limits [-80, +80 deg], Dutch-angle roll on wall-runs,
  continuous pinch-zoom (0.8x-1.6x) and presets (CLOSE/NORMAL/FAR), pure Java 4x4
  view matrix math. Details: TEST_REPORT.md §Phase 4.
- PHASE 5: touch controls HUD layout engine (`TouchLayout.java`), dedicated CROUCH
  button with headroom validation, button scaling (0.8x-1.3x) and opacity (0.4-1.0),
  left-handed layout mirroring (joystick on right, buttons on left), joystick sensitivity
  multiplier (0.5x-1.5x), non-overlapping pairwise disjoint button positioning,
  and settings menu scroll container. Details: TEST_REPORT.md §Phase 5.
- PHASE 6: world map road network (`RoadNetwork.java`) with 33 nodes and Dijkstra
  shortest paths across regions and river bridge; step-by-step waypoint progression
  and dynamic auto-rerouting on >12m deviation (`NavigationTracker.java`); 896m
  viewport projection engine (`MapViewController.java`) with anchor-stable pan/zoom,
  rotating HUD minimap (heading straight UP), cardinal compass rim indicators,
  POI snapping, tap-to-set waypoint routing, 3D waypoint beacon beam, and fullscreen
  interactive 2D map overlay. Details: TEST_REPORT.md §Phase 6.
- PHASE 7: 8-minute Day/Night simulation (`DayNightCycle.java`) with continuous sun/moon
  orbital orientation, smooth diurnal atmospheric palettes (Dawn, Day, Dusk, Night),
  weather-lite atmospheric modulation (Clear, Overcast, Misty, Rain), and graphics
  presets (LOW/MED/HIGH); walkable architectural interiors (`BuildingInterior.java`)
  with wide doorway openings, solid ceilings, and survival furniture props for key
  POIs (Safehouse, Workshop, Garage, Ranger Hut). Details: TEST_REPORT.md §Phase 7.
