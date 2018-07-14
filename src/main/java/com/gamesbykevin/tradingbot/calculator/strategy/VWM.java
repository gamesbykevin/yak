package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.indicator.volume.VWMA;

import java.util.List;

/**
 * Volume Weighted Moving Average
 */
public class VWM extends Strategy {

    //how to access our indicator objects
    private static int INDEX_VWMA_SLOW;
    private static int INDEX_VWMA_FAST;

    //configurable values
    private static final int PERIODS_VWMA_SLOW = 12;
    private static final int PERIODS_VWMA_FAST = 26;

    public VWM() {

        //call parent
        super(Key.VWM);

        //add our indicator objects
        INDEX_VWMA_SLOW = addIndicator(new VWMA(PERIODS_VWMA_SLOW));
        INDEX_VWMA_FAST = addIndicator(new VWMA(PERIODS_VWMA_FAST));
    }

    @Override
    public boolean hasBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //access our indicators
        VWMA slow = (VWMA)getIndicator(INDEX_VWMA_SLOW);
        VWMA fast = (VWMA)getIndicator(INDEX_VWMA_FAST);

        //if the fast crosses above, let's buy
        if (getRecent(fast.getVWMA(), 2) < getRecent(slow.getVWMA(), 2) &&
                getRecent(fast.getVWMA()) > getRecent(slow.getVWMA()))
            return true;

        //no signal
        return false;
    }

    @Override
    public boolean hasSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //access our indicators
        VWMA slow = (VWMA)getIndicator(INDEX_VWMA_SLOW);
        VWMA fast = (VWMA)getIndicator(INDEX_VWMA_FAST);

        //if the fast goes below, sell
        if (getRecent(fast.getVWMA()) < getRecent(slow.getVWMA()))
            return true;

        //no signal
        return false;
    }
}