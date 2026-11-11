import com.devagent.realworldsurvival3d.Inventory;
import com.devagent.realworldsurvival3d.Item;
import com.devagent.realworldsurvival3d.Mission;
import com.devagent.realworldsurvival3d.MissionManager;
import com.devagent.realworldsurvival3d.SaveFileManager;
import com.devagent.realworldsurvival3d.SaveState;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * SaveLoadTest - JVM unit test suite for Phase 12 (Save/Load Persistence).
 * Tests snapshot serialization, CRC32 checksum integrity verification,
 * atomic file operations, backup rotation, automatic corruption recovery,
 * and game state restoration across Open-World and Parkour modes.
 */
public class SaveLoadTest {
    private static int passed = 0;
    private static int failed = 0;

    private static void check(boolean condition, String desc) {
        if (condition) {
            System.out.println("  PASS  " + desc);
            passed++;
        } else {
            System.err.println("  FAIL  " + desc);
            failed++;
        }
    }

    private static void approx(float actual, float expected, float eps, String desc) {
        check(Math.abs(actual - expected) <= eps, desc + " (" + actual + " ~ " + expected + ")");
    }

    public static void main(String[] args) {
        System.out.println("=== SaveLoadTest (Phase 12) ===");

        File testDir = new File("tests/build/test_saves");
        testDir.mkdirs();
        File testFile = new File(testDir, "save_game_test.dat");

        try {
            testSaveStateDefaults();
            testSaveStateSerializationAndCrc();
            testCrcCorruptionDetection();
            testOpenWorldStateFidelity();
            testParkourStateFidelity();
            testAtomicFileSaving(testFile);
            testBackupRotation(testFile);
            testAutomaticCorruptionRecovery(testFile);
            testMissionManagerRestoration();
            testInventoryRestoration();
            testSaveCleanup(testFile);
        } finally {
            SaveFileManager.deleteSave(testFile);
            testDir.delete();
        }

        System.out.println("\nSaveLoadTest: " + passed + " passed, " + failed + " failed\n");
        if (failed > 0) System.exit(1);
    }

    // -----------------------------------------------------------------
    // Group 1: SaveState Defaults
    // -----------------------------------------------------------------
    private static void testSaveStateDefaults() {
        System.out.println("SaveState: default values");
        SaveState s = new SaveState();
        check(s.version == SaveState.CURRENT_VERSION, "version matches CURRENT_VERSION");
        check(s.timestamp > 0L, "timestamp initialized");
        approx(s.playerHealth, 100.0f, 0.001f, "default health is 100");
        approx(s.playerStamina, 100.0f, 0.001f, "default stamina is 100");
        check(s.score == 0, "default score is 0");
        check(s.currency == 0, "default currency is 0");
        check(s.storyMissionIndex == 0, "default storyMissionIndex is 0");
        check(!s.storyCompleted, "default storyCompleted is false");
        check(s.discoveredPoiNames.isEmpty(), "default discoveredPoiNames is empty");
        check(s.inventorySlots.isEmpty(), "default inventorySlots is empty");
    }

    // -----------------------------------------------------------------
    // Group 2: SaveState Serialization & CRC32
    // -----------------------------------------------------------------
    private static void testSaveStateSerializationAndCrc() {
        System.out.println("SaveState: serialization & CRC32 verification");
        SaveState s = new SaveState();
        s.score = 750;
        s.currency = 125;
        s.playerX = 210.5f;
        s.playerZ = 60.2f;

        String serialized = s.serialize();
        check(serialized != null && !serialized.isEmpty(), "serialized text generated");
        check(serialized.startsWith(SaveState.HEADER), "serialized text starts with valid header");
        check(serialized.contains("CRC:0x"), "serialized text contains CRC hex header");

        try {
            SaveState restored = SaveState.deserialize(serialized);
            check(restored != null, "deserialization succeeded");
            check(restored.score == 750, "restored score matches");
            check(restored.currency == 125, "restored currency matches");
            approx(restored.playerX, 210.5f, 0.01f, "restored playerX matches");
            approx(restored.playerZ, 60.2f, 0.01f, "restored playerZ matches");
        } catch (Exception e) {
            check(false, "deserialization threw unexpected error: " + e.getMessage());
        }
    }

