package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.Period.Fields;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;

import java.util.ArrayList;
import java.util.List;

import static com.gamesbykevin.tradingbot.calculator.strategy.SMA.calculateSMA;

public class BB extends Strategy {

    /**
     * Typical # periods is 20
     */
    public static final int PERIODS_BB = 20;

    //our lists
    private List<Double> middle, upper, lower;

    public BB(int periods) {

        //call parent
        super(periods);

        //create our lists
        this.middle = new ArrayList<>();
        this.upper = new ArrayList<>();
        this.lower = new ArrayList<>();
    }

    public BB() {
        this(PERIODS_BB);
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
        if (currentPrice < getRecent(getLower()))
            agent.setBuy(true);

        //display our data
        displayData(agent, agent.hasBuy());
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //if the current price goes above our upper line, let's sell
        if (currentPrice > getRecent(getUpper()))
            agent.setReasonSell(ReasonSell.Reason_Strategy);

        //display our data
        displayData(agent, agent.getReasonSell() != null);
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display the information
        display(agent, "Upper: ", getUpper(), getPeriods() / 4, write);
        display(agent, "Middle: ", getMiddle(), getPeriods() / 4, write);
        display(agent, "Lower: ", getLower(), getPeriods() / 4, write);
    }

    @Override
    public void calculate(List<Period> history) {

        //calculate our sma values
        calculateSMA(history, getMiddle(), getPeriods(), Fields.Close);

        for (int index = 0; index < getMiddle().size(); index++) {

            //get the sma value
            double sma = getMiddle().get(index);

            //get the standard deviation
            double standardDeviation = calculateStandardDeviation(history, sma, index + getPeriods());

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