package com.dashtiss.tpsnitch;

import com.google.common.collect.Lists;
import eu.midnightdust.lib.config.MidnightConfig;
import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class Config extends MidnightConfig{

    public static final String LOGS = "logs";

    @Entry(
            name = "Time Between Logs",
            category = LOGS

    )
    public static int TimeBetweenTicks = 600; // in Ticks, default is 30 seconds. 30*20=600

    @Entry(
            category = LOGS
    )
    public static boolean DebugMode = false;

    @Entry(
            name = "Log To File",
            category = LOGS
    )
    public static boolean LogToFile = true;

    @Entry
    public static String LogFileName = "tpsnitch.log";

    @Entry(
            name = "Log File Path"
    )
    @Comment(name="The path to the log file")
    public static String LogFilePath = "JsonLogs/";

    @Entry(
            name = "Verbose"
    )
    @Comment(name="If true, the mod will log all events to the console")
    public static boolean Verbose = false;
}
