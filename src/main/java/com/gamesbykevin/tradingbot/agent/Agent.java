package com.gamesbykevin.tradingbot.agent;

import com.coinbase.exchange.api.entity.Product;
import com.coinbase.exchange.api.orders.Order;
import com.gamesbykevin.tradingbot.Main;
import com.gamesbykevin.tradingbot.calculator.Calculator.Duration;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.strategy.Strategy;
import com.gamesbykevin.tradingbot.transaction.Transaction;
import com.gamesbykevin.tradingbot.transaction.Transaction.Result;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;
import com.gamesbykevin.tradingbot.util.LogFile;
import com.gamesbykevin.tradingbot.wallet.Wallet;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import static com.gamesbykevin.tradingbot.Main.PAPER_TRADING_FEES;
import static com.gamesbykevin.tradingbot.agent.AgentManager.TradingStrategy;
import static com.gamesbykevin.tradingbot.agent.AgentHelper.*;
import static com.gamesbykevin.tradingbot.agent.AgentManagerHelper.displayMessage;
import static com.gamesbykevin.tradingbot.util.Email.getFileDateDesc;

public class Agent implements IAgent {

    //list of wallet for each product we are investing
    private Wallet wallet;

    //object used to write to a text file
    private PrintWriter writer;

    //do we have an order?
    private Order order = null;

    //list of transactions
    private List<Transaction> transactions;

    //do we stop trading
    private boolean stopTrading = false;

    //the reason why we are selling
    private ReasonSell reason;

    //what is our assigned trading strategy
    private TradingStrategy tradingStrategy = null;

    //the product we are trading
    private final String productId;

    //what is our hard stop amount
    private double hardStopPrice = 0;

    //what is our hard stop ratio when we sell our stock
    private float hardStopRatio = 0;

    //let's keep track of how low and high our money goes
    private double fundsMin, fundsMax;

    //do we buy stock?
    private boolean buy = false;

    //keep track of the reject/cancel, buy/sell orders
    private int countRejectedBuy = 0, countRejectedSell = 0;
    private int countCancelBuy = 0, countCancelSell = 0;

    //what is the  lowest price and highest price
    private double priceLow = 0, priceHigh = 0;

    //what duration is this agent trading against?
    private Duration duration;

    //the candle time of the order
    private long orderTime;

    //how many times have we checked to see if our sell order is filled
    private int attempts = 0;

    //track the price history of the current period
    private double[] priceHistory;

    protected Agent(double funds, String productId, TradingStrategy tradingStrategy, Duration duration) {

        //assign our duration
        setDuration(duration);

        //create new list of transactions
        this.transactions = new ArrayList<>();

        //store the product reference
        this.productId = productId;

        //set order null
        this.order = null;

        //create new array to track recent periods
        this.priceHistory = new double[CURRENT_PRICE_HISTORY + 1];

        //set our initial trading strategy
        setTradingStrategy(tradingStrategy);

        //we don't want to buy when reset
        setBuy(false);

        //we don't want to sell either
        setReasonSell(null);

        //we don't want to stop trading
        setStopTrading(false);

        //reset our hard stop
        setHardStopPrice(0);

        //clear our list
        this.transactions.clear();

        //create a wallet so we can track our investments
        this.wallet = new Wallet(funds);

        //assign our min/max when we start
        this.setFundsMin(funds);
        this.setFundsMax(funds);
    }

    @Override
    public synchronized void update(Strategy strategy, List<Period> history, Product product, double currentPrice) {

        //skip if we lost too much $
        if (hasStopTrading())
            return;

        //get our current assets
        double assets = getAssets(currentPrice);

        //track our min/max value
        if (assets < getFundsMin()) {

            //set the new minimum if lower than previous
            setFundsMin(assets);

        } else if (assets > getFundsMax()) {

            //set new maximum if greater than previous
            setFundsMax(assets);

        }

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
            checkPriceRange(currentPrice);

            //are we selling?
            boolean selling = getOrder().getSide().equalsIgnoreCase(Action.Sell.getDescription());

            //what is the status of our order?
            Status status = null;

            //what is the price of the order
            final double orderPrice = Double.parseDouble(order.getPrice());

            //construct message
            String message = "Waiting. Product " + product.getId();
            message += " Current $" + currentPrice;

            if (selling) {
                message += ", Purchase $" + round(getWallet().getPurchasePrice());
            } else {
                message += ", Purchase $" + round(orderPrice);
            }

            message += ", Hard Stop $" + round(getHardStopPrice());
            message += ", Quantity: " + getWallet().getQuantity();

            //we are waiting
            displayMessage(this, message, true);

            //if we are selling and the sell price is less than the purchase price we will chase the sell
            if (selling && orderPrice < getWallet().getPurchasePrice()) {

                //if we exceeded our attempts we will cancel the limit order
                if (getAttempts() >= SELL_ATTEMPT_LIMIT)
                    cancel = true;

                //keep track of our attempts
                setAttempts(getAttempts() + 1);
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

                    //update our wallet with the order info
                    fillOrder(getOrder(), product);

                    //now that the order has been filled, remove it
                    setOrder(null);

                    //reset the price history for our next trade
                    resetPriceHistory();
                    break;

                case Rejected:

                    //keep track of the rejected orders
                    if (selling) {
                        countRejectedSell++;
                    } else {
                        countRejectedBuy++;
                    }

                    //if the order has been rejected we will remove it
                    setOrder(null);
                    break;

                case Cancelled:

                    //keep track of the cancel orders
                    if (selling) {
                        countCancelSell++;
                    } else {
                        countCancelBuy++;
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

                //reset our order tracking for the next trade
                setCountCancelBuy(0);
                setCountCancelSell(0);
                setCountRejectedBuy(0);
                setCountRejectedSell(0);

                //set to null so next trade will create a new log file
                this.writer = null;
            }
        }
    }

