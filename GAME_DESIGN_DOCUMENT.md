# REALWORLD 3D — Game Design Document

**Version:** 2.0 · **Date:** 2026-11-11

## 1. High Concept
An original open-world survival adventure: six months after "the static" cut the
city off, you play **Rin**, a courier with the last working signal kit. Cross
the broken districts, rebuild the network, and send the long signal home —
while feral stalkers hunt anything that moves after dusk.

Genre: third-person open-world action-adventure.
Platform: Android ( GLES2, minSdk 24 ). Session length: 45–60 min story + free roam.

## 2. World
One continuous 896 × 896 m map (−448…+448), streamed in 64 m chunks:

| Region | ID | Feel | Key locations |
|---|---|---|---|
| City Core | 1 | Dense grid, shops, safe zone | Safehouse HQ, Market St, Garage |
| Residential | 2 | Low houses, gardens | Corner store, block yards |
| Riverside Village | 3 | Wooden houses, pier | Bridge, docks, campfire |
| West Forest | 4 | Pine forest, trails | Ranger hut, hidden glades |
| North Hills | 5 | High terrain, radio tower | Watchtower, relay mast |
| River | 6 | Water corridor | Bridge crossing, weir |

Roads: ring road + cross streets (graph). Interiors: shops/safehouse have
open doorways and walkable insides. Safe zone = City Core plaza (no hostiles,
slow healing).

## 3. Story — "The Long Signal"
1. **Static** (intro) — Reach the safehouse; the city is silent.
2. **Salvage** — Collect 4 radio parts (forest / hills / village / city).
3. **The Engineer** — Talk to Mara at the workshop (residential).
4. **High Ground** — Drive a vehicle to the north watchtower.
5. **Nightfall** — Survive the stalker wave at the village bridge.
6. **The Long Signal** — Return to the safehouse; transmission restored.

Side objectives: 5 hidden data slates; supply caches (restock inventory).
Failure: death → retry mission (world persists).

## 4. Player Systems
- Movement: idle/walk/jog/run/sprint (stamina)/jump/fall/land/crouch.
- Survival: health (regen in safe zone), stamina, adrenaline-lite (score combo).
- Interaction: pickups, doors, containers (crates), NPC dialog, vehicles.
- Inventory: supplies, medkits, radio parts, data slates (quantities + descriptions).

## 5. NPC & Enemies
- Civilians (8): walk road routes, idle around POIs, react (stop, face, chat line).
- Hostile stalkers (up to 6 at night / in hostile zones): patrol → detect (18 m,
  LOS) → chase → attack; give up and reset to patrol; defeated by attack or outrun.

## 6. Vehicles
- Car (4 seats visual) & motorcycle: enter/exit, throttle/steer/brake/reverse,
  collision damage, health-limited top speed, follow-cam, horn (ACTION).

## 7. Animation (fallback disclosure)
No licensed skeletal animation set is available offline. **Fallback:** procedural
limb animation state machine on box-composite characters (arms/legs swing by
gait phase, poses per state, 0.18 s blends). All animations are original and
generated; none are copied from third parties. This is identified as the
animation quality ceiling of this build.

## 8. Audio
Original synthesized music (day: calm pads; night: tense low pulse), 18 SFX,
weather-lite wind layer, dialog bleeps. Separate master/music/SFX volumes.

## 9. UI
Main menu (mode select: Open World / Rooftop Parkour), settings, pause,
inventory, fullscreen map, HUD (health/stamina, mission tracker, minimap,
waypoint guide, prompt line), mission complete / game over screens.

## 10. Controls
See CONTROLS.md. Everything is reachable with two thumbs; layout configurable
(size/opacity/left-handed).
