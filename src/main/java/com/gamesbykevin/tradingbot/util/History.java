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

    private static String DIRECTORY = "history";

    private static String FILENAME = "candles.txt";

    /**
     * Character separating each piece of data in the history
     */
    private static final String DELIMITER = ",";

    /**
     * Display updates when loading files
     */
    public static final int NOTIFY_LIMIT = 2500;

    public static synchronized void load(List<Period> history, String productId, Candle candle, PrintWriter writer) {

        //get the directory where our history is stored
        File directory = new File(getDirectory(productId, candle));

        //if the directory does not exist, there is nothing to load
        if (!directory.exists())
            return;

        //start loading history
        displayMessage("Loading history: " + getDirectory(productId, candle), writer);

        //how big is our history
        final int size = history.size();

        //we will load from every file in the directory
        for (File file : directory.listFiles()) {

            try {

                //start reading the file
                BufferedReader bufferedReader = new BufferedReader(new FileReader(file));

                int count =  0;

                //check every line of the text file
                while(true) {

                    if (count % NOTIFY_LIMIT == 0)
                        displayMessage(productId + " " + candle.description + " count: " + count);

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

                    //Period data = GSon.getGson().fromJson(line, Period.class);
                    tmpData = null;


                    //add to our count
                    count++;
                }

                //recycle
                bufferedReader.close();
                bufferedReader = null;

            } catch (Exception e) {
                e.printStackTrace();
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

    protected static synchronized boolean write(List<Period> history, String productId, Candle duration) {

        try {

            //create our print writer
            PrintWriter pw = getPrintWriter(FILENAME, getDirectory(productId, duration));

            for (int i = 0; i < history.size(); i++) {

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

                //pw.println(GSon.getGson().toJson(history.get(i)));
            }

            //write all remaining bytes
            pw.flush();

            //close the file
            pw.close();

            //flag null for garbage collection
            pw = null;

            //we have success
            return true;

        } catch (Exception e) {

            //print error message
            e.printStackTrace();

            //we weren't successful
            return false;
        }
    }

    private static String getDirectory(String productId, Candle duration) {
        return (DIRECTORY + FILE_SEPARATOR + productId + FILE_SEPARATOR + duration.description);
    }
}