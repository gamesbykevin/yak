package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Calculation;
import com.gamesbykevin.tradingbot.calculator.Calculator.Candle;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.indicator.Indicator;

import java.util.ArrayList;
import java.util.List;

public abstract class Strategy extends Calculation {

    //does this strategy need to wait for new candle data to check for a buy signal?
    private boolean wait = false;

    //list of indicators we are using
    private List<Indicator> indicators;

    /**
     * The default number of periods we check for the short $
     */
    public static final int DEFAULT_PERIODS_SHORT = 5;

    /**
     * The default number of periods we check when confirming an uptrend
     */
    public static final int DEFAULT_PERIODS_CONFIRM_INCREASE = 5;

    /**
     * The default number of periods we check when confirming a downtrend
     */
    public static final int DEFAULT_PERIODS_CONFIRM_DECREASE = 5;

    /**
     * Different trading strategies we can use
     */
    public enum Key {
        AE,
        AR,
        AS,
        BBAR,
        BBER,
        BBR,
        CA,
        CC,
        DM,
        EC,
        EMA3,
        EMAC,
        EMAR,
        EMAS,
        ERS,
        FA,
        FADOA,
        FAO,
        FMFI,
        HASO,
        LDB,
        MA,
        MACS,
        MARS,
        MER,
        MH,
        MP,
        MS,
        NR4,
        NR7,
        NVE,
        PSE,
        PVE,
        RA,
        SEMAS,
        SMASR,
        SOADX,
        SOEMA,
        SSR,
        TWOA,
        TWOR,
        VS,
        VWM,
    }

    private final Key key;

    //track the time so we know when the current candle will end
    private long timeEnd;

    //did we setup the time to wait?
    private boolean timeWait = false;

    protected Strategy(Key key) {
        this.key = key;
    }

    protected void resetTimeTrade() {

        //flag that we haven't setup the wait time
        this.timeWait = false;
    }

    protected boolean hasSetupTimeTrade() {
        return this.timeWait;
    }

    protected void setupTimeTrade(Candle candle) {

        //setup the time if we haven't already yet
        if (!hasSetupTimeTrade()) {

            //calculate when we are near the end of the candle
            this.timeEnd = System.currentTimeMillis() + (long)((candle.duration * .8) * 1000L);

            //we have setup the wait time
            this.timeWait = true;
        }
    }

    protected boolean hasTimeTrade() {

        //if we haven't setup the time we can't tell if enough time lapsed
        if (!hasSetupTimeTrade())
            return false;

        //if enough time has passed we are close to the end of the current period
        if (System.currentTimeMillis() >= this.timeEnd)
            return true;

        //not enough time has lapsed
        return false;
    }

    public Key getKey() {
        return this.key;
    }

    protected int addIndicator(Indicator indicator) {

        //add the indicator to the list
        getIndicators().add(indicator);

        //return the index of the indicator
        return (getIndicators().size() - 1);
    }

    protected Indicator getIndicator(int index) {
        return getIndicators().get(index);
    }

    /**
     * Get the list of indicators for this strategy
     * @return List of existing indicators
     */
    private List<Indicator> getIndicators()
    {
        if (this.indicators == null)
            this.indicators = new ArrayList<>();

        return this.indicators;
    }

    @Override
    public final void displayData(Agent agent, boolean write) {

        for (int index =  0; index < getIndicators().size(); index++) {
            getIndicator(index).displayData(agent, write);
        }
    }

    public final void calculate(List<Period> history, int newPeriods) {

        for (int index =  0; index < getIndicators().size(); index++) {
            getIndicator(index).calculate(history, newPeriods);
        }
    }

    @Override
    public final void cleanup() {

        for (int index =  0; index < getIndicators().size(); index++) {
            getIndicator(index).cleanup();
        }
    }

    protected double getShortLow(List<Period> history) {

        //our final result
        double low = 0;

        //check the recent history for a short $
        for (int index = history.size() - DEFAULT_PERIODS_SHORT; index < history.size(); index++) {

            //get the current value
            double tmp = history.get(index).low;

            //if not set or there is a better low
            if (low == 0 || tmp < low)
                low = tmp;
        }

        //return our result
        return low;
    }

    public void goShort(Agent agent, double price) {
        agent.getTrade().goShort(agent, price);
    }

    public abstract boolean hasBuySignal(Agent agent, List<Period> history, double currentPrice);

    public abstract boolean hasSellSignal(Agent agent, List<Period> history, double currentPrice);

    /**
     * Does the strategy need to wait?
     * @return true = we need to wait for new candle data, false = otherwise
     */
    public boolean hasWait() {
        return this.wait;
    }

    /**
     * Set the strategy to wait for new candle data
     * @param wait true = we want this strategy to wait for new candle data, false = otherwise
     */
    public void setWait(boolean wait) {
        this.wait = wait;
    }
}