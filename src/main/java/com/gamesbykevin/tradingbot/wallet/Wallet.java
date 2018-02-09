package com.gamesbykevin.tradingbot.wallet;

public class Wallet {

    //money we have to invest
    private double funds = 0;

    //quantity of stock we bought
    private double quantity = 0;

    //the price we bought the stock
    private double purchasePrice = 0;

    //current price of stock
    private double currentPrice = 0;

    /**
     * If we lose an overall % of our funds let's stop the bleeding
     */
    public static float STOP_TRADING_RATIO;

    //how many funds did we start with
    private double startingFunds;

    public Wallet(double funds) {
        setFunds(funds);
        setStartingFunds(funds);
    }

    public void setStartingFunds(final double startingFunds) {
        this.startingFunds = startingFunds;
    }

    public double getStartingFunds() {
        return this.startingFunds;
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

    public void setCurrentPrice(final double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public double getCurrentPrice() {
        return this.currentPrice;
    }

    public void setPurchasePrice(final double purchasePrice) {
        this.purchasePrice = purchasePrice;
    }

    public double getPurchasePrice() {
        return this.purchasePrice;
    }

    public double getCurrentAssets() {
        return (getQuantity() * getCurrentPrice()) + getFunds();
    }
}