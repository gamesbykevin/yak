package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.Period.Fields;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.EMA;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.PS;

import java.util.List;

/**
 * Parabolic SAR / EMA's
 */
public class PSE extends Strategy {

    //how to access our indicator objects
    private static int INDEX_EMA_SLOW;
    private static int INDEX_EMA_FAST;
    private static int INDEX_PS;

    //configurable values
    private static final int PERIODS_EMA_SLOW = 12;
    private static final int PERIODS_EMA_FAST = 5;

    public PSE() {

        //call parent
        super(Key.PSE);

        //add our indicator objects
        INDEX_EMA_SLOW = addIndicator(new EMA(PERIODS_EMA_SLOW));
        INDEX_EMA_FAST = addIndicator(new EMA(PERIODS_EMA_FAST));
        INDEX_PS = addIndicator(new PS());
    }

    @Override
    public boolean hasBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //access our indicators
        EMA objEmaSlow = (EMA)getIndicator(INDEX_EMA_SLOW);
        EMA objEmaFast = (EMA)getIndicator(INDEX_EMA_FAST);
        PS objPs = (PS)getIndicator(INDEX_PS);

        if (getRecent(objEmaFast) > getRecent(objEmaSlow) && getRecent(objPs.getSar()) < getRecent(history, Fields.Low))
            return true;

        //no signal
        return false;
    }

    @Override
    public boolean hasSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //access our indicators
        PS objPs = (PS)getIndicator(INDEX_PS);

        if (getRecent(objPs.getSar()) > getRecent(history, Fields.High))
            return true;

        //no signal
        return false;
    }
}