package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;

import java.util.ArrayList;
import java.util.List;

public abstract class SO extends Strategy {

    //indicator list
    private List<Double> stochasticOscillator;

    //our market rate
    private List<Double> marketRate;

    /**
     * Number of periods for stochastic oscillator
     */
    public static final int PERIODS_SO = 14;

    /**
     * Number of periods we calculate sma to get our indicator
     */
    public static final int PERIODS_SMA = 3;

    protected int periodsSMA, periodsSO;

    public SO(int periodsSMA, int periodsSO) {

        //calling our parent with a default value
        super(0);

        //assign our periods
        this.periodsSMA = periodsSMA;
        this.periodsSO = periodsSO;

        //create new list(s)
        this.stochasticOscillator = new ArrayList<>();
        this.marketRate = new ArrayList<>();
    }

    public SO() {
        this(PERIODS_SMA, PERIODS_SO);
    }

    public List<Double> getStochasticOscillator() {
        return this.stochasticOscillator;
    }

    public List<Double> getMarketRate() {
        return this.marketRate;
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display the information
        display(agent, "SO: ", getStochasticOscillator(), periodsSO / 4, write);
        display(agent, "MR: ", getMarketRate(), periodsSO / 4, write);
    }

    @Override
    public void calculate(List<Period> history) {

        //clear our list(s)
        getStochasticOscillator().clear();
        getMarketRate().clear();

        for (int i = 0; i < history.size(); i++) {

            //we don't have enough data yet
            if (i <= periodsSO)
                continue;

            //what is the high and low for our period range
            double high = getMaxPeriod(history, i, true).high;
            double low = getMaxPeriod(history, i, false).low;

            //what is the market value
            double marketValue = ((history.get(i).close - low) / (high - low)) * 100.0d;

            //add to our market rate list
            getMarketRate().add(marketValue);
        }

        //the indicator will be a 3 period sma of marketRate
        for (int i = 0; i < getMarketRate().size(); i++) {

            //we don't have enough data yet
            if (i < periodsSMA)
                continue;

            double sum = 0;

            for (int x = i - periodsSMA; x < i; x++) {
                sum += getMarketRate().get(x);
            }

            //add our 3 period sma as our result
            getStochasticOscillator().add((sum / (float)periodsSMA));
        }
    }

    private Period getMaxPeriod(List<Period> history, int index, boolean high) {

        Period result = null;

        //check these periods for the high or low
        for (int i = index - periodsSO; i <= index; i++) {

            //check the current period
            Period current = history.get(i);

            if (result == null) {

                //set a default period to start
                result = current;

            } else {

                //are we looking for the highest high, or lowest low
                if (high) {

                    //if we have a new high, this will be the period to beat
                    if (current.high > result.high)
                        result = current;

                } else {

                    //if we have a new low, this will be the period to beat
                    if (current.low < result.low)
                        result = current;
                }

            }
        }

        //return our result
        return result;
    }
}