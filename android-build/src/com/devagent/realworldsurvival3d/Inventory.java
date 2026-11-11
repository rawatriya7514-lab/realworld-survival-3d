package com.devagent.realworldsurvival3d;

import java.util.ArrayList;
import java.util.List;

/**
 * Inventory - Backpack grid and item management engine (Phase 10).
 *
 * Implements:
 *  - 12-slot inventory with item stacking
 *  - Consumables (Medkit +45 HP, Energy Drink +stamina, Stamina Booster)
 *  - Crafting / quest items (Radio Parts, Data Slates, Fuel Canister, Vehicle Key)
 *  - Usable item logic applying real-time buffs to Player health/stamina/adrenaline
 *  - Pure Java (100% JVM-testable without Android runtime).
 */
public class Inventory {

    public static final int MAX_SLOTS = 12;

    public static class Slot {
        public Item.Type type;
        public int count;
        public int maxStack;
        public String name;
        public String description;

        public Slot(Item.Type type, int count, int maxStack, String name, String description) {
            this.type = type;
            this.count = count;
            this.maxStack = maxStack;
            this.name = name;
            this.description = description;
        }
    }

    private final List<Slot> slots = new ArrayList<>();

    public Inventory() {
        // Initial loadout: 2 Medkits, 1 Energy Drink
        addItem(Item.Type.MEDKIT, 2);
        addItem(Item.Type.ENERGY_DRINK, 1);
    }

    public List<Slot> getSlots() {
        return slots;
    }

    public int getSlotCount() {
        return slots.size();
    }

    public void clear() {
        slots.clear();
    }

    public boolean hasSpaceFor(Item.Type type, int count) {
        int maxStack = getMaxStack(type);
        int remaining = count;

        for (Slot s : slots) {
            if (s.type == type && s.count < maxStack) {
                remaining -= (maxStack - s.count);
                if (remaining <= 0) return true;
            }
        }
        int freeSlots = MAX_SLOTS - slots.size();
        return (freeSlots * maxStack >= remaining);
    }

    public boolean addItem(Item.Type type, int count) {
        if (count <= 0 || !hasSpaceFor(type, count)) return false;

        int maxStack = getMaxStack(type);
        int toAdd = count;

        // 1. Stack into existing slots
        for (Slot s : slots) {
            if (s.type == type && s.count < maxStack) {
                int add = Math.min(toAdd, maxStack - s.count);
                s.count += add;
                toAdd -= add;
                if (toAdd <= 0) return true;
            }
        }

        // 2. Create new slots
        while (toAdd > 0 && slots.size() < MAX_SLOTS) {
            int add = Math.min(toAdd, maxStack);
            slots.add(new Slot(type, add, maxStack, getItemName(type), getItemDescription(type)));
            toAdd -= add;
        }

        return toAdd <= 0;
    }

    public boolean removeItem(Item.Type type, int count) {
        if (count <= 0 || getItemCount(type) < count) return false;

        int toRemove = count;
        for (int i = slots.size() - 1; i >= 0 && toRemove > 0; i--) {
            Slot s = slots.get(i);
            if (s.type == type) {
                int rem = Math.min(toRemove, s.count);
                s.count -= rem;
                toRemove -= rem;
                if (s.count <= 0) {
                    slots.remove(i);
                }
            }
        }
        return true;
    }

    public int getItemCount(Item.Type type) {
        int total = 0;
        for (Slot s : slots) {
            if (s.type == type) total += s.count;
        }
        return total;
    }

    public boolean hasItem(Item.Type type) {
        return getItemCount(type) > 0;
    }

    /**
     * Consumes an item from inventory and applies survival effects to the player.
     */
    public boolean useItem(Item.Type type, Player player) {
        if (!hasItem(type) || player == null) return false;

        boolean applied = false;
        switch (type) {
            case MEDKIT:
                if (player.health < player.maxHealth) {
                    player.health = Math.min(player.maxHealth, player.health + 45.0f);
                    applied = true;
                }
                break;

            case ENERGY_DRINK:
                player.stamina = player.maxStamina;
                player.adrenaline = Math.min(player.maxAdrenaline, player.adrenaline + 30.0f);
                applied = true;
                break;

            case PARKOUR_TOKEN:
                player.score += 50;
                player.currency += 10;
                applied = true;
                break;

            case DATA_DRIVE:
                player.score += 100;
                player.currency += 50;
                applied = true;
                break;

            default:
                // Quest / non-consumable items cannot be directly ingested
                return false;
        }

        if (applied) {
            removeItem(type, 1);
        }
        return applied;
    }

    public static int getMaxStack(Item.Type type) {
        switch (type) {
            case MEDKIT:
            case ENERGY_DRINK:
                return 5;
            case PARKOUR_TOKEN:
            case DATA_DRIVE:
                return 20;
            case RADIO_PART:
                return 4;
            case DATA_SLATE:
                return 5;
            default:
                return 10;
        }
    }

    public static String getItemName(Item.Type type) {
        switch (type) {
            case MEDKIT: return "Military Medkit";
            case ENERGY_DRINK: return "Electrolyte Surge";
            case DATA_DRIVE: return "Encrypted Drive";
            case PARKOUR_TOKEN: return "Agility Token";
            case RADIO_PART: return "Transceiver Circuit";
            case DATA_SLATE: return "Survivor Slate";
            default: return "Survival Item";
        }
    }

    public static String getItemDescription(Item.Type type) {
        switch (type) {
            case MEDKIT: return "Restores 45 HP. Essential trauma dressing.";
            case ENERGY_DRINK: return "Instantly refills stamina and boosts adrenaline.";
            case DATA_DRIVE: return "Valuable corporate telemetry. Worth 50 currency.";
            case PARKOUR_TOKEN: return "Courier mark of honor. Grants points and currency.";
            case RADIO_PART: return "Key component for the long-range signal transmitter.";
            case DATA_SLATE: return "Audio log documenting the outbreak. Worth 150 points.";
            default: return "General scavenged equipment.";
        }
    }
}
