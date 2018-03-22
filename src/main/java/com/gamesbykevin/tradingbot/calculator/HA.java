package com.gamesbykevin.tradingbot.calculator;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonBuy;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;

import java.util.ArrayList;
import java.util.List;

import static com.gamesbykevin.tradingbot.agent.AgentManager.displayMessage;

/**
 * Heikin-Ashi
 */
public class HA extends Indicator {

    private List<Period> haPeriods;

    /**
     * How many periods do we calculate Heikin-Ashi
     */
    public final static int PERIODS_HA = 7;

    public HA() {

        //call parent
        super(PERIODS_HA);

        //create new list
        this.haPeriods = new ArrayList<>();
    }

    private List<Period> getHaPeriods() {
        return this.haPeriods;
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        boolean confirmBearish = true;
        boolean confirmBullish = false;

        //check each candle to confirm they are bearish
        for (int i = history.size() - getPeriods(); i < history.size() - 2; i++) {

            //all candles should be bearish
            if (isBearish(history.get(i))) {
                confirmBearish = false;
                break;
            }
        }

        //if we confirmed a previous bearish trend, let's check for bullish
        if (confirmBearish) {

            //if the 2 recent periods are bullish, we are good
            if (isBullish(history.get(history.size() - 2)) && isBullish(history.get(history.size() - 1))) {
                confirmBullish = true;
            } else {
                confirmBullish = false;
            }
        }

        //if we confirm bullish, let's buy
        if (confirmBullish)
            agent.setReasonBuy(ReasonBuy.Reason_10);

        //display our data
        displayData(agent, agent.getReasonBuy() != null);
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        boolean confirmBearish = false;
        boolean confirmBullish = true;

        //check each candle to confirm they are bullish
        for (int i = history.size() - getPeriods(); i < history.size() - 2; i++) {

            //all candles should be bullish
            if (isBullish(history.get(i))) {
                confirmBullish = false;
                break;
            }
        }

        //if we confirmed a previous bullish trend, let's check for bearish
        if (confirmBullish) {

            //if the 2 recent periods are bearish, we are good
            if (isBearish(history.get(history.size() - 2)) && isBearish(history.get(history.size() - 1))) {
                confirmBearish = true;
            } else {
                confirmBearish = false;
            }
        }

        //if we confirm bullish, let's sell
        if (confirmBearish)
            agent.setReasonSell(ReasonSell.Reason_13);

        //display our data
        displayData(agent, agent.getReasonSell() != null);
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        String desc = "";

        for (int i = getHaPeriods().size() - getPeriods(); i < getHaPeriods().size(); i++) {

            if (desc.length() > 0)
                desc = desc + ", ";

            if (isBearish(getHaPeriods().get(i))) {
                desc = desc + "bearish";
            } else if (isBullish(getHaPeriods().get(i))) {
                desc = desc + "bullish";
            }
        }

        displayMessage(agent, "HA: " + desc, write);
    }

    @Override
    public void calculate(List<Period> history) {

        //clear the list
        getHaPeriods().clear();

        //check every period possible
        for (int i = 0; i < history.size(); i++) {

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
                if (current.high >= haPeriod.open && current.high >= haPeriod.close) {
                    haPeriod.high = current.high;
                } else if (haPeriod.open >= current.high && haPeriod.open >= haPeriod.close) {
                    haPeriod.high = haPeriod.open;
                } else if (haPeriod.close >= current.high && haPeriod.close >= haPeriod.open) {
                    haPeriod.high = haPeriod.close;
                }

                /**
                 * The Heikin-Ashi Low is the minimum of three data points:
                 * 1. The current period's low
                 * 2. The current Heikin-Ashi candlestick open
                 * 3. The current Heikin-Ashi candlestick close
                 */
                if (current.high <= haPeriod.open && current.high <= haPeriod.close) {
                    haPeriod.low = current.high;
                } else if (haPeriod.open <= current.high && haPeriod.open <= haPeriod.close) {
                    haPeriod.low = haPeriod.open;
                } else if (haPeriod.close <= current.high && haPeriod.close <= haPeriod.open) {
                    haPeriod.low = haPeriod.close;
                }
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
    private boolean isBearish(Period period) {
        return (period.close < period.open);
    }

    /**
     * Is the candle bullish?
     * @param period The period we want to check
     * @return true if the open is less than when the period closed, false otherwise
     */
    private boolean isBullish(Period period) {
        return (period.close > period.open);
    }
}