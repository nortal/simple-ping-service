package com.nortal.ping.simple;

import java.io.IOException;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * @author Margus Hanni <margus.hanni@nortal.com>
 */
public class PingServiceLogger {

    private static FileHandler file;
    private static SimpleFormatter formatter;

    public static void setup() throws IOException {

        // get the global logger to configure it

        Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

        // suppress the logging output to the console
        Logger rootLogger = Logger.getLogger("");

        Handler[] handlers = rootLogger.getHandlers();

        if (handlers[0] instanceof ConsoleHandler) {
            rootLogger.removeHandler(handlers[0]);
        }

        logger.setLevel(Level.INFO);

        file = new FileHandler("ping.log", Integer.MAX_VALUE, 1, true);

        // create a TXT formatter
        formatter = new SimpleFormatter();

        file.setFormatter(formatter);

        logger.addHandler(file);
    }
}
