package com.gamesbykevin.tradingbot.agent;

import com.coinbase.exchange.api.entity.Product;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.strategy.Strategy;
import com.gamesbykevin.tradingbot.order.BasicOrderHelper.Action;
import com.gamesbykevin.tradingbot.trade.Trade;
import com.gamesbykevin.tradingbot.trade.TradeHelper.ReasonSell;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static com.gamesbykevin.tradingbot.agent.AgentManagerHelper.displayMessage;
import static com.gamesbykevin.tradingbot.agent.AgentMessageHelper.*;
import static com.gamesbykevin.tradingbot.order.LimitOrderHelper.createLimitOrder;
import static com.gamesbykevin.tradingbot.trade.TradeHelper.createTrade;
import static com.gamesbykevin.tradingbot.wallet.Wallet.STOP_TRADING_RATIO;

public class AgentHelper {

    /**
     * How much do we round the decimals when purchasing stock
     */
    public static final int ROUND_DECIMALS_PRICE = 2;

    /**
     * How much do we round the decimals when choosing quantity
     */
    public static final int ROUND_DECIMALS_QUANTITY = 3;

    /**
     * Let's protect our investment in case the stock price drops too much (when we are above our sma)
     */
    public static float HARD_STOP_RATIO_ABOVE_SMA;

    /**
     * Let's protect our investment in case the stock price drops too much (when we are below our sma)
     */
    public static float HARD_STOP_RATIO_BELOW_SMA;

    /**
     * If the stock price increases soo much let's sell and take profit while we can (when we are above our sma)
     */
    public static float HARD_SELL_RATIO_ABOVE_SMA;

    /**
     * If the stock price increases soo much let's sell and take profit while we can (when we are below our sma)
     */
    public static float HARD_SELL_RATIO_BELOW_SMA;

    /**
     * Do we want to send a notification for every transaction?
     */
    public static boolean NOTIFICATION_EVERY_TRANSACTION = false;

    /**
     * How many times do we wait for the sell order to fill before we cancel
     */
    public static int SELL_ATTEMPT_LIMIT = 10;

    /**
     * How many times do we wait for the buy order to fill before we cancel
     */
    public static int BUY_ATTEMPT_LIMIT = 10;

    /**
     * How many current prices do we track looking for a decline when selling?
     */
    public static int CURRENT_PRICE_HISTORY;

    protected static void checkStanding(Agent agent) {

        //if we lost too much money and have no quantity pending, we will stop trading
        if (agent.getWallet().getFunds() < (STOP_TRADING_RATIO * agent.getWallet().getFundsBeforeTrade()) && agent.getWallet().getQuantity() <= 0) {

            //flag the agent to stop
            agent.setStop(true);

            //send notify message
            displayMessageStopTrading(agent);
        }

        //if our money has gone up, increase the stop trading limit
        if (agent.getWallet().getFunds() > agent.getWallet().getFundsBeforeTrade()) {

            final double oldLimit = (STOP_TRADING_RATIO * agent.getWallet().getFundsBeforeTrade());
            agent.getWallet().setFundsBeforeTrade(agent.getWallet().getFunds());
            final double newLimit = (STOP_TRADING_RATIO * agent.getWallet().getFundsBeforeTrade());
            displayMessageLimitIncrease(agent, oldLimit, newLimit);
        }
    }

    public static BigDecimal round(double number) {
        return round(ROUND_DECIMALS_QUANTITY, number);
    }

    public static BigDecimal round(int decimals, float number) {
        return round(decimals, BigDecimal.valueOf(number));
    }

    public static BigDecimal round(int decimals, double number) {
        return round(decimals, BigDecimal.valueOf(number));
    }

