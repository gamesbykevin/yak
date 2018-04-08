package com.gamesbykevin.tradingbot.util;

import com.gamesbykevin.tradingbot.agent.AgentManager;
import com.gamesbykevin.tradingbot.calculator.Period;

import java.io.*;

import static com.gamesbykevin.tradingbot.calculator.CalculatorHelper.addHistory;
import static com.gamesbykevin.tradingbot.calculator.CalculatorHelper.sortHistory;
import static com.gamesbykevin.tradingbot.calculator.CalculatorHelper.updateHistory;
import static com.gamesbykevin.tradingbot.util.LogFile.getPrintWriter;
import static com.gamesbykevin.tradingbot.util.PropertyUtil.displayMessage;
import static com.gamesbykevin.tradingbot.util.PropertyUtil.writeFile;

/**
 * This class will manage the historical candle data we get from said exchange
 */
public class History {

    private static String DIRECTORY = "history";

    private static String FILENAME = "candles.txt";

    public static void load(AgentManager manager) {

        //get the directory where our history is stored
        File directory = new File(getDirectory(manager));

        //if the directory does not exist, there is nothing to load
        if (!directory.exists())
            return;

        //start loading history
        displayMessage("Loading history: " + getDirectory(manager), manager.getWriter());

        //how big is our history
        final int size = manager.getCalculator().getHistory().size();

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
                    addHistory(manager.getCalculator().getHistory(), data);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //now let's make sure everything is sorted in order
        sortHistory(manager.getCalculator().getHistory());

        //any records loaded
        final int change = manager.getCalculator().getHistory().size() - size;

        //display records loaded
        displayMessage(change + " records added", manager.getWriter());

        //we are done loading history
        displayMessage("Done loading history: " + getDirectory(manager), manager.getWriter());
    }

    public static void write(AgentManager manager) {

        try {

            //create our print writer
            PrintWriter pw = getPrintWriter(FILENAME, getDirectory(manager));

            for (int i = 0; i < manager.getCalculator().getHistory().size(); i++) {
                pw.println(GSon.getGson().toJson(manager.getCalculator().getHistory().get(i)));
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
        return (DIRECTORY + "\\" + manager.getProductId() + "\\" + manager.getMyDuration().description);
    }
}