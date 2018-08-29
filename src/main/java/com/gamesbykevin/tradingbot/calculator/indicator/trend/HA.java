package com.gamesbykevin.tradingbot.calculator.indicator.trend;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Calculator;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.indicator.Indicator;

import java.util.ArrayList;
import java.util.List;

import static com.gamesbykevin.tradingbot.agent.AgentManagerHelper.displayMessage;

/**
 * Heiken-Ashi
 */
public class HA extends Indicator {

    //we will create our own candles
    private List<Period> haPeriods;

    public HA() {

        //call parent
        super(Indicator.Key.HA, 0);

        //create new list
        this.haPeriods = new ArrayList<>();
    }

    public List<Period> getHaPeriods() {
        return this.haPeriods;
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        String desc = "";

        for (int i = getHaPeriods().size() - RECENT_PERIODS; i < getHaPeriods().size(); i++) {

            if (desc.length() > 0)
                desc = desc + ", ";

            if (isBearish(getHaPeriods().get(i))) {
                desc = desc + "bearish";
            } else if (isBullish(getHaPeriods().get(i))) {
                desc = desc + "bullish";
            }
        }

        displayMessage(agent, "HA (" + getPeriods() + "): " + desc, write);
    }

    @Override
    public void calculate(List<Period> history, int newPeriods) {

        //where do we start
        int start = getHaPeriods().isEmpty() ? 0 : history.size() - newPeriods;

        //check the latest periods only for accurate results
        for (int i = start; i < history.size(); i++) {

            //get the current period
            Period current = history.get(i);

            //create new period
            Period haPeriod = new Period();

            //The Heikin-Ashi Close is simply an average of the open, high, low and close for the current period
            haPeriod.close = ((current.open + current.close + current.high + current.low) / 4);

            //if no results the first value will be calculated differently
            if (getHaPeriods().isEmpty()) {

                //The Heikin-Ashi Open is the average of the prior Heikin-Ashi candlestick open + close of the prior Heikin-Ashi candlestick
                haPeriod.open = ((current.open + current.close) / 2);

                //first high value is the current high
                haPeriod.high = current.high;

                //first low value is the current low
                haPeriod.low = current.low;

            } else {

                //get the previous Heikin-Ashi period
                Period haPrevious = getHaPeriods().get(getHaPeriods().size() - 1);

                //The Heikin-Ashi Open is the average of the prior Heikin-Ashi candlestick open + close of the prior Heikin-Ashi candlestick
                haPeriod.open = ((haPrevious.open + haPrevious.close) / 2);

                /**
                 * The Heikin-Ashi High is the maximum of three data points:
                 * 1. The current period's high
                 * 2. The current Heikin-Ashi candlestick open
                 * 3. The current Heikin-Ashi candlestick close
                 */
                haPeriod.high = current.high;

                if (haPeriod.open > haPeriod.high)
                    haPeriod.high = haPeriod.open;
                if (haPeriod.close > haPeriod.high)
                    haPeriod.high = haPeriod.close;

                /**
                 * The Heikin-Ashi Low is the minimum of three data points:
                 * 1. The current period's low
                 * 2. The current Heikin-Ashi candlestick open
                 * 3. The current Heikin-Ashi candlestick close
                 */
                haPeriod.low = current.low;

                if (haPeriod.open < haPeriod.low)
                    haPeriod.low = haPeriod.open;
                if (haPeriod.close < haPeriod.low)
                    haPeriod.low = haPeriod.close;
            }

            //add the period to the list
            getHaPeriods().add(haPeriod);
        }
    }

    /**
     * Is the candle bearish?
     * @param period The period we want to check
     * @return true if the close is less than when the period opened, false otherwise
     */
    public boolean isBearish(Period period) {
        return (period.close < period.open);
    }

    /**
     * Is the candle bullish?
     * @param period The period we want to check
     * @return true if the open is less than when the period closed, false otherwise
     */
    public boolean isBullish(Period period) {
        return (period.close > period.open);
    }

    @Override
    public void cleanup() {

        //don't allow the size to grow too much
        while (getHaPeriods().size() > Calculator.HISTORICAL_PERIODS_MINIMUM) {
            getHaPeriods().remove(0);
        }
    }
}