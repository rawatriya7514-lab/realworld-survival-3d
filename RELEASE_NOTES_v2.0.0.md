# RealWorld Survival 3D — v2.0.0 (Wuxia Jianghu Open-World Action RPG)

A major evolution and architectural upgrade inspired by *Where Winds Meet* gameplay mechanics, featuring a 100% original Wuxia martial arts open-world action RPG engine built from scratch in pure Java and OpenGL ES 2.0.

---

## What's New in v2.0.0

### Phase 1 — Character Customization & Presets
- **Customizable Appearance:** Male & Female character models, face shapes (Oval, Square, Sharp, Round, Chiseled), skin tones (Fair, Warm, Tan, Deep, Bronze), eye colors, hairstyles (Topknot, High Ponytail, Flowing Waves, Braided Martial, Braided Warrior, Shaved Temple), hair colors, facial hair styles, outfits (Azure Robes, Shadow Vest, Scholar Hanfu, Armored Doupao, Wanderer Linen), and accessories (Bamboo Hat, Jade Amulet, Dragon Girdle, Silk Scarf, Demon Mask).
- **5 Original Jianghu Presets:** Wandering Swordsman, Shadow Blade, Mystic Scholar, Dragon Vanguard, and Jade Lotus Prodigy.
- **Persistence:** Selected character appearance, equipment, and colors persist seamlessly across restarts.

### Phase 2 — Advanced Movement & Traversal Ability System
- **Locomotion:** Walk, jog, run, sprint, crouch, slide, backward motion, and strafing with smooth acceleration and directional rotation.
- **Triple Jump & Air Maneuvers:** Ground jump -> Air jump 1 -> Air jump 2 with directional air control.
- **Air Dash & Aerial Descent Slam:** High-speed horizontal air dash; fast aerial descent plunge with radial ground-slam shockwave VFX upon landing.
- **Glider Traversal:** Aerodynamic glider with pitch/yaw steering, wind resistance, stamina drain, and smooth flight camera.
- **Swimming & Diving:** Surface swimming, underwater diving with a 30-second Oxygen meter, and safe shore emergence.
- **Traversal Progression:** Unlockable movement abilities earned through quests and exploration.

### Phase 3 & 4 — Martial Arts Action Combat & Defense System
- **Combos & Attack Windows:**
  - Fast Light Attack 3-hit combo (`Light -> Light -> Light`).
  - Mixup Finisher (`Light -> Heavy`).
  - Guard Breaker Combo (`Heavy -> Heavy`).
  - Aerial Strike and Dodge Riposte attacks.
  - Charged Heavy Attack (1.0s charge for 2.2x damage and guard break).
- **0.20s Perfect Parry Window:**
  - Completely negates incoming damage (0 damage).
  - Interrupts enemy attack animations and triggers hit-stop micro-freeze.
  - Spawns parry spark VFX and generates Qi energy.
  - Opens a 1.2s counterattack riposte window.
- **Normal Block & Guard Break:**
  - 70% damage reduction; drains stamina per blocked impact; guard breaks when stamina is depleted.
- **Directional Dodge Roll:**
  - 0.25s invulnerability frames (i-frames) allowing skill-based evasion through enemy swings.

### Phase 5 — 9-Weapon System & Instant Weapon Switching
- **9 Weapon Classes:**
  1. *Sword:* Balanced damage and speed with standard martial combos.
  2. *Dual Blades:* Extremely rapid attack speed and combo fluidity.
  3. *Spear:* Extended reach and piercing thrust attacks.
  4. *Greatsword:* High impact damage, wide cleaves, and massive guard damage.
  5. *Staff:* Agile sweeping strikes and high poise damage.
  6. *Bow:* Ranged precision projectile attacks.
  7. *Fan:* Rapid wind attacks and mystic Qi synergies.
  8. *Umbrella:* Defensive martial style combining blocks with concealed strikes.
  9. *Rope Dart:* Extended reach and enemy pulling mechanics.
- **Two-Weapon Active Loadout:** Instant weapon switching button with weapon-specific moveset updates and real-time 3D mesh rendering.
- **Weapon Progression:** Levels 1 to 5 with scalable base damage, attack speed, range, and guard damage.

### Phase 6 — Mystic Qi Energy & Abilities
- **Qi Resource Meter:** 0–100 energy pool, refueled through martial combos and perfect parries.
- **4 Ability Categories:**
  - *Offensive:* Qi Slash (ranged energy arc) and Ground Shockwave (radial earth fissure).
  - *Defensive:* Iron Body (absorbs 75 damage) and Dragon Deflection (projectile counter).
  - *Movement:* Wind Step (instant blink dash) and Skyward Leap (vertical aerial boost).
  - *Control:* Celestial Pull (yanks distant enemies to player) and Meridian Freeze (2.5s stun).

### Phase 7 — Skill Tree & Inner Ways Passives
- **7 Mastery Branches:** Sword, Spear, Dual Weapon, Defensive, Movement, Mystic, and Survival.
- **5 Original Inner Ways:**
  - *Flowing River:* +15% stamina regeneration rate and Qi refund on parry.
  - *Mountain Unshaken:* -30% guard stamina drain and +20 max posture.
  - *Viper's Precision:* +15% critical strike chance and riposte damage.
  - *Windrider's Grace:* -50% glider stamina drain and +20% flight speed.
  - *Tiger's Ferocity:* +20% combo finisher and charged attack damage.

### Phase 8 — Advanced Enemy AI & Multi-Phase Boss
- **7 Enemy Classes:** Basic Fighter, Fast Attacker, Heavy Attacker, Ranged Attacker, Defensive Fighter, Elite Fighter, and Boss.
- **Combat AI:** Telegraph warning flashes, directional combos, blocking, dodging, posture stagger gauge, and recovery.
- **Original Multi-Phase Boss Encounter ("Shadow Blade Master Ren"):**
  - *Phase 1:* Disciplined martial sword combos and defensive parries.
  - *Phase 2:* Enraged speed, dual-blade flurry, and ground shockwave attacks.
  - *Phase 3:* Awakened Mystic Qi form with celestial pull and arena-wide strikes.
  - Complete victory rewards screen with EXP, gold, and weapon upgrade items.

### Phase 9 — Open World Regions & Fast Travel
- **7 Interconnected Regional Zones:** Ancient City, Emerald Bamboo Grove, Mist Peak Hamlet, Dragon Cloud Monastery, Azure Dragon River, Training Grounds, and Desert Stele Ruins.
- **5 Fast Travel Shrines:** Instant teleportation with waypoint tracking and exploration % calculations.

### Phase 10 — NPCs, Reputation & Jianghu World Life
- **Karma & Reputation:** Dynamic morality scale (-100 Villain to +100 Hero).
- **Merchant Price Scaling:** 20% discount for virtuous heroes; 40% markup for villains with bounties.
- **Crime & Bounty System:** Crime detection, magistrate bounty accumulation, guard pursuit, and bounty clearance.
- **Jianghu NPCs:** Wandering Master (dialogue & training), Master Blacksmith (weapon upgrading), Herb Alchemist (medicine), Imperial Guards, and Merchants.
- **Side Activities:** Gathering mountain herbs and mining ore veins.

### Phase 11 — Android Touch Combat UI & Layout Editor
- **Left Side:** Dynamic movement joystick and sprint button.
- **Right Side:** Light Attack, Heavy Attack, Block/Parry, Dodge, Jump, Weapon Switch, Qi Skill, Target Lock-On.
- **Top Side:** Health Bar, Stamina Bar, Qi Bar, Target Health & Posture Gauges, Minimap, Objective Tracker.
- **Customization:** Adaptive scaling, touch geometry separation, and left-handed layout toggle.

### Phase 12 — Combat Camera & Visual Effects
- 3rd-person orbital follow camera with target lock-on tracking.
- Camera trauma shake on heavy impacts and shockwaves.
- Hit-stop micro-freeze effect on parries and heavy finishers.
- Real-time 3D weapon meshes and aerodynamic glider wings rendering in OpenGL ES 2.0.

---

## Quality Assurance & Verification
- **Test Suites:** 17 automated test suites passing with 100% success rate (including 127 tests in `tests/WuxiaSystemTest.java`).
- **Signing Scheme:** Universal Android compatibility signed with **v1 (JAR)**, **v2 (APK Signature Scheme v2)**, and **v3 (APK Signature Scheme v3)**.
- **Installation:** Delivered directly to `/sdcard/Download/RealWorldSurvival3D.apk` for immediate installation on Android devices.