    // -----------------------------------------------------------------
    // Group 3: CRC32 Corruption Detection
    // -----------------------------------------------------------------
    private static void testCrcCorruptionDetection() {
        System.out.println("SaveState: corruption detection");
        SaveState s = new SaveState();
        s.score = 500;
        String valid = s.serialize();

        // 1. Alter single byte in payload (modify score from 500 to 999)
        String corruptedPayload = valid.replace("score:500", "score:999");
        boolean threwPayload = false;
        try {
            SaveState.deserialize(corruptedPayload);
        } catch (SaveState.CorruptedSaveException e) {
            threwPayload = true;
        } catch (Exception ignored) {}
        check(threwPayload, "payload tampering triggers CorruptedSaveException");

        // 2. Corrupt CRC header hex
        String corruptedCrc = valid.replace("CRC:0x", "CRC:0xDEADBEEF");
        boolean threwCrc = false;
        try {
            SaveState.deserialize(corruptedCrc);
        } catch (SaveState.CorruptedSaveException e) {
            threwCrc = true;
        } catch (Exception ignored) {}
        check(threwCrc, "tampered CRC hex triggers CorruptedSaveException");

        // 3. Truncated content
        boolean threwTrunc = false;
        try {
            SaveState.deserialize("# REALWORLD SURVIVAL 3D SAVE FILE v1\n");
        } catch (SaveState.CorruptedSaveException e) {
            threwTrunc = true;
        } catch (Exception ignored) {}
        check(threwTrunc, "truncated save content rejected");
    }

    // -----------------------------------------------------------------
    // Group 4: Open World Full Snapshot Fidelity
    // -----------------------------------------------------------------
    private static void testOpenWorldStateFidelity() {
        System.out.println("SaveState: Open-World full snapshot fidelity");
        SaveState orig = new SaveState();
        orig.gameMode = 1;
        orig.playerX = 180.25f;
        orig.playerY = 4.50f;
        orig.playerZ = -95.75f;
        orig.playerYaw = 245.0f;
        orig.playerHealth = 85.0f;
        orig.playerMaxHealth = 120.0f;
        orig.playerStamina = 70.0f;
        orig.playerAdrenaline = 25.0f;
        orig.score = 1650;
        orig.currency = 340;
        orig.bestScore = 2400;

        orig.storyMissionIndex = 3;
        orig.storyCurrentCount = 2;
        orig.storyTimer = 18.5f;
        orig.storyCompleted = false;
        orig.dataSlatesCollected = 3;
        orig.dataSlatesStatus[0] = true;
        orig.dataSlatesStatus[1] = false;
        orig.dataSlatesStatus[2] = true;
        orig.dataSlatesStatus[3] = true;
        orig.dataSlatesStatus[4] = false;

        orig.discoveredPoiNames.add("Central Plaza");
        orig.discoveredPoiNames.add("Safehouse");
        orig.discoveredPoiNames.add("Workshop");

        orig.inventorySlots.add(new SaveState.SlotEntry("MEDKIT", 4));
        orig.inventorySlots.add(new SaveState.SlotEntry("ENERGY_DRINK", 2));
        orig.inventorySlots.add(new SaveState.SlotEntry("RADIO_PART", 3));

        orig.dayNightTime = 240.0f;
        orig.weatherPreset = 2;
        orig.activeVehicleIndex = 0;
        orig.vehicles.add(new SaveState.VehicleState("Scout SUV", 180f, 4.5f, -95f, 245f, 195f));
        orig.vehicles.add(new SaveState.VehicleState("Trail Bike", 215f, 1.8f, 52f, 90f, 110f));

        try {
            String str = orig.serialize();
            SaveState res = SaveState.deserialize(str);

            check(res.gameMode == 1, "gameMode is OPEN_WORLD");
            approx(res.playerX, 180.25f, 0.01f, "playerX restored");
            approx(res.playerY, 4.50f, 0.01f, "playerY restored");
            approx(res.playerZ, -95.75f, 0.01f, "playerZ restored");
            approx(res.playerYaw, 245.0f, 0.01f, "playerYaw restored");
            approx(res.playerHealth, 85.0f, 0.01f, "playerHealth restored");
            approx(res.playerMaxHealth, 120.0f, 0.01f, "playerMaxHealth restored");
            approx(res.playerStamina, 70.0f, 0.01f, "playerStamina restored");
            approx(res.playerAdrenaline, 25.0f, 0.01f, "playerAdrenaline restored");
            check(res.score == 1650, "score restored");
            check(res.currency == 340, "currency restored");
            check(res.bestScore == 2400, "bestScore restored");

            check(res.storyMissionIndex == 3, "storyMissionIndex restored");
            check(res.storyCurrentCount == 2, "storyCurrentCount restored");
            approx(res.storyTimer, 18.5f, 0.01f, "storyTimer restored");
            check(res.dataSlatesCollected == 3, "dataSlatesCollected restored");
            check(res.dataSlatesStatus[0] && !res.dataSlatesStatus[1] && res.dataSlatesStatus[2],
                    "dataSlatesStatus array restored");

            check(res.discoveredPoiNames.size() == 3, "discovered POIs size restored");
            check(res.discoveredPoiNames.contains("Workshop"), "POI Workshop restored");

            check(res.inventorySlots.size() == 3, "inventory slots size restored");
            check(res.inventorySlots.get(0).typeName.equals("MEDKIT") && res.inventorySlots.get(0).count == 4,
                    "MEDKIT slot restored");
            check(res.inventorySlots.get(2).typeName.equals("RADIO_PART") && res.inventorySlots.get(2).count == 3,
                    "RADIO_PART slot restored");

            approx(res.dayNightTime, 240.0f, 0.01f, "dayNightTime restored");
            check(res.weatherPreset == 2, "weatherPreset restored");
            check(res.activeVehicleIndex == 0, "activeVehicleIndex restored");
            check(res.vehicles.size() == 2, "vehicles count restored");
            approx(res.vehicles.get(0).health, 195.0f, 0.01f, "SUV health restored");
        } catch (Exception e) {
            check(false, "Open-World fidelity test failed: " + e.getMessage());
        }
    }

