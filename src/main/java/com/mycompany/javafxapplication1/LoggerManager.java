package com.mycompany.javafxapplication1;

import java.io.IOException;
import java.util.logging.*;

public class LoggerManager {
    private static final Logger LOGGER = Logger.getLogger(LoggerManager.class.getName());

    static {
        try {
            // Set up file handler to write logs to "app.log"
            FileHandler fileHandler = new FileHandler("app.log", true);
            fileHandler.setFormatter(new SimpleFormatter());
            
            // Console Handler to show logs in the terminal
            ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setLevel(Level.INFO);

            // Add handlers to the logger
            LOGGER.addHandler(fileHandler);
            LOGGER.addHandler(consoleHandler);
            LOGGER.setUseParentHandlers(false); // Prevent duplicate logging in console
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Logger getLogger() {
        return LOGGER;
    }
}
