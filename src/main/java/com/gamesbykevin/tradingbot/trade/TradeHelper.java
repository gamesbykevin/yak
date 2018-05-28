package com.gamesbykevin.tradingbot.trade;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.agent.AgentHelper;
import com.gamesbykevin.tradingbot.agent.AgentManager;
import com.gamesbykevin.tradingbot.trade.Trade.Result;
import com.gamesbykevin.tradingbot.util.LogFile;

import java.util.concurrent.TimeUnit;

import static com.gamesbykevin.tradingbot.agent.AgentHelper.NOTIFICATION_EVERY_TRANSACTION;
import static com.gamesbykevin.tradingbot.agent.AgentHelper.round;
import static com.gamesbykevin.tradingbot.agent.AgentManagerHelper.displayMessage;
import static com.gamesbykevin.tradingbot.util.Email.sendEmail;
import static com.gamesbykevin.tradingbot.util.LogFile.FILE_SEPARATOR;

public class TradeHelper {

    //how many decimals do we round when displaying the dollar description
    public static final int DESCRIPTION_DECIMALS_ACCURACY = 4;

    //the trades directoru
    public static final String TRADES_DIR = "trades";

    /**
     * The reasons for why we sold
     */
    public enum ReasonSell {

        Reason_Strategy("Sold based on strategy logic"),
        Reason_HardStop("We have hit our hard stop"),
        Reason_PriceDecline("Current $ has declined");

        private final String description;

        ReasonSell(String description) {
            this.description = description;
        }

        public String getDescription() {
            return this.description;
        }
    }

    private static String createTradeDuration(Trade trade) {

        //get the total milliseconds lapsed
        final long duration = trade.getDuration();

        //create our message
        return "Duration of the order from buy to sell (HH:MM:SS): " + getDurationDesc(duration);
    }

    public static String getDurationDesc(final long duration) {

        return String.format(
            "%02d:%02d:%02d",
            TimeUnit.MILLISECONDS.toHours(duration),
            TimeUnit.MILLISECONDS.toMinutes(duration) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(duration)),
            TimeUnit.MILLISECONDS.toSeconds(duration) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration))
        );
    }

    public static void displayTradeSummary(Agent agent, Trade trade) {

        String subject = "", text = "";

        if (trade.getOrderBuy() != null && trade.getOrderSell() == null) {

            //setup our notification message
            subject = "Purchase " + trade.getProductId();

            //start off with the product to start the transaction description
            text = "Buy " + trade.getProductId();

            //what is the quantity
            text += ", quantity: " + trade.getQuantityBuy();

            //what is the price
            text += " @ $" + trade.getOrderBuy();

            //display our fees
            text += ", buy fee $" + trade.getFeeBuy();

        } else if (trade.getOrderSell() != null) {

            //start off with the product to start the transaction description
            text = "Sell " + trade.getProductId();

            //what is the quantity
            text += ", quantity: " + trade.getQuantitySell();

            //what is the price
            text += " @ $" + trade.getPriceSell();

            //how much did we pay initially
            text += ", purchase $" + trade.getPriceBuy();

            //display our fee
            text += ", buy fee $" + trade.getFeeBuy();

            //display our fee
            text += ", sell fee $" + trade.getFeeSell() + "\n";

            //what was the low / high $ during the trade
            text += "Stock Max $" + trade.getPriceMax() + ", Stock Min $" + trade.getPriceMin() + "\n";

            //add our trade duration message
            text += createTradeDuration(trade) + "\n";

            //what happened during this order
            text += "Order Attempt Summary" + "\n";
            text += "Buy Reject:  " + trade.getCountRejectedBuy()  + "\n";
            text += "Buy Cancel:  " + trade.getCountCancelBuy()    + "\n";
            text += "Sell Reject: " + trade.getCountRejectedSell() + "\n";
            text += "Sell Cancel: " + trade.getCountCancelSell()   + "\n";

            //what is the reason for selling
            displayMessage(agent, "Reason sell: " + trade.getReasonSell().getDescription(), true);

            //display the amount of fees that we paid
            displayMessage(agent, "Fees $" + trade.getFeeSell(), true);

            //get the total fees
            final double fees = trade.getFeeBuy() + trade.getFeeSell();

            //did we win or lose?
            if (trade.getResult() == Result.Win) {
                subject = "We made $" + AgentHelper.round(DESCRIPTION_DECIMALS_ACCURACY, trade.getAmount() - fees);
            } else {
                subject = "We lost $" + AgentHelper.round(DESCRIPTION_DECIMALS_ACCURACY, trade.getAmount() + fees);
            }
        }

        //display and write to log
        displayMessage(agent, subject, true);
        displayMessage(agent, text, true);

        //are we going to notify every transaction?
        if (NOTIFICATION_EVERY_TRANSACTION && subject.length() > 0 && text.length() > 0)
            sendEmail(subject, text);

    }

    public static String getAverageDurationDesc(Agent agent) {
        return "Avg time: " + getDurationDesc(getAverageDuration(agent));
    }

    public static long getAverageDuration(Agent agent) {

        //how many transactions
        int count = 0;

        //total duration
        long duration = 0;

        //if empty return 0
        if (agent.getTrades().isEmpty())
            return 0;

        //check every transaction
        for (int i = 0; i < agent.getTrades().size(); i++) {

            //get the current trade
            Trade trade = agent.getTrades().get(i);

            if (trade.getResult() == null)
                continue;

            //keep track of total transactions
            count++;

            //add total duration
            duration += trade.getDuration();
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
            for (int x = 0; x < agent.getTrades().size(); x++) {

                Trade trade = agent.getTrades().get(x);

                //skip if no match
                if (trade.getResult() == null || trade.getResult() != result)
                    continue;

                //if there is a match increase the count
                if (trade.getReasonSell() == reasons[i]) {
                    count++;
                    amount += trade.getAmount();
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
        for (int i = 0; i < agent.getTrades().size(); i++) {

            //get the current trade
            Trade trade = agent.getTrades().get(i);

            if (trade.getResult() == null)
                continue;

            //if there is a match keep track
            if (trade.getResult() == result)
                amount += trade.getAmount();
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
        for (int i = 0; i < agent.getTrades().size(); i++) {

            //get the current trade
            Trade trade = agent.getTrades().get(i);

            if (trade.getResult() == null)
                continue;

            //if there is a match keep track
            if (trade.getResult() == result)
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
        for (int i = 0; i < agent.getTrades().size(); i++) {

            //get the current trade
            Trade trade = agent.getTrades().get(i);

            //add to our total fees
            fees += (trade.getFeeBuy() + trade.getFeeSell());
        }

        return fees;
    }

    public static String getDirectory(String productId) {
        return LogFile.getLogDirectory() + FILE_SEPARATOR + productId + FILE_SEPARATOR + TRADES_DIR + FILE_SEPARATOR;
    }

    public static void createTrade(Agent agent) {

        //if we have no trades or the most recent trade has been sold, we need to start a new trade
        if (agent.getTrades().isEmpty() || agent.getTrade().getOrderSell() != null) {

            //if there are no trades, create one
            agent.getTrades().add(new Trade(agent.getProductId(), agent.getCandle()));

        } else {

            //we can use the existing trade
            agent.getTrade().restart();

        }
    }
}