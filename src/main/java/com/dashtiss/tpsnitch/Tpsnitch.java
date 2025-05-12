package com.dashtiss.tpsnitch;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import eu.midnightdust.lib.config.MidnightConfig;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;

import net.minecraft.server.MinecraftServer;

import org.slf4j.LoggerFactory;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Optional;

public class Tpsnitch implements ModInitializer {

    // Using SLF4J for logging, good practice in Fabric mods
    public static final Logger LOGGER = LoggerFactory.getLogger("tpsnitch");

    // Keep track of server stats
    private int players = 0;
    private double tps = 0;
    private long mstp = 0;

    // Timer for periodic updates
    private int tickTimer = 0;
    // How often to update the stats file (in ticks). 20 ticks = 1 second.
    private static int updateIntervalTicks = 600; // Default: 30 seconds (30 * 20)

    // Simple flag for development builds - helps with debugging output
    @SuppressWarnings("FieldCanBeLocal")
    private final boolean IS_DEVELOPMENT_BUILD = false;

    // Modrinth API details for update checks
    private static final String MODRINTH_API_URL = "https://api.modrinth.com/v2/project/R6W27fZZ/version";
    // Identify ourselves to the API provider - replace with your actual info!
    private static final String USER_AGENT = "TPSnitchMod/1.0 (github.com/dashtiss/TPSnitch)";

