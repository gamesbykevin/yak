package com.gamesbykevin.tradingbot.util;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.agent.AgentManager;
import com.gamesbykevin.tradingbot.calculator.Calculator;
import com.gamesbykevin.tradingbot.calculator.Calculator.Duration;
import com.gamesbykevin.tradingbot.calculator.Period;

import java.io.*;
import java.util.List;

import static com.gamesbykevin.tradingbot.calculator.CalculatorHelper.addHistory;
import static com.gamesbykevin.tradingbot.calculator.CalculatorHelper.sortHistory;
import static com.gamesbykevin.tradingbot.util.LogFile.getPrintWriter;
import static com.gamesbykevin.tradingbot.util.PropertyUtil.displayMessage;

/**
 * This class will manage the historical candle data we get from said exchange
 */
public class History {

    private static String DIRECTORY = "history";

    private static String FILENAME = "candles.txt";

    public static synchronized void load(AgentManager manager) {
        load(manager.getCalculator().getHistory(), manager.getProductId(), manager.getMyDuration(), manager.getWriter());
    }

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

                //check every line of the text file
                while(true) {

                    //read the line in the text file
                    final String line = bufferedReader.readLine();

                    //if null we are done reading our file
                    if (line == null)
                        break;

                    //the line is a json string we can convert to array
                    Period data = GSon.getGson().fromJson(line, Period.class);

                    //add period to our history
                    addHistory(history, data);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //now let's make sure everything is sorted in order
        sortHistory(history);

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

    private static String getDirectory(AgentManager manager) {
        return getDirectory(manager.getProductId(), manager.getMyDuration());
    }

    private static String getDirectory(String productId, Duration duration) {
        return (DIRECTORY + "\\" + productId + "\\" + duration.description);
    }
}