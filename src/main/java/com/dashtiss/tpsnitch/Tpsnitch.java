package com.dashtiss.tpsnitch;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.midnightdust.lib.config.MidnightConfig;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import org.slf4j.LoggerFactory;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Optional;

// Jackson imports for JSON parsing
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

// Fabric Loader API for getting mod version
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;


public class Tpsnitch implements ModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("tpsnitch");;
    private int Players = 0;
    private double TPS = 0;
    private double MSTP = 0;

    private int tickTimer = 0;

    public int TimeBetweenTicks = 600; // in Ticks, default is 30 seconds. 30*20=600

    private boolean hasCreatedFile = false;

    private final boolean DEVELOPMENT_BUILD = true; // Set to true for development builds, false for production
    private final Runtime.Version RUNTIME_VERSION = Runtime.version(); // Get the current runtime version

    // Modrinth API URL for the project versions
    private static final String MODRINTH_API_URL = "https://api.modrinth.com/v2/project/R6W27fZZ/version";
    // It's good practice to identify your application to the API provider
    private static final String USER_AGENT = "TPSnitchMod/1.0 (github.com/dashtiss/TPSnitch)"; // ** Replace with your actual info **

    /**
     * This method is called once when the mod is initialized.
     * <p>
     * It sets up listeners for the following events:
     * <ul>
     * <li>Player joins</li>
     * <li>Player disconnects</li>
     * <li>Server tick ends</li>
     * </ul>
     * <p>
     * The event listeners are used to keep track of the number of players online
     * and to calculate the TPS and MSTP.
     * <p>
     * The TPS and MSTP are calculated on each server tick, which is done by the
     * ServerTickEvents.END_SERVER_TICK listener.
     * <p>
     * The listeners are registered on the server side only.
     */
    @Override
    public void onInitialize() {

        // Log the start of the mod initialization process (Keep as INFO)
        LOGGER.info("TPSnitch mod initialization started.");

        // Initialize MidnightConfig for the mod
        MidnightConfig.init("tpsnitch", Config.class);

        // Change Classloader log to DEBUG
        LOGGER.debug("Classloader name: {}", Tpsnitch.class.getClassLoader().getClass().getName());
        // Will check if the mod is running on the server side
        if (!Tpsnitch.class.getClassLoader().getClass().getName().equals("net.fabricmc.loader.impl.launch.knot.KnotClassLoader")) {
            // Log that the mod is not running on the server side (Keep as ERROR)
            LOGGER.error("Mod is not running on the server side.");
            return;
        }
        // Change server environment check log to DEBUG
        LOGGER.debug("Server environment check passed.");

        // Check if the mod is running in development mode
        if (DEVELOPMENT_BUILD) {
            // Log that the mod is running in development mode (Keep as DEBUG)
            LOGGER.warn("Mod is running in development mode.");
        }






        // Change config initialization log to DEBUG
        LOGGER.debug("MidnightConfig initialized for 'tpsnitch'.");

        // Listener for when a player joins the server
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            // Increase the number of players
            Players += 1;
            // Log the player joining and the updated player count (Keep as DEBUG)
            LOGGER.debug("Player joined the server. Current player count: {}", Players);
        });
        // Log that the Player JOIN event listener has been registered (Keep as DEBUG)
        LOGGER.debug("Registered Player JOIN event listener.");

        // Listener for when a player disconnects from the server
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            // Decrease the number of players
            Players -= 1;
            // Log the player disconnecting and the updated player count (Keep as DEBUG)
            LOGGER.debug("Player disconnected from the server. Current player count: {}", Players);
        });
        // Log that the Player DISCONNECT event listener has been registered (Keep as DEBUG)
        LOGGER.debug("Registered Player DISCONNECT event listener.");

        // Listener for when a server tick ends
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            // Increment the tick timer on each server tick
            tickTimer++;
            // Log the current tick timer value (Keep as TRACE, as this happens frequently)
            LOGGER.trace("Server tick ended. tickTimer: {}", tickTimer);

            // Check if the tick timer has reached the specified interval
            if (tickTimer >= TimeBetweenTicks) {
                // Log before performing calculations and saving the file (Keep as DEBUG)
                LOGGER.debug("Tick timer reached TimeBetweenTicks ({}). Calculating TPS/MSTP and saving file.", TimeBetweenTicks);

                // Calculate TPS and MSTP
                TPS = getTPS(server);
                MSTP = getMSTP(server);

                // Call the saveFile method
                saveFile();

                // Reset the tick timer
                tickTimer = 0;
                // Log that the tick timer has been reset (Keep as DEBUG)
                LOGGER.debug("Tick timer reset to 0.");

                // --- Perform update check periodically ---
                // You might want to do this less frequently than every tick interval,
                // maybe on server start or once a day. For demonstration, we'll call it here.
                //checkForUpdate();
            }
        });
        // Log that the Server TICK END event listener has been registered (Keep as DEBUG)
        LOGGER.debug("Registered Server TICK END event listener.");

        // Log that the mod initialization is complete (Keep as INFO)
        LOGGER.info("TPSnitch mod initialization complete.");

        // --- Initial update check on server start ---
        // It's often good to check for updates once when the server starts.
        checkForUpdate();
    }

    /**
     * Saves the current player count, TPS, and MSTP to a file.
     * It handles renaming the previous log file on the first save after server start.
     */
    public void saveFile() {
        // Log the values being saved (Changed to DEBUG and wrapped in Config.Verbose check)
        // Assumes Config.Verbose is a boolean field in your Config class
        if (Config.Verbose) {
            LOGGER.debug("Saving data - Players: {}, TPS: {}, MSTP: {}", Players, TPS, MSTP);
        }

        // Using GSON save it to the logs folder with each time the server starts up creating a new log
        // Check if this is the first time saving since the server started
        if (!hasCreatedFile) {
            LOGGER.debug("First file save since server start. Renaming previous log file.");
            hasCreatedFile = true;
            // will rename the old log file from latest.json to old-{Number}.json
            // Assuming FileHandler.renameLatestJson exists and works
            FileHandler.renameLatestJson(Config.LogFilePath);
            LOGGER.debug("Previous log file renamed.");

            // Save the current data to the new file
            // Assuming FileHandler.saveFile exists and works
            FileHandler.saveFile(Players, TPS, MSTP, Config.LogFilePath);
            LOGGER.debug("Initial data saved to file: {}", Config.LogFilePath);
        }
        else {
            // If it's not the first save, just save the current data
            FileHandler.saveFile(Players, TPS, MSTP, Config.LogFilePath);
            LOGGER.debug("Data updated in file: {}", Config.LogFilePath);
        }
    }

    /**
     * Calculates the Mean Server Tick Period (MSTP) for the given Minecraft server.
     *
     * @param server the Minecraft server instance
     * @return the average tick time in seconds as a double, or 0 if the tick time is unavailable
     */
    public static double getMSTP(@NotNull MinecraftServer server) {
        // Get the average tick time in nanoseconds
        long nanos = server.getAverageTickTimeNanos();
        // Log the raw nanoseconds value (Keep as TRACE)
        LOGGER.trace("getMSTP called. Raw average tick time nanos: {}", nanos);

        // Check if the tick time is available (nanos > 0).
        // Note: server.getAverageTickTimeNanos() returns 0 if the server hasn't ticked enough yet.
        if (nanos <= 0) { // Use <= 0 to also handle potential negative values, though unlikely
            // Log if tick time is unavailable and return 0 (Keep as DEBUG)
            LOGGER.debug("Average tick time nanos is <= 0. Returning MSTP as 0.");
            return 0;
        }
        // Calculate MSTP in seconds
        double mstpSeconds = (double) nanos / 1000000000.0; // Use 1000000000.0 for double division
        // Log the calculated MSTP in seconds (Keep as TRACE)
        LOGGER.trace("Calculated MSTP: {} seconds", mstpSeconds);
        return mstpSeconds;
    }

    /**
     * Calculates the TPS (ticks per second) of the given Minecraft server.
     * <p>
     * This is a convenience method that wraps the {@link #getMSTP(MinecraftServer)} method.
     * <p>
     * The TPS is calculated as 20 divided by the average tick time in seconds (since target TPS is 20).
     * If the average tick time is unavailable (i.e. the server is not running or hasn't ticked enough), 0 is returned.
     *
     * @param server the Minecraft server instance
     * @return the ticks per second, or 0 if the tick time is unavailable
     */
    public static double getTPS(@NotNull MinecraftServer server) {
        // Get the MSTP in seconds
        double mstp = getMSTP(server);
        // Log the MSTP value obtained (Keep as TRACE)
        LOGGER.trace("getTPS called. MSTP obtained: {}", mstp);

        // If MSTP is 0, return 20 TPS (or 0 if you prefer to indicate no data)
        // A common approach is to return 20 if MSTP is very low, indicating full speed.
        if (mstp <= 0) {
            LOGGER.debug("MSTP is <= 0. Returning TPS as 20 (assuming full speed).");
            return 20.0; // Assuming full speed if tick time is 0 or negative
        }

        // Calculate TPS (capped at 20)
        // TPS = 1000ms / MSTP_in_ms OR 1 second / MSTP_in_seconds
        // Since getMSTP returns seconds, TPS = 1.0 / mstp, capped at 20.
        double tps = Math.min((1.0 / mstp), 20.0); // Use 1.0 and 20.0 for double calculations
        // Log the calculated TPS (Keep as TRACE)
        LOGGER.trace("Calculated TPS: {}", tps);
        return tps;
    }

    /**
     * Checks the Modrinth API for the latest version of the mod.
     * Logs whether a new version is available.
     */
    public void checkForUpdate() {
        LOGGER.info("Checking for mod updates on Modrinth...");

        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(MODRINTH_API_URL))
                .GET()
                .header("User-Agent", USER_AGENT) // Identify your application
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String responseBody = response.body();
                ObjectMapper objectMapper = new ObjectMapper();

                // The API returns a JSON array of versions
                List<ModrinthVersion> versions = objectMapper.readValue(responseBody, new TypeReference<List<ModrinthVersion>>() {});

                if (versions != null && !versions.isEmpty()) {
                    // The API usually returns versions sorted by publish date, newest first.
                    ModrinthVersion latestModrinthVersion = versions.get(0);
                    String latestVersionNumber = latestModrinthVersion.getVersionNumber();

                    String currentModVersion = getModVersion(); // Get the version of THIS mod

                    LOGGER.info("Current mod version: {}", currentModVersion);
                    LOGGER.info("Latest Modrinth version: {}", latestVersionNumber);

                    // Simple string comparison. For more complex versioning (e.g., 1.0.0-beta.1),
                    // you might need a dedicated version comparison library.
                    // This assumes version numbers are comparable lexicographically or numerically.
                    // For example, "1.10.0" > "1.9.0" and "2.0.0" > "1.10.0"
                    if (latestVersionNumber != null && currentModVersion != null && latestVersionNumber.compareTo(currentModVersion) > 0) {
                        LOGGER.warn("A new version of TPSnitch is available: {} (Current: {})", latestVersionNumber, currentModVersion);
                        // You could add code here to notify server operators, etc.
                    } else {
                        LOGGER.info("Mod is up to date.");
                    }

                } else {
                    LOGGER.warn("Modrinth API returned an empty list of versions for project R6W27fZZ.");
                }

            } else {
                LOGGER.error("Failed to check for updates. Modrinth API returned status code: {}", response.statusCode());
                LOGGER.error("Response body: {}", response.body());
            }

        } catch (IOException | InterruptedException e) {
            LOGGER.error("Error during update check: {}", e.getMessage());
            if (Config.Verbose) { // Only print stack trace in verbose mode
                e.printStackTrace();
            }
        }
    }

    /**
     * Gets the current version of this mod using FabricLoader.
     * This method retrieves the version from the mod's fabric.mod.json file.
     *
     * @return The current version string of the mod, or "UNKNOWN" if not found.
     */
    private String getModVersion() {
        // Use FabricLoader to get the mod container and its metadata
        Optional<ModContainer> modContainer = FabricLoader.getInstance().getModContainer("tpsnitch"); // Use your modid here

        if (modContainer.isPresent()) {
            ModMetadata metadata = modContainer.get().getMetadata();
            return metadata.getVersion().getFriendlyString();
        } else {
            // Log a warning if the mod container is not found
            LOGGER.warn("Could not find mod container for 'tpsnitch' to get version.");
            return "UNKNOWN"; // Return UNKNOWN if the mod container isn't found
        }
    }

    /**
     * Static inner class to represent a Modrinth version object for JSON parsing.
     * This keeps the data structure definition within the main mod file.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class ModrinthVersion {
        // Use @JsonProperty to map JSON fields with snake_case to Java fields with camelCase
        private String id;
        @JsonProperty("project_id")
        private String projectId;
        private String name;
        @JsonProperty("version_number")
        private String versionNumber; // This is the field we'll use for comparison
        private String changelog;
        @JsonProperty("version_type")
        private String versionType; // e.g., "release", "beta", "alpha"
        @JsonProperty("date_published")
        private String datePublished;
        private int downloads;
        @JsonProperty("game_versions")
        private List<String> gameVersions;
        // Add other fields if you need them

        // Default constructor (needed by Jackson)
        public ModrinthVersion() {}

        // --- Getters ---
        public String getId() { return id; }
        public String getProjectId() { return projectId; }
        public String getName() { return name; }
        public String getVersionNumber() { return versionNumber; }
        public String getChangelog() { return changelog; }
        public String getVersionType() { return versionType; }
        public String getDatePublished() { return datePublished; }
        public int getDownloads() { return downloads; }
        public List<String> getGameVersions() { return gameVersions; }

        // --- Setters (optional, depending on if you need to modify the object) ---
        public void setId(String id) { this.id = id; }
        public void setProjectId(String projectId) { this.projectId = projectId; }
        public void setName(String name) { this.name = name; }
        public void setVersionNumber(String versionNumber) { this.versionNumber = versionNumber; }
        public void setChangelog(String changelog) { this.changelog = changelog; }
        public void setVersionType(String versionType) { this.versionType = versionType; }
        public void setDatePublished(String datePublished) { this.datePublished = datePublished; }
        public void setDownloads(int downloads) { this.downloads = downloads; }
        public void setGameVersions(List<String> gameVersions) { this.gameVersions = gameVersions; }


        @Override
        public String toString() {
            return "ModrinthVersion{" +
                    "name='" + name + '\'' +
                    ", versionNumber='" + versionNumber + '\'' +
                    ", versionType='" + versionType + '\'' +
                    ", datePublished='" + datePublished + '\'' +
                    ", gameVersions=" + gameVersions +
                    '}';
        }
    }
}
