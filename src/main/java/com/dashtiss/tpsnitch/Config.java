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
            name = "Log File Path"
    )
    @Comment(name="The name of the log file")
    public static String LogFilePath = "TPSLogs.json";

    @Entry(
            name = "Verbose"
    )
    @Comment(name="If true, the mod will log all events to the console")
    public static boolean Verbose = false;

    @Entry(
            name = "Max Logs",
            category = LOGS
    )
    public static int MaxLogs = 100; // Maximum number of logs to keep
}
