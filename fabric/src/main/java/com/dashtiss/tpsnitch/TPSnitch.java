package com.dashtiss.tpsnitch;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Map;
import com.google.gson.JsonObject;
import com.dashtiss.tpsnitch.TpsnitchConfig;

public class TPSnitch implements ModInitializer {
    private static TpsnitchConfig config = TpsnitchConfig.get();
    private static AtomicInteger playerCount = new AtomicInteger(0);
    private static long lastLogTime = 0;
    private static Map<String, JsonObject> logs = new ConcurrentHashMap<>();

    @Override
    public void onInitialize() {
        // Constants.LOG.info("Hello Fabric world!");
        CommonClass.init();

        // Reset player count on server start
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            playerCount.set(0);
            if (config.debug) Constants.LOG.info("[TPSnitch] Server started, player count reset.");
        });

        // Track player join/leave
        ServerPlayConnectionEvents.JOIN.register((handler, server, player) -> {
            playerCount.incrementAndGet();
            if (config.debug) Constants.LOG.info("[TPSnitch] Player joined, player count: " + playerCount.get());
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            playerCount.decrementAndGet();
            if (config.debug) Constants.LOG.info("[TPSnitch] Player left, player count: " + playerCount.get());
        });

        // Log TPS, MSPT, player count every interval
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            long now = System.currentTimeMillis();
            if (now - lastLogTime >= config.logIntervalSeconds * 1000L) {
                double tps = getTPS(server);
                double mspt = getMSPT(server);
                int players = playerCount.get();
                String timestamp = Instant.now().toString();

                JsonObject obj = new JsonObject();
                obj.addProperty("TPS", tps);
                obj.addProperty("MSPT", mspt);
                obj.addProperty("PlayerCount", players);
                logs.put(timestamp, obj);

                // Save to file
                JsonObject fileObj = new JsonObject();
                for (Map.Entry<String, JsonObject> entry : logs.entrySet()) {
                    fileObj.add(entry.getKey(), entry.getValue());
                }
                boolean saved = new CommonClass().saveJson(fileObj.toString(), config.logFileName, config.debug);
                if (config.debug) Constants.LOG.info("[TPSnitch] Log saved: " + saved);
                lastLogTime = now;
            }
        });
    }

    // Utility: Get MSPT
    public static double getMSPT(MinecraftServer server) {
        // Vanilla keeps last 100 tick times in server.tickTimes[]
        double tickTimes = server.getAverageTickTimeNanos();

        return tickTimes / 1_000_000.0;
    }

    // Utility: Get TPS
    public static double getTPS(MinecraftServer server) {
        double mspt = getMSPT(server);
        return Math.min(20.0, 1000.0 / mspt);
    }
}
