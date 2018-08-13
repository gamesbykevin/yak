package com.gamesbykevin.tradingbot.calculator.indicator.momentun;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.indicator.Indicator;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.SMA;

import java.util.ArrayList;
import java.util.List;

/**
 * Stochastic Oscillator
 */
public final class SO extends Indicator {

    //our market rate
    private List<Double> marketRateBasic;

    //our sma objects
    private SMA objMarketFullSMA, objStochasticOscillator;

    //list of configurable values
    private static final int PERIODS_MARKET_RATE_BASIC = 14;
    private static final int PERIODS_MARKET_RATE_FULL = 3;
    private static final int PERIODS_STOCHASTIC = 3;

    public SO() {
        this(PERIODS_MARKET_RATE_BASIC, PERIODS_MARKET_RATE_FULL, PERIODS_STOCHASTIC);
    }

    public SO(int periodsMarketRateBasic, int periodsMarketRateFull, int periodsStochastic) {

        //call parent
        super(Indicator.Key.SO, periodsMarketRateBasic);

        //create new list(s)
        this.marketRateBasic = new ArrayList<>();
        this.objMarketFullSMA = new SMA(periodsMarketRateFull);
        this.objStochasticOscillator = new SMA(periodsStochastic);
    }

    public int getPeriodsMarketRateBasic() {
        return this.getPeriods();
    }

    /**
     * Get the stochastic oscillator %D
     */
    public List<Double> getStochasticOscillator() {
        return this.objStochasticOscillator.getSma();
    }

    /**
     * Get the market rate %K (not smoothed with sma)
     */
    public List<Double> getMarketRateBasic() {
        return this.marketRateBasic;
    }

    /**
     * Get the specified period sma of the market rate %K
     */
    public List<Double> getMarketRateFull() {
        return this.objMarketFullSMA.getSma();
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display the information
        display(agent, "Market Rate Basic (" + getPeriodsMarketRateBasic() + ") %K: ", getMarketRateBasic(), write);
        display(agent, "Market Rate Full (" + objMarketFullSMA.getPeriods() + ") %K: ", getMarketRateFull(), write);
        display(agent, "Stochastic (" + objStochasticOscillator.getPeriods() + ") %D: ", getStochasticOscillator(), write);
    }

    @Override
    public void calculate(List<Period> history, int newPeriods) {

        //where do we start
        int start = getMarketRateBasic().isEmpty() ? 0 : history.size() - newPeriods;

        //calculate our data
        for (int i = start; i < history.size(); i++) {

            //we don't have enough data yet
            if (i < getPeriodsMarketRateBasic())
                continue;

            //what is the high and low for our period range
            double high = getMaxPeriod(history, i, true).high;
            double low = getMaxPeriod(history, i, false).low;

            //what is the market value
            double marketValue = ((history.get(i).close - low) / (high - low)) * 100.0d;

            //add to our market rate list
            getMarketRateBasic().add(marketValue);
        }

        //now we use a sma to create our market rate values
        objMarketFullSMA.calculateSMA(getMarketRateBasic(), newPeriods);

        //now we use a sma of that to create our stochastic oscillator values
        objStochasticOscillator.calculateSMA(getMarketRateFull(), newPeriods);
    }

    private Period getMaxPeriod(List<Period> history, int index, boolean high) {

        Period result = null;

        //check these periods for the high or low
        for (int i = index - getPeriodsMarketRateBasic(); i < index; i++) {

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

    @Override
    public void cleanup() {
        cleanup(getMarketRateBasic());
        objMarketFullSMA.cleanup();
        objStochasticOscillator.cleanup();
    }
}