    /**
     * This is the main entry point for our mod.
     * It's called by Fabric when the mod is initialized.
     * We set up our event listeners here to track server state.
     */
    @Override
    public void onInitialize() {
        updateIntervalTicks = Config.TimeBetweenTicks;
        LOGGER.info("TPSnitch mod is starting up.");

        // Load our configuration settings
        MidnightConfig.init("tpsnitch", Config.class);
        LOGGER.debug("Configuration loaded.");

        // A quick check to make sure we're running in the server environment.
        // This helps prevent issues if the mod somehow ends up on a client.
        if (!Tpsnitch.class.getClassLoader().getClass().getName().equals("net.fabricmc.loader.impl.launch.knot.KnotClassLoader")) {
            LOGGER.error("TPSnitch is a server-side mod and cannot run on the client. Initialization aborted.");
            return;
        }
        LOGGER.debug("Environment check passed - running on server.");

        // Warn if this is a development build
        if (IS_DEVELOPMENT_BUILD) {
            LOGGER.warn("[DEVELOPMENT BUILD] Running in development mode.");
        }

        // --- Register Event Listeners ---

        // Listen for players joining the server to update the player count.
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            players++;
            LOGGER.debug("Player joined. Current players: {}", players);
        });
        LOGGER.debug("Registered player join listener.");

        // Listen for players leaving the server to update the player count.
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            players--;
            LOGGER.debug("Player disconnected. Current players: {}", players);
        });
        LOGGER.debug("Registered player disconnect listener.");

        // Listen for the end of each server tick. This is where we'll calculate stats periodically.
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tickTimer++;
            // Use TRACE level for very frequent logs like this
            LOGGER.trace("Server tick ended. tickTimer: {}", tickTimer);

            // Check if it's time to update the stats file
            if (tickTimer >= updateIntervalTicks) {
                LOGGER.debug("Update interval reached. Calculating and saving server stats.");

                // Calculate the current TPS and MSTP
                tps = getTPS(server);
                mstp = getMSTP(server);

                // Save the current stats to a file
                saveStatsToFile();

                // Reset the timer for the next interval
                tickTimer = 0;
                LOGGER.debug("Tick timer reset.");
            }
        });
        LOGGER.debug("Registered server tick listener.");

        LOGGER.info("TPSnitch mod initialized successfully.");

        // Check for updates shortly after server starts
        checkForUpdate();
    }

    /**
     * Saves the current player count, TPS, and MSTP to our designated log file.
     * Handles the initial file creation/naming if needed.
     * Assumes Config.LogFilePath points to the desired output file (e.g., "tpsnitch/latest.json").
     */
    private void saveStatsToFile() {
        // Only log verbose details if the config setting is enabled
        if (Config.Verbose) {
            LOGGER.debug("Saving stats - Players: {}, TPS: {}, MSTP: {}", players, tps, mstp);
        }

        // On the very first save after server start, we might want special handling
        // (like renaming a previous log file if that's part of the design).
        // Your original code had logic for this, but FileHandler.saveFile should
        // ideally handle creating parent directories and overwriting the target file.
        // If renaming old logs is needed, that logic would go here before saving.

        // Assuming FileHandler.saveFile handles creating directories and writing the file
        FileHandler.saveFile(players, tps, mstp, Config.LogFilePath);
        LOGGER.debug("Server stats saved to file: {}", Config.LogFilePath);

        // If you had logic here to rename the previous log, you'd set firstFileSave = false;
        // after the first save. For now, I've simplified based on the FileHandler assumption.
    }

    /**
     * Calculates the Mean Server Tick Period (MSTP) from the server's internal data.
     * MSTP is the average time it takes for the server to complete one tick.
     *
     * @param server The Minecraft server instance.
     * @return The average tick time in milliseconds, or 0 if data isn't available yet.
     */
    public static long getMSTP(@NotNull MinecraftServer server) {
        // The server provides the average tick time in nanoseconds.
        long nanos = server.getAverageTickTimeNanos();
        LOGGER.trace("Raw average tick time nanos: {}", nanos);

        // If nanos is 0 or negative, the server hasn't collected enough data yet.
        if (nanos <= 0) {
            LOGGER.debug("Average tick time data not available yet. Returning MSTP as 0.");
            return 0;
        }

        // Convert nanoseconds to milliseconds for MSTP
        long mstpMillis = nanos / 1_000_000; // Use 1_000_000 for clarity
        LOGGER.trace("Calculated MSTP: {} ms", mstpMillis);
        return mstpMillis;
    }

    /**
     * Calculates the server's TPS (Ticks Per Second) based on the MSTP.
     * The target TPS for Minecraft is 20.
     *
     * @param server The Minecraft server instance.
     * @return The calculated TPS, capped at 20.0, or 20.0 if MSTP data isn't available (assuming full speed).
     */
    public static double getTPS(@NotNull MinecraftServer server) {
        // Get the MSTP in milliseconds.
        double mstpMillis = getMSTP(server);
        LOGGER.trace("MSTP in milliseconds: {}", mstpMillis);

        // If MSTP is 0 or negative, we assume the server is running at full speed (20 TPS).
        if (mstpMillis <= 0) {
            LOGGER.debug("MSTP is <= 0. Assuming full speed (20 TPS).");
            return 20.0;
        }

        // Calculate TPS: 1000 milliseconds / MSTP in milliseconds.
        // Cap the result at 20.0, as TPS cannot exceed the target.
        double tps = Math.min(1000.0 / mstpMillis, 20.0);
        LOGGER.trace("Calculated TPS: {}", tps);
        return tps;
    }

    /**
     * Checks the Modrinth API to see if a newer version of this mod is available.
     * Logs the result.
     */
    private void checkForUpdate() {
        LOGGER.info("Checking Modrinth for TPSnitch updates...");

        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(MODRINTH_API_URL))
                .GET()
                .header("User-Agent", USER_AGENT) // Be polite and identify yourself
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String responseBody = response.body();
                ObjectMapper objectMapper = new ObjectMapper();

                // The Modrinth API returns a list of versions, usually newest first.
                List<ModrinthVersion> versions = objectMapper.readValue(responseBody, new TypeReference<List<ModrinthVersion>>() {});

                if (versions != null && !versions.isEmpty()) {
                    // Get the very first version in the list - that should be the latest.
                    ModrinthVersion latestModrinthVersion = versions.getFirst();
                    String latestVersionNumber = latestModrinthVersion.getVersionNumber();

                    String currentModVersion = getModVersion(); // Get the version of *this* running mod

                    LOGGER.info("Your current mod version: {}", currentModVersion);
                    LOGGER.info("Latest version on Modrinth: {}", latestVersionNumber);

                    // Simple version comparison. This works for standard versioning (like 1.0.0, 1.1.0).
                    // For more complex schemes (like 1.0.0-beta.1), you might need a dedicated library.
                    if (latestVersionNumber != null && currentModVersion != null && latestVersionNumber.compareTo(currentModVersion) > 0) {
                        LOGGER.warn("A newer version of TPSnitch is available! Version: {} (You have {})", latestVersionNumber, currentModVersion);
                        // Consider adding a server-side notification here for operators.
                    } else {
                        LOGGER.info("TPSnitch is up to date.");
                    }

                } else {
                    LOGGER.warn("Modrinth API returned no versions for project R6W27fZZ. Cannot check for updates.");
                }

            } else {
                LOGGER.error("Failed to check for updates. Modrinth API responded with status code: {}", response.statusCode());
                // Optionally log the response body for debugging: LOGGER.error("Response body: {}", response.body());
            }

        } catch (IOException | InterruptedException e) {
            LOGGER.error("An error occurred while checking for updates: {}", e.getMessage());
            if (Config.Verbose) { // Only print the full stack trace if verbose logging is on
                LOGGER.error(e.getMessage(), e);
            }
        }
    }

    /**
     * Retrieves the version string of *this* mod using Fabric's API.
     * Looks up the version defined in the mod's `fabric.mod.json`.
     *
     * @return The mod's version string, or "UNKNOWN" if it couldn't be found.
     */
    private String getModVersion() {
        // FabricLoader helps us find information about loaded mods.
        // We look for the mod container using our mod ID ("tpsnitch").
        Optional<ModContainer> modContainer = FabricLoader.getInstance().getModContainer("tpsnitch");

        if (modContainer.isPresent()) {
            // If found, get the metadata and extract the version.
            ModMetadata metadata = modContainer.get().getMetadata();
            return metadata.getVersion().getFriendlyString();
        } else {
            // Log a warning if we can't find our own mod container (shouldn't happen normally).
            LOGGER.warn("Could not find the mod container for 'tpsnitch' to get the version.");
            return "UNKNOWN"; // Indicate that the version couldn't be determined.
        }
    }

    /**
     * Simple static inner class to help Jackson parse the JSON response
     * from the Modrinth API for version information.
     * We ignore any fields we don't need using @JsonIgnoreProperties.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class ModrinthVersion {
        // Use @JsonProperty to map snake_case JSON fields to camelCase Java fields
        private String id;
        @JsonProperty("project_id")
        private String projectId;
        private String name;
        @JsonProperty("version_number")
        private String versionNumber; // This is the key field for version comparison
        // Add other fields if you need them from the API response

        // Default constructor required by Jackson for deserialization
        public ModrinthVersion() {}

        // --- Getters for the fields we care about ---
        public String getVersionNumber() { return versionNumber; }

        // You could add setters if you needed to modify these objects after parsing,
        // but for this case, getters are sufficient.

        // Optional: Override toString for easier debugging
        @Override
        public String toString() {
            return "ModrinthVersion{" +
                    "name='" + name + '\'' +
                    ", versionNumber='" + versionNumber + '\'' +
                    '}';
        }
    }
}
