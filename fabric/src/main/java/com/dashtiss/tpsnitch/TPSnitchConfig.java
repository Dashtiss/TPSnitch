package com.dashtiss.tpsnitch;

public class TPSnitchConfig {
    public boolean debug = false;
    public String logFileName = "tpsnitch_log.json";
    public int logIntervalSeconds = 10;

    public TPSnitchConfig() {}
    public TPSnitchConfig(boolean debug, String logFileName, int logIntervalSeconds) {
        this.debug = debug;
        this.logFileName = logFileName;
        this.logIntervalSeconds = logIntervalSeconds;
    }
}
