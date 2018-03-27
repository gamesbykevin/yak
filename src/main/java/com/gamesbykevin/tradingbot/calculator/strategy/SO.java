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

    //our simple moving average of closing price
    private List<Double> smaPrice;

    /**
     * Number of periods for stochastic oscillator
     */
    private static final int PERIODS_SO = 14;

    /**
     * Number of periods we calculate sma so to get our indicator
     */
    private static final int PERIODS_SMA_SO = 3;

    /**
     * Number of periods we calculate sma price
     */
    private static final int PERIODS_SMA_PRICE = 200;

    /**
     * Security is considered over sold
     */
    private static final double OVER_SOLD = 20.0d;

    /**
     * Security is considered over bought
     */
    private static final double OVER_BOUGHT = 80.0d;

    //store our periods
    private final int periodsSmaSO, periodsSO, periodsSmaPrice;

    public SO(int periodsSmaSO, int periodsSO, int periodsSmaPrice) {

        //calling our parent with a default value
        super(0);

        //assign our periods
        this.periodsSmaSO = periodsSmaSO;
        this.periodsSO = periodsSO;
        this.periodsSmaPrice = periodsSmaPrice;

        //create new list(s)
        this.stochasticOscillator = new ArrayList<>();
        this.marketRate = new ArrayList<>();
        this.smaPrice = new ArrayList<>();
    }

    public SO() {
        this(PERIODS_SMA_SO, PERIODS_SO, PERIODS_SMA_PRICE);
    }

    public List<Double> getStochasticOscillator() {
        return this.stochasticOscillator;
    }

    public List<Double> getMarketRate() {
        return this.marketRate;
    }

    public List<Double> getSmaPrice() {
        return this.smaPrice;
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //if the close is above the average
        if (getRecent(history, Fields.Close) > getRecent(getSmaPrice())) {

            //get the last 2 indicator values
            double previous = getRecent(getStochasticOscillator(), 2);
            double current = getRecent(getStochasticOscillator());

            //if previously oversold, and no longer
            if (previous < OVER_SOLD && current >  OVER_SOLD)
                agent.setBuy(true);
        }

        //display info
        displayData(agent, agent.hasBuy());
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //if the close is now below the average
        if (getRecent(history, Fields.Close) < getRecent(getSmaPrice())) {

            //get the last 2 indicator values
            double previous = getRecent(getStochasticOscillator(), 2);
            double current = getRecent(getStochasticOscillator());

            //if previously over bought, and no longer
            if (previous > OVER_BOUGHT && current < OVER_BOUGHT)
                agent.setReasonSell(ReasonSell.Reason_Strategy);
        }

        //display info
        displayData(agent, agent.getReasonSell() != null);
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display the information
        display(agent, "SO: %D ", getStochasticOscillator(), periodsSO, write);
        display(agent, "MR: %K ", getMarketRate(), periodsSO, write);
        display(agent, "SMA: ", getSmaPrice(), periodsSO, write);
    }

    @Override
    public void calculate(List<Period> history) {

        //clear our list(s)
        getMarketRate().clear();

        for (int i = 0; i < history.size(); i++) {

            //we don't have enough data yet
            if (i < periodsSO)
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
        calculateSMA(getMarketRate(), getStochasticOscillator(), periodsSmaSO);

        //calculate sma for the closing price
        calculateSMA(history, getSmaPrice(), periodsSmaPrice, Fields.Close);
    }

    private Period getMaxPeriod(List<Period> history, int index, boolean high) {

        Period result = null;

        //check these periods for the high or low
        for (int i = index - periodsSO; i < index; i++) {

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