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

    //list of configurable values
    private static int PERIODS_LONG = 60;
    private static int PERIODS_SHORT = 10;
    private static int PERIODS_STOCH_RSI = 14;
    private static double OVER_BOUGHT = .90d;
    private static double OVER_SOLD = .10d;

    private final int periodsLong, periodsShort, periodsStochRsi;
    private final double overBought, overSold;

    public SR() {
        this(PERIODS_LONG, PERIODS_SHORT, PERIODS_STOCH_RSI, OVER_BOUGHT, OVER_SOLD);
    }

    public SR(int periodsLong, int periodsShort, int periodsStochRsi, double overBought, double overSold) {

        //create our rsi object
        this.rsiObj = new RSI(1, PERIODS_STOCH_RSI, 0, 0);

        //create new lists
        this.stochRsi = new ArrayList<>();
        this.smaPriceLong = new ArrayList<>();
        this.smaPriceShort = new ArrayList<>();

        this.periodsLong = periodsLong;
        this.periodsShort = periodsShort;
        this.periodsStochRsi = periodsStochRsi;
        this.overBought = overBought;
        this.overSold = overSold;
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
            if (getRecent(getStochRsi()) < overSold)
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
            if (getRecent(getStochRsi()) > overBought)
                agent.setReasonSell(ReasonSell.Reason_Strategy);
        }

        //display our data
        displayData(agent, agent.getReasonSell() != null);
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display the information
        this.rsiObj.displayData(agent, write);
        display(agent, "STOCH RSI: ", getStochRsi(), write);
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
            if (i < periodsStochRsi)
                continue;

            double rsiHigh = -1, rsiLow = 101;

            //check the recent periods for our calculations
            for (int x = i - periodsStochRsi; x < i; x++) {

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
        calculateSMA(history, smaPriceShort, periodsShort, Fields.Close);
        calculateSMA(history, smaPriceLong, periodsLong, Fields.Close);
    }
}