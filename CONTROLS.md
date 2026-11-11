# REALWORLD 3D — Controls (living document, finalized at Phase 5)

## Touch layout
| Zone / Button | Location | Action |
|---|---|---|
| Virtual joystick | left half (dynamic base) | analog movement |
| Camera drag | right half (outside buttons) | orbit look (sensitivity + invert-Y in Settings) |
| JUMP | bottom-right | jump / double context |
| SPRINT | right cluster | toggle sprint (stamina) |
| CROUCH | right cluster | crouch toggle |
| ACTION | right cluster | interact: pickups, NPC talk, enter/exit vehicle, containers |
| ATTACK | right cluster | melee (stomp/slide tackle vs stalkers) |
| MAP | top area | open fullscreen map |
| PAUSE | top-right | pause menu |
| INVENTORY | bottom (shortcut) | inventory screen |

## Vehicle mode (when driving)
Joystick Y = throttle/reverse, X = steering; BRAKE soft-key on SPRINT slot;
ACTION = enter/exit; horn on double-tap ACTION (mapped in code as needed).

## Camera
One-finger drag rotates; pinch on the right side zooms (0.8–1.6); camera auto
avoids walls; reset when opening map; optional shoulder offset.

## Settings (Controls section)
Joystick sensitivity · camera sensitivity · button size (0.8–1.3×) ·
button opacity (0.4–1.0) · left-handed layout · camera inversion.

All buttons are wired to real gameplay functions — no decorative buttons.