    // -----------------------------------------------------------------
    // Group 5: Parkour Campaign Full Snapshot Fidelity
    // -----------------------------------------------------------------
    private static void testParkourStateFidelity() {
        System.out.println("SaveState: Parkour campaign fidelity");
        SaveState orig = new SaveState();
        orig.gameMode = 0; // PARKOUR
        orig.parkourCurrentMission = 2;
        orig.parkourCompletedMissions = 1;
        orig.itemsCollected = 4;
        orig.score = 850;

        try {
            SaveState res = SaveState.deserialize(orig.serialize());
            check(res.gameMode == 0, "gameMode is PARKOUR");
            check(res.parkourCurrentMission == 2, "parkourCurrentMission restored");
            check(res.parkourCompletedMissions == 1, "parkourCompletedMissions restored");
            check(res.itemsCollected == 4, "itemsCollected restored");
            check(res.score == 850, "score restored");
        } catch (Exception e) {
            check(false, "Parkour fidelity test failed: " + e.getMessage());
        }
    }

    // -----------------------------------------------------------------
    // Group 6: Atomic File Saving
    // -----------------------------------------------------------------
    private static void testAtomicFileSaving(File testFile) {
        System.out.println("SaveFileManager: atomic file save");
        SaveFileManager.deleteSave(testFile);

        SaveState s = new SaveState();
        s.score = 1111;
        boolean ok = SaveFileManager.saveAtomic(testFile, s);
        check(ok, "saveAtomic succeeded");
        check(testFile.exists(), "target save file exists on disk");
        check(testFile.length() > 0, "target save file is non-empty");

        File tmpFile = new File(testFile.getPath() + ".tmp");
        check(!tmpFile.exists(), "temporary .tmp file cleaned up");

        SaveState loaded = SaveFileManager.loadSafe(testFile);
        check(loaded != null, "loadSafe loaded valid state");
        check(loaded.score == 1111, "loaded score matches saved score");
    }

    // -----------------------------------------------------------------
    // Group 7: Backup Rotation
    // -----------------------------------------------------------------
    private static void testBackupRotation(File testFile) {
        System.out.println("SaveFileManager: backup rotation (.bak)");
        File bakFile = new File(testFile.getPath() + ".bak");
        check(!bakFile.exists(), "backup file does not exist before rotation");

        // Save new version with score 2222
        SaveState s2 = new SaveState();
        s2.score = 2222;
        boolean ok = SaveFileManager.saveAtomic(testFile, s2);
        check(ok, "second saveAtomic succeeded");
        check(bakFile.exists(), "backup .bak file created during rotation");

        // Verify primary has score 2222 and backup has score 1111
        SaveState primaryState = SaveFileManager.loadSafe(testFile);
        check(primaryState != null && primaryState.score == 2222, "primary file has updated score 2222");

        try {
            SaveState backupState = SaveState.deserialize(SaveFileManager.readFileToString(bakFile));
            check(backupState != null && backupState.score == 1111, "backup file contains previous score 1111");
        } catch (Exception e) {
            check(false, "failed to read backup: " + e.getMessage());
        }
    }

