package com.gamesbykevin.tradingbot.calculator.indicator.trend;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.indicator.Indicator;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;

import static com.gamesbykevin.tradingbot.util.PropertyUtil.displayMessage;

public class FIB extends Indicator {

    /**
     * Magical fibonacci ratios
     */
    public static final float[] PERCENTAGES = {0.0f, .236f, .382f, .5f, .618f, .764f, 1.0f, 1.382f};

    //list of fibonacci levels
    private HashMap<Float, Level> levels;

    //configurable value(s)
    public static final int PERIODS = 100;

    //our high and low candles
    private Period candleHigh, candleLow;

    public FIB() {
        this(PERIODS);
    }

    public FIB(int periods) {

        //call parent
        super(Key.FIB, periods);
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        PrintWriter writer = (write) ? agent.getWriter() : null;

        //display our data
        displayMessage("Candle high: " + getCandleDesc(getCandleHigh()), writer);
        displayMessage("Candle low: " + getCandleDesc(getCandleLow()), writer);

        //display our data
        for (int i = 0; i < PERCENTAGES.length; i++) {
            if (getLevel(PERCENTAGES[i]) != null)
                displayMessage("FIB - " + getLevel(PERCENTAGES[i]).getDesc(), writer);
        }
    }

    @Override
    public void calculate(List<Period> history, int newPeriods) {

        //reset our values
        for (int index = 0; index < PERCENTAGES.length; index++) {
            getLevel(PERCENTAGES[index]).reset();
        }

        //reset candles
        this.candleHigh = null;
        this.candleLow = null;

        //check the most recent periods for our low and high
        for (int index = history.size() - getPeriods(); index < history.size(); index++) {

            //get the current period
            Period center = history.get(index);

            //if lower or not set yet, let's assign
            if (getCandleLow() == null || center.low < getCandleLow().low)
                this.candleLow = center;

            //if higher or not set yet, let's assign
            if (getCandleHigh() == null || center.high > getCandleHigh().high)
                this.candleHigh = center;
        }

        //now that we have our low and high, let's calculate the retracement levels
        for (int index = 0; index < PERCENTAGES.length; index++) {
            getLevel(PERCENTAGES[index]).calculate();
        }
    }

    @Override
    public void cleanup() {
        //no cleanup needed
    }

    private class Level {

        public float percentage;

        public double uptrendRetracement;
        public double uptrendExtension;
        public double downtrendRetracement;
        public double downtrendExtension;

        private void reset() {
            this.uptrendRetracement = 0;
            this.uptrendExtension = 0;
            this.downtrendRetracement = 0;
            this.downtrendExtension = 0;
        }

        /**
         * https://www.easycalculation.com/finance/learn-fibonacci-retracement.php
         */
        private void calculate() {

            double high = getCandleHigh().high;
            double low = getCandleLow().low;

            //calculate our lines
            this.uptrendRetracement = high - ((high - low) * percentage);
            this.uptrendExtension = high + ((high - low) * percentage);
            this.downtrendRetracement = low + ((high - low) * percentage);
            this.downtrendExtension = low - ((high - low) * percentage);
        }

        private String getDesc() {
            return "UR: " + uptrendRetracement + ", UE: " + uptrendExtension +
                    ", DR: " + downtrendRetracement + ", DR: " + downtrendExtension;
        }
    }

    private String getCandleDesc(Period period) {

        if (period == null)
            return "null";

        return "Time: " + period.time +
                ", High: " + period.high + ", Low: " + period.low +
                ", Open: " + period.open + ", Close: " + period.close +
                ", Volume: " + period.volume;
    }

    public HashMap<Float, Level> getLevels() {

        //create new hash map if null
        if (this.levels == null)
            this.levels = new HashMap<>();

        return this.levels;
    }

    public Level getLevel(float percentage) {

        if (getLevels().get(percentage) == null)
            getLevels().put(percentage, new Level());

        return getLevels().get(percentage);
    }

    public Period getCandleHigh() {
        return this.candleHigh;
    }

    public Period getCandleLow() {
        return this.candleLow;
    }
}