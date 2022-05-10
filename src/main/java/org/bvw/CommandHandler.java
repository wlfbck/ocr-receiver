package org.bvw;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.Properties;

public class CommandHandler {

    private static Logger logger = LogManager.getLogger();

    private Robot robot = new Robot();

    Properties properties;

    public CommandHandler(Properties properties) throws AWTException {
        this.properties = properties;
    }

    public void processCommand(String command) {
        if(!properties.containsKey("command_" + command)) {
            logger.warn("command is not mapped: " + command);
            return;
        }

        String key = properties.getProperty("command_" + command);
        if(!parseable(key)) {
            logger.error("Properties contain bad mapping! '" + "command_" + command + "' maps to '" + key + "'...");
            return;
        }
        Integer keyCode = Integer.parseInt(key, 16);
        robot.keyPress(keyCode);
        robot.delay(10);
        robot.keyRelease(keyCode);
        logger.info("pressed keycode '" + keyCode + "' which should be key '" + KeyEvent.getKeyText(keyCode) + "'.");
    }

    private boolean parseable(String key) {
        try {
            Integer.parseInt(key, 16);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
