package com.gamesbykevin.tradingbot.util;

import com.gamesbykevin.tradingbot.calculator.strategy.Strategy;

import java.io.File;
import java.io.PrintWriter;

public class LogFile {

    public static final String LOG_DIRECTORY = "logs";

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
