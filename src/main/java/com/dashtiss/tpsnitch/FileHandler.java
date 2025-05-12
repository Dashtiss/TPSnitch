package com.dashtiss.tpsnitch;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.JsonSyntaxException;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap; // Use TreeMap to keep keys (timestamps) sorted
import java.text.SimpleDateFormat;

public class FileHandler {


    // Date format to use for the JSON keys in the stats file
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    // Maximum number of log entries to keep
    private static int MAX_LOG_ENTRIES = 100;

    public FileHandler(Config config) {
        MAX_LOG_ENTRIES = Config.MaxLogs; // Set the max log entries from the config
    }

    /**
     * Saves server statistics (Players, TPS, MSTP) to a JSON file specified by logFilePath.
     * The data is added under a timestamp key, appending to existing data in that file.
     * This method ensures the parent directory of the log file exists and limits
     * the number of log entries to MAX_LOG_ENTRIES.
     *
     * @param Players The current number of players online.
     * @param TPS     The server's Ticks Per Second.
     * @param MSTP    The server's Milliseconds Per Tick.
     * @param logFilePath The path to the JSON file where the stats will be saved.
     * This path should be relative to the server's base directory
     * to place files there.
     */
    public static void saveFile(int Players, double TPS, long MSTP, String logFilePath) {
        // 1. Get the current timestamp
        String timestamp = DATE_FORMAT.format(new Date());

        // 2. Create a map for the current stats
        Map<String, Object> currentStats = new HashMap<>();
        currentStats.put("tps", TPS);
        currentStats.put("mspt", MSTP); // Note: Using MSTP as provided, but MSPT is more common
        currentStats.put("playerCount", Players);

        // 3. Define the type for Gson to handle the nested map structure
        // Using TreeMap to ensure keys are sorted for easy removal of the oldest entry
        Type dataType = new TypeToken<TreeMap<String, Map<String, Object>>>(){}.getType();

        // 4. Read existing data from the JSON file
        TreeMap<String, Map<String, Object>> allStats = new TreeMap<>(); // Use TreeMap here
        File jsonFile = new File(logFilePath);
        Gson gson = new GsonBuilder().setPrettyPrinting().create(); // Use pretty printing for readability

        // Ensure the parent directory exists before reading or writing
        File parentDir = jsonFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            // System.out.println("Parent directory does not exist, attempting to create: " + parentDir.getAbsolutePath()); // Removed print
            boolean dirsCreated = parentDir.mkdirs(); // Create parent directories if they don't exist
            if (dirsCreated) {
                // System.out.println("Successfully created parent directory: " + parentDir.getAbsolutePath()); // Removed print
            } else {
                System.err.println("Error: Failed to create parent directory: " + parentDir.getAbsolutePath());
                return; // Exit the function if parent directory creation fails
            }
        }

        if (jsonFile.exists() && jsonFile.length() > 0) {
            try (FileReader reader = new FileReader(jsonFile)) {
                // Read the existing JSON data
                TreeMap<String, Map<String, Object>> existingData = gson.fromJson(reader, dataType); // Read into TreeMap
                if (existingData != null) {
                    allStats.putAll(existingData); // Add existing data to our map
                }
            } catch (IOException e) {
                System.err.println("Error reading JSON file '" + logFilePath + "': " + e.getMessage());
                // For now, we'll just print an error and proceed as if the file was empty
            } catch (JsonSyntaxException e) {
                System.err.println("Error parsing JSON file (invalid syntax) '" + logFilePath + "': " + e.getMessage());
                // For now, we'll just print an error and proceed as if the file was empty
            }
        }

        // 5. Add the new entry
        allStats.put(timestamp, currentStats);

        // 6. Enforce the maximum number of log entries
        while (allStats.size() > MAX_LOG_ENTRIES) {
            // TreeMap keeps keys sorted, so the first key is the oldest timestamp
            String oldestTimestamp = allStats.firstKey();
            allStats.remove(oldestTimestamp);
            // System.out.println("Removed oldest log entry with timestamp: " + oldestTimestamp + " to maintain limit."); // Removed print
        }


        // 7. Write the updated data back to the JSON file
        try (FileWriter writer = new FileWriter(jsonFile)) {
            gson.toJson(allStats, writer);
            // System.out.println("Successfully logged stats for timestamp: " + timestamp + " to " + logFilePath); // Removed print
        } catch (IOException e) {
            System.err.println("Error writing to JSON file '" + logFilePath + "': " + e.getMessage());
            // Decide how to handle the error
        }
    }
}
