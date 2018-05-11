package com.gamesbykevin.tradingbot.calculator.indicator.volatility;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.Period.Fields;
import com.gamesbykevin.tradingbot.calculator.indicator.Indicator;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.SMA;

import java.util.ArrayList;
import java.util.List;

import static com.gamesbykevin.tradingbot.calculator.indicator.trend.SMA.calculateSMA;

public class BB extends Indicator {

    //list of configurable values
    public static int PERIODS = 10;

    private final int periods;

    private final float multiplier;

    //our lists
    private List<Double> upper, lower, width;

    //our middle values
    private SMA middle;

    public static final float MULTIPLIER = 2.0f;

    public BB() {
        this(PERIODS, MULTIPLIER);
    }

    public BB(int periods, float multiplier) {

        this.periods = periods;
        this.multiplier = multiplier;

        //create our lists
        this.middle = new SMA(periods);
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

    public SMA getMiddle() {
        return this.middle;
    }

    public List<Double> getLower() {
        return this.lower;
    }

    public List<Double> getWidth() {
        return this.width;
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display the information
        display(agent, "Upper:  ", getUpper(), write);
        display(agent, "Middle: ", getMiddle().getSma(), write);
        display(agent, "Lower:  ", getLower(), write);
        display(agent, "Width:  ", getWidth(), write);
    }

    @Override
    public void calculate(List<Period> history, int newPeriods) {

        //calculate our sma values
        getMiddle().calculate(history, newPeriods);

        //where do we start
        int start = getWidth().isEmpty() ? 0 : getMiddle().getSma().size() - newPeriods;

        for (int index = start; index < getMiddle().getSma().size(); index++) {

            //do we have enough data to calculate
            if (index < getPeriods())
                continue;

            //get the sma value
            double sma = getMiddle().getSma().get(index);

            //get the standard deviation
            double standardDeviation = getStandardDeviation(history, sma, index);

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