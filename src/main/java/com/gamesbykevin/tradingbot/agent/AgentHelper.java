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
     * If the stock price increases let's set a bar so in case the price goes back down we can still sell and make some $
     */
    public static float HARD_STOP_RATIO;

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

    protected static void checkBuy(Agent agent, Strategy strategy, Strategy strategyChild, List<Period> history, List<Period> historyChild, Product product, double price) {

        //if we need to wait for the next candle period we won't continue
        if (strategy.hasWait()) {

            //we are still waiting
            displayMessage(agent, "Waiting for next candle. Available funds $" + agent.getWallet().getFunds(), false);
            return;
        }

        //check for a buy signal
        boolean buy = strategy.hasBuySignal(agent, history, price);

        //if we have a buy signal, check the child to confirm
        if (buy && !strategyChild.hasBuySignal(agent, historyChild, price))
            buy = false;

        //display our data
        strategy.displayData(agent, buy);
        strategyChild.displayData(agent, buy);

        //we will buy if there is a reason
        if (buy) {

            //create our trade object
            createTrade(agent);

            //get the current trade
            Trade trade = agent.getTrade();

            //what is the lowest and highest price during this trade?
            trade.setPriceMin(price);
            trade.setPriceMax(price);

            //let's set our hard stop $
            trade.setHardStopPrice(price - (price * HARD_STOP_RATIO));

            //write hard stop amount to our log file
            displayMessage(agent, "Current Price $" + price + ", Hard stop $" + trade.getHardStopPrice(), true);

            //create and assign our limit order
            agent.setOrder(createLimitOrder(agent, Action.Buy, product, price));

        } else {

            //we are still waiting
            displayMessage(agent, "Waiting. Available funds $" + agent.getWallet().getFunds(), false);
        }
    }

    protected static void checkSell(Agent agent, Strategy strategy, Strategy strategyChild, List<Period> history, List<Period> historyChild, Product product, double price) {

        //get the latest closing price
        final double close = history.get(history.size() - 1).close;

        //get the current trade
        Trade trade = agent.getTrade();

        //keep track of the lowest / highest price during a single trade
        trade.checkPriceMinMax(price);

        //right now we don't have a reason to sell until we check
        trade.setReasonSell(null);

        //check our strategy for a sell signal, and check the child as well
        if (strategy.hasSellSignal(agent, history, price) || strategyChild.hasSellSignal(agent, historyChild, price))
            trade.setReasonSell(ReasonSell.Reason_Strategy);

        //if $ declines we sell, else we update the $ history
        if (hasDecline(trade.getPriceHistory())) {
            trade.setReasonSell(ReasonSell.Reason_PriceDecline);
        } else {
            trade.updatePriceHistory(price);
        }

        //if $ dropped below our hard stop, we must sell, else we adjust our stop $
        if (close <= agent.getTrade().getHardStopPrice()) {
            trade.setReasonSell(ReasonSell.Reason_HardStop);
        } else {
            trade.adjustHardStopPrice(agent, close);
        }

        //display our data
        strategy.displayData(agent, trade.getReasonSell() != null);
        strategyChild.displayData(agent, trade.getReasonSell() != null);

        //display recent stock prices
        displayMessagePriceDecline(agent);

        //if there is a reason to sell then we will sell
        if (trade.getReasonSell() != null) {

            //since we are selling let's adjust our hard stop
            trade.adjustHardStopPrice(agent, price);

            //if there is a reason, display message
            displayMessage(agent, trade.getReasonSell().getDescription(), true);

            //reset our attempt counter for our sell order
            trade.setAttempts(0);

            //create and assign our limit order at the last period closing price
            agent.setOrder(createLimitOrder(agent, Action.Sell, product, price));

            //we want to wait until the next candle period before we check to buy stock again after this sells
            strategy.setWait(true);

        } else {

            //display our waiting message
            displayMessageOrderPending(agent, price);
        }
    }
}