package com.dashtiss.tpsnitch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Constants used throughout the TPSnitch mod.
 * Contains mod identifiers and a logger instance.
 * 
 * This class serves as a central location for storing and retrieving constants
 * that are used throughout the TPSnitch mod. It provides a single point of
 * reference for mod identifiers and logging.
 */
public class Constants {

    /** Mod ID for internal reference. */
    public static final String MOD_ID = "tpsnitch";
    /** Mod name for logging and display. */
    public static final String MOD_NAME = "TPSnitch";
    /** Logger instance for TPSnitch. */
    public static final Logger LOG = LoggerFactory.getLogger(MOD_NAME);
}