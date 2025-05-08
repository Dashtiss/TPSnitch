package com.dashtiss.tpsnitch;

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

        // Will check if the mod is running on the server side
        if (!Tpsnitch.class.getClassLoader().getClass().getName().equals("net.fabricmc.server.FabricServer")) {
            LOGGER.error("This mod is only intended to be run on the server side.");
            return;
        }

        // Listener for when a player joins the server
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            // Increase the number of players
            Players += 1;
        });
        // Listener for when a player disconnects from the server
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            // Decrease the number of players
            Players -= 1;
        });
        // Listener for when a server tick ends
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            // Calculate the TPS and MSTP
            tickTimer++;
            if (tickTimer >= TimeBetweenTicks) {
                TPS = getTPS(server);
                MSTP = getMSTP(server);
                saveFile();
                tickTimer = 0;
            }
        });
    }

    public void saveFile() {
        LOGGER.info("Players: {}", Players);
        LOGGER.info("TPS: {}", TPS);
        LOGGER.info("MSTP: {}", MSTP);
    }
    /**
     * Calculates the Mean Server Tick Period (MSTP) for the given Minecraft server.
     *
     * @param server the Minecraft server instance
     * @return the average tick time in seconds as a double, or 0 if the tick time is unavailable
     */
    public static double getMSTP(@NotNull MinecraftServer server) {
        long nanos = server.getAverageTickTimeNanos();
        if (nanos == 0) {
            return 0;
        }
        return (double) nanos / 1000000000;
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
        return (int) Math.min((1000 / getMSTP(server)), 20);
    }
}