    private void fillOrder(final Order order, final Product product) {

        if (order.getSide().equalsIgnoreCase(AgentHelper.Action.Buy.getDescription())) {

            //create a new transaction to track
            Transaction transaction = new Transaction();

            //update our transaction
            transaction.update(this, product, order);

            //add to our list
            this.transactions.add(transaction);

        } else if (order.getSide().equalsIgnoreCase(AgentHelper.Action.Sell.getDescription())) {

            //get the most recent transaction so we can complete it
            Transaction transaction = transactions.get(transactions.size() - 1);

            //update our transaction
            transaction.update(this, product, order);

            //display wins and losses
            displayMessage(this, TransactionHelper.getDescWins(this), true);
            displayMessage(this, TransactionHelper.getDescLost(this), true);

            //display the total fees paid
            displayMessage(this, "Fees $" + TransactionHelper.getTotalFees(this), true);

            //display average transaction time
            displayMessage(this, TransactionHelper.getAverageDurationDesc(this), true);

            //display the total $ amount invested in stocks
            displayMessage(this, AgentHelper.getStockInvestmentDesc(this), true);

            //display the count and reasons why we sold our stock
            TransactionHelper.displaySellReasonCount(this, Result.Win);
            TransactionHelper.displaySellReasonCount(this, Result.Lose);

        }
    }

    public void adjustHardStopPrice(double newPrice) {

        //what is the increase we check to see if we set a new hard stop amount
        double increase = (getWallet().getPurchasePrice() * getHardStopRatio());

        //if the price has increased some more, let's set a new hard stop
        if (newPrice > getHardStopPrice() + increase && newPrice > getWallet().getPurchasePrice() + increase) {

            //set our new hard stop limit slightly below the current stock price
            setHardStopPrice(newPrice - increase);

            //write hard stop amount to our log file
            displayMessage(this, "New hard stop $" + getHardStopPrice(), true);
        }
    }

    protected double getAssets(double currentPrice) {
        return (getWallet().getQuantity() * currentPrice) + getWallet().getFunds();
    }

    public void checkPriceRange(double currentPrice) {

        //what is the lowest and highest price during this trade
        if (currentPrice < getPriceLow())
            setPriceLow(currentPrice);
        if (currentPrice > getPriceHigh())
            setPriceHigh(currentPrice);
    }

    public PrintWriter getWriter() {

        if (this.writer == null)
            this.writer = LogFile.getPrintWriter(getTradingStrategy() + "-" + getDuration().description + "-" + getHardStopRatio() + "-" + getFileDateDesc() + ".log", getDirectory());

        return this.writer;
    }

    public String getProductId() {
        return this.productId;
    }

    public float getHardStopRatio() {
        return this.hardStopRatio;
    }

    public void setHardStopRatio(float hardStopRatio) {
        this.hardStopRatio = hardStopRatio;
    }

    public int getCountRejectedBuy() {
        return this.countRejectedBuy;
    }

    public void setCountRejectedBuy(int countRejectedBuy) {
        this.countRejectedBuy = countRejectedBuy;
    }

    public int getCountRejectedSell() {
        return this.countRejectedSell;
    }

    public void setCountRejectedSell(int countRejectedSell) {
        this.countRejectedSell = countRejectedSell;
    }

    public int getCountCancelBuy() {
        return this.countCancelBuy;
    }

    public void setCountCancelBuy(int countCancelBuy) {
        this.countCancelBuy = countCancelBuy;
    }

    public int getCountCancelSell() {
        return this.countCancelSell;
    }

    public void setCountCancelSell(int countCancelSell) {
        this.countCancelSell = countCancelSell;
    }

    public String getDirectory() {
        return LogFile.getLogDirectory() + "\\" + getProductId() + "\\" + "trades" + "\\";
    }

    public double getPriceLow() {
        return this.priceLow;
    }

    public void setPriceLow(double priceLow) {
        this.priceLow = priceLow;
    }

    public double getPriceHigh() {
        return this.priceHigh;
    }

