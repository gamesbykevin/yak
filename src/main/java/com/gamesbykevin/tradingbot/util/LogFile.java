package com.gamesbykevin.tradingbot.util;

import java.io.File;
import java.io.PrintWriter;

public class LogFile {

    public static PrintWriter getPrintWriter(final String filename) {

        try {
            return new PrintWriter(filename, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
