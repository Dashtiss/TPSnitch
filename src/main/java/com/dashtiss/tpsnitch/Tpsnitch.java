package com.dashtiss.tpsnitch;

import eu.midnightdust.lib.config.MidnightConfig;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Tpsnitch implements ModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("tpsnitch");
    private int Players = 0;
    private double TPS = 0;
    private double MSTP = 0;

    private int tickTimer = 0;

    public int TimeBetweenTicks = 600; // in Ticks, default is 30 seconds. 30*20=600

    private boolean hasCreatedFile = false;

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
        // Log the start of the mod initialization process
        LOGGER.info("TPSnitch mod initialization started.");

        LOGGER.info("Classloader name: {}", Tpsnitch.class.getClassLoader().getClass().getName());
        // Will check if the mod is running on the server side
        if (!Tpsnitch.class.getClassLoader().getClass().getName().equals("net.fabricmc.loader.impl.launch.knot.KnotClassLoader")) {
            // Log that the mod is not running on the server side
            LOGGER.error("Mod is not running on the server side.");
            return;
        }
        // Log that the server environment check was successful
        LOGGER.info("Server environment check passed.");

        // Initialize MidnightConfig for the mod
        MidnightConfig.init("tpsnitch", Config.class);
        // Log that the configuration has been initialized
        LOGGER.info("MidnightConfig initialized for 'tpsnitch'.");

        // Listener for when a player joins the server
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            // Increase the number of players
            Players += 1;
            // Log the player joining and the updated player count (debug level)
            LOGGER.debug("Player joined the server. Current player count: {}", Players);
        });
        // Log that the Player JOIN event listener has been registered
        LOGGER.debug("Registered Player JOIN event listener.");

        // Listener for when a player disconnects from the server
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            // Decrease the number of players
            Players -= 1;
            // Log the player disconnecting and the updated player count (debug level)
            LOGGER.debug("Player disconnected from the server. Current player count: {}", Players);
        });
        // Log that the Player DISCONNECT event listener has been registered
        LOGGER.debug("Registered Player DISCONNECT event listener.");

        // Listener for when a server tick ends
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            // Increment the tick timer on each server tick
            tickTimer++;
            // Log the current tick timer value (trace level, as this happens frequently)
            LOGGER.trace("Server tick ended. tickTimer: {}", tickTimer);

            // Check if the tick timer has reached the specified interval
            if (tickTimer >= TimeBetweenTicks) {
                // Log before performing calculations and saving the file
                LOGGER.debug("Tick timer reached TimeBetweenTicks ({}). Calculating TPS/MSTP and saving file.", TimeBetweenTicks);

                // Calculate TPS and MSTP
                TPS = getTPS(server);
                MSTP = getMSTP(server);

                // Call the saveFile method
                saveFile();

                // Reset the tick timer
                tickTimer = 0;
                // Log that the tick timer has been reset
                LOGGER.debug("Tick timer reset to 0.");
            }
        });
        // Log that the Server TICK END event listener has been registered
        LOGGER.debug("Registered Server TICK END event listener.");

        // Log that the mod initialization is complete
        LOGGER.info("TPSnitch mod initialization complete.");
    }

    /**
     * Saves the current player count, TPS, and MSTP to a file.
     * It handles renaming the previous log file on the first save after server start.
     */
    public void saveFile() {
        // Log the values being saved (info level)
        LOGGER.info("Saving data - Players: {}, TPS: {}, MSTP: {}", Players, TPS, MSTP);

        // Using GSON save it to the logs folder with each time the server starts up creating a new log
        // Check if this is the first time saving since the server started
        if (!hasCreatedFile) {
            LOGGER.debug("First file save since server start. Renaming previous log file.");
            hasCreatedFile = true;
            // will rename the old log file from latest.json to old-{Number}.json
            FileHandler.renameLatestJson(Config.LogFilePath);
            LOGGER.debug("Previous log file renamed.");

            // Save the current data to the new file
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
        // Log the raw nanoseconds value (debug level)
        LOGGER.trace("getMSTP called. Raw average tick time nanos: {}", nanos);

        // Check if the tick time is available (nanos > 0)
        if (nanos == 0) {
            // Log if tick time is unavailable and return 0
            LOGGER.debug("Average tick time nanos is 0. Returning MSTP as 0.");
            return 0;
        }
        // Calculate MSTP in seconds
        double mstpSeconds = (double) nanos / 1000000000;
        // Log the calculated MSTP in seconds (debug level)
        LOGGER.trace("Calculated MSTP: {} seconds", mstpSeconds);
        return mstpSeconds;
    }

    /**
     * Calculates the TPS (ticks per second) of the given Minecraft server.
     * <p>
     * This is a convenience method that wraps the {@link #getMSTP(MinecraftServer)} method.
     * <p>
     * The TPS is calculated as 1000 divided by the average tick time in seconds.
     * If the average tick time is unavailable (i.e. the server is not running), 0 is returned.
     *
     * @param server the Minecraft server instance
     * @return the ticks per second, or 0 if the tick time is unavailable
     */
    public static double getTPS(MinecraftServer server) {
        // Get the MSTP in seconds
        double mstp = getMSTP(server);
        // Log the MSTP value obtained (trace level)
        LOGGER.trace("getTPS called. MSTP obtained: {}", mstp);

        // If MSTP is 0, return 0 TPS
        if (mstp == 0) {
            LOGGER.debug("MSTP is 0. Returning TPS as 0.");
            return 0;
        }

        // Calculate TPS (capped at 20)
        double tps = Math.min((1000 / mstp), 20);
        // Log the calculated TPS (debug level)
        LOGGER.trace("Calculated TPS: {}", tps);
        return tps;
    }
}
