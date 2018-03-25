package com.gamesbykevin.tradingbot;

import com.gamesbykevin.tradingbot.agent.AgentHelper;
import com.gamesbykevin.tradingbot.agent.AgentManager;
import com.gamesbykevin.tradingbot.transaction.Transaction;

import static com.gamesbykevin.tradingbot.Main.FUNDS;
import static com.gamesbykevin.tradingbot.Main.NOTIFICATION_DELAY;
import static com.gamesbykevin.tradingbot.util.Email.sendEmail;
import static com.gamesbykevin.tradingbot.util.PropertyUtil.displayMessage;

public class MainHelper {

    //how often do we display/write the strategy description to the log file (milliseconds)
    private static final long STRATEGY_DELAY = 300000;

    //when was the last time we displayed the strategy summary
    private static long PREVIOUS_STRATEGY_DESC = 0;

    //what are the previous total assets
    private static double TOTAL_PREVIOUS = 0;

    //the previous time we sent a notification
    private static long PREVIOUS_TIME = System.currentTimeMillis();

    //how long has the bot been running
    protected static final long START = System.currentTimeMillis();

    protected static String getStrategySummaryDesc(Main main) {

        String strategyDesc = "Strategy Summary\n";

        //now show the total $ per trading strategy
        for (AgentManager.TradingStrategy strategy : AgentManager.TradingStrategy.values()) {

            double amount = 0;

            //check each manager looking for the strategy
            for (AgentManager agentManager : main.getAgentManagers().values()) {
                amount += agentManager.getAssets(strategy);
            }

            strategyDesc = strategyDesc + strategy.toString() + " $" + AgentHelper.formatValue(amount) + "\n";
        }

        //return our result
        return strategyDesc;
    }

    protected static void manageStatusUpdate(Main main) {

        //text of our notification message
        String subject = "", text = "\n";

        //how much did we start with
        text = text + "Started with $" + FUNDS + "\n";

        //how long has the bot been running
        text = text + "Bot Running: " + Transaction.getDurationDesc(System.currentTimeMillis() - START) + "\n\n";

        double total = 0;

        //print the summary of each agent manager
        for (AgentManager agentManager : main.getAgentManagers().values()) {

            //get the total assets for the current product
            final double assets = agentManager.getTotalAssets();

            //add to our total
            total += assets;

            //add to our details
            text = text + agentManager.getProductId() + " - $" + AgentHelper.formatValue(assets) + "\n";

            //display each agent's funds as well
            text = text + agentManager.getAgentDetails();

            //add line break in the end
            text = text + "\n";
        }

        text = text + "\n";

        //get our strategy summary
        final String strategyDesc = getStrategySummaryDesc(main);

        //add strategy summary to the overall message
        text = text + strategyDesc;

        subject = "Total assets $" + AgentHelper.formatValue(total);

        //print our total assets if they have changed
        if (total != TOTAL_PREVIOUS) {
            displayMessage(subject, main.getWriter());
            TOTAL_PREVIOUS = total;
        }

        //if enough time has passed send ourselves a notification
        if (System.currentTimeMillis() - PREVIOUS_TIME >= NOTIFICATION_DELAY) {

            //send our total assets in an email
            sendEmail(subject, text);

            //update the timer
            PREVIOUS_TIME = System.currentTimeMillis();

            //force our logic to write the strategy desc
            PREVIOUS_STRATEGY_DESC = System.currentTimeMillis() - STRATEGY_DELAY;

        } else {

            //how much time remaining
            final long duration = NOTIFICATION_DELAY - (System.currentTimeMillis() - PREVIOUS_TIME);

            //show when the next notification message will be sent
            displayMessage("Next notification message in " + Transaction.getDurationDesc(duration));
        }


        //we want to write this to log file periodically even if no notification message
        if (System.currentTimeMillis() - PREVIOUS_STRATEGY_DESC >= STRATEGY_DELAY) {

            //write summary strategy to log file
            displayMessage(strategyDesc, main.getWriter());

            //update our timer
            PREVIOUS_STRATEGY_DESC = System.currentTimeMillis();
        }
    }
}