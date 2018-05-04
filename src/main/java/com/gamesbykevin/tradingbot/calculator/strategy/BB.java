package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.Period.Fields;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;

import java.util.ArrayList;
import java.util.List;

import static com.gamesbykevin.tradingbot.calculator.strategy.SMA.calculateSMA;

public class BB extends Strategy {

    //list of configurable values
    protected static int PERIODS = 10;

    private final int periods;

    private final float multiplier;

    //our lists
    private List<Double> middle, upper, lower, width;

    private static final float MULTIPLIER = 2.0f;

    public BB() {
        this(PERIODS, MULTIPLIER);
    }

    public BB(int periods, float multiplier) {

        this.periods = periods;
        this.multiplier = multiplier;

        //create our lists
        this.middle = new ArrayList<>();
        this.upper = new ArrayList<>();
        this.lower = new ArrayList<>();
        this.width = new ArrayList<>();
    }

    public int getPeriods() {
        return this.periods;
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

    public List<Double> getWidth() {
        return this.width;
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //get the current and previous values
        double previousLower = getRecent(getLower(), 2);
        double currentLower = getRecent(getLower());
        double previousPrice = getRecent(history, Fields.Close);

        //if the previous price was below lower and just crossed above it let's buy
        if (previousPrice < previousLower && currentPrice > currentLower)
            agent.setBuy(true);

        //display our data
        displayData(agent, agent.hasBuy());
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //get the current and previous values
        double currentLower = getRecent(getLower());
        double previousMiddle = getRecent(getMiddle(), 2);
        double currentMiddle = getRecent(getMiddle());
        double previousPrice = getRecent(history, Fields.Close);

        //if we fall below the lower, we need to sell
        if (currentPrice < currentLower)
            agent.setReasonSell(ReasonSell.Reason_Strategy);

        //if we were above the middle and just fell below it
        if (previousPrice > previousMiddle && currentPrice < currentMiddle)
            agent.setReasonSell(ReasonSell.Reason_Strategy);

        //if the current price goes above our upper line, let's sell
        if (currentPrice > getRecent(getUpper()))
            agent.setReasonSell(ReasonSell.Reason_Strategy);

        //display our data
        displayData(agent, agent.getReasonSell() != null);
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display the information
        display(agent, "Upper:  ", getUpper(), write);
        display(agent, "Middle: ", getMiddle(), write);
        display(agent, "Lower:  ", getLower(), write);
        display(agent, "Width:  ", getWidth(), write);
    }

    @Override
    public void calculate(List<Period> history) {

        //clear our lists
        getMiddle().clear();
        getUpper().clear();
        getLower().clear();
        getWidth().clear();

        //calculate our sma values
        calculateSMA(history, getMiddle(), getPeriods(), Fields.Close);

        for (int index = 0; index < getMiddle().size(); index++) {

            //get the sma value
            double sma = getMiddle().get(index);

            //get the standard deviation
            double standardDeviation = getStandardDeviation(history, sma, index + getPeriods());

            //calculate our upper value
            double upper = sma + (standardDeviation * multiplier);

            //calculate our lower value
            double lower = sma - (standardDeviation * multiplier);

            //add our upper value
            getUpper().add(upper);

            //add our lower value
            getLower().add(lower);

            //our width is the difference between the upper and lower
            getWidth().add(upper - lower);
        }
    }

    private double getStandardDeviation(List<Period> history, double sma, int index) {

        double sum = 0;

        for (int x = index - getPeriods(); x < index; x++) {

            //subtract the simple moving average from the price, then square it, now add it to our total sum
            sum += Math.pow(history.get(x).close - sma, 2);
        }

        //calculate the new average
        double average = sum / (double)getPeriods();

        //return the square root of our average aka standard deviation
        return Math.sqrt(average);
    }
}