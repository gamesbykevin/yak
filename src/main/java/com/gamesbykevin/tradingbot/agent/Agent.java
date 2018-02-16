package com.gamesbykevin.tradingbot.agent;

import com.coinbase.exchange.api.entity.Product;
import com.coinbase.exchange.api.orders.Order;
import com.gamesbykevin.tradingbot.Main;
import com.gamesbykevin.tradingbot.rsi.Calculator;
import com.gamesbykevin.tradingbot.rsi.Calculator.Duration;
import com.gamesbykevin.tradingbot.util.Email;
import com.gamesbykevin.tradingbot.util.LogFile;
import com.gamesbykevin.tradingbot.util.PropertyUtil;
import com.gamesbykevin.tradingbot.wallet.Wallet;

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import static com.gamesbykevin.tradingbot.agent.AgentHelper.*;
import static com.gamesbykevin.tradingbot.util.Email.getFileDateDesc;
import static com.gamesbykevin.tradingbot.util.Email.sendEmail;
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

    //do we stop trading
    private boolean stopTrading = false;

    //number of attempts we try to verify the order
    private int attempts = 0;

    //what is the current rsi value
    private float rsiCurrent;

    //what time did we purchase?
    private long purchaseTime;

    public Agent(final Product product, final double funds) {

        //store the product this agent is trading
        this.product = product;

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
            displayMessage("Product (" + getProductId() + ") RSI = " + getRsiCurrent() + ", Stock Price $" + currentPrice, true);

            //if we don't have a pending order
            if (getOrder() == null) {

                if (getWallet().getQuantity() > 0) {

                    //we have quantity let's see if we can sell it
                    checkSell(this);

                } else {

                    //we don't have any quantity so let's see if we can buy
                    checkBuy(this);

                }

                //reset our attempts counter
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

                        //if we are successful purchasing, store the purchase time
                        if (getOrder().getSide().equalsIgnoreCase(Action.Buy.getDescription())) {

                            //keep track of the transaction time
                            purchaseTime = System.currentTimeMillis();

                        } else {

                            //how long did it take?
                            final long duration = System.currentTimeMillis() - purchaseTime;

                            //set our time
                            Calendar calendar = Calendar.getInstance();
                            calendar.setTimeInMillis(duration);

                            //the format of our time
                            final String pattern = "mm:ss.SSS";

                            //how we are going to format our time
                            DateFormat formatter = new SimpleDateFormat(pattern);

                            //format into a time we can read
                            String desc = formatter.format(calendar.getTime());

                            //display the time it took to sell the stock
                            displayMessage("Duration of the order from buy to sell: " + desc + " (" + pattern + ")", true);
                        }

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
                    displayMessage("    Funds $" + getWallet().getFunds(), true);
                    displayMessage("Old limit $" + oldRatio, true);
                    displayMessage("New limit $" + newRatio, true);
                    displayMessage("If your funds fall below the new limit we will stop trading", true);
                }

                //notify if this agent has stopped trading
                if (hasStopTrading()) {
                    String subject = "We stopped trading: " + getProductId();
                    String text1 = "Funds $" + getWallet().getFunds();
                    String text2 = "Limit $" + (STOP_TRADING_RATIO * getWallet().getStartingFunds());
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

        //our notification message
        String subject = null, text = null;

        //get the purchase price from the order
        BigDecimal price = BigDecimal.valueOf(Double.parseDouble(order.getPrice()));
        price.setScale(AgentHelper.ROUND_DECIMALS_PRICE, RoundingMode.HALF_DOWN);

        //get the quantity from the order
        BigDecimal quantity = BigDecimal.valueOf(Double.parseDouble(order.getSize()));
        quantity.setScale(AgentHelper.ROUND_DECIMALS_QUANTITY, RoundingMode.HALF_DOWN);

        if (order.getSide().equalsIgnoreCase(AgentHelper.Action.Buy.getDescription())) {

            //if our buy order has been filled, update our wallet to have the current purchase price
            getWallet().setPurchasePrice(price.doubleValue());

            //update our available funds based on our purchase
            getWallet().setFunds(getWallet().getFunds() - (price.doubleValue() * quantity.doubleValue()));

            //add the quantity to our wallet
            getWallet().setQuantity(getWallet().getQuantity() + quantity.doubleValue());

            //setup our notification message
            subject = "Purchase " + getProductId();
            text = "Buy " + getProductId() + " quantity: " + quantity + " @ $" + getWallet().getPurchasePrice();

            //display the transaction
            displayMessage(text, true);

        } else if (order.getSide().equalsIgnoreCase(AgentHelper.Action.Sell.getDescription())) {

            //if our sell order has been filled, update our wallet with our new funds
            getWallet().setFunds(getWallet().getFunds() + (price.doubleValue() * quantity.doubleValue()));

            //update the quantity as well
            getWallet().setQuantity(getWallet().getQuantity() - quantity.doubleValue());

            //figure out the total price we bought the stock for
            final double bought = (getWallet().getPurchasePrice() * quantity.doubleValue());

            //figure out the total price we sold the stock for
            final double sold = (price.doubleValue() * quantity.doubleValue());

            //display money made / lost
            if (bought > sold) {
                subject = "We lost $" + (bought - sold);
            } else {
                subject = "We made $" + (sold - bought);
            }

            //the transaction description
            text = "Sell " + getProductId() + " quantity: " + quantity + " @ $" + price + ", purchase $" + getWallet().getPurchasePrice() + ", remaining funds $" + getWallet().getFunds();

            //display and write to log
            displayMessage(subject, true);
            displayMessage(text, true);

        } else {
            throw new RuntimeException("Side not handled here: " + order.getSide());
        }

        //are we going to notify every transaction
        if (NOTIFICATION_EVERY_TRANSACTION && subject != null && text != null)
            sendEmail(subject, text);
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

    protected void displayMessage(Exception e, boolean write) {
        PropertyUtil.displayMessage(e, write, writer);
    }

    protected void displayMessage(String message, boolean write) {
        PropertyUtil.displayMessage(message, write, writer);
    }

    protected Calculator getCalculator() {
        return this.calculator;
    }

    protected Wallet getWallet() {
        return this.wallet;
    }

    protected Product getProduct() {
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
}