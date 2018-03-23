package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonBuy;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;

import java.util.ArrayList;
import java.util.List;

import static com.gamesbykevin.tradingbot.calculator.strategy.EMA.calculateSMA;

public class BB extends Strategy {

    /**
     * Typical # periods is 20
     */
    private static final int PERIODS = 20;

    //our lists
    private List<Double> middle, upper, lower;

    public BB() {

        super(PERIODS);

        this.middle = new ArrayList<>();
        this.upper = new ArrayList<>();
        this.lower = new ArrayList<>();
    }

    public List<Double> getUpper() {
        return this.upper;
    }

    public List<Double> getMiddle() {
        return this.middle;
    }

    public List<Double> getLower() {
        return this.lower;
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //if the current price goes below our lower line, let's buy
        if (currentPrice < lower.get(lower.size() - 1))
            agent.setReasonBuy(ReasonBuy.Reason_13);

        //display our data
        displayData(agent, agent.getReasonBuy() != null);
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //if the current price goes above our upper line, let's sell
        if (currentPrice > upper.get(upper.size() - 1))
            agent.setReasonSell(ReasonSell.Reason_14);

        //display our data
        displayData(agent, agent.getReasonSell() != null);
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display the information
        display(agent, "Upper: ", getUpper(), PERIODS / 4, write);
        display(agent, "Middle: ", getMiddle(), PERIODS / 4, write);
        display(agent, "Lower: ", getLower(), PERIODS / 4, write);
    }

    @Override
    public void calculate(List<Period> history) {

        //clear our lists
        getUpper().clear();
        getMiddle().clear();
        getLower().clear();

        for (int i = 0; i < history.size(); i++) {

            //we need enough data to calculate
            if (i <= getPeriods())
                continue;

            //calculate simple moving average
            final double sma = calculateSMA(history, i, getPeriods());

            //add our middle values
            getMiddle().add(sma);

            //get the standard deviation
            double standardDeviation = calculateStandardDeviation(history, sma, i);

            //add our upper value
            getUpper().add(sma + (standardDeviation * 2.0d));

            //add our lower value
            getLower().add(sma - (standardDeviation * 2.0d));
        }
    }

    private double calculateStandardDeviation(List<Period> history, double sma, int index) {

        double sum = 0;

        for (int x = index - getPeriods(); x < index; x++) {

            //subtract the simple moving average from the price, then square it, now add it to our total sum
            sum += (Math.pow(history.get(x).close - sma, 2));
        }

        //calculate the new average
        double average = sum / (double)getPeriods();

        //return the square root of our average aka standard deviation
        return Math.sqrt(average);
    }
}