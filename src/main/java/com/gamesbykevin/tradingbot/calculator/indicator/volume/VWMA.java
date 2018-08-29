package com.gamesbykevin.tradingbot.calculator.indicator.volume;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.indicator.Indicator;

import java.util.ArrayList;
import java.util.List;

/**
 * Volume Weighted Moving Average
 */
public class VWMA extends Indicator {

    //our final result
    private List<Double> vwma;

    public VWMA(int periods) {

        //call parent
        super(Key.VWAP, periods);
    }

    public List<Double> getVWMA() {

        //instantiate if null
        if (this.vwma == null)
            this.vwma = new ArrayList<>();

        return this.vwma;
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display the information
        display(agent, "VWM (" + getPeriods() + ") $ ", getVWMA(), write);
    }

    @Override
    public void calculate(List<Period> history, int newPeriods) {

        //where do we start
        int start = getVWMA().isEmpty() ? 0 : (history.size() - newPeriods);

        //calculate missing values
        for (int index = start; index < history.size(); index++) {

            //make sure we have enough data to calculate
            if (index >= getPeriods())
                getVWMA().add(calculateVWMA(history, index));
        }
    }

    private double calculateVWMA(List<Period> history, int end) {

        //our top and bottom values
        double numerator = 0;
        double denominator = 0;

        //check the latest for the calculation
        for (int index = end - getPeriods() + 1; index <= end; index++) {

            Period period = history.get(index);
            numerator += (period.close * period.volume);
            denominator += (period.volume);
        }

        //don't divide by 0
        if (numerator == 0 || denominator == 0)
            return 0;

        //return our result
        return (numerator / denominator);
    }

    @Override
    public void cleanup() {
        cleanup(getVWMA());
    }
}