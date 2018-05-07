package com.gamesbykevin.tradingbot.calculator.indicator.momentun;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.indicator.Indicator;

import java.util.ArrayList;
import java.util.List;

import static com.gamesbykevin.tradingbot.calculator.indicator.trend.SMA.calculateSMA;

/**
 * Stochastic Oscillator
 */
public final class SO extends Indicator {

    //indicator list
    private List<Double> stochasticOscillator;

    //our market rate
    private List<Double> marketRate;

    //the sma of our market rate
    private List<Double> marketRateSma;

    //list of configurable values
    private static final int PERIODS_MARKET_RATE = 14;
    private static final int PERIODS_MARKET_RATE_SMA = 3;
    private static final int PERIODS_STOCHASTIC_SMA = 3;

    private final int periodsMR, periodsMarketRateSMA, periodsStochasticSMA;

    public SO() {
        this(PERIODS_MARKET_RATE, PERIODS_MARKET_RATE_SMA, PERIODS_STOCHASTIC_SMA);
    }

    public SO(int periodsMR, int periodsMarketRateSMA, int periodsStochasticSMA) {

        this.periodsMR = periodsMR;
        this.periodsMarketRateSMA = periodsMarketRateSMA;
        this.periodsStochasticSMA = periodsStochasticSMA;

        //create new list(s)
        this.stochasticOscillator = new ArrayList<>();
        this.marketRate = new ArrayList<>();
        this.marketRateSma = new ArrayList<>();
    }

    public int getPeriodsStochasticSMA() {
        return this.periodsStochasticSMA;
    }

    public int getPeriodsMarketRateSMA() {
        return this.periodsMarketRateSMA;
    }

    public int getPeriodsMR() {
        return this.periodsMR;
    }

    /**
     * Get the stochastic oscillator %D
     */
    public List<Double> getStochasticOscillator() {
        return this.stochasticOscillator;
    }

    /**
     * Get the market rate %K (not smoothed with sma)
     */
    public List<Double> getMarketRate() {
        return this.marketRate;
    }

    /**
     * Get the specified period sma of the market rate %K
     */
    public List<Double> getMarketRateSma() {
        return this.marketRateSma;
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display the information
        display(agent, "Market Rate:     %K ", getMarketRate(), write);
        display(agent, "Market Rate Sma: %K ", getMarketRateSma(), write);
        display(agent, "Stochastic:      %D ", getStochasticOscillator(), write);
    }

    @Override
    public void calculate(List<Period> history) {

        //clear our list(s)
        getMarketRate().clear();

        for (int i = 0; i < history.size(); i++) {

            //we don't have enough data yet
            if (i < getPeriodsMR())
                continue;

            //what is the high and low for our period range
            double high = getMaxPeriod(history, i, true).high;
            double low = getMaxPeriod(history, i, false).low;

            //what is the market value
            double marketValue = ((history.get(i).close - low) / (high - low)) * 100.0d;

            //add to our market rate list
            getMarketRate().add(marketValue);
        }

        //now we use a sma to create our market rate values
        calculateSMA(getMarketRate(), getMarketRateSma(), getPeriodsMarketRateSMA());

        //now we use a sma of that to create our stochastic oscillator values
        calculateSMA(getMarketRateSma(), getStochasticOscillator(), getPeriodsStochasticSMA());
    }

    private Period getMaxPeriod(List<Period> history, int index, boolean high) {

        Period result = null;

        //check these periods for the high or low
        for (int i = index - getPeriodsMR(); i < index; i++) {

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