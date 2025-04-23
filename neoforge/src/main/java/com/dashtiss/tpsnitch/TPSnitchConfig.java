package com.dashtiss.tpsnitch;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class TPSnitchConfig {
    public static final ModConfigSpec COMMON_SPEC;
    public static final TPSnitchConfig COMMON;

    public final ModConfigSpec.BooleanValue debug;
    public final ModConfigSpec.ConfigValue<String> logFileName;
    public final ModConfigSpec.IntValue logIntervalSeconds;

    static {
        Pair<TPSnitchConfig, ModConfigSpec> pair = new ModConfigSpec.Builder().configure(TPSnitchConfig::new);
        COMMON_SPEC = pair.getRight();
        COMMON = pair.getLeft();
        // Config registration is now handled in the mod class constructor.
    }

    TPSnitchConfig(ModConfigSpec.Builder builder) {
        debug = builder.comment("Enable debug logging").define("debug", false);
        logFileName = builder.comment("Log file name").define("logFileName", "tpsnitch_log.json");
        logIntervalSeconds = builder.comment("Log interval in seconds").defineInRange("logIntervalSeconds", 10, 1, 3600);
    }

    public static boolean isDebug() { return COMMON.debug.get(); }
    public static String getLogFileName() { return COMMON.logFileName.get(); }
    public static int getLogIntervalSeconds() { return COMMON.logIntervalSeconds.get(); }
}
