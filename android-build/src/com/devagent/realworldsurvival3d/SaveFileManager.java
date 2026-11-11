package com.devagent.realworldsurvival3d;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;

/**
 * SaveFileManager - Pure Java atomic file persistence and automatic corruption recovery.
 * Decoupled from Android Context for 100% JVM testability.
 */
public class SaveFileManager {

    /**
     * Atomically writes state to file using .tmp staging and .bak rotation.
     * Prevents save file corruption during sudden crashes or power loss.
     */
    public static boolean saveAtomic(File targetFile, SaveState state) {
        if (targetFile == null || state == null) return false;

        File parent = targetFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        File tempFile = new File(targetFile.getPath() + ".tmp");
        File bakFile = new File(targetFile.getPath() + ".bak");

        String serialized = state.serialize();
        FileOutputStream fos = null;
        try {
            // Step 1: Write to temp file
            fos = new FileOutputStream(tempFile);
            fos.write(serialized.getBytes(StandardCharsets.UTF_8));
            fos.flush();
            fos.getFD().sync(); // Ensure bytes are physically written to storage media
            fos.close();
            fos = null;

            // Step 2: Rotate existing target file to backup
            if (targetFile.exists()) {
                if (bakFile.exists()) {
                    bakFile.delete();
                }
                targetFile.renameTo(bakFile);
            }

            // Step 3: Atomic rename temp to target
            boolean renamed = tempFile.renameTo(targetFile);
            if (!renamed) {
                // Fallback copy if rename fails on some exotic filesystems
                copyFile(tempFile, targetFile);
                tempFile.delete();
            }
            return true;
        } catch (Exception e) {
            System.err.println("saveAtomic failed: " + e.getMessage());
            if (tempFile.exists()) {
                tempFile.delete();
            }
            return false;
        } finally {
            if (fos != null) {
                try { fos.close(); } catch (Exception ignored) {}
            }
        }
    }

    /**
     * Loads game state from primary file. If primary is corrupted or missing,
     * automatically attempts recovery from .bak backup file.
     */
    public static SaveState loadSafe(File targetFile) {
        if (targetFile == null) return null;

        // Try primary file
        if (targetFile.exists() && targetFile.length() > 0) {
            try {
                String content = readFileToString(targetFile);
                return SaveState.deserialize(content);
            } catch (Exception e) {
                System.err.println("Primary save file corrupted (" + e.getMessage() + "). Attempting backup recovery...");
            }
        }

        // Try backup file
        File bakFile = new File(targetFile.getPath() + ".bak");
        if (bakFile.exists() && bakFile.length() > 0) {
            try {
                String content = readFileToString(bakFile);
                SaveState recovered = SaveState.deserialize(content);
                System.out.println("Successfully recovered game state from backup save!");
                // Restore primary file from valid backup
                saveAtomic(targetFile, recovered);
                return recovered;
            } catch (Exception e) {
                System.err.println("Backup save file also corrupted: " + e.getMessage());
            }
        }

        return null;
    }

    public static boolean hasValidSave(File targetFile) {
        if (targetFile == null) return false;
        if (targetFile.exists() && targetFile.length() > 0) {
            try {
                SaveState s = SaveState.deserialize(readFileToString(targetFile));
                return s != null;
            } catch (Exception ignored) {}
        }
        File bakFile = new File(targetFile.getPath() + ".bak");
        if (bakFile.exists() && bakFile.length() > 0) {
            try {
                SaveState s = SaveState.deserialize(readFileToString(bakFile));
                return s != null;
            } catch (Exception ignored) {}
        }
        return false;
    }

    public static boolean deleteSave(File targetFile) {
        if (targetFile == null) return false;
        boolean deleted = true;
        if (targetFile.exists()) deleted &= targetFile.delete();
        File bakFile = new File(targetFile.getPath() + ".bak");
        if (bakFile.exists()) deleted &= bakFile.delete();
        File tmpFile = new File(targetFile.getPath() + ".tmp");
        if (tmpFile.exists()) tmpFile.delete();
        return deleted;
    }

    public static String readFileToString(File file) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }

    private static void copyFile(File src, File dst) throws Exception {
        try (FileInputStream in = new FileInputStream(src);
             FileOutputStream out = new FileOutputStream(dst)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            out.flush();
            out.getFD().sync();
        }
    }
}
