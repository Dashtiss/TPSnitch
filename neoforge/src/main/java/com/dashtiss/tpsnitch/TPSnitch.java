package com.dashtiss.tpsnitch;

import net.minecraft.server.MinecraftServer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.bus.api.SubscribeEvent;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Map;
import com.google.gson.JsonObject;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

@Mod(Constants.MOD_ID)
public class TPSnitch {
    private static TpsnitchConfig config = TpsnitchConfig.get();
    private static AtomicInteger playerCount = new AtomicInteger(0);
    private static long lastLogTime = 0;
    private static Map<String, JsonObject> logs = new ConcurrentHashMap<>();

    public TPSnitch(ModContainer container) {
        // Register config with NeoForge's ModContainer
        // container.registerConfig(ModConfig.Type.COMMON, TpsnitchConfig.COMMON_SPEC); // Removed: handled by NightConfig
        CommonClass.init();
        // Event bus registration may need to be handled elsewhere if required
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        playerCount.set(0);
        if (config.debug) Constants.LOG.info("[TPSnitch] Server started, player count reset.");
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        playerCount.incrementAndGet();
        if (config.debug) Constants.LOG.info("[TPSnitch] Player joined, player count: " + playerCount.get());
    }

    @SubscribeEvent
    public void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        playerCount.decrementAndGet();
        if (config.debug) Constants.LOG.info("[TPSnitch] Player left, player count: " + playerCount.get());
    }

    @SubscribeEvent
    public void onServerTick(LevelTickEvent.Post event) {
        if (!event.getLevel().isClientSide()) {
            MinecraftServer server = event.getLevel().getServer();
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
        }
    }

    // Dummy implementations; replace with real logic as needed
    private double getTPS(MinecraftServer server) {
        double mspt = getMSPT(server);
        return Math.min(20.0, 1000.0 / mspt);
    }
    private double getMSPT(MinecraftServer server) {
        double tickTimes = server.getAverageTickTimeNanos();

        return tickTimes / 1_000_000.0;

    }
}