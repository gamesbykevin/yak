package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.Period.Fields;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;

import java.util.ArrayList;
import java.util.List;

import static com.gamesbykevin.tradingbot.calculator.strategy.SMA.calculateSMA;

/**
 * Stoch RSI
 */
public class SR extends Strategy {

    //our lists for the stock price
    private List<Double> smaPriceLong, smaPriceShort;

    //our rsi object
    private RSI rsiObj;

    //list of stoch rsi values
    private List<Double> stochRsi;

    /**
     * How many periods do we calculate for our long sma
     */
    private static final int PERIODS_LONG = 60;

    /**
     * How many periods do we calculate for our short sma
     */
    private static final int PERIODS_SHORT = 10;

    /**
     * The number of periods to calculate our values
     */
    private static final int PERIODS_STOCH_RSI = 14;

    /**
     * Indicator security is over bought
     */
    private static final double OVER_BOUGHT = .90d;

    /**
     * Indicator security is over sold
     */
    private static final double OVER_SOLD = .10d;

    public SR() {
        this(PERIODS_STOCH_RSI);
    }

    public SR(int periods) {

        //call parent with periods
        super(periods);

        //create our rsi object
        this.rsiObj = new RSI(PERIODS_STOCH_RSI, 1, 0, 0);

        //create new lists
        this.stochRsi = new ArrayList<>();
        this.smaPriceLong = new ArrayList<>();
        this.smaPriceShort = new ArrayList<>();
    }

    public List<Double> getStochRsi() {
        return this.stochRsi;
    }

    public List<Double> getSmaPriceLong() {
        return this.smaPriceLong;
    }

    public List<Double> getSmaPriceShort() {
        return this.smaPriceShort;
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        double dailyShort = getRecent(getSmaPriceShort());
        double dailyLong = getRecent(getSmaPriceLong());

        //if our short sma is greater than our long sma and our current close is less than the short sma
        if (dailyShort > dailyLong && getRecent(history, Fields.Close) < dailyShort) {

            //then if the rsi is showing over sold, we should buy
            if (getRecent(getStochRsi()) < OVER_SOLD)
                agent.setBuy(true);
        }

        //display our data
        displayData(agent, agent.hasBuy());
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        double dailyShort = getRecent(getSmaPriceShort());
        double dailyLong = getRecent(getSmaPriceLong());

        //if our short sma is less than our long sma and our current close is greater than the short sma
        if (dailyShort < dailyLong && getRecent(history, Fields.Close) > dailyShort) {

            //then if the rsi is showing over bought, we should sell
            if (getRecent(getStochRsi()) > OVER_BOUGHT)
                agent.setReasonSell(ReasonSell.Reason_Strategy);
        }

        //display our data
        displayData(agent, agent.getReasonSell() != null);
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display the information
        this.rsiObj.displayData(agent, write);
        display(agent, "STOCH RSI: ", getStochRsi(), getPeriods() / 3, write);
    }

    @Override
    public void calculate(List<Period> history) {

        //calculate
        this.rsiObj.calculate(history);

        //clear our list
        getStochRsi().clear();

        //check every period
        for (int i = 0; i < rsiObj.getRsiVal().size(); i++) {

            //skip until we have enough data
            if (i < getPeriods())
                continue;

            double rsiHigh = -1, rsiLow = 101;

            //check the recent periods for our calculations
            for (int x = i - getPeriods(); x < i; x++) {

                //get the current rsi value
                double rsi = rsiObj.getRsiVal().get(x);

                //locate our high and low
                if (rsi < rsiLow)
                    rsiLow = rsi;
                if (rsi > rsiHigh)
                    rsiHigh = rsi;
            }

            //calculate stoch rsi value
            double stochRsi = (rsiObj.getRsiVal().get(i) - rsiLow) / (rsiHigh - rsiLow);

            //add our new value to the list
            getStochRsi().add(stochRsi);
        }

        //calculate our short and long sma values
        calculateSMA(history, smaPriceShort, PERIODS_SHORT, Fields.Close);
        calculateSMA(history, smaPriceLong, PERIODS_LONG, Fields.Close);
    }
}