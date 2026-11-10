package com.devagent.realworldsurvival3d;

/**
 * Item - Collectible survival & parkour pickups scattered across rooftops.
 */
public class Item {
    public enum Type {
        ENERGY_DRINK,
        MEDKIT,
        DATA_DRIVE,
        PARKOUR_TOKEN
    }

    public Type type;
    public float x, y, z;
    public boolean collected = false;
    public float rotation = 0f;
    public float bobOffset = 0f;

    public Item(Type type, float x, float y, float z) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void update(float dt) {
        if (collected) return;
        rotation += 90f * dt;
        if (rotation >= 360f) rotation -= 360f;
        bobOffset = (float) Math.sin(System.currentTimeMillis() * 0.004) * 0.2f;
    }

    public void onCollected(Player player) {
        collected = true;
        switch (type) {
            case ENERGY_DRINK:
                player.stamina = player.maxStamina;
                player.adrenaline = Math.min(player.maxAdrenaline, player.adrenaline + 30f);
                player.score += 25;
                break;
            case MEDKIT:
                player.health = Math.min(player.maxHealth, player.health + 45f);
                player.score += 25;
                break;
            case DATA_DRIVE:
                player.currency += 50;
                player.score += 100;
                break;
            case PARKOUR_TOKEN:
                player.currency += 10;
                player.score += 50;
                player.adrenaline = Math.min(player.maxAdrenaline, player.adrenaline + 15f);
                break;
        }
    }
}
