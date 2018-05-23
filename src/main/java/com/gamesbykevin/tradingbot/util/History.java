package com.gamesbykevin.tradingbot.util;

import com.gamesbykevin.tradingbot.calculator.Calculator.Duration;
import com.gamesbykevin.tradingbot.calculator.Period;

import java.io.*;
import java.util.List;

import static com.gamesbykevin.tradingbot.calculator.utils.CalculatorHelper.addHistory;
import static com.gamesbykevin.tradingbot.calculator.utils.CalculatorHelper.sortHistory;
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
     * Display updates when loading files
     */
    private static final int NOTIFY_LIMIT = 1500;

    public static synchronized void load(List<Period> history, String productId, Duration duration, PrintWriter writer) {

        //get the directory where our history is stored
        File directory = new File(getDirectory(productId, duration));

        //if the directory does not exist, there is nothing to load
        if (!directory.exists())
            return;

        //start loading history
        displayMessage("Loading history: " + getDirectory(productId, duration), writer);

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
                        displayMessage("Count: " + count);

                    //read the line in the text file
                    final String line = bufferedReader.readLine();

                    //if null we are done reading our file
                    if (line == null)
                        break;

                    //the line is a json string we can convert to array
                    Period data = GSon.getGson().fromJson(line, Period.class);

                    //add period to our history
                    addHistory(history, data);

                    //add to our count
                    count++;
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

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
        displayMessage("Done loading history: " + getDirectory(productId, duration), writer);
    }

    public static synchronized void write(List<Period> history, String productId, Duration duration) {

        try {

            //create our print writer
            PrintWriter pw = getPrintWriter(FILENAME, getDirectory(productId, duration));

            for (int i = 0; i < history.size(); i++) {
                pw.println(GSon.getGson().toJson(history.get(i)));
            }

            //write all remaining bytes
            pw.flush();

            //close the file
            pw.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String getDirectory(String productId, Duration duration) {
        return (DIRECTORY + FILE_SEPARATOR + productId + FILE_SEPARATOR + duration.description);
    }
}