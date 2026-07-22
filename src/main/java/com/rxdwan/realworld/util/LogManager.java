package com.rxdwan.realworld.util;

import com.rxdwan.realworld.RealWorldPlugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Handles logging of crime events to persistent text files.
 * All writes are appended — files are never overwritten.
 */
public class LogManager {

    private final File crimeLogFile;
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public LogManager(RealWorldPlugin plugin) {
        File logsDir = new File(plugin.getDataFolder(), "logs");
        if (!logsDir.exists()) {
            logsDir.mkdirs();
        }
        this.crimeLogFile = new File(logsDir, "crime_log.txt");
    }

    /**
     * Logs a crime-related event.
     *
     * @param action     The action type (WANTED, BOUNTY_INCREASE, ARREST, BAIL, PARDON, etc.)
     * @param playerName The player's name
     * @param playerUUID The player's UUID
     * @param crimeName  The crime name (or "N/A")
     * @param bounty     The bounty amount
     * @param adminName  The admin name (or null)
     */
    public void logCrime(String action, String playerName, String playerUUID,
                         String crimeName, double bounty, String adminName) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String admin = (adminName != null) ? adminName : "N/A";
        String line = String.format("[%s] %s | Player: %s (%s) | Crime: %s | Bounty: $%.2f | Admin: %s",
                timestamp, action, playerName, playerUUID, crimeName, bounty, admin);
        appendToFile(crimeLogFile, line);
    }

    private void appendToFile(File file, String line) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(file, true))) {
            writer.println(line);
        } catch (IOException e) {
            // Silently fail — logging should not crash the plugin
        }
    }
}
