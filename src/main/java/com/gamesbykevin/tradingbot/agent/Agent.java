package com.gamesbykevin.tradingbot.agent;

import com.gamesbykevin.tradingbot.rsi.Calculator;
import com.gamesbykevin.tradingbot.rsi.Calculator.Duration;
import com.gamesbykevin.tradingbot.util.LogFile;
import com.gamesbykevin.tradingbot.util.PropertyUtil;
import com.gamesbykevin.tradingbot.wallet.Wallet;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import static com.gamesbykevin.tradingbot.util.Email.getDateDesc;

public class Agent {

    //our reference to calculate rsi
    private final Calculator calculator;

    //list of wallet for each product we are investing
    private final Wallet wallet;

    //object used to write to a text file
    private final PrintWriter writer;

    //history of stock prices
    private final List<Double> history;

    //the product we are trading
    private final String productId;

    //when is the last time we loaded historical data
    private long previous;

    //are we updating the agent?
    private boolean working = false;

    public Agent(final String productId, final double funds) {

        //store the product this agent is trading
        this.productId = productId;

        //create our object to write to a text file
        this.writer = LogFile.getPrintWriter(productId + "-" + getDateDesc() + ".log");

        //update historical data and sleep
        this.calculator = new Calculator(productId);
        this.calculator.update(Duration.OneMinute);
        this.previous = System.currentTimeMillis();

        //create a wallet so we can track our investments
        this.wallet = new Wallet(funds);

        //create new list of historical stock prices
        this.history = new ArrayList<>();

        //display message and write to file
        displayMessage("Product: " + productId + ", Starting $" + funds, true);
    }

    public synchronized void update() {

        //don't continue if we are working
        if (working)
            return;

        //flag that this agent is working
        working = true;

        //get the current price in case it changes while we are still processing
        final double currentPrice = getCurrentPrice();

        try {

            //skip if we are no longer trading this coin
            if (wallet.hasStopTrading())
                return;

            //we can't continue if we can't access the current price
            if (history.isEmpty())
                return;

            //we don't need to update every second
            if (System.currentTimeMillis() - previous >= Duration.OneMinute.duration * 1000) {

                //update our historical data and update the last update
                this.calculator.update(Duration.OneMinute);
                this.previous = System.currentTimeMillis();
            }

            //check if there is a trend
            calculator.calculateTrend(this);

            //calculate rsi
            calculator.calculateRsi(this, currentPrice);

            //now let's check our wallet
            wallet.update(this, calculator, productId, currentPrice);

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        //flag that we are no longer working
        working = false;
    }

    private double getCurrentPrice() {
        return this.history.get(this.history.size() - 1);
    }

    public void addHistory(final double price) {
        this.history.add(price);
    }

    public double getAssets() {
        return wallet.getTotalAssets();
    }

    public void displayMessage(String message, boolean write) {
        PropertyUtil.displayMessage(message, write, writer);
    }
}