package com.gamesbykevin.tradingbot;

import com.gamesbykevin.tradingbot.agent.AgentManagerHelper;
import com.gamesbykevin.tradingbot.calculator.Calculator;
import com.gamesbykevin.tradingbot.calculator.Period.Fields;
import com.gamesbykevin.tradingbot.calculator.strategy.Strategy;
import com.gamesbykevin.tradingbot.trade.TradeHelper;

import static com.gamesbykevin.tradingbot.Main.FUNDS;
import static com.gamesbykevin.tradingbot.Main.NOTIFICATION_DELAY;
import static com.gamesbykevin.tradingbot.Main.getTradingCurrencies;
import static com.gamesbykevin.tradingbot.agent.AgentHelper.round;
import static com.gamesbykevin.tradingbot.calculator.Calculation.getRecent;
import static com.gamesbykevin.tradingbot.calculator.Calculator.MY_TRADING_STRATEGIES;
import static com.gamesbykevin.tradingbot.calculator.Calculator.PERIODS_SMA;
import static com.gamesbykevin.tradingbot.trade.TradeHelper.NEW_LINE;
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
        String subject = "", text = "";

        //how much did we start with
        text = "Started with $" + FUNDS + NEW_LINE;

        //how long has the bot been running
        text += "Bot Running: " + TradeHelper.getDurationDesc(System.currentTimeMillis() - START) + NEW_LINE;

        //show the sma summary so we are aware of our progress
        if (PERIODS_SMA > 0)
            text += getSmaSummary(main) + NEW_LINE + NEW_LINE;

        double total = 0;

        //print the summary of each agent manager
        for (int i = 0; i < getTradingCurrencies().length; i++) {

            //get the total assets for the current product
            final double assets = main.getAgentManagers().get(getTradingCurrencies()[i]).getTotalAssets();

            //add to our total
            total += assets;

            //add to our details
            text += main.getAgentManagers().get(getTradingCurrencies()[i]).getProductId() + " - $" + round(assets) + NEW_LINE;

            //display each agent's funds as well
            text += AgentManagerHelper.getAgentDetails(main.getAgentManagers().get(getTradingCurrencies()[i]));

            //add line break in the end
            text += NEW_LINE;
        }

        text = text + NEW_LINE;

        //format our message
        subject = "Total assets $" + round(total);

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

            String desc = getProgressSummary(main) + NEW_LINE + NEW_LINE + NEW_LINE + text;

            //send our total assets in an email
            sendEmail(subject, desc);

            //write summary to log
            displayMessage(desc, main.getWriter());

            //update the timer
            PREVIOUS_TIME = System.currentTimeMillis();
        }
    }

    /**
     * In this method we will display all the 200 period smas for each coin
     * @param main
     * @return
     */
    private static String getSmaSummary(Main main) {

        String desc = "";

        for (int i = 0; i < getTradingCurrencies().length; i++) {

            Calculator calc = main.getAgentManagers().get(getTradingCurrencies()[i]).getCalculator();

            //get the recent values
            final double close = getRecent(calc.getHistory(), Fields.Close);
            final double sma = getRecent(calc.getObjSMA().getSma());

            desc += getTradingCurrencies()[i] + " - Close $" + close + ", " + PERIODS_SMA + " SMA $" +  round(sma) + NEW_LINE;

        }

        //add an extra line
        desc += NEW_LINE;

        //return our message
        return desc;
    }

    /**
     * Here we will add the agents together by trading strategy
     * @param main
     * @return
     */
    private static String getProgressSummary(Main main) {

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
            DESC[index] = key.toString() + " $" + round(total) + NEW_LINE;
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