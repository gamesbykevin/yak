package com.gamesbykevin.tradingbot.wallet;

import static com.gamesbykevin.tradingbot.agent.Agent.displayMessage;
import static com.gamesbykevin.tradingbot.util.Email.sendEmail;

import com.gamesbykevin.tradingbot.product.Product;
import com.gamesbykevin.tradingbot.rsi.Calculator;

public class Wallet {

    //money we have to invest
    private double funds = 0;

    //quantity of stock we bought
    private double quantity = 0;

    //the price we bought the stock
    private double purchasePrice = 0;

    /**
     * The support line meaning the stock is oversold
     */
    public static float SUPPORT_LINE = 30.0f;

    /**
     * The resistance line meaning the stock is overbought
     */
    public static float RESISTANCE_LINE = 60.0f;

    /**
     * The starting ratio point to sell if the stock drops too much to stop the bleeding
     */
    public static float SELL_RATIO = .15f;

    /**
     * If we lose an overall % of our funds let's stop the bleeding
     */
    public static float STOP_TRADING_RATIO = .75f;

    //how many funds did we start with
    private final double startingFunds;

    //should we stop trading
    private boolean stopTrading = false;

    public Wallet(double funds) {
        this.funds = funds;
        this.startingFunds = funds;
    }

    public void setStopTrading(boolean stopTrading) {
        this.stopTrading = stopTrading;
    }

    public boolean hasStopTrading() {
        return this.stopTrading;
    }

    public void setFunds(double funds) {
        this.funds = funds;
    }

    public double getFunds() {
        return this.funds;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public double getQuantity() {
        return this.quantity;
    }

    /**
     * Get our total assets for this wallet
     * @return Include our available funds plus the stock we bought at our purchase price
     */
    public double getTotalAssets() {
        return getFunds() + (purchasePrice * getQuantity());
    }

    public void update(final float rsi, final String productId, final double currentPrice) {

        //if we have quantity check current stock price
        if (quantity > 0) {

            final double priceHigh = purchasePrice;
            final double priceLow = purchasePrice - (purchasePrice * SELL_RATIO);

            boolean sell = false;

            if (currentPrice > priceHigh && rsi >= RESISTANCE_LINE) {

                //it grew enough, sell it
                sell = true;

            } else if (currentPrice <= priceLow) {

                //it dropped enough, sell it
                sell = true;

                //we also need to stop trading
                setStopTrading(true);

            } else {

                displayMessage("Waiting. Product " + productId + " Current $" + currentPrice + ", Purchase $" + purchasePrice + ", Quantity: " + getQuantity(), true);
            }

            //if we are selling our stock
            if (sell) {

                //add the money back to our total funds
                setFunds(getFunds() + (currentPrice * getQuantity()));

                final double priceBought = (this.purchasePrice * getQuantity());
                final double priceSold = (currentPrice * getQuantity());

                String subject, text;

                //display money changed
                if (priceBought > priceSold) {
                    subject = "We lost $" + (priceBought - priceSold);
                } else {
                    subject = "We made $" + (priceSold - priceBought);
                }

                //display the transaction
                text = "Sell " + productId + " quantity: " + getQuantity() + " @ $" + currentPrice + " remaining funds $" + getFunds();

                //display message(s)
                displayMessage(subject, true);
                displayMessage(text, true);

                //send message
                sendEmail(subject, text);

                //reset quantity back to 0
                setQuantity(0);
            }

            //if we lost too much money we will stop trading
            if (getFunds() < (STOP_TRADING_RATIO * startingFunds))
                setStopTrading(true);

            if (hasStopTrading()) {
                String subject = "We stopped trading " + productId;
                String text = "Funds dropped below our comfort level ($" + getFunds() + "). Stopped Trading for " + productId;
                displayMessage(text,true);

                //send message
                sendEmail(subject, text);
            }

        } else {

            boolean buy = false;

            //buy if the stock is oversold
            if (rsi < SUPPORT_LINE)
                buy = true;

            //are we buying stock
            if (buy) {

                //how much can we buy?
                final double availableQuantity = getFunds() / currentPrice;

                //store the purchase price
                this.purchasePrice = currentPrice;

                //store the quantity we bought
                setQuantity(availableQuantity);

                //our funds are now gone
                setFunds(0);

                //display the transaction
                displayMessage("Buy " + productId + " quantity: " + getQuantity() + " @ $" + this.purchasePrice, true);

            } else {
                displayMessage("Waiting. Product " + productId + ", Available funds $" + getFunds(), true);
            }
        }
    }
}