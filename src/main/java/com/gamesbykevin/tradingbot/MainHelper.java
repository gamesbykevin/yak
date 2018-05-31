package com.gamesbykevin.tradingbot;

import com.gamesbykevin.tradingbot.agent.AgentHelper;
import com.gamesbykevin.tradingbot.agent.AgentManagerHelper;
import com.gamesbykevin.tradingbot.calculator.strategy.Strategy;
import com.gamesbykevin.tradingbot.trade.TradeHelper;

import static com.gamesbykevin.tradingbot.Main.FUNDS;
import static com.gamesbykevin.tradingbot.Main.NOTIFICATION_DELAY;
import static com.gamesbykevin.tradingbot.Main.TRADING_CURRENCIES;
import static com.gamesbykevin.tradingbot.calculator.Calculator.MY_TRADING_STRATEGIES;
import static com.gamesbykevin.tradingbot.util.Email.hasContactAddress;
import static com.gamesbykevin.tradingbot.util.Email.sendEmail;
import static com.gamesbykevin.tradingbot.util.PropertyUtil.displayMessage;

public class MainHelper {

    //what are the previous total assets
    private static double TOTAL_PREVIOUS = 0;

    //the previous time we sent a notification
    private static long PREVIOUS_TIME = System.currentTimeMillis();

    //how long has the bot been running
    protected static final long START = System.currentTimeMillis();

    //keep a list of our totals and desc
    private static double[] TOTALS;
    private static String[] DESC;

    protected static void manageStatusUpdate(Main main) {

        //text of our notification message
        String subject = "", text = "\n";

        //how much did we start with
        text = text + "Started with $" + FUNDS + "\n";

        //how long has the bot been running
        text = text + "Bot Running: " + TradeHelper.getDurationDesc(System.currentTimeMillis() - START) + "\n\n";

        double total = 0;

        //print the summary of each agent manager
        for (int i = 0; i < TRADING_CURRENCIES.length; i++) {

            //get the total assets for the current product
            final double assets = main.getAgentManagers().get(TRADING_CURRENCIES[i]).getTotalAssets();

            //add to our total
            total += assets;

            //add to our details
            text = text + main.getAgentManagers().get(TRADING_CURRENCIES[i]).getProductId() + " - $" + AgentHelper.round(assets) + "\n";

            //display each agent's funds as well
            text = text + AgentManagerHelper.getAgentDetails(main.getAgentManagers().get(TRADING_CURRENCIES[i]));

            //add line break in the end
            text = text + "\n";
        }

        text = text + "\n";

        //format our message
        subject = "Total assets $" + AgentHelper.round(total);

        if (total != TOTAL_PREVIOUS) {

            //if assets changed, write to log file
            displayMessage(subject, main.getWriter());

            //update the new total
            TOTAL_PREVIOUS = total;

        } else {

            //else just print to console
            displayMessage(subject);

        }

        //if enough time has passed send ourselves a notification
        if (System.currentTimeMillis() - PREVIOUS_TIME >= NOTIFICATION_DELAY) {

            //send our total assets in an email
            sendEmail(subject, getProgressSummary(main) + "\n\n\n" + text);

            //update the timer
            PREVIOUS_TIME = System.currentTimeMillis();
        }
    }

    /**
     * Here we will add the agents together by trading strategy
     * @param main
     * @return
     */
    protected static String getProgressSummary(Main main) {

        String result = "";

        if (MY_TRADING_STRATEGIES == null)
            return result;

        if (DESC == null || TOTALS == null) {

            DESC = new String[MY_TRADING_STRATEGIES.length];
            TOTALS = new double[MY_TRADING_STRATEGIES.length];

        } else {

            for (int i = 0; i < DESC.length; i++) {
                DESC[i] = "";
                TOTALS[i] = 0;
            }
        }

        int index = 0;

        for (int i = 0; i < MY_TRADING_STRATEGIES.length; i++) {

            //get the current strategy
            final Strategy.Key key = MY_TRADING_STRATEGIES[i];

            double total = 0;

            for (int m = 0; m < main.getProducts().size(); m++) {
                total += main.getAgentManagers().get(main.getProducts().get(m).getId()).getTotalAssets(key);
            }

            //assign our data
            DESC[index] = key.toString() + " $" + AgentHelper.round(total) + "\n";
            TOTALS[index] = total;

            //increase index
            index++;
        }

        //sort by most $ first
        for (int i = 0; i < TOTALS.length; i++) {

            for (int j = i + 1; j < TOTALS.length; j++) {

                //order if next is greater
                if (TOTALS[j] > TOTALS[i]) {

                    double tmp1 = TOTALS[i];
                    double tmp2 = TOTALS[j];

                    String desc1 = DESC[i];
                    String desc2 = DESC[j];

                    TOTALS[i] = tmp2;
                    TOTALS[j] = tmp1;

                    DESC[i] = desc2;
                    DESC[j] = desc1;

                }
            }
        }

        for (int i = 0; i < DESC.length; i++) {
            result += DESC[i];
        }

        return result;
    }

    /**
     * Print to console when next notification message will send
     */
    protected static void displayNextStatusUpdateDesc() {

        //we con't send if there is no address
        if (!hasContactAddress())
            return;

        //show when the next notification message will be sent
        displayMessage("Next notification message in " + TradeHelper.getDurationDesc(NOTIFICATION_DELAY - (System.currentTimeMillis() - PREVIOUS_TIME)));
    }
}