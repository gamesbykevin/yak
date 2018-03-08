package com.gamesbykevin.tradingbot.agent;

import com.coinbase.exchange.api.entity.Product;
import com.coinbase.exchange.api.orders.Order;
import com.gamesbykevin.tradingbot.Main;
import com.gamesbykevin.tradingbot.calculator.Calculator;
import com.gamesbykevin.tradingbot.calculator.Calculator.Duration;
import com.gamesbykevin.tradingbot.transaction.Transaction;
import com.gamesbykevin.tradingbot.transaction.Transaction.Result;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonBuy;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;
import com.gamesbykevin.tradingbot.util.Email;
import com.gamesbykevin.tradingbot.util.LogFile;
import com.gamesbykevin.tradingbot.util.PropertyUtil;
import com.gamesbykevin.tradingbot.wallet.Wallet;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import static com.gamesbykevin.tradingbot.agent.AgentHelper.*;
import static com.gamesbykevin.tradingbot.calculator.Calculator.PERIODS_EMA_LONG;
import static com.gamesbykevin.tradingbot.calculator.Calculator.PERIODS_EMA_SHORT;
import static com.gamesbykevin.tradingbot.util.Email.getFileDateDesc;
import static com.gamesbykevin.tradingbot.wallet.Wallet.STOP_TRADING_RATIO;

public class Agent {

    //our reference to calculate calculator
    private final Calculator calculator;

    //list of wallet for each product we are investing
    private final Wallet wallet;

    //object used to write to a text file
    private final PrintWriter writer;

    //the product we are trading
    private final Product product;

    //when is the last time we loaded historical data
    private long previous;

    //are we updating the agent?
    private boolean working = false;

    //do we have an order?
    private Order order = null;

    //list of transactions
    private List<Transaction> transactions;

    //do we stop trading
    private boolean stopTrading = false;

    //number of attempts we try to verify the order
    private int attempts = 0;

    //what is the current calculator value
    private float rsiCurrent;

    //the reason why we are buying
    private ReasonBuy reasonBuy;

    //the reason why we are selling
    private ReasonSell reasonSell;

    //current price of stock
    private double currentPrice = 0;

    //the ema for our short period and long period
    private double emaShort = 0, emaLong = 0;

    private double emaShortPrevious = 0, emaLongPrevious = 0;

    //the duration of data we are checking
    private final Duration myDuration;

    public Agent(final Product product, final double funds, final Duration myDuration) {

        //store our duration
        this.myDuration = myDuration;

        //store the product this agent is trading
        this.product = product;

        //create new list of transactions
        this.transactions = new ArrayList<>();

        //create our object to write to a text file
        this.writer = LogFile.getPrintWriter(product.getId() + "-" + getFileDateDesc() + ".log");

        //create our calculator
        this.calculator = new Calculator(product.getId());

        //update the previous run time, so it runs immediately since we don't have data yet
        this.previous = System.currentTimeMillis() - (myDuration.duration * 1000);

        //create a wallet so we can track our investments
        this.wallet = new Wallet(funds);

        //display message and write to file
        displayMessage("Product: " + product.getId() + ", Starting $" + funds, true);
    }

    public synchronized void update(final double currentPrice) {

        //don't continue if we are currently working
        if (working)
            return;

        //flag that this agent is working
        working = true;

        try {

            //skip if we are no longer trading this coin
            if (hasStopTrading())
                return;

            //keep track of the current price
            setCurrentPrice(currentPrice);

            //we don't need to update every second
            if (System.currentTimeMillis() - previous >= (myDuration.duration / 6) * 1000) {

                //display message as sometimes the call is not successful
                displayMessage("Making rest call to retrieve history " + getProductId(), true);

                //update our historical data and update the last update
                boolean success = getCalculator().update(myDuration);

                if (success) {

                    //rest call is successful
                    displayMessage("Rest call successful.", true);

                } else {

                    //rest call isn't successful
                    displayMessage("Rest call is NOT successful.", true);
                }

                this.previous = System.currentTimeMillis();
            }

            //if we don't have an active order look at the market data for a chance to buy
            if (getOrder() == null) {

                //create tmp reference for previous values
                final double previousEmaShort = getEmaShortPrevious();
                final double previousEmaLong = getEmaLongPrevious();

                //the current values will now be the previous
                setEmaShortPrevious(getEmaShort());
                setEmaLongPrevious(getEmaLong());

                //calculate our new ema values based on the previous
                setEmaShort(getCalculator().calculateEMA(PERIODS_EMA_SHORT, getCurrentPrice(), previousEmaShort));
                setEmaLong(getCalculator().calculateEMA(PERIODS_EMA_LONG, getCurrentPrice(), previousEmaLong));

                displayMessage(getProductId() + " - " + PERIODS_EMA_SHORT + " period EMA: " + getEmaShort(), true);
                displayMessage(getProductId() + " - " + PERIODS_EMA_LONG + " period EMA: " + getEmaLong(), true);

                //check if there is a trend with the current stock price
                getCalculator().calculateTrend(currentPrice);

                //calculate the current rsi
                setRsiCurrent(getCalculator().getRsiCurrent(currentPrice));

                //what is the calculator
                displayMessage("Product (" + getProductId() + ") RSI = " + getRsiCurrent() + ", Stock Price $" + AgentHelper.formatValue(currentPrice), true);

                if (getWallet().getQuantity() > 0) {

                    //we have quantity let's see if we can sell it
                    checkSell(this);

                } else {

                    //we don't have any quantity so let's see if we can buy
                    checkBuy(this);
                }

                //reset our attempts counter, which is used when we create a limit order
                setAttempts(0);

            } else {

                //what is the status of our order
                AgentHelper.Status status = null;

                if (Main.PAPER_TRADING) {

                    //if we are paper trading assume the order has been completed
                    status = AgentHelper.Status.Filled;

                } else {

                    //let's check if our order is complete
                    status = updateLimitOrder(this, getOrder().getId());
                }

                //so what do we do now
                switch (status) {

                    case Filled:

                        //update our wallet with the order info
                        fillOrder(getOrder());

                        //now that the order has been filled, remove it
                        setOrder(null);
                        break;

                    case Rejected:
                    case Cancelled:

                        //if the order has been rejected or cancelled we will remove it
                        setOrder(null);
                        break;

                    case Open:
                    case Pending:
                    case Done:

                        //do nothing
                        break;
                }

                //if we lost too much money and have no quantity pending, we will stop trading
                if (getWallet().getFunds() < (STOP_TRADING_RATIO * getWallet().getStartingFunds()) && getWallet().getQuantity() <= 0)
                    setStopTrading(true);

                //if our money has gone up, increase the stop trading limit
                if (getWallet().getFunds() > getWallet().getStartingFunds()) {

                    final double oldRatio = (STOP_TRADING_RATIO * getWallet().getStartingFunds());
                    getWallet().setStartingFunds(getWallet().getFunds());
                    final double newRatio = (STOP_TRADING_RATIO * getWallet().getStartingFunds());
                    displayMessage("Good news, stop trading limit has increased", true);
                    displayMessage("    Funds $" + AgentHelper.formatValue(getWallet().getFunds()), true);
                    displayMessage("Old limit $" + AgentHelper.formatValue(oldRatio), true);
                    displayMessage("New limit $" + AgentHelper.formatValue(newRatio), true);
                    displayMessage("If your funds fall below the new limit we will stop trading", true);
                }

                //notify if this agent has stopped trading
                if (hasStopTrading()) {
                    String subject = "We stopped trading: " + getProductId();
                    String text1 = "Funds $" + AgentHelper.formatValue(getWallet().getFunds());
                    String text2 = "Limit $" + AgentHelper.formatValue(STOP_TRADING_RATIO * getWallet().getStartingFunds());
                    displayMessage(subject, true);
                    displayMessage(text1,true);
                    displayMessage(text2,true);

                    //send email notification
                    Email.sendEmail(subject, text1 + "\n" + text2);
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            displayMessage(ex, true);
        }

        //flag that we are no longer working
        working = false;
    }

    private void fillOrder(final Order order) {

        if (order.getSide().equalsIgnoreCase(AgentHelper.Action.Buy.getDescription())) {

            //create a new transaction to track
            Transaction transaction = new Transaction();

            //update our transaction
            transaction.update(this, order);

            //add to our list
            this.transactions.add(transaction);

        } else if (order.getSide().equalsIgnoreCase(AgentHelper.Action.Sell.getDescription())) {

            //get the most recent transaction so we can complete it
            Transaction transaction = transactions.get(transactions.size() - 1);

            //update our transaction
            transaction.update(this, order);

            //display wins and losses
            displayMessage(TransactionHelper.getDescWins(this), true);
            displayMessage(TransactionHelper.getDescLost(this), true);

            //display average transaction time
            displayMessage(TransactionHelper.getAverageDurationDesc(this), true);

            //display the total $ amount invested in stocks
            displayMessage(AgentHelper.getStockInvestmentDesc(this), true);

            //display the count for each buying reason for when we win
            TransactionHelper.displayBuyingReasonCount(this, Result.Win);

            //display the count for each selling reason for when we win
            TransactionHelper.displaySellingReasonCount(this, Result.Win);

            //display the count for each buying reason for when we lose
            TransactionHelper.displayBuyingReasonCount(this, Result.Lose);

            //display the count for each selling reason for when we lose
            TransactionHelper.displaySellingReasonCount(this, Result.Lose);
        }
    }

    public String getProductId() {
        return this.product.getId();
    }

    public void setOrder(final Order order) {
        this.order = order;
    }

    public Order getOrder() {
        return this.order;
    }

    /**
     * Get the total assets
     * @return The total funds available + the quantity of stock we currently own @ the current stock price
     */
    public double getAssets() {
        return (getWallet().getQuantity() * getCurrentPrice()) + getWallet().getFunds();
    }

    protected Calculator getCalculator() {
        return this.calculator;
    }

    public Wallet getWallet() {
        return this.wallet;
    }

    public Product getProduct() {
        return this.product;
    }

    public void setStopTrading(boolean stopTrading) {
        this.stopTrading = stopTrading;
    }

    public boolean hasStopTrading() {
        return this.stopTrading;
    }

    protected void setAttempts(final int attempts) {
        this.attempts = attempts;
    }

    protected int getAttempts() {
        return this.attempts;
    }

    public void displayMessage(Exception e, boolean write) {
        PropertyUtil.displayMessage(e, write, writer);
    }

    public void displayMessage(String message, boolean write) {
        PropertyUtil.displayMessage(message, write, writer);
    }

    public void setReasonBuy(final ReasonBuy reasonBuy) {
        this.reasonBuy = reasonBuy;
    }

    public void setReasonSell(final ReasonSell reasonSell) {
        this.reasonSell = reasonSell;
    }

    public ReasonBuy getReasonBuy() {
        return this.reasonBuy;
    }

    public ReasonSell getReasonSell() {
        return this.reasonSell;
    }

    public List<Transaction> getTransactions() {
        return this.transactions;
    }

    public void setCurrentPrice(final double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public double getCurrentPrice() {
        return this.currentPrice;
    }

    public double getEmaLong() {
        return this.emaLong;
    }

    public double getEmaShort() {
        return this.emaShort;
    }

    public void setEmaLong(double emaLong) {
        this.emaLong = emaLong;
    }

    public void setEmaShort(double emaShort) {
        this.emaShort = emaShort;
    }

    public double getEmaLongPrevious() {
        return this.emaLongPrevious;
    }

    public double getEmaShortPrevious() {
        return this.emaShortPrevious;
    }

    public void setEmaLongPrevious(double emaLongPrevious) {
        this.emaLongPrevious = emaLongPrevious;
    }

    public void setEmaShortPrevious(double emaShortPrevious) {
        this.emaShortPrevious = emaShortPrevious;
    }

    private void setRsiCurrent(final float rsiCurrent) {
        this.rsiCurrent = rsiCurrent;
    }

    public float getRsiCurrent() {
        return this.rsiCurrent;
    }
}