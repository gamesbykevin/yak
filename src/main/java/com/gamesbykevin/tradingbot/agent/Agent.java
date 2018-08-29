package com.gamesbykevin.tradingbot.agent;

import com.coinbase.exchange.api.entity.Product;
import com.coinbase.exchange.api.orders.Order;
import com.gamesbykevin.tradingbot.Main;
import com.gamesbykevin.tradingbot.calculator.Calculator;
import com.gamesbykevin.tradingbot.calculator.Calculator.Candle;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.strategy.Strategy;
import com.gamesbykevin.tradingbot.order.BasicOrderHelper.Action;
import com.gamesbykevin.tradingbot.order.BasicOrderHelper.Status;
import com.gamesbykevin.tradingbot.trade.Trade;
import com.gamesbykevin.tradingbot.trade.TradeHelper;
import com.gamesbykevin.tradingbot.util.LogFile;
import com.gamesbykevin.tradingbot.wallet.Wallet;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import static com.gamesbykevin.tradingbot.Main.PAPER_TRADING_FEES;
import static com.gamesbykevin.tradingbot.agent.AgentHelper.*;
import static com.gamesbykevin.tradingbot.agent.AgentManagerHelper.displayMessage;
import static com.gamesbykevin.tradingbot.agent.AgentMessageHelper.*;
import static com.gamesbykevin.tradingbot.calculator.Calculation.getRecent;
import static com.gamesbykevin.tradingbot.calculator.Calculator.PERIODS_SMA;
import static com.gamesbykevin.tradingbot.order.LimitOrderHelper.cancelOrder;
import static com.gamesbykevin.tradingbot.order.LimitOrderHelper.updateLimitOrder;
import static com.gamesbykevin.tradingbot.trade.TradeHelper.displayTradeSummary;
import static com.gamesbykevin.tradingbot.util.Email.getFileDateDesc;

public class Agent {

    //list of wallet for each product we are investing
    private Wallet wallet;

    //object used to write to a text file
    private PrintWriter writer;

    //do we have an order?
    private Order order = null;

    //list of transactions
    private List<Trade> trades;

    //do we stop trading
    private boolean stop = false;

    //what is our assigned trading strategy
    private Strategy.Key strategyKey = null;

    //the product we are trading
    private final String productId;

    //the candle time of the order
    private long orderTime;

    //the candle duration we are trading
    private Candle candle;

    protected Agent(double funds, String productId, Strategy.Key strategyKey, Candle candle) {

        //create new list of transactions
        this.trades = new ArrayList<>();

        //store the product reference
        this.productId = productId;

        //create a wallet so we can track our investments
        this.wallet = new Wallet(funds);

        //set order null
        setOrder(null);

        //set our initial trading strategy
        setStrategyKey(strategyKey);

        //we don't want to stop trading just yet
        setStop(false);

        //the current candle we are trading on
        setCandle(candle);
    }

