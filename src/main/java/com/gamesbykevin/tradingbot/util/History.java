package com.gamesbykevin.tradingbot.util;

import com.gamesbykevin.tradingbot.calculator.Calculator.Candle;
import com.gamesbykevin.tradingbot.calculator.Period;

import java.io.*;
import java.util.List;

import static com.gamesbykevin.tradingbot.calculator.CalculatorHelper.addHistory;
import static com.gamesbykevin.tradingbot.calculator.CalculatorHelper.sortHistory;
import static com.gamesbykevin.tradingbot.calculator.Period.*;
import static com.gamesbykevin.tradingbot.util.LogFile.FILE_SEPARATOR;
import static com.gamesbykevin.tradingbot.util.LogFile.getPrintWriter;
import static com.gamesbykevin.tradingbot.util.PropertyUtil.displayMessage;

/**
 * This class will manage the historical candle data we get from said exchange
 */
public class History {

    /**
     * Parent directory for our candles
     */
    private static String DIRECTORY = "history";

    /**
     * The name of the file will start with this
     */
    private static String FILENAME_DESC = "candles_";

    /**
     * What is the filename extension
     */
    private static String FILENAME_EXT = ".txt";

    /**
     * Character separating each piece of data in the history
     */
    private static final String DELIMITER = ",";

    /**
     * Display updates when loading files
     */
    public static final int NOTIFY_LIMIT = 2500;

    /**
     * How many lines can we have per .txt file
     */
    public static final int FILE_LINE_LIMIT = 10000;

    private static synchronized String getFileName(File directory, boolean existing) {

        final int length = directory.listFiles().length;

        //if looking for the existing file name
        if (existing) {

            if (length == 1)
                return directory.listFiles()[0].getName();

             return FILENAME_DESC + length + FILENAME_EXT;
        } else {
            return FILENAME_DESC + (length + 1) + FILENAME_EXT;
        }
    }

    public static synchronized void load(List<Period> history, String productId, Candle candle, PrintWriter writer, boolean archive) {

        //the directory where our history is stored
        File directory = null;

        //get the directory where our history is stored
        if (candle.dependency == null) {
            directory = new File(getDirectory(productId, candle));
        } else {
            directory = new File(getDirectory(productId, candle.dependency));
        }

        //if the directory does not exist, there is nothing to load
        if (!directory.exists())
            return;

        //start loading history
        displayMessage("Loading history: " + getDirectory(productId, candle), writer);

        //how big is our history
        final int size = history.size();

        //if we are archiving files we only need to look at the recent file
        if (archive) {

            //get the filename of the recent file
            String filename = getFileName(directory, true);

            //search every file in the directory
            for (File file : directory.listFiles()) {

                //if the file names match load this 1 file, or if there is only 1 file existing
                if (file.getName().equalsIgnoreCase(filename) || directory.listFiles().length == 1) {
                    loadFile(file, history, productId, candle);
                    break;
                }
            }

        } else {

            //we will load from every file in the directory
            for (File file : directory.listFiles()) {

                //load the current file
                loadFile(file, history, productId, candle);
            }
        }

        //mark null
        directory = null;

        //notify user
        displayMessage("Sorting records...");

        //now let's make sure everything is sorted in order
        sortHistory(history);

        //notify user
        displayMessage("Sorting done");

        //any records loaded
        final int change = history.size() - size;

        //display records loaded
        displayMessage(change + " records added", writer);

        //we are done loading history
        displayMessage("Done loading history: " + getDirectory(productId, candle), writer);
    }

    private static synchronized void loadFile(File file, List<Period> history, String productId, Candle candle) {

        try {

            //start reading the file
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));

            int count =  0;

            //check every line of the text file
            while(true) {

                if (count % NOTIFY_LIMIT == 0)
                    displayMessage(file.getName() + " - " + productId + " " + candle.description + " count: " + count);

                //read the line in the text file
                final String line = bufferedReader.readLine();

                //if null we are done reading our file
                if (line == null)
                    break;

                //the line is a json string we can convert to array
                String[] tmpData = line.split(DELIMITER);

                //add period to our history
                addHistory(history,
                    Long.parseLong(tmpData[PERIOD_INDEX_TIME]),
                    Double.parseDouble(tmpData[PERIOD_INDEX_LOW]),
                    Double.parseDouble(tmpData[PERIOD_INDEX_HIGH]),
                    Double.parseDouble(tmpData[PERIOD_INDEX_OPEN]),
                    Double.parseDouble(tmpData[PERIOD_INDEX_CLOSE]),
                    Double.parseDouble(tmpData[PERIOD_INDEX_VOLUME])
                );

                tmpData = null;

                //add to our count
                count++;
            }

            //recycle
            bufferedReader.close();
            bufferedReader = null;

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            file = null;
        }
    }

    protected static synchronized boolean write(List<Period> history, String productId, Candle duration) {

        try {

            //directory path
            String directoryPath = getDirectory(productId, duration);

            //the directory where we will save our candle data
            File directory = new File(directoryPath);

            if (history.size() < FILE_LINE_LIMIT) {

                //write data to existing file
                writeFile(getFileName(directory, true), directoryPath, history);

            } else {

                //continue until we separate the data into smaller files
                while (history.size() > 0) {

                    //write the next # of lines to a new text file
                    writeFile(getFileName(directory, false), directoryPath, history);
                }

            }


            //we have success
            return true;

        } catch (Exception e) {

            //print error message
            e.printStackTrace();

            //we weren't successful
            return false;
        }
    }

    private static void writeFile(String filename, String directory, List<Period> history) {

        //create our print writer
        PrintWriter pw = getPrintWriter(filename, directory);

        for (int i = 0; i < FILE_LINE_LIMIT; i++) {

            //if we exceed the size of the file, exit
            if (i >= history.size())
                break;

            //get the current period
            Period period = history.get(i);

            //write data to file
            pw.println(
                period.time + DELIMITER +
                period.low + DELIMITER +
                period.high + DELIMITER +
                period.open + DELIMITER +
                period.close + DELIMITER +
                period.volume
            );
        }

        //remove records written to a file
        for (int i = 0; i < FILE_LINE_LIMIT; i++) {

            //if nothing left to remove, exit loop
            if (history.isEmpty())
                break;

            //remove the first line
            history.remove(0);
        }
        //write all remaining bytes
        pw.flush();

        //close the file
        pw.close();

        //flag null for garbage collection
        pw = null;
    }

    private static String getDirectory(String productId, Candle duration) {
        return (DIRECTORY + FILE_SEPARATOR + productId + FILE_SEPARATOR + duration.description);
    }
}