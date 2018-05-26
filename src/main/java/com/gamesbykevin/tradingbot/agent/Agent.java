package com.gamesbykevin.tradingbot.agent;

import com.coinbase.exchange.api.entity.Product;
import com.coinbase.exchange.api.orders.Order;
import com.gamesbykevin.tradingbot.Main;
import com.gamesbykevin.tradingbot.calculator.Calculator.Candle;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.strategy.Strategy;
import com.gamesbykevin.tradingbot.trade.Trade;
import com.gamesbykevin.tradingbot.trade.TradeHelper;
import com.gamesbykevin.tradingbot.trade.TradeHelper.ReasonSell;
import com.gamesbykevin.tradingbot.util.LogFile;
import com.gamesbykevin.tradingbot.wallet.Wallet;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.gamesbykevin.tradingbot.Main.PAPER_TRADING_FEES;
import static com.gamesbykevin.tradingbot.agent.AgentManager.StrategyKey;
import static com.gamesbykevin.tradingbot.agent.AgentHelper.*;
import static com.gamesbykevin.tradingbot.agent.AgentManagerHelper.displayMessage;
import static com.gamesbykevin.tradingbot.agent.AgentMessageHelper.*;
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
    private boolean stopTrading = false;

    //the reason why we are selling
    private ReasonSell reason;

    //what is our assigned trading strategy
    private StrategyKey strategyKey = null;

    //the product we are trading
    private final String productId;

    //do we buy stock?
    private boolean buy = false;

    //the candle time of the order
    private long orderTime;

    protected Agent(double funds, String productId, StrategyKey strategyKey) {

        //create new list of transactions
        this.trades = new ArrayList<>();

        //store the product reference
        this.productId = productId;

        //set order null
        this.order = null;

        //set our initial trading strategy
        setStrategyKey(strategyKey);

        //we don't want to buy when reset
        setBuy(false);

        //we don't want to sell either
        setReasonSell(null);

        //we don't want to stop trading
        setStopTrading(false);

        //create a wallet so we can track our investments
        this.wallet = new Wallet(funds);
    }

    public synchronized void update(Strategy strategy, HashMap<Candle, List<Period>> history, Product product, double currentPrice) {

        //skip if we lost too much $
        if (hasStopTrading())
            return;

        //do we cancel the order
        boolean cancel = false;

        //if a new period occurs and we have an order, we will cancel
        if (history.get(history.size() - 1).time != getOrderTime() && getOrder() != null)
            cancel = true;

        //if we don't have an active order look at the market data
        if (getOrder() == null) {

            //if we have quantity make sure we have the minimum or else we won't be able to sell
            if (getWallet().getQuantity() > 0 && getWallet().getQuantity() >= product.getBase_min_size()) {

                //check if we in position to sell our stock
                checkSell(strategy, history, product, currentPrice);

            } else {

                //we don't have any quantity so let's see if we can buy
                checkBuy(strategy, history, product, currentPrice);

            }

            //if an order was created track the create time
            if (getOrder() != null)
                setOrderTime(history.get(history.size() - 1).time);

        } else {

            //keep track of the price range during a single trade
            getTrade().checkPriceMinMax(currentPrice);

            //are we selling?
            boolean selling = getOrder().getSide().equalsIgnoreCase(Action.Sell.getDescription());

            //what is the status of our order?
            Status status;

            //what is the price of the order
            final double orderPrice = Double.parseDouble(order.getPrice());

            //display our pending order
            displayMessageOrderPending(this, currentPrice);

            //if we are selling and the sell price is less than the purchase price we will chase the sell
            if (selling && orderPrice < getWallet().getPurchasePrice()) {

                //if we exceeded our attempts we will cancel the limit order
                if (getTrade().getAttempts() >= SELL_ATTEMPT_LIMIT)
                    cancel = true;

                //keep track of our attempts
                getTrade().setAttempts(getTrade().getAttempts() + 1);
            }

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
                        if (currentPrice > orderPrice)
                            status = Status.Filled;

                    } else {

                        //the limit order will fill when the price goes at or below the order price
                        if (currentPrice < orderPrice)
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

                    //get the recent trade
                    Trade trade = getTrades().get(getTrades().size() - 1);

                    //update the agent and trade status
                    trade.update(this);

                    //if we sold, display trade and totals
                    if (order.getSide().equalsIgnoreCase(AgentHelper.Action.Sell.getDescription())) {
                        displayTradeSummary(this, trade);
                        displayMessageAllTradesSummary(this);
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

            //if this is true we have finished a trade
            if (selling && status == Status.Filled) {

                //check the standing of our agent now that we have sold successfully
                checkStanding(this);

                //set to null so next trade will create a new log file
                this.writer = null;
            }
        }
    }

    protected double getAssets(double currentPrice) {
        return (getWallet().getQuantity() * currentPrice) + getWallet().getFunds();
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

    public void setStopTrading(boolean stopTrading) {
        this.stopTrading = stopTrading;
    }

    public boolean hasStopTrading() {
        return this.stopTrading;
    }

    public void setReasonSell(final ReasonSell reason) {
        this.reason = reason;
    }

    public ReasonSell getReasonSell() {
        return this.reason;
    }

    public List<Trade> getTrades() {
        return this.trades;
    }

    public Trade getTrade() {
        return getTrades().get(getTrades().size() - 1);
    }

    public void setBuy(boolean buy) {
        this.buy = buy;
    }

    public boolean hasBuy() {
        return this.buy;
    }

    public long getOrderTime() {
        return this.orderTime;
    }

    public void setOrderTime(long orderTime) {
        this.orderTime = orderTime;
    }

    public void setStrategyKey(StrategyKey strategyKey) {
        this.strategyKey = strategyKey;
    }

    public StrategyKey getStrategyKey() {
        return this.strategyKey;
    }

    private void checkSell(Strategy strategy, HashMap<Candle, List<Period>> history, Product product, double currentPrice) {

        //keep track of the lowest / highest price during a single trade
        getTrade().checkPriceMinMax(currentPrice);

        //start without a reason to sell
        setReasonSell(null);

        //check for a sell signal
        strategy.checkSellSignal(this, history, currentPrice);

        //get the latest closing price
        final double closePrice = history.get(history.size() - 1).close;

        //if current price has declined x number of times, we will sell
        if (hasDecline(getTrade().getPriceHistory())) {

            //display message
            displayMessagePriceDecline(this);

            //assign our reason for the sell
            setReasonSell(ReasonSell.Reason_PriceDecline);

        } else {

            //add the current price history to the list
            getTrade().addPriceHistory(currentPrice);
        }

        //if the price dropped below our hard stop, we must sell to cut our losses
        if (closePrice <= getTrade().getHardStopPrice()) {

            //reason for selling is that we hit our hard stop
            setReasonSell(ReasonSell.Reason_HardStop);

        } else {

            //since the close price is above the hard stop price, let's see if we can adjust
            getTrade().adjustHardStopPrice(this, closePrice);
        }

        //display our data
        strategy.displayData(this, getReasonSell() != null);

        //if there is a reason to sell then we will sell
        if (getReasonSell() != null) {

            //if there is a reason, display message
            displayMessage(this, getReasonSell().getDescription(), true);

            //reset our attempt counter for our sell order
            getTrade().setAttempts(0);

            //create and assign our limit order at the last period closing price
            setOrder(createLimitOrder(this, Action.Sell, product, currentPrice));

            //we want to wait until the next candle period before we check to buy stock again after this sells
            strategy.setWait(true);

        } else {

            //display our waiting message
            displayMessageCheckSellWaiting(this, currentPrice);
        }
    }

    private void checkBuy(Strategy strategy, HashMap<Candle, List<Period>> history, Product product, double currentPrice) {

        //flag buy false before we check
        setBuy(false);

        //we don't have a reason to sell just yet
        setReasonSell(null);

        //reset our hard stop until we actually buy
        getTrade().setHardStopPrice(0);

        //if the strategy does not need to wait for new candle data
        if (!strategy.hasWait()) {

            //check for a buy signal
            strategy.checkBuySignal(this, history, currentPrice);

            //display our data
            strategy.displayData(this, hasBuy());
        }

        //we will buy if there is a reason
        if (hasBuy()) {

            //what is the lowest and highest price during this trade?
            getTrade().setPriceMin(currentPrice);
            getTrade().setPriceMax(currentPrice);

            //let's set our hard stop if it hasn't been set already
            if (getTrade().getHardStopPrice() == 0)
                getTrade().setHardStopPrice(currentPrice - (currentPrice * HARD_STOP_RATIO));

            //write hard stop amount to our log file
            displayMessage(this, "Current Price $" + currentPrice + ", Hard stop $" + getTrade().getHardStopPrice(), true);

            //create and assign our limit order
            setOrder(createLimitOrder(this, Action.Buy, product, currentPrice));

        } else {

            //we are still waiting
            displayMessage(this, "Waiting. Available funds $" + getWallet().getFunds(), false);
        }
    }
}