    public synchronized void update(Calculator calculator, Product product, double price, final boolean aboveSMA) {

        //skip if we aren't allowed to trade
        if (hasStop())
            return;

        //do we cancel the order?
        boolean cancel = false;

        //locate our historical list
        List<Period> history = calculator.getHistory();

        //locate the strategy
        Strategy strategy = calculator.getStrategy(getStrategyKey());

        if (getOrder() != null) {

            //if the latest period does not match the order period a new period has started, cancel our order
            if (history.get(history.size() - 1).time != getOrderTime())
                cancel = true;
        }

        //if we don't have an active order look at the market data
        if (getOrder() == null) {

            //if we have quantity make sure we have the minimum or else we won't be able to sell
            if (getWallet().getQuantity() > 0 && getWallet().getQuantity() >= product.getBase_min_size()) {

                //check if we in position to sell our stock
                checkSell(this, strategy, history, product, price, aboveSMA);

            } else {

                //we don't have any quantity so let's see if we can buy
                checkBuy(this, strategy, history, product, price, aboveSMA);

            }

            //if an order was created track the create time
            if (getOrder() != null)
                setOrderTime(history.get(history.size() - 1).time);

        } else {

            //keep track of the price range during a single trade
            getTrade().checkPriceMinMax(price);

            //are we selling?
            boolean selling = getOrder().getSide().equalsIgnoreCase(Action.Sell.getDescription());

            //what is the status of our order?
            Status status;

            //what is the price of the order
            final double orderPrice = Double.parseDouble(getOrder().getPrice());

            //display our pending order
            displayMessageOrderPending(this, price);

            if (selling) {

                //if we exceeded our attempts we will cancel the limit order
                if (getTrade().getAttempts() >= SELL_ATTEMPT_LIMIT)
                    cancel = true;

            } else {

                //if we exceeded our attempts we will cancel the limit order
                if (getTrade().getAttempts() >= BUY_ATTEMPT_LIMIT)
                    cancel = true;
            }

            //keep track of our attempts
            getTrade().setAttempts(getTrade().getAttempts() + 1);

            //paper trading will try to treat same as live trading with limit/market orders
            if (Main.PAPER_TRADING) {

                //if we are applying fees to paper trades we will treat them as a market order
                if (PAPER_TRADING_FEES) {

                    //automatically mark as filled
                    status = Status.Filled;

                } else {

                    //for now the status will be pending
                    status = Status.Pending;

                    //the limit orders work different if buying or selling
                    if (selling) {

                        //the limit order will fill when the price goes at or above the order price
                        if (price > orderPrice)
                            status = Status.Filled;

                    } else {

                        //the limit order will fill when the price goes at or below the order price
                        if (price < orderPrice)
                            status = Status.Filled;

                    }

                    //if the order hasn't been filled and we want to cancel
                    if (cancel && status != Status.Filled) {

                        //if we were unsuccessful in our attempts, cancel the order
                        status = Status.Cancelled;
                        displayMessage(this, "Cancelling order", true);

                    }
                }

            } else {

                //let's check if our order is complete
                status = updateLimitOrder(this, getOrder().getId());

                //if we have exceeded our waiting limit and the order has not settled we will cancel the order
                if (cancel && !getOrder().getSettled())
                    cancelOrder(this, getOrder().getId());

            }

            //so what do we do now
            switch (status) {

                case Filled:

                    //order has been filled
                    displayMessage(this, "Order filled, current $" + price, true);

                    //get the recent trade
                    Trade trade = getTrade();

                    //update the agent and trade status
                    trade.update(this);

                    //display the trade summary AFTER we update the trade
                    displayTradeSummary(this, trade);

                    //if we sold, display trade and totals
                    if (selling) {

                        //show all summary of trades
                        displayMessageAllTradesSummary(this);

                        //check the standing of our agent now that we have sold successfully
                        checkStanding(this);

                        //set to null so next trade will create a new log file
                        this.writer = null;
                    }

                    //now that the order has been filled, remove it
                    setOrder(null);
                    break;

                case Rejected:

                    //keep track of the rejected orders
                    if (selling) {
                        getTrade().addCountRejectedSell();
                    } else {
                        getTrade().addCountRejectedBuy();
                    }

                    //if the order has been rejected we will remove it
                    setOrder(null);
                    break;

                case Cancelled:

                    //keep track of the cancel orders
                    if (selling) {
                        getTrade().addCountCancelSell();
                    } else {
                        getTrade().addCountCancelBuy();
                    }

                    //if the order has been cancelled we will remove it
                    setOrder(null);
                    break;

                case Open:
                case Pending:
                case Done:

                    //do nothing
                    break;
            }
        }
    }

    public Candle getCandle() {
        return this.candle;
    }

    private void setCandle(Candle candle) {
        this.candle = candle;
    }

    protected double getAssets() {

        if (getTrades().isEmpty())
            return getWallet().getFunds();

        return (getWallet().getQuantity() * getTrade().getCurrentPrice()) + getWallet().getFunds();
    }

    public PrintWriter getWriter() {

        if (this.writer == null)
            this.writer = LogFile.getPrintWriter(getStrategyKey() + "-" + getFileDateDesc() + ".log", TradeHelper.getDirectory(getProductId()));

        return this.writer;
    }

    public String getProductId() {
        return this.productId;
    }

    public void setOrder(final Order order) {
        this.order = order;
    }

    public Order getOrder() {
        return this.order;
    }

    public Wallet getWallet() {
        return this.wallet;
    }

    public void setStop(boolean stop) {
        this.stop = stop;
    }

    public boolean hasStop() {
        return this.stop;
    }

    public List<Trade> getTrades() {
        return this.trades;
    }

    public Trade getTrade() {
        return getTrades().get(getTrades().size() - 1);
    }

    public long getOrderTime() {
        return this.orderTime;
    }

    public void setOrderTime(long orderTime) {
        this.orderTime = orderTime;
    }

    public void setStrategyKey(Strategy.Key strategyKey) {
        this.strategyKey = strategyKey;
    }

    public Strategy.Key getStrategyKey() {
        return this.strategyKey;
    }
}