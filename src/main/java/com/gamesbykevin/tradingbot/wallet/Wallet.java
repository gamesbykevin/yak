package com.gamesbykevin.tradingbot.wallet;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.rsi.Calculator;

import static com.gamesbykevin.tradingbot.util.Email.sendEmail;

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
    public static float SUPPORT_LINE;

    /**
     * The resistance line meaning the stock is overbought
     */
    public static float RESISTANCE_LINE;

    /**
     * The starting ratio point to sell if the stock drops too much to stop the bleeding
     */
    public static float SELL_LOSS_RATIO;

    /**
     * If the stock increases enough we will sell regardless of rsi value
     */
    public static float SELL_GAIN_RATIO;

    /**
     * If we lose an overall % of our funds let's stop the bleeding
     */
    public static float STOP_TRADING_RATIO;

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

    public void update(final Agent agent, final Calculator calculator, final String productId, final double currentPrice) {

        String subject = null, text = null;

        //if we have quantity check current stock price
        if (quantity > 0) {

            final double priceHigh = purchasePrice;
            final double priceGain = purchasePrice + (purchasePrice * SELL_GAIN_RATIO);
            final double priceLow = purchasePrice - (purchasePrice * SELL_LOSS_RATIO);

            //do we sell the stock
            boolean sell = false;

            if (currentPrice > priceHigh && calculator.getRsi() >= RESISTANCE_LINE) {

                //it grew enough, sell it
                sell = true;

            } else if (currentPrice >= priceGain) {

                //regardless of rsi, if we made enough money to sell it
                sell = true;

            } else if (currentPrice <= priceLow) {

                //it dropped enough, sell it
                sell = true;

                //we also need to stop trading
                //setStopTrading(true);

            } else {

                agent.displayMessage("Waiting. Product " + productId + " Current $" + currentPrice + ", Purchase $" + purchasePrice + ", Quantity: " + getQuantity(), true);
            }

            //if we are selling our stock
            if (sell) {

                //add the money back to our total funds
                setFunds(getFunds() + (currentPrice * getQuantity()));

                final double priceBought = (this.purchasePrice * getQuantity());
                final double priceSold = (currentPrice * getQuantity());

                //display money changed
                if (priceBought > priceSold) {
                    subject = "We lost $" + (priceBought - priceSold);
                } else {
                    subject = "We made $" + (priceSold - priceBought);
                }

                //display the transaction
                text = "Sell " + productId + " quantity: " + getQuantity() + " @ $" + currentPrice + " remaining funds $" + getFunds();

                //display message(s)
                agent.displayMessage(subject, true);
                agent.displayMessage(text, true);

                //reset quantity back to 0
                setQuantity(0);
            }

            //if we lost too much money and have no quantity we will stop trading
            if (getFunds() < (STOP_TRADING_RATIO * startingFunds) && quantity <= 0)
                setStopTrading(true);

            if (hasStopTrading()) {
                subject = "We stopped trading " + productId;
                text = "Funds dropped below our comfort level ($" + getFunds() + "). Stopped Trading for " + productId;
                agent.displayMessage(text,true);
            }

        } else {

            boolean buy = false;

            //if the stock is oversold we are on the right track
            if (calculator.getRsi() < SUPPORT_LINE) {

                switch (calculator.getTrend()) {

                    case Upward:

                        //if the rsi is low and we see a constant upward trend, we will buy
                        if (calculator.getBreaks() < 1) {
                            buy = true;
                            agent.displayMessage("There is a constant upward trend", true);
                        }
                        break;

                    //we like downward trends
                    case Downward:

                        //there is a downward trend but some breaks so we think it will go back upwards
                        if (calculator.getBreaks() > 3) {
                            buy = true;
                        } else if (calculator.getBreaks() < 1) {
                            agent.displayMessage("There is a constant downward trend, and we will wait a little longer", true);
                        } else {
                            agent.displayMessage("There is a downward trend, but not enough breaks", true);
                        }
                        break;
                }
            } else {

                //if there is a constant upward trend lets buy anyway regardless of rsi
                switch (calculator.getTrend()) {
                    case Upward:
                        if (calculator.getBreaks() < 1) {
                            buy = true;
                            agent.displayMessage("There is a constant upward trend", true);
                        }
                        break;
                }
            }

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
                agent.displayMessage("Buy " + productId + " quantity: " + getQuantity() + " @ $" + this.purchasePrice, true);

            } else {
                agent.displayMessage("Waiting. Product " + productId + ", Available funds $" + getFunds(), true);
            }
        }

        //send message
        if (subject != null && text != null)
            sendEmail(subject, text);

    }
}