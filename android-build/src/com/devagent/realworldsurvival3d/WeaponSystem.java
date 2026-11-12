package com.devagent.realworldsurvival3d;

import java.util.ArrayList;
import java.util.List;

/**
 * WeaponSystem - Manages two-weapon loadouts, instant weapon switching, and weapon arsenals (Phase 5).
 */
public class WeaponSystem {

    public WuxiaWeapon primaryWeapon;
    public WuxiaWeapon secondaryWeapon;
    public int activeSlot = 0; // 0 = primary, 1 = secondary

    public final List<WuxiaWeapon> inventoryWeapons = new ArrayList<>();

    // Switching animation state
    public float switchCooldown = 0f;
    public static final float SWITCH_DURATION = 0.25f;
    public boolean justSwitched = false;

    public WeaponSystem() {
        primaryWeapon = new WuxiaWeapon(WuxiaWeapon.WeaponType.SWORD);
        secondaryWeapon = new WuxiaWeapon(WuxiaWeapon.WeaponType.DUAL_BLADES);
        inventoryWeapons.add(primaryWeapon);
        inventoryWeapons.add(secondaryWeapon);

        // Pre-populate inventory with available weapon classes
        inventoryWeapons.add(new WuxiaWeapon(WuxiaWeapon.WeaponType.SPEAR));
        inventoryWeapons.add(new WuxiaWeapon(WuxiaWeapon.WeaponType.GREATSWORD));
        inventoryWeapons.add(new WuxiaWeapon(WuxiaWeapon.WeaponType.STAFF));
        inventoryWeapons.add(new WuxiaWeapon(WuxiaWeapon.WeaponType.BOW));
        inventoryWeapons.add(new WuxiaWeapon(WuxiaWeapon.WeaponType.FAN));
        inventoryWeapons.add(new WuxiaWeapon(WuxiaWeapon.WeaponType.UMBRELLA));
        inventoryWeapons.add(new WuxiaWeapon(WuxiaWeapon.WeaponType.ROPE_DART));
    }

    /** Returns currently active weapon. */
    public WuxiaWeapon getActiveWeapon() {
        return activeSlot == 0 ? primaryWeapon : secondaryWeapon;
    }

    /** Returns currently inactive weapon. */
    public WuxiaWeapon getInactiveWeapon() {
        return activeSlot == 0 ? secondaryWeapon : primaryWeapon;
    }

    /**
     * Instantly switches between primary and secondary weapon loadout.
     * Returns true if switch was performed.
     */
    public boolean switchWeapon() {
        if (switchCooldown > 0f) return false;
        activeSlot = (activeSlot == 0) ? 1 : 0;
        switchCooldown = SWITCH_DURATION;
        justSwitched = true;
        return true;
    }

    /** Equips a weapon from inventory into primary or secondary slot. */
    public void equipWeapon(int slot, WuxiaWeapon.WeaponType type) {
        WuxiaWeapon target = null;
        for (WuxiaWeapon w : inventoryWeapons) {
            if (w.type == type) {
                target = w;
                break;
            }
        }
        if (target == null) {
            target = new WuxiaWeapon(type);
            inventoryWeapons.add(target);
        }

        if (slot == 0) {
            primaryWeapon = target;
        } else {
            secondaryWeapon = target;
        }
    }

    public void update(float dt) {
        justSwitched = false;
        if (switchCooldown > 0f) {
            switchCooldown = Math.max(0f, switchCooldown - dt);
        }
    }

    public String serialize() {
        StringBuilder sb = new StringBuilder();
        sb.append(activeSlot).append("|");
        sb.append(primaryWeapon.serialize()).append("|");
        sb.append(secondaryWeapon.serialize()).append("|");
        for (int i = 0; i < inventoryWeapons.size(); i++) {
            sb.append(inventoryWeapons.get(i).serialize());
            if (i < inventoryWeapons.size() - 1) sb.append(";");
        }
        return sb.toString();
    }

    public void deserialize(String str) {
        if (str == null || str.trim().isEmpty()) return;
        try {
            String[] parts = str.split("\\|");
            if (parts.length >= 1) activeSlot = Integer.parseInt(parts[0]);
            if (parts.length >= 2) primaryWeapon = WuxiaWeapon.deserialize(parts[1]);
            if (parts.length >= 3) secondaryWeapon = WuxiaWeapon.deserialize(parts[2]);
            if (parts.length >= 4) {
                inventoryWeapons.clear();
                String[] list = parts[3].split(";");
                for (String wStr : list) {
                    if (!wStr.trim().isEmpty()) {
                        inventoryWeapons.add(WuxiaWeapon.deserialize(wStr));
                    }
                }
            }
        } catch (Exception ignored) {}
    }
}
