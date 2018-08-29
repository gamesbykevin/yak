package com.gamesbykevin.tradingbot.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;

import static com.gamesbykevin.tradingbot.util.Email.getFileDateDesc;

public class LogFile {

    private static String LOG_DIRECTORY;

    private static PrintWriter WRITER;

    public static String getFilenameMain() {
        return "main.log";
    }

    public static String getFilenameEmail() {
        return "email.log";
    }

    public static String getFilenameHistoryTracker() {
        return "history_tracker.log";
    }

    public static String getFilenameManager() {
        return "manager.log";
    }

    public static String getFilenameJsonOrder() {
        return "gdax_json.log";
    }

    public static final String FILE_SEPARATOR = System.getProperty("file.separator");

    public static String getLogDirectory() {

        if (LOG_DIRECTORY == null)
            LOG_DIRECTORY = "logs-" + getFileDateDesc();

        return LOG_DIRECTORY;
    }

    public static PrintWriter getPrintWriterJsonOrder() {

        if (WRITER == null)
            WRITER = getPrintWriter(getFilenameJsonOrder(), getLogDirectory());

        //return our object
        return WRITER;
    }

    public static PrintWriter getPrintWriter(final String filename, final String directories) {

        try {

            //create a new directory
            File file = new File(directories);

            //if the directory does not exist, create it
            if (!file.exists())
                file.mkdirs();

            //create new print writer
            return new PrintWriter(directories + FILE_SEPARATOR + filename, "UTF-8");

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}