    public void setPriceHigh(double priceHigh) {
        this.priceHigh = priceHigh;
    }

    public void setDuration(final Duration duration) {
        this.duration = duration;
    }

    public Duration getDuration() {
        return this.duration;
    }

    public int getAttempts() {
        return this.attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
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

    public List<Transaction> getTransactions() {
        return this.transactions;
    }

    public double getHardStopPrice() {
        return this.hardStopPrice;
    }

    public void setHardStopPrice(double hardStopPrice) {
        this.hardStopPrice = hardStopPrice;
    }

    public double getFundsMax() {
        return this.fundsMax;
    }

    public double getFundsMin() {
        return this.fundsMin;
    }

    public void setFundsMax(double fundsMax) {
        this.fundsMax = fundsMax;
    }

    public void setFundsMin(double fundsMin) {
        this.fundsMin = fundsMin;
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

    public void setTradingStrategy(TradingStrategy tradingStrategy) {
        this.tradingStrategy = tradingStrategy;
    }

    public TradingStrategy getTradingStrategy() {
        return this.tradingStrategy;
    }

    public double[] getPriceHistory() {
        return this.priceHistory;
    }

    public void resetPriceHistory() {
        for (int i = 0; i < getPriceHistory().length; i++) {
            getPriceHistory()[i] = 0;
        }
    }

    public void addPriceHistory(double price) {

        //we don't need to add if it matches the latest price
        if (price == getPriceHistory()[getPriceHistory().length - 1])
            return;

        //update every period in our array
        for (int i = 0; i < getPriceHistory().length - 1; i++) {

            //move the value back
            getPriceHistory()[i] = getPriceHistory()[i + 1];
        }

        //add the new price to the end
        getPriceHistory()[getPriceHistory().length - 1] = price;
    }

    private void checkSell(Strategy strategy, List<Period> history, Product product, double currentPrice) {

        //keep track of the lowest / highest price during a single trade
        checkPriceRange(currentPrice);

        //start without a reason to sell
        setReasonSell(null);

        //check for a sell signal
        strategy.checkSellSignal(this, history, currentPrice);

        //get the latest closing price
        final double closePrice = history.get(history.size() - 1).close;

        //if current price has declined x number of times, we will sell
        if (hasDecline(getPriceHistory())) {

            String message = "Price Decline: ";

            //construct our message
            for (int i = 0; i < getPriceHistory().length; i++) {
                message += "$" + getPriceHistory()[i];

                if (i < getPriceHistory().length - 1)
                    message += ", ";
            }

            //display the recent prices so we can see the decline
            displayMessage(this, message, true);

            //assign our reason for the sell
            setReasonSell(ReasonSell.Reason_PriceDecline);

        } else {

            //add the current price history to the list
            addPriceHistory(currentPrice);
        }

        //if the price dropped below our hard stop, we must sell to cut our losses
        if (closePrice <= getHardStopPrice()) {

            //reason for selling is that we hit our hard stop
            setReasonSell(ReasonSell.Reason_HardStop);

        } else {

            //since the close price is above the hard stop price, let's see if we can adjust
            adjustHardStopPrice(closePrice);
        }

        //display our data
        strategy.displayData(this, getReasonSell() != null);

        //if there is a reason to sell then we will sell
        if (getReasonSell() != null) {

            //if there is a reason, display message
            displayMessage(this, getReasonSell().getDescription(), true);

            //reset our attempt counter for our sell order
            setAttempts(0);

            //create and assign our limit order at the last period closing price
            setOrder(createLimitOrder(this, Action.Sell, product, currentPrice));

            //we want to wait until the next candle period before we check to buy stock again after this sells
            strategy.setWait(true);

        } else {

            //construct message
            String message = "Waiting. Product " + product.getId();
            message += " Current $" + currentPrice;
            message += ", Purchase $" + getWallet().getPurchasePrice();
            message += ", Hard Stop $" + round(getHardStopPrice());
            message += ", Quantity: " + getWallet().getQuantity();

            //we are waiting
            displayMessage(this, message, true);
        }
    }

    private void checkBuy(Strategy strategy, List<Period> history, Product product, double currentPrice) {

        //flag buy false before we check
        setBuy(false);

        //we don't have a reason to sell just yet
        setReasonSell(null);

        //reset our hard stop until we actually buy
        setHardStopPrice(0);

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
            setPriceLow(currentPrice);
            setPriceHigh(currentPrice);

            //let's set our hard stop if it hasn't been set already
            if (getHardStopPrice() == 0)
                setHardStopPrice(currentPrice - (currentPrice * getHardStopRatio()));

            //write hard stop amount to our log file
            displayMessage(this, "Current Price $" + currentPrice + ", Hard stop $" + getHardStopPrice(), true);

            //create and assign our limit order
            setOrder(createLimitOrder(this, Action.Buy, product, currentPrice));

        } else {

            //we are still waiting
            displayMessage(this, "Waiting. Available funds $" + getWallet().getFunds(), false);
        }
    }
}