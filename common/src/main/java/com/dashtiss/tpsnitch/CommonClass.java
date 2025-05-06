package com.dashtiss.tpsnitch;

import com.dashtiss.tpsnitch.platform.Services;
import com.dashtiss.tpsnitch.TpsnitchConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Items;

/**
 * Shared logic for TPSnitch across all supported loaders.
 * Place cross-platform logic here.
 */
public class CommonClass {

    /**
     * Initialize common logic. Called by loader-specific entry points.
     * Logs mod load status and demonstrates cross-platform service use.
     */
    public static void init() {
        TpsnitchConfig.get(); // Force config to load and create file if missing

        // Uncomment for verbose startup logging:
        // Constants.LOG.info("Hello from Common init on {}! we are currently in a {} environment!", Services.PLATFORM.getPlatformName(), Services.PLATFORM.getEnvironmentName());
        // Constants.LOG.info("The ID for diamonds is {}", BuiltInRegistries.ITEM.getKey(Items.DIAMOND));

        // Check if TPSnitch is loaded using the platform abstraction
        if (Services.PLATFORM.isModLoaded("tpsnitch")) {
            Constants.LOG.info("TPSnitch is loaded!");
        } else {
            Constants.LOG.warn("TPSnitch is NOT loaded!");
        }
    }

    /**
     * Saves a JSON string to a file, pretty-printing it for readability.
     * @param json Raw JSON string
     * @param path Path to write the file
     * @param debug Whether to log the save message
     * @return true if successful, false otherwise
     */
    public boolean saveJson(String json, String path, boolean debug) {
        try {
            // Format JSON for readability
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            JsonElement jsonElement = JsonParser.parseString(json);
            String prettyJson = gson.toJson(jsonElement);

            java.nio.file.Files.writeString(java.nio.file.Paths.get(path), prettyJson);
            if (debug) {
                Constants.LOG.info("Saved pretty-printed JSON to {}", path);
            }
            return true;
        } catch (Exception e) {
            Constants.LOG.error("Failed to save JSON to {}: {}", path, e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}