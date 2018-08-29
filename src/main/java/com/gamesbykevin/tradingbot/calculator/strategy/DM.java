package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.DMT;

import java.util.List;

/**
 * Demark Trend
 */
public class DM extends Strategy {

    //how to access our indicator objects
    private static int INDEX_DM;

    public DM() {

        //call parent
        super(Key.DM);

        //add our indicator objects
        INDEX_DM = addIndicator(new DMT());
    }

    @Override
    public boolean hasBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //get our indicator object(s)
        DMT dmt = (DMT)getIndicator(INDEX_DM);

        //no signal if no data
        if (dmt.getBearSlopeData().isEmpty() || dmt.getBullSlopeData().isEmpty())
            return false;

        //let's look at the current period
        Period period = history.get(history.size() - 1);

        //let's make sure the lines are on a path to cross each other
        if (dmt.getSlopeBear() < 0 && dmt.getSlopeBull() > 0) {

            //we don't want the lines to cross yet
            if (getRecent(dmt.getBearSlopeData()) > getRecent(dmt.getBullSlopeData())) {

                //now if the candle closes above the bear line, let's buy
                if (period.close > getRecent(dmt.getBearSlopeData()) && period.close > period.open)
                    return true;
            }
        }

        //no signal
        return false;
    }

    @Override
    public boolean hasSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //get our indicator object(s)
        DMT dmt = (DMT)getIndicator(INDEX_DM);

        //no signal if no data
        if (dmt.getBearSlopeData().isEmpty() || dmt.getBullSlopeData().isEmpty())
            return false;

        //let's look at the current period
        Period period = history.get(history.size() - 1);

        //if the candle breaks below the bullish line we'll sell
        if (period.close < getRecent(dmt.getBullSlopeData()) && period.close < period.open)
            return true;

        //no signal
        return false;
    }
}