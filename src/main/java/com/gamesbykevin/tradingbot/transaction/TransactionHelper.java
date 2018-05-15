package com.gamesbykevin.tradingbot.transaction;

import com.gamesbykevin.tradingbot.agent.Agent;

import com.gamesbykevin.tradingbot.transaction.Transaction.Result;

import static com.gamesbykevin.tradingbot.agent.AgentHelper.round;
import static com.gamesbykevin.tradingbot.agent.AgentManagerHelper.displayMessage;

public class TransactionHelper {

    /**
     * The reasons for why we sold
     */
    public enum ReasonSell {

        Reason_Strategy("Sold based on strategy logic"),
        Reason_HardStop("We have hit our hard stop"),
        Reason_PriceDecline("Current $ has declined"),
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
        for (int i = 0; i < agent.getTransactions().size(); i++) {

            if (agent.getTransactions().get(i).getResult() == null)
                continue;

            //keep track of total transactions
            count++;

            //add total duration
            duration += agent.getTransactions().get(i).getDuration();
        }

        //if nothing, return 0
        if (count == 0)
            return 0;

        //return the average duration
        return (duration / count);
    }

    public static void displaySellReasonCount(Agent agent, Result result) {

        //obtain list of reasons to sell
        ReasonSell[] reasons = ReasonSell.values();

        //check each reason
        for (int i = 0; i < reasons.length; i++) {

            //keep track of the count
            int count = 0;

            //keep track of the money involved
            double amount = 0;

            //look at each transaction
            for (int x = 0; x < agent.getTransactions().size(); x++) {

                //skip if no match
                if (agent.getTransactions().get(x).getResult() == null || agent.getTransactions().get(x).getResult() != result)
                    continue;

                //if there is a match increase the count
                if (agent.getTransactions().get(x).getReasonSell() == reasons[i]) {
                    count++;
                    amount += agent.getTransactions().get(x).getAmount();
                }
            }

            //display the count if greater than 0
            if (count > 0)
                displayMessage(agent, result.toString() + " Sell " + reasons[i].toString() +  " total " + count + ", $" + round(amount) + ". " + reasons[i].getDescription(), true);
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
        for (int i = 0; i < agent.getTransactions().size(); i++) {

            if (agent.getTransactions().get(i).getResult() == null)
                continue;

            //if there is a match keep track
            if (agent.getTransactions().get(i).getResult() == result)
                amount += agent.getTransactions().get(i).getAmount();
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
        for (int i = 0; i < agent.getTransactions().size(); i++) {

            if (agent.getTransactions().get(i).getResult() == null)
                continue;

            //if there is a match keep track
            if (agent.getTransactions().get(i).getResult() == result)
                count++;
        }

        //return our result
        return count;
    }

    public static String getDescLost(Agent agent) {
        return "Lost :" + getCount(agent, Result.Lose) + ", $" + round(getAmount(agent, Result.Lose));
    }

    public static String getDescWins(Agent agent) {
        return "Wins :" + getCount(agent, Result.Win) + ", $" + round(getAmount(agent, Result.Win));
    }

    public static double getTotalFees(Agent agent) {

        //how many fees have we paid?
        double fees = 0;

        //total the fees from all of our transactions
        for (int i = 0; i < agent.getTransactions().size(); i++) {
            fees += (agent.getTransactions().get(i).getFeeBuy() + agent.getTransactions().get(i).getFeeSell());
        }

        return fees;
    }
}