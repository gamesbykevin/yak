package com.gamesbykevin.tradingbot.agent;

import com.coinbase.exchange.api.entity.Product;
import com.coinbase.exchange.api.orders.Order;
import com.gamesbykevin.tradingbot.Main;
import com.gamesbykevin.tradingbot.rsi.Calculator;
import com.gamesbykevin.tradingbot.rsi.Calculator.Duration;
import com.gamesbykevin.tradingbot.transaction.Transaction;
import com.gamesbykevin.tradingbot.transaction.Transaction.Result;
import com.gamesbykevin.tradingbot.util.Email;
import com.gamesbykevin.tradingbot.util.LogFile;
import com.gamesbykevin.tradingbot.util.PropertyUtil;
import com.gamesbykevin.tradingbot.wallet.Wallet;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import static com.gamesbykevin.tradingbot.agent.AgentHelper.*;
import static com.gamesbykevin.tradingbot.transaction.Transaction.TIME_FORMAT_AVG;
import static com.gamesbykevin.tradingbot.util.Email.getFileDateDesc;
import static com.gamesbykevin.tradingbot.wallet.Wallet.STOP_TRADING_RATIO;

public class Agent {

    //our reference to calculate rsi
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

    //what is the current rsi value
    private float rsiCurrent;

    public Agent(final Product product, final double funds) {

        //store the product this agent is trading
        this.product = product;

        //create new list of transactions
        this.transactions = new ArrayList<>();

        //create our object to write to a text file
        this.writer = LogFile.getPrintWriter(product.getId() + "-" + getFileDateDesc() + ".log");

        //update historical data and sleep
        this.calculator = new Calculator(product.getId());
        this.calculator.update(Duration.OneMinute);
        this.previous = System.currentTimeMillis();

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

            //the wallet will keep track of the current price
            getWallet().setCurrentPrice(currentPrice);

            //we don't need to update every second
            if (System.currentTimeMillis() - previous >= Duration.OneMinute.duration * 1000) {

                //update our historical data and update the last update
                getCalculator().update(Duration.OneMinute);
                this.previous = System.currentTimeMillis();
            }

            //check if there is a trend
            getCalculator().calculateTrend();

            //calculate rsi
            this.rsiCurrent = getCalculator().getRsiCurrent(currentPrice);

            //what is the rsi
            displayMessage("Product (" + getProductId() + ") RSI = " + getRsiCurrent() + ", Stock Price $" + AgentHelper.formatValue(currentPrice), true);

            //if we don't have a pending order
            if (getOrder() == null) {

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

                    //send email
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

    public float getRsiCurrent() {
        return this.rsiCurrent;
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
            displayMessage(getDescWins(), true);
            displayMessage(getDescLost(), true);

            //display average transaction time
            displayMessage(getAverageDurationDesc(), true);

            //display the total $ amount invested in stocks
            displayMessage(getStockInvestmentDesc(), true);
        }
    }

    public String getDescLost() {
        return "Lost :" + getCount(Result.Lose) + ", $" + AgentHelper.formatValue(getAmount(Result.Lose));
    }

    public String getDescWins() {
        return "Wins :" + getCount(Result.Win) + ", $" + AgentHelper.formatValue(getAmount(Result.Win));
    }

    public String getStockInvestmentDesc() {
        return "Owned Stock: " + AgentHelper.formatValue(getWallet().getQuantity());
    }

    public String getAverageDurationDesc() {
        return "Avg time: " + Transaction.getDurationDesc(getAverageDuration(), TIME_FORMAT_AVG);
    }

    private long getAverageDuration() {

        //how many transactions
        int count = 0;

        //total duration
        long duration = 0;

        //if empty return 0
        if (transactions.isEmpty())
            return 0;

        //check every transaction
        for (Transaction transaction : transactions) {

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

    /**
     * Get the total count of our transactions
     * @param result Do we want to check for wins or losses?
     * @return The total count of the specified result
     */
    public int getCount(Result result) {

        int count = 0;

        //check every transaction
        for (Transaction transaction : transactions) {

            if (transaction.getResult() == null)
                continue;

            //if there is a match keep track
            if (transaction.getResult() == result)
                count++;
        }

        //return our result
        return count;
    }

    /**
     * Get the total $ amount for our transaction
     * @param result Do we want to check for wins or losses?
     * @return The total $ amount of the specified result
     */
    public double getAmount(Result result) {

        double amount = 0;

        //check every transaction
        for (Transaction transaction : transactions) {

            if (transaction.getResult() == null)
                continue;

            //if there is a match keep track
            if (transaction.getResult() == result)
                amount += transaction.getAmount();
        }

        //return our result
        return amount;
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
        return getWallet().getCurrentAssets();
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
}