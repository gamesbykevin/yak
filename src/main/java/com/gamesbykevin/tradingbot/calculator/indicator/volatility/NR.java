package com.gamesbykevin.tradingbot.calculator.indicator.volatility;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.indicator.Indicator;

import java.util.List;

import static com.gamesbykevin.tradingbot.agent.AgentManagerHelper.displayMessage;

/**
 * Narrow Range
 */
public class NR extends Indicator {

    //our narrow range candle
    private Period narrowRangeCandle;

    public NR(int periods) {

        //call parent
        super(Key.NR, periods);
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display our range if it exists
        if (getNarrowRangeCandle() != null) {
            displayMessage(agent, "NR (" + getPeriods() + ") Buy  break $" + getNarrowRangeCandle().high, write);
            displayMessage(agent, "NR (" + getPeriods() + ") Sell break $" + getNarrowRangeCandle().low, write);
        }
    }

    @Override
    public void calculate(List<Period> history, int newPeriods) {

        //look at the most recent periods
        for (int index = history.size() - getPeriods(); index < history.size(); index++) {

            //get the current period
            Period period = history.get(index);

            //if we don't have our candle, or if the current period has a smaller range
            if (getNarrowRangeCandle() == null || getRange(period) < getRange(getNarrowRangeCandle())) {

                //set our new narrow range candle
                setNarrowRangeCandle(period);
            }
        }
    }

    @Override
    public void cleanup() {
        //don't need to clean anything
    }

    private double getRange(Period period) {
        return (period.high - period.low);
    }

    public Period getNarrowRangeCandle() {
        return narrowRangeCandle;
    }

    private void setNarrowRangeCandle(Period narrowRangeCandle) {
        this.narrowRangeCandle = narrowRangeCandle;
    }
}