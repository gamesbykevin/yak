package com.gamesbykevin.tradingbot.agent;

import com.coinbase.exchange.api.entity.Product;
import com.gamesbykevin.tradingbot.rsi.Calculator;
import com.gamesbykevin.tradingbot.rsi.Calculator.Duration;
import com.gamesbykevin.tradingbot.util.LogFile;
import com.gamesbykevin.tradingbot.util.PropertyUtil;
import com.gamesbykevin.tradingbot.wallet.Wallet;

import java.io.PrintWriter;

import static com.gamesbykevin.tradingbot.util.Email.getDateDesc;

public class Agent {

    //our reference to calculate rsi
    private final Calculator calculator;

    //list of wallet for each product we are investing
    private final Wallet wallet;

    //object used to write to a text file
    private final PrintWriter writer;

    //what is the current stock price
    private double currentStockPrice;

    //the product we are trading
    private final Product product;

    //when is the last time we loaded historical data
    private long previous;

    //are we updating the agent?
    private boolean working = false;

    public Agent(final Product product, final double funds) {

        //store the product this agent is trading
        this.product = product;

        //create our object to write to a text file
        this.writer = LogFile.getPrintWriter(product.getId() + "-" + getDateDesc() + ".log");

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
            if (wallet.hasStopTrading())
                return;

            //add the current price to our history
            setCurrentPrice(currentPrice);

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
            wallet.update(this, calculator, product, currentPrice);

        } catch (Exception ex) {
            ex.printStackTrace();
            displayMessage(ex, true);
        }

        //flag that we are no longer working
        working = false;
    }

    public String getProductId() {
        return this.product.getId();
    }

    private double getCurrentPrice() {
        return this.currentStockPrice;
    }

    private void setCurrentPrice(final double currentStockPrice) {
        this.currentStockPrice = currentStockPrice;
    }

    /**
     * Get the total assets
     * @return The total funds available + the value of stock we currently have @ the current stock price
     */
    public double getAssets() {
        return (wallet.getQuantity() * getCurrentPrice()) + wallet.getFunds();
    }

    public void displayMessage(Exception e, boolean write) {
        PropertyUtil.displayMessage(e, write, writer);
    }

    public void displayMessage(String message, boolean write) {
        PropertyUtil.displayMessage(message, write, writer);
    }
}