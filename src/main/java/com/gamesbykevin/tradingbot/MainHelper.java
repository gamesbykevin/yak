package com.gamesbykevin.tradingbot;

import com.gamesbykevin.tradingbot.agent.AgentHelper;
import com.gamesbykevin.tradingbot.agent.AgentManager.TradingStrategy;
import com.gamesbykevin.tradingbot.transaction.Transaction;

import java.util.HashMap;

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

    protected static void manageStatusUpdate(Main main) {

        //text of our notification message
        String subject = "", text = "\n";

        //how much did we start with
        text = text + "Started with $" + FUNDS + "\n";

        //how long has the bot been running
        text = text + "Bot Running: " + Transaction.getDurationDesc(System.currentTimeMillis() - START) + "\n\n";

        double total = 0;

        //print the summary of each agent manager
        for (int i = 0; i < TRADING_CURRENCIES.length; i++) {

            //get the total assets for the current product
            final double assets = main.getAgentManagers().get(TRADING_CURRENCIES[i]).getTotalAssets();

            //add to our total
            total += assets;

            //add to our details
            text = text + main.getAgentManagers().get(TRADING_CURRENCIES[i]).getProductId() + " - $" + AgentHelper.formatValue(assets) + "\n";

            //display each agent's funds as well
            text = text + main.getAgentManagers().get(TRADING_CURRENCIES[i]).getAgentDetails();

            //add line break in the end
            text = text + "\n";
        }

        text = text + "\n";

        //format our message
        subject = "Total assets $" + AgentHelper.formatValue(total);

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
            sendEmail(subject, text);

            //update the timer
            PREVIOUS_TIME = System.currentTimeMillis();
        }
    }

    /**
     * Print to console when next notification message will send
     */
    protected static void displayNextStatusUpdateDesc() {

        //we con't send if there is no address
        if (!hasContactAddress())
            return;

        //show when the next notification message will be sent
        displayMessage("Next notification message in " + Transaction.getDurationDesc(NOTIFICATION_DELAY - (System.currentTimeMillis() - PREVIOUS_TIME)));
    }
}