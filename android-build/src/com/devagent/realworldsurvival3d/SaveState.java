package com.devagent.realworldsurvival3d;

import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;

/**
 * SaveState - Complete serializable game state snapshot with CRC32 integrity verification.
 * Pure Java, decoupled from Android runtime for 100% JVM unit testability.
 */
public class SaveState {
    public static final int CURRENT_VERSION = 1;
    public static final String HEADER = "# REALWORLD SURVIVAL 3D SAVE FILE v" + CURRENT_VERSION;

    public static class CorruptedSaveException extends Exception {
        public CorruptedSaveException(String message) {
            super(message);
        }
    }

    public static class SlotEntry {
        public String typeName;
        public int count;

        public SlotEntry(String typeName, int count) {
            this.typeName = typeName;
            this.count = count;
        }
    }

    public static class VehicleState {
        public String name;
        public float x, y, z, yaw, health;

        public VehicleState(String name, float x, float y, float z, float yaw, float health) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.health = health;
        }
    }

    public int version = CURRENT_VERSION;
    public long timestamp = System.currentTimeMillis();

    // Mode: 0 = PARKOUR, 1 = OPEN_WORLD
    public int gameMode = 1;

    // Player state
    public float playerX = 150f;
    public float playerY = 1.8f;
    public float playerZ = 20f;
    public float playerYaw = 0f;
    public float playerHealth = 100f;
    public float playerMaxHealth = 100f;
    public float playerStamina = 100f;
    public float playerAdrenaline = 0f;
    public int score = 0;
    public int currency = 0;
    public int bestScore = 0;

    // Parkour Campaign Mode
    public int parkourCurrentMission = 1;
    public int parkourCompletedMissions = 0;
    public int itemsCollected = 0;

    // Open World Story Mode ("The Long Signal")
    public int storyMissionIndex = 0;
    public int storyCurrentCount = 0;
    public float storyTimer = 0f;
    public boolean storyCompleted = false;
    public int dataSlatesCollected = 0;
    public boolean[] dataSlatesStatus = new boolean[5];

    // POIs discovered
    public final List<String> discoveredPoiNames = new ArrayList<>();

    // Inventory: 12 slots
    public final List<SlotEntry> inventorySlots = new ArrayList<>();

    // Day/Night Environment
    public float dayNightTime = 120.0f; // seconds
    public int weatherPreset = 0;

    // Vehicles: index of active vehicle (-1 if on foot)
    public int activeVehicleIndex = -1;
    public final List<VehicleState> vehicles = new ArrayList<>();

    // Wuxia Subsystem States (Phases 1, 5, 6, 7, 10)
    public String customizationStr = "";
    public String weaponsStr = "";
    public String qiStr = "";
    public String skillTreeStr = "";
    public String reputationStr = "";

    public SaveState() {
        // Defaults to initial survival state
    }

    /** Serializes the entire state into a validated string with CRC32 header. */
    public String serialize() {
        StringBuilder sb = new StringBuilder();
        sb.append("version:").append(version).append("\n");
        sb.append("timestamp:").append(timestamp).append("\n");
        sb.append("game_mode:").append(gameMode).append("\n");
        sb.append("player_x:").append(playerX).append("\n");
        sb.append("player_y:").append(playerY).append("\n");
        sb.append("player_z:").append(playerZ).append("\n");
        sb.append("player_yaw:").append(playerYaw).append("\n");
        sb.append("player_health:").append(playerHealth).append("\n");
        sb.append("player_max_health:").append(playerMaxHealth).append("\n");
        sb.append("player_stamina:").append(playerStamina).append("\n");
        sb.append("player_adrenaline:").append(playerAdrenaline).append("\n");
        sb.append("score:").append(score).append("\n");
        sb.append("currency:").append(currency).append("\n");
        sb.append("best_score:").append(bestScore).append("\n");
        sb.append("parkour_current_mission:").append(parkourCurrentMission).append("\n");
        sb.append("parkour_completed_missions:").append(parkourCompletedMissions).append("\n");
        sb.append("items_collected:").append(itemsCollected).append("\n");
        sb.append("story_mission_index:").append(storyMissionIndex).append("\n");
        sb.append("story_current_count:").append(storyCurrentCount).append("\n");
        sb.append("story_timer:").append(storyTimer).append("\n");
        sb.append("story_completed:").append(storyCompleted).append("\n");
        sb.append("data_slates_collected:").append(dataSlatesCollected).append("\n");

        sb.append("data_slates_status:");
        for (int i = 0; i < dataSlatesStatus.length; i++) {
            sb.append(dataSlatesStatus[i] ? "1" : "0");
            if (i < dataSlatesStatus.length - 1) sb.append(",");
        }
        sb.append("\n");

        sb.append("discovered_pois:");
        for (int i = 0; i < discoveredPoiNames.size(); i++) {
            sb.append(discoveredPoiNames.get(i));
            if (i < discoveredPoiNames.size() - 1) sb.append(",");
        }
        sb.append("\n");

        sb.append("inventory_slots:");
        for (int i = 0; i < inventorySlots.size(); i++) {
            SlotEntry s = inventorySlots.get(i);
            sb.append(s.typeName).append(":").append(s.count);
            if (i < inventorySlots.size() - 1) sb.append(";");
        }
        sb.append("\n");

        sb.append("day_night_time:").append(dayNightTime).append("\n");
        sb.append("weather_preset:").append(weatherPreset).append("\n");
        sb.append("active_vehicle_idx:").append(activeVehicleIndex).append("\n");

        sb.append("vehicles:");
        for (int i = 0; i < vehicles.size(); i++) {
            VehicleState v = vehicles.get(i);
            sb.append(v.name).append(":").append(v.x).append(":").append(v.y).append(":")
              .append(v.z).append(":").append(v.yaw).append(":").append(v.health);
            if (i < vehicles.size() - 1) sb.append(";");
        }
        sb.append("\n");

        if (customizationStr != null && !customizationStr.isEmpty()) {
            sb.append("customization:").append(customizationStr).append("\n");
        }
        if (weaponsStr != null && !weaponsStr.isEmpty()) {
            sb.append("weapons:").append(weaponsStr).append("\n");
        }
        if (qiStr != null && !qiStr.isEmpty()) {
            sb.append("qi_system:").append(qiStr).append("\n");
        }
        if (skillTreeStr != null && !skillTreeStr.isEmpty()) {
            sb.append("skill_tree:").append(skillTreeStr).append("\n");
        }
        if (reputationStr != null && !reputationStr.isEmpty()) {
            sb.append("reputation:").append(reputationStr).append("\n");
        }

        String payload = sb.toString();
        CRC32 crc = new CRC32();
        try {
            crc.update(payload.getBytes("UTF-8"));
        } catch (Exception e) {
            crc.update(payload.getBytes());
        }
        long checksum = crc.getValue();
        return HEADER + "\nCRC:0x" + Long.toHexString(checksum).toUpperCase() + "\n" + payload;
    }

    /** Deserializes a validated save string with CRC32 integrity check. */
    public static SaveState deserialize(String content) throws CorruptedSaveException {
        if (content == null || content.trim().isEmpty()) {
            throw new CorruptedSaveException("Save content is empty");
        }
        String[] lines = content.split("\r?\n");
        if (lines.length < 3) {
            throw new CorruptedSaveException("Save content is truncated (less than 3 lines)");
        }
        if (!lines[0].startsWith("# REALWORLD SURVIVAL 3D SAVE FILE")) {
            throw new CorruptedSaveException("Invalid save file header: " + lines[0]);
        }
        if (!lines[1].startsWith("CRC:0x")) {
            throw new CorruptedSaveException("Missing CRC checksum header: " + lines[1]);
        }

        String expectedHex = lines[1].substring(6).trim();
        long expectedCrc;
        try {
            expectedCrc = Long.parseLong(expectedHex, 16);
        } catch (Exception e) {
            throw new CorruptedSaveException("Malformed CRC hex: " + expectedHex);
        }

        StringBuilder payloadSb = new StringBuilder();
        for (int i = 2; i < lines.length; i++) {
            payloadSb.append(lines[i]).append("\n");
        }
        String payload = payloadSb.toString();
        CRC32 crc = new CRC32();
        try {
            crc.update(payload.getBytes("UTF-8"));
        } catch (Exception e) {
            crc.update(payload.getBytes());
        }
        long actualCrc = crc.getValue();
        if (actualCrc != expectedCrc) {
            throw new CorruptedSaveException("Save file corruption detected! Checksum mismatch: expected 0x"
                    + expectedHex + " but computed 0x" + Long.toHexString(actualCrc).toUpperCase());
        }

        SaveState s = new SaveState();
        for (int i = 2; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            int colon = line.indexOf(':');
            if (colon <= 0) continue;
            String key = line.substring(0, colon).trim();
            String val = line.substring(colon + 1).trim();

            switch (key) {
                case "version": s.version = parseInt(val, CURRENT_VERSION); break;
                case "timestamp": s.timestamp = parseLong(val, s.timestamp); break;
                case "game_mode": s.gameMode = parseInt(val, 1); break;
                case "player_x": s.playerX = parseFloat(val, s.playerX); break;
                case "player_y": s.playerY = parseFloat(val, s.playerY); break;
                case "player_z": s.playerZ = parseFloat(val, s.playerZ); break;
                case "player_yaw": s.playerYaw = parseFloat(val, s.playerYaw); break;
                case "player_health": s.playerHealth = parseFloat(val, s.playerHealth); break;
                case "player_max_health": s.playerMaxHealth = parseFloat(val, s.playerMaxHealth); break;
                case "player_stamina": s.playerStamina = parseFloat(val, s.playerStamina); break;
                case "player_adrenaline": s.playerAdrenaline = parseFloat(val, s.playerAdrenaline); break;
                case "score": s.score = parseInt(val, s.score); break;
                case "currency": s.currency = parseInt(val, s.currency); break;
                case "best_score": s.bestScore = parseInt(val, s.bestScore); break;
                case "parkour_current_mission": s.parkourCurrentMission = parseInt(val, s.parkourCurrentMission); break;
                case "parkour_completed_missions": s.parkourCompletedMissions = parseInt(val, s.parkourCompletedMissions); break;
                case "items_collected": s.itemsCollected = parseInt(val, s.itemsCollected); break;
                case "story_mission_index": s.storyMissionIndex = parseInt(val, s.storyMissionIndex); break;
                case "story_current_count": s.storyCurrentCount = parseInt(val, s.storyCurrentCount); break;
                case "story_timer": s.storyTimer = parseFloat(val, s.storyTimer); break;
                case "story_completed": s.storyCompleted = Boolean.parseBoolean(val); break;
                case "data_slates_collected": s.dataSlatesCollected = parseInt(val, s.dataSlatesCollected); break;
                case "data_slates_status":
                    String[] parts = val.split(",");
                    for (int p = 0; p < parts.length && p < s.dataSlatesStatus.length; p++) {
                        s.dataSlatesStatus[p] = "1".equals(parts[p].trim());
                    }
                    break;
                case "discovered_pois":
                    s.discoveredPoiNames.clear();
                    if (!val.isEmpty()) {
                        String[] names = val.split(",");
                        for (String n : names) {
                            String trimmed = n.trim();
                            if (!trimmed.isEmpty()) s.discoveredPoiNames.add(trimmed);
                        }
                    }
                    break;
                case "inventory_slots":
                    s.inventorySlots.clear();
                    if (!val.isEmpty()) {
                        String[] slots = val.split(";");
                        for (String slotStr : slots) {
                            String[] sp = slotStr.split(":");
                            if (sp.length >= 2) {
                                s.inventorySlots.add(new SlotEntry(sp[0].trim(), parseInt(sp[1].trim(), 1)));
                            }
                        }
                    }
                    break;
                case "day_night_time": s.dayNightTime = parseFloat(val, s.dayNightTime); break;
                case "weather_preset": s.weatherPreset = parseInt(val, s.weatherPreset); break;
                case "active_vehicle_idx": s.activeVehicleIndex = parseInt(val, -1); break;
                case "vehicles":
                    s.vehicles.clear();
                    if (!val.isEmpty()) {
                        String[] vList = val.split(";");
                        for (String vStr : vList) {
                            String[] vp = vStr.split(":");
                            if (vp.length >= 6) {
                                s.vehicles.add(new VehicleState(
                                        vp[0].trim(),
                                        parseFloat(vp[1], 0f),
                                        parseFloat(vp[2], 0f),
                                        parseFloat(vp[3], 0f),
                                        parseFloat(vp[4], 0f),
                                        parseFloat(vp[5], 100f)
                                ));
                            }
                        }
                    }
                    break;
                case "customization": s.customizationStr = val; break;
                case "weapons": s.weaponsStr = val; break;
                case "qi_system": s.qiStr = val; break;
                case "skill_tree": s.skillTreeStr = val; break;
                case "reputation": s.reputationStr = val; break;
            }
        }
        return s;
    }

    private static float parseFloat(String val, float def) {
        try { return Float.parseFloat(val); } catch (Exception e) { return def; }
    }

    private static int parseInt(String val, int def) {
        try { return Integer.parseInt(val); } catch (Exception e) { return def; }
    }

    private static long parseLong(String val, long def) {
        try { return Long.parseLong(val); } catch (Exception e) { return def; }
    }
}
