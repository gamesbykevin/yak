package com.gamesbykevin.tradingbot.transaction;

import com.gamesbykevin.tradingbot.agent.Agent;

import com.gamesbykevin.tradingbot.agent.AgentManager;
import com.gamesbykevin.tradingbot.transaction.Transaction.Result;

import static com.gamesbykevin.tradingbot.agent.AgentHelper.formatValue;
import static com.gamesbykevin.tradingbot.agent.AgentManager.displayMessage;

public class TransactionHelper {

    /**
     * The reasons for why we buy
     */
    public enum ReasonBuy {

        Reason_1("There is a swing detected in the EMA"),
        Reason_2("MACD Strategy is showing a swing"),
        Reason_3("There is a divergence in the RSI"),
        Reason_4("Volume has a divergence"),
        Reason_5("MACS crossover and above trend line"),
        Reason_6("ADX is trending and +DI has crossed above -DI"),
        Reason_7("2 period rsi is above 90"),
        Reason_8("NR Price breakout"),
        Reason_9("MACD Histogram/Price is showing divergence"),
        Reason_10("Heikin-Ashi candles are now going bullish"),
        Reason_11("RSI is < support, ADX dm+ is above dm-"),
        Reason_12("RSI is < support, macd has divergence"),
        Reason_13("Price is below BB lower line"),
        Reason_14("Price is below long ema line, bb middle line, and rsi is below 50"),
        Reason_15("SO has a divergence"),
        Reason_16("SO has a crossover"),
        Reason_17("SO has EMA crossover and SO indicator < 50"),
        ;

        private final String description;

        ReasonBuy(String description) {
            this.description = description;
        }

        public String getDescription() {
            return this.description;
        }
    }

    /**
     * The reasons for why we sell
     */
    public enum ReasonSell {

        Reason_0("We have hit our hard stop"),
        Reason_1("There is a swing detected in the EMA"),
        Reason_2("MACD Strategy is showing a swing"),
        Reason_3("The current stock price is below both short and long emas"),
        Reason_4("There is a divergence in the RSI"),
        Reason_5("Volume has a divergence"),
        Reason_6("MACS crossover and below trend line"),
        Reason_7("-DI has crossed below +DI"),
        Reason_8("2 period rsi is below 10"),
        Reason_9("NR Period close is > purchase price"),
        Reason_10("MACD Histogram/Price is showing divergence"),
        Reason_11("Heikin-Ashi candles are now going bearish"),
        Reason_12("RSI is > resistance, ADX dm- is above dm+"),
        Reason_13("RSI is > resistance, macd has divergence"),
        Reason_14("Price is above BB upper line"),
        Reason_15("Price is above long ema line, bb middle line, and rsi is above 50"),
        Reason_16("SO has a divergence"),
        Reason_17("SO has a crossover"),
        Reason_18("SO has EMA crossover and SO indicator > 50"),
        ;

        private final String description;

        ReasonSell(String description) {
            this.description = description;
        }

        public String getDescription() {
            return this.description;
        }
    }

    public static String getAverageDurationDesc(Agent agent) {
        return "Avg time: " + Transaction.getDurationDesc(getAverageDuration(agent));
    }

    public static long getAverageDuration(Agent agent) {

        //how many transactions
        int count = 0;

        //total duration
        long duration = 0;

        //if empty return 0
        if (agent.getTransactions().isEmpty())
            return 0;

        //check every transaction
        for (Transaction transaction : agent.getTransactions()) {

            if (transaction.getResult() == null)
                continue;

            //keep track of total transactions
            count++;

            //add total duration
            duration += transaction.getDuration();
        }

        //if nothing, return 0
        if (count == 0)
            return 0;

        //return the average duration
        return (duration / count);
    }

    public static void displaySellingReasonCount(Agent agent, Result result) {

        //check each reason
        for (ReasonSell sell : ReasonSell.values()) {

            //keep track of the count
            int count = 0;

            //keep track of the money involved
            double amount = 0;

            //look at each transaction
            for (Transaction transaction : agent.getTransactions()) {

                //skip if no match
                if (transaction.getResult() == null || transaction.getResult() != result)
                    continue;

                //if there is a match increase the count
                if (transaction.getReasonSell() == sell) {
                    count++;
                    amount += transaction.getAmount();
                }
            }

            //display the count if greater than 0
            if (count > 0)
                displayMessage(agent, result.toString() + " Sell " + sell.toString() +  " total " + count + ", $" + formatValue(amount) + ". " + sell.getDescription(), true);
        }
    }

    public static void displayBuyingReasonCount(Agent agent, Result result) {

        //check each reason
        for (ReasonBuy buy : ReasonBuy.values()) {

            //keep track of the count
            int count = 0;

            //keep track of the money involved
            double amount = 0;

            //look at each transaction
            for (Transaction transaction : agent.getTransactions()) {

                //skip if no match
                if (transaction.getResult() == null || transaction.getResult() != result)
                    continue;

                //if there is a match increase the count
                if (transaction.getReasonBuy() == buy) {
                    count++;
                    amount += transaction.getAmount();
                }
            }

            //display the count if greater than 0
            if (count > 0)
                displayMessage(agent, result.toString() + " Buy " + buy.toString() +  " total " + count + ", $" + formatValue(amount) + ". " + buy.getDescription(), true);
        }
    }

    /**
     * Get the total $ amount for our transaction
     * @param result Do we want to check for wins or losses?
     * @return The total $ amount of the specified result
     */
    public static double getAmount(Agent agent, Result result) {

        double amount = 0;

        //check every transaction
        for (Transaction transaction : agent.getTransactions()) {

            if (transaction.getResult() == null)
                continue;

            //if there is a match keep track
            if (transaction.getResult() == result)
                amount += transaction.getAmount();
        }

        //return our result
        return amount;
    }

    /**
     * Get the total count of our transactions
     * @param result Do we want to check for wins or losses?
     * @return The total count of the specified result
     */
    public static int getCount(Agent agent, Result result) {

        int count = 0;

        //check every transaction
        for (Transaction transaction : agent.getTransactions()) {

            if (transaction.getResult() == null)
                continue;

            //if there is a match keep track
            if (transaction.getResult() == result)
                count++;
        }

        //return our result
        return count;
    }

    public static String getDescLost(Agent agent) {
        return "Lost :" + getCount(agent, Result.Lose) + ", $" + formatValue(getAmount(agent, Result.Lose));
    }

    public static String getDescWins(Agent agent) {
        return "Wins :" + getCount(agent, Result.Win) + ", $" + formatValue(getAmount(agent, Result.Win));
    }
}