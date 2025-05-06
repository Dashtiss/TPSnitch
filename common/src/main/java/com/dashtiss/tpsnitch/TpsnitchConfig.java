package com.dashtiss.tpsnitch;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * TPSnitch configuration loaded from config/tpsnitch.json.
 */
public class TpsnitchConfig {
    /** If true, enables debug logging. */
    public boolean debug = false;
    /** The file name for TPSnitch logs. */
    public String logFileName = "tpsnitch_log.json";
    /** Log interval in seconds. */
    public int logIntervalSeconds = 10;

    private static final Path CONFIG_PATH = Paths.get("config/tpsnitch.json");
    private static TpsnitchConfig INSTANCE;

    public static TpsnitchConfig get() {
        if (INSTANCE == null) {
            INSTANCE = load();
        }
        return INSTANCE;
    }

    private static TpsnitchConfig load() {
        TpsnitchConfig cfg = new TpsnitchConfig();
        try {
            if (Files.exists(CONFIG_PATH)) {
                JsonObject config = JsonParser.parseReader(new FileReader(CONFIG_PATH.toFile())).getAsJsonObject();
                cfg.debug = config.has("debug") && config.get("debug").getAsBoolean();
                cfg.logFileName = config.has("logFileName") ? config.get("logFileName").getAsString() : "tpsnitch_log.json";
                cfg.logIntervalSeconds = config.has("logIntervalSeconds") ? config.get("logIntervalSeconds").getAsInt() : 10;
            } else {
                cfg.save();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return cfg;
    }

    public void save() {
        try (FileWriter writer = new FileWriter(CONFIG_PATH.toFile())) {
            JsonObject config = new JsonObject();
            config.add("debug", new JsonPrimitive(debug));
            config.add("logFileName", new JsonPrimitive(logFileName));
            config.add("logIntervalSeconds", new JsonPrimitive(logIntervalSeconds));
            writer.write(config.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}