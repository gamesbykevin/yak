package com.gamesbykevin.tradingbot.agent;

import com.gamesbykevin.tradingbot.rsi.Calculator;
import com.gamesbykevin.tradingbot.rsi.Calculator.Duration;
import com.gamesbykevin.tradingbot.wallet.Wallet;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import static com.gamesbykevin.tradingbot.util.Email.getDateDesc;

public class Agent implements Runnable {

    //our reference to calculate rsi
    private final Calculator calculator;

    //list of wallet for each product we are investing
    private final Wallet wallet;

    //object used to write to a text file
    public static PrintWriter WRITER;

    //history of stock prices
    private final List<Double> history;

    //the product we are trading
    private final String productId;

    //we want to run a thread
    private Thread thread;

    //how long do we sleep the thread for
    public static final long DELAY = 1000;

    //when is the last time we loaded historical data
    private long previous;

    public Agent(final String productId, final double funds) throws Exception {

        //store the product this agent is trading
        this.productId = productId;

        //create our object to write to a text file
        WRITER = new PrintWriter(productId + "-" + getDateDesc() + ".log", "UTF-8");

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

        //create new thread and start it
        this.thread = new Thread(this);
        this.thread.start();
    }

    @Override
    public void run() {

        while (true) {

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
                    calculator.update(Duration.OneMinute);
                    this.previous = System.currentTimeMillis();
                }

                //calculate rsi
                calculator.calculateRsi(getCurrentPrice());

                //now let's check our wallet
                wallet.update(calculator.getRsi(), productId, getCurrentPrice());

                //sleep the thread
                Thread.sleep(DELAY);

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public double getCurrentPrice() {
        return this.history.get(this.history.size() - 1);
    }

    public void addHistory(final double price) {
        this.history.add(price);
    }

    public double getAssets() {
        return wallet.getTotalAssets();
    }

    public static void displayMessage(String message, boolean write) {

        //print to console
        System.out.println(message);

        if (write) {
            WRITER.println(getDateDesc() + ":  " + message);
            WRITER.flush();
        }
    }
}