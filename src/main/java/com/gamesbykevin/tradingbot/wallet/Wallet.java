package com.gamesbykevin.tradingbot.wallet;

public class Wallet {

    /**
     * If we lose an overall % of our funds let's stop the bleeding
     */
    public static float STOP_TRADING_RATIO;

    //money we have to invest
    private double funds = 0;

    //quantity of stock we bought
    private float quantity = 0;

    //how many funds did we start with before our next trade
    private double fundsBeforeTrade;

    //how many funds did we start with overall
    private final double initialFunds;

    public Wallet(double funds) {
        setFunds(funds);
        setFundsBeforeTrade(funds);
        this.initialFunds = funds;
    }

    public double getInitialFunds() {
        return this.initialFunds;
    }

    public double getFundsBeforeTrade() {
        return this.fundsBeforeTrade;
    }

    public void setFundsBeforeTrade(double fundsBeforeTrade) {
        this.fundsBeforeTrade = fundsBeforeTrade;
    }

    public void addFunds(double funds) {
        setFunds(getFunds() + funds);
    }

    public void subtractFunds(double funds) {
        setFunds(getFunds() - funds);
    }

    private void setFunds(double funds) {
        this.funds = funds;
    }

    public double getFunds() {
        return this.funds;
    }

    public void addQuantity(float quantity) {
        setQuantity(getQuantity() + quantity);
    }

    public void subtractQuantity(float quantity) {
        setQuantity(getQuantity() - quantity);
    }

    private void setQuantity(float quantity) {
        this.quantity = quantity;
    }

    public float getQuantity() {
        return this.quantity;
    }
}