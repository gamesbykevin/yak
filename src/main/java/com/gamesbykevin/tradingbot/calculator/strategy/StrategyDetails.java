package com.gamesbykevin.tradingbot.calculator.strategy;

public class StrategyDetails {

    //store our details
    private double funds = 0, fundsMin = 0, fundsMax = 0, fundsPrevious = 0;

    public StrategyDetails(double startFunds) {
        setFundsPrevious(startFunds);
    }

    public void setFundsMin(double fundsMin) {
        this.fundsMin = fundsMin;
    }

    public void setFundsMax(double fundsMax) {
        this.fundsMax = fundsMax;
    }

    public double getFundsMin() {
        return this.fundsMin;
    }

    public double getFundsMax() {
        return this.fundsMax;
    }

    public double getFunds() {
        return this.funds;
    }

    public void setFunds(double funds) {
        this.funds = funds;
    }

    public double getFundsPrevious() {
        return this.fundsPrevious;
    }

    public void setFundsPrevious(double fundsPrevious) {
        this.fundsPrevious = fundsPrevious;
    }
}
