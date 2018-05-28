package com.gamesbykevin.tradingbot.calculator.indicator.volatility;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.indicator.Indicator;

import java.util.ArrayList;
import java.util.List;

/**
 * Average True Range
 */
public class ATR extends Indicator {

    //list of configurable values
    public static int PERIODS = 14;

    //list(s) of true range values
    private List<Double> trueRange, trueRangeAverage;

    public ATR() {
        this(PERIODS);
    }

    public ATR(int periods) {

        //call parent
        super(Indicator.Key.ATR, periods);

        //create our list(s)
        this.trueRange = new ArrayList<>();
        this.trueRangeAverage = new ArrayList<>();
    }

    public List<Double> getTrueRange() {
        return this.trueRange;
    }

    public List<Double> getAverageTrueRange() {
        return this.trueRangeAverage;
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display the information
        display(agent, "True Range: ", getTrueRange(), write);
        display(agent, "Avg True Range (" + getPeriods() + "): ", getAverageTrueRange(), write);
    }

    @Override
    public void calculate(List<Period> history, int newPeriods) {

        //where do we start?
        int start = getAverageTrueRange().isEmpty() ? 0 : history.size() - newPeriods;

        for (int i = start; i < history.size(); i++) {

            //get the current and previous periods
            Period curr = history.get(i);
            Period prev = (i > 0) ? history.get(i - 1) : null;

            //start with a value
            double value = curr.high - curr.low;

            //check previous period if it exists
            if (prev != null) {

                //we want the greatest value
                if (Math.abs(curr.high - prev.close) > value)
                    value = Math.abs(curr.high - prev.close);
                if (Math.abs(curr.low - prev.close) > value)
                    value = Math.abs(curr.low - prev.close);
            }

            //add the greatest value to the list
            getTrueRange().add(value);
        }

        //if the list is empty we need to calculate our first value
        if (getAverageTrueRange().isEmpty()) {

            //what is our sum?
            double sum = 0;

            //now we calculate our average
            for (int i = 0; i < getPeriods(); i++) {

                //add our total sum
                sum += getTrueRange().get(i);
            }

            //calculate our first value
            double average = (sum / (float) getPeriods());

            //add the first value to our list
            getAverageTrueRange().add(average);
        }

        //where do we start when calculating the average true range?
        start = getAverageTrueRange().size() <= 1 ? 0 : getTrueRange().size() - newPeriods;

        //start where we left off and calculate the rest
        for (int i = start; i < getTrueRange().size(); i++) {

            //get the previous value
            double previous = getRecent(getAverageTrueRange());

            //calculate our new true average
            double current = ((previous * (getPeriods() - 1)) + getTrueRange().get(i)) / (float)getPeriods();

            //add to the list
            getAverageTrueRange().add(current);
        }
    }

    @Override
    public void cleanup() {
        cleanup(getTrueRange());
        cleanup(getAverageTrueRange());
    }
}