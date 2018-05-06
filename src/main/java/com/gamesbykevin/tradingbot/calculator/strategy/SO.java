package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.Period.Fields;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;

import java.util.ArrayList;
import java.util.List;

import static com.gamesbykevin.tradingbot.calculator.strategy.SMA.calculateSMA;

/**
 * Stochastic Oscillator
 */
public final class SO extends Strategy {

    //indicator list
    private List<Double> stochasticOscillator;

    //our market rate
    private List<Double> marketRate;

    //list of configurable values
    private static final int PERIODS = 12;
    private static final int PERIODS_SMA = 3;

    private final int periods, periodsSMA;

    public SO() {
        this(PERIODS, PERIODS_SMA);
    }

    public SO(int periods, int periodsSMA) {

        this.periods = periods;
        this.periodsSMA = periodsSMA;

        //create new list(s)
        this.stochasticOscillator = new ArrayList<>();
        this.marketRate = new ArrayList<>();
    }

    /**
     * Get the stochastic osciallator %D
     */
    public List<Double> getStochasticOscillator() {
        return this.stochasticOscillator;
    }

    /**
     * Get the market rate %K
     */
    public List<Double> getMarketRate() {
        return this.marketRate;
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //display info
        displayData(agent, agent.hasBuy());
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //display info
        displayData(agent, agent.getReasonSell() != null);
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display the information
        display(agent, "SO: %D ", getStochasticOscillator(), write);
        display(agent, "MR: %K ", getMarketRate(), write);
    }

    @Override
    public void calculate(List<Period> history) {

        //clear our list(s)
        getMarketRate().clear();

        for (int i = 0; i < history.size(); i++) {

            //we don't have enough data yet
            if (i < periods)
                continue;

            //what is the high and low for our period range
            double high = getMaxPeriod(history, i, true).high;
            double low = getMaxPeriod(history, i, false).low;

            //what is the market value
            double marketValue = ((history.get(i).close - low) / (high - low)) * 100.0d;

            //add to our market rate list
            getMarketRate().add(marketValue);
        }

        //calculate sma for our indicator
        calculateSMA(getMarketRate(), getStochasticOscillator(), periodsSMA);
    }

    private Period getMaxPeriod(List<Period> history, int index, boolean high) {

        Period result = null;

        //check these periods for the high or low
        for (int i = index - periods; i < index; i++) {

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