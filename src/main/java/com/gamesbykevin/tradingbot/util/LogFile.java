package com.gamesbykevin.tradingbot.util;

import com.gamesbykevin.tradingbot.calculator.strategy.Strategy;

import java.io.File;
import java.io.PrintWriter;

import static com.gamesbykevin.tradingbot.util.Email.getFileDateDesc;

public class LogFile {

    private static String LOG_DIRECTORY;

    public static String getFilenameMain() {
        return "main.log";
    }

    public static String getFilenameAgent() {
        return "agent.log";
    }

    public static String getFilenameManager() {
        return "manager.log";
    }

    public static String getLogDirectory() {

        if (LOG_DIRECTORY == null)
            LOG_DIRECTORY = "logs-" + getFileDateDesc();

        return LOG_DIRECTORY;
    }

    public static PrintWriter getPrintWriter(final String filename, final String directories) {

        try {

            //create a new directory
            File file = new File(directories);

            //if the directory does not exist, create it
            if (!file.exists())
                file.mkdirs();

            return new PrintWriter(directories + "\\" + filename, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}