    // -----------------------------------------------------------------
    // Group 8: Automatic Corruption Recovery from Backup
    // -----------------------------------------------------------------
    private static void testAutomaticCorruptionRecovery(File testFile) {
        System.out.println("SaveFileManager: automatic corruption recovery");
        File bakFile = new File(testFile.getPath() + ".bak");
        check(testFile.exists() && bakFile.exists(), "both primary and backup exist");

        // Intentionally corrupt primary file by truncating with garbage bytes
        try (FileOutputStream fos = new FileOutputStream(testFile)) {
            fos.write("CORRUPTED_GARBAGE_DATA_123456789".getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            check(false, "failed to corrupt primary file: " + e.getMessage());
        }

        // loadSafe should detect corruption in primary and recover from backup
        SaveState recovered = SaveFileManager.loadSafe(testFile);
        check(recovered != null, "loadSafe recovered state despite primary corruption");
        check(recovered.score == 1111, "recovered state accurately restored from backup (score 1111)");

        // Verify primary file was re-written with healthy recovered state
        SaveState reloadedPrimary = SaveFileManager.loadSafe(testFile);
        check(reloadedPrimary != null && reloadedPrimary.score == 1111,
                "primary file healed with recovered data");
    }

    // -----------------------------------------------------------------
    // Group 9: MissionManager State Restoration
    // -----------------------------------------------------------------
    private static void testMissionManagerRestoration() {
        System.out.println("MissionManager: state restoration");
        MissionManager mm = new MissionManager();
        boolean[] slateFlags = new boolean[] { true, false, true, false, false };

        mm.restoreState(2, 3, 15.0f, false, 2, slateFlags);

        check(mm.getCurrentMissionIndex() == 2, "missionIndex restored to 2");
        check(mm.getMissions().get(0).isCompleted, "mission 1 marked completed");
        check(mm.getMissions().get(1).isCompleted, "mission 2 marked completed");

        Mission m3 = mm.getCurrentMission();
        check(m3 != null && m3.isActive, "mission 3 is active");
        check(m3.currentCount == 3, "mission 3 currentCount restored to 3");
        approx(m3.timer, 15.0f, 0.01f, "mission 3 timer restored to 15.0");
        check(mm.dataSlatesCollected == 2, "dataSlatesCollected restored to 2");
        check(mm.getDataSlates().get(0).collected, "data slate 0 collected flag restored");
        check(!mm.getDataSlates().get(1).collected, "data slate 1 uncollected flag restored");
    }

    // -----------------------------------------------------------------
    // Group 10: Inventory State Restoration
    // -----------------------------------------------------------------
    private static void testInventoryRestoration() {
        System.out.println("Inventory: state restoration");
        Inventory inv = new Inventory();
        inv.clear();
        check(inv.getSlotCount() == 0, "inventory cleared to 0 slots");

        inv.addItem(Item.Type.MEDKIT, 3);
        inv.addItem(Item.Type.ENERGY_DRINK, 2);
        inv.addItem(Item.Type.RADIO_PART, 4);

        check(inv.getSlotCount() == 3, "3 restored slots populated");
        check(inv.getItemCount(Item.Type.MEDKIT) == 3, "MEDKIT count is 3");
        check(inv.getItemCount(Item.Type.ENERGY_DRINK) == 2, "ENERGY_DRINK count is 2");
        check(inv.getItemCount(Item.Type.RADIO_PART) == 4, "RADIO_PART count is 4");
    }

    // -----------------------------------------------------------------
    // Group 11: Cleanup
    // -----------------------------------------------------------------
    private static void testSaveCleanup(File testFile) {
        System.out.println("SaveFileManager: cleanup");
        check(SaveFileManager.hasValidSave(testFile), "hasValidSave is true before delete");
        boolean ok = SaveFileManager.deleteSave(testFile);
        check(ok, "deleteSave returned true");
        check(!testFile.exists(), "primary file deleted");
        File bak = new File(testFile.getPath() + ".bak");
        check(!bak.exists(), "backup file deleted");
        check(!SaveFileManager.hasValidSave(testFile), "hasValidSave is false after delete");
    }
}