    public static BigDecimal round(int decimals, BigDecimal number) {

        try {
            return number.setScale(decimals, RoundingMode.HALF_DOWN);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return BigDecimal.valueOf(0d);
    }

    public static String getStockInvestmentDesc(Agent agent) {
        return "Owned Stock: " + round(agent.getWallet().getQuantity());
    }

    protected static boolean hasDecline(double[] price) {

        for (int i = 0; i < price.length - 1; i++) {

            //if the next price is more than the current or 0, we can't confirm decline yet
            if (price[i] < price[i + 1] || price[i] <= 0 || price[i + 1] <= 0)
                return false;
        }

        //every value continued to go down, so we have a decline
        return true;
    }

    protected static void checkBuy(Agent agent, Strategy strategy, List<Period> history, Product product, double price, final boolean aboveSMA) {

        //if we need to wait for the next candle period we won't continue
        if (strategy.hasWait()) {

            //we are still waiting
            displayMessage(agent, "Waiting for next candle. Available funds $" + agent.getWallet().getFunds(), false);
            return;
        }

        //check for a buy signal
        boolean buy = strategy.hasBuySignal(agent, history, price);

        //display our data
        strategy.displayData(agent, buy);

        //we will buy if there is a reason
        if (buy) {

            //create our trade object
            createTrade(agent);

            //get the current trade
            Trade trade = agent.getTrade();

            //what is the lowest and highest price during this trade?
            trade.setPriceMin(price);
            trade.setPriceMax(price);

            if (aboveSMA) {

                //let's set our hard stop $ if it isn't already set
                if (trade.getHardStopPrice() == 0)
                    trade.setHardStopPrice(price - (price * HARD_STOP_RATIO_ABOVE_SMA));

                //let's sell if the $ goes above this amount if it isn't already set
                if (trade.getHardSellPrice() == 0)
                    trade.setHardSellPrice(price + (price * HARD_SELL_RATIO_ABOVE_SMA));

            } else {

                //let's set our hard stop $ if it isn't already set
                if (trade.getHardStopPrice() == 0)
                    trade.setHardStopPrice(price - (price * HARD_STOP_RATIO_BELOW_SMA));

                //let's sell if the $ goes above this amount if it isn't already set
                if (trade.getHardSellPrice() == 0)
                    trade.setHardSellPrice(price + (price * HARD_SELL_RATIO_BELOW_SMA));

            }

            //write hard stop amount to our log file
            displayMessage(agent, "Current Price $" + price + ", Hard stop $" + round(trade.getHardStopPrice()) + ", Hard sell $" + round(trade.getHardSellPrice()), true);

            //create and assign our limit order
            agent.setOrder(createLimitOrder(agent, Action.Buy, product, price));

        } else {

            //we are still waiting
            displayMessage(agent, "Waiting. Available funds $" + agent.getWallet().getFunds(), false);
        }
    }

    protected static void checkSell(Agent agent, Strategy strategy, List<Period> history, Product product, double price, final boolean aboveSMA) {

        //get the latest closing price
        final double close = history.get(history.size() - 1).close;

        //get the current trade
        Trade trade = agent.getTrade();

        //keep track of the lowest / highest price during a single trade
        trade.checkPriceMinMax(price);

        //right now we don't have a reason to sell until we check
        trade.setReasonSell(null);

        //check our strategy for a sell signal, and check the child as well
        if (strategy.hasSellSignal(agent, history, price))
            trade.setReasonSell(ReasonSell.Reason_Strategy);

        //if we are above the sma and the price is less, let's wait for it to turn around
        //if (aboveSMA && price < trade.getPriceBuy())
        //    trade.setReasonSell(null);

        //if $ declines we sell, else we update the $ history
        if (hasDecline(trade.getPriceHistory())) {
            trade.setReasonSell(ReasonSell.Reason_PriceDecline);
        } else {
            trade.updatePriceHistory(price);
        }

        //if the close $ has increased so much, let's just take the profit
        if (close >= trade.getHardSellPrice())
            trade.setReasonSell(ReasonSell.Reason_Increase);

        /**
         * If the latest historical $ are all below the hard stop $ then we need to sell
         * Reason for checking multiple is to filter out false signals when price dips quickly for 1 second
         */
        if (agent.getTrade().hasConfirmedHardStop()) {

            //we have a reason to sell
            trade.setReasonSell(ReasonSell.Reason_HardStop);

        } else {

            /**
             * Check multiple prices in our history to confirm price increase
             * Reason for checking multiple is to filter our false price increases
             */
            if (trade.hasConfirmedIncrease(price)) {

                double increase;

                //the $ increase will vary
                if (aboveSMA) {
                    increase = trade.getPriceBuy() * HARD_STOP_RATIO_ABOVE_SMA;
                } else {
                    increase = trade.getPriceBuy() * HARD_STOP_RATIO_BELOW_SMA;
                }

                //adjust our hard stop $ in case we have a better price
                trade.goShort(agent, price - increase);
            }
        }

        //display our data
        strategy.displayData(agent, trade.getReasonSell() != null);

        //display recent stock prices
        displayMessagePriceDecline(agent);

        //if there is a reason to sell then we will sell
        if (trade.getReasonSell() != null) {

            //if there is a reason, display message
            displayMessage(agent, trade.getReasonSell().getDescription(), true);

            //reset our attempt counter for our sell order
            trade.setAttempts(0);

            //create and assign our limit order at the current $
            agent.setOrder(createLimitOrder(agent, Action.Sell, product, price));

            //we want to wait until the next candle period before we check to buy stock again after this sells
            strategy.setWait(true);

        } else {

            //display our waiting message
            displayMessageOrderPending(agent, price);
        }
    }
}