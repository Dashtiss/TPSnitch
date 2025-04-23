package com.dashtiss.tpsnitch;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerJoinCallback;
import net.fabricmc.fabric.api.event.player.PlayerLeaveCallback;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Map;
import com.google.gson.JsonObject;

public class TPSnitch implements ModInitializer {
    private static TPSnitchConfig config = new TPSnitchConfig();
    private static AtomicInteger playerCount = new AtomicInteger(0);
    private static long lastLogTime = 0;
    private static Map<String, JsonObject> logs = new ConcurrentHashMap<>();

    @Override
    public void onInitialize() {
        Constants.LOG.info("Hello Fabric world!");
        CommonClass.init();

        // Reset player count on server start
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            playerCount.set(0);
            if (config.debug) Constants.LOG.info("[TPSnitch] Server started, player count reset.");
        });

        // Track player join/leave
        PlayerJoinCallback.EVENT.register((player, server) -> {
            int count = playerCount.incrementAndGet();
            if (config.debug) Constants.LOG.info("[TPSnitch] Player joined. Count: " + count);
        });
        PlayerLeaveCallback.EVENT.register((player, server) -> {
            int count = playerCount.decrementAndGet();
            if (config.debug) Constants.LOG.info("[TPSnitch] Player left. Count: " + count);
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
                boolean saved = CommonClass.INSTANCE.saveJson(fileObj.toString(), config.logFileName);
                if (config.debug) Constants.LOG.info("[TPSnitch] Log saved: " + saved);
                lastLogTime = now;
            }
        });
    }

    // Utility: Get MSPT
    public static double getMSPT(MinecraftServer server) {
        // Vanilla keeps last 100 tick times in server.tickTimes[]
        long[] tickTimes = server.getTickTimes();
        long sum = 0;
        for (long t : tickTimes) sum += t;
        return sum / (double) tickTimes.length / 1_000_000.0; // ns to ms
    }

    // Utility: Get TPS
    public static double getTPS(MinecraftServer server) {
        double mspt = getMSPT(server);
        return Math.min(20.0, 1000.0 / mspt);
    }
}
