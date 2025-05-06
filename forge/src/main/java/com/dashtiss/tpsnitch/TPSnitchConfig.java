package com.dashtiss.tpsnitch;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.ModLoadingContext;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Forge-specific configuration for TPSnitch.
 */
public class TPSnitchConfig {
    public static final ForgeConfigSpec COMMON_SPEC;
    public static final TPSnitchConfig COMMON;

    public final ForgeConfigSpec.BooleanValue debug;
    public final ForgeConfigSpec.ConfigValue<String> logFileName;
    public final ForgeConfigSpec.IntValue logIntervalSeconds;

    static {
        Pair<TPSnitchConfig, ForgeConfigSpec> pair = new ForgeConfigSpec.Builder().configure(TPSnitchConfig::new);
        COMMON_SPEC = pair.getRight();
        COMMON = pair.getLeft();
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, COMMON_SPEC);
    }

    TPSnitchConfig(ForgeConfigSpec.Builder builder) {
        debug = builder.comment("Enable debug logging").define("debug", false);
        logFileName = builder.comment("Log file name").define("logFileName", "tpsnitch_log.json");
        logIntervalSeconds = builder.comment("Log interval in seconds").defineInRange("logIntervalSeconds", 10, 1, 3600);
    }

    public static boolean isDebug() {
        return COMMON.debug.get();
    }

    public static String getLogFileName() {
        return COMMON.logFileName.get();
    }

    public static int getLogIntervalSeconds() {
        return COMMON.logIntervalSeconds.get();
    }
}
