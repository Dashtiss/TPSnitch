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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.text.SimpleDateFormat;

public class FileHandler {

    // Pattern to match filenames like "old-123.json" and capture the number
    private static final Pattern OLD_FILE_PATTERN = Pattern.compile("^old-(\\d+)\\.json$");

    // Date format to use for the JSON keys in the stats file
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * Renames the 'latest.json' file in a given directory to 'old-{number}.json',
     * where {number} is the next available sequential integer based on existing
     * 'old-*.json' files.
     *
     * @param directoryPath The absolute or relative path to the directory
     * containing the 'latest.json' file.
     */
    public static void renameLatestJson(String directoryPath) {
        // 1. Validate the directory
        File directory = new File(directoryPath);
        if (!directory.exists() || !directory.isDirectory()) {
            System.err.println("Error: Directory does not exist or is not a directory: " + directoryPath);
            return;
        }

        // 2. Find existing 'old-*.json' files and determine the highest number
        File[] existingOldFiles = directory.listFiles((dir, name) -> OLD_FILE_PATTERN.matcher(name).matches());

        int highestNumber = -1; // Start with -1 so the first file becomes old-0.json

        if (existingOldFiles != null) {
            for (File file : existingOldFiles) {
                Matcher matcher = OLD_FILE_PATTERN.matcher(file.getName());
                if (matcher.find()) {
                    try {
                        int number = Integer.parseInt(matcher.group(1));
                        if (number > highestNumber) {
                            highestNumber = number;
                        }
                    } catch (NumberFormatException e) {
                        // Should not happen with the regex, but good practice
                        System.err.println("Warning: Could not parse number from filename: " + file.getName());
                    }
                }
            }
        }

        int nextNumber = highestNumber + 1; // The next available number

        // 3. Locate the 'latest.json' file
        File latestFile = new File(directory, "latest.json");
        if (!latestFile.exists() || !latestFile.isFile()) {
            System.err.println("Error: 'latest.json' not found in directory: " + directoryPath);
            return;
        }

        // 4. Define the new file name
        File newFile = new File(directory, "old-" + nextNumber + ".json");

        // 5. Perform the rename operation
        boolean success = latestFile.renameTo(newFile);

        if (success) {
            System.out.println("Successfully renamed 'latest.json' to '" + newFile.getName() + "'");
        } else {
            System.err.println("Error: Failed to rename 'latest.json' to '" + newFile.getName() + "'");
            // Possible reasons: file is open, permissions issue, etc.
        }
    }

    /**
     * Saves server statistics (Players, TPS, MSTP) to a JSON file.
     * The data is added under a timestamp key, appending to existing data.
     *
     * @param Players The current number of players online.
     * @param TPS     The server's Ticks Per Second.
     * @param MSTP    The server's Milliseconds Per Tick.
     * @param logFilePath The path to the JSON file where the stats will be saved.
     */
    public static void saveFile(int Players, double TPS, double MSTP, String logFilePath) {
        // 1. Get the current timestamp
        String timestamp = DATE_FORMAT.format(new Date());

        // 2. Create a map for the current stats
        Map<String, Object> currentStats = new HashMap<>();
        currentStats.put("tps", TPS);
        currentStats.put("mspt", MSTP); // Note: Using MSTP as provided, but MSPT is more common
        currentStats.put("playerCount", Players);

        // 3. Define the type for Gson to handle the nested map structure
        Type dataType = new TypeToken<Map<String, Map<String, Object>>>(){}.getType();

        // 4. Read existing data from the JSON file
        Map<String, Map<String, Object>> allStats = new HashMap<>();
        File jsonFile = new File(logFilePath);
        Gson gson = new GsonBuilder().setPrettyPrinting().create(); // Use pretty printing for readability

        // Ensure the parent directory exists
        File parentDir = jsonFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs(); // Create parent directories if they don't exist
        }


        if (jsonFile.exists() && jsonFile.length() > 0) {
            try (FileReader reader = new FileReader(jsonFile)) {
                // Read the existing JSON data
                Map<String, Map<String, Object>> existingData = gson.fromJson(reader, dataType);
                if (existingData != null) {
                    allStats.putAll(existingData); // Add existing data to our map
                }
            } catch (IOException e) {
                System.err.println("Error reading JSON file: " + e.getMessage());
                // Decide how to handle the error - maybe log it differently or throw?
                return; // Exit the function on read error
            } catch (JsonSyntaxException e) {
                System.err.println("Error parsing JSON file (invalid syntax): " + e.getMessage());
                // Handle invalid JSON - maybe back it up and start fresh?
                // For now, we'll just print an error and proceed as if the file was empty
            }
        }

        // 5. Add the new entry
        allStats.put(timestamp, currentStats);

        // 6. Write the updated data back to the JSON file
        try (FileWriter writer = new FileWriter(jsonFile)) {
            gson.toJson(allStats, writer);
            System.out.println("Successfully logged stats for timestamp: " + timestamp + " to " + logFilePath);
        } catch (IOException e) {
            System.err.println("Error writing to JSON file: " + e.getMessage());
            // Decide how to handle the error
        }
    }
}
