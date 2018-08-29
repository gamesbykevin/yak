package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.SMA;

import java.util.List;

/**
 * Simple moving average
 */
public class SM extends Strategy {

    //how to access our indicator objects
    private static int INDEX_SMA_SHORT;
    private static int INDEX_SMA_LONG;

    //configurable values
    private static final int PERIODS_SMA_SHORT = 50;
    private static final int PERIODS_SMA_LONG = 200;

    public SM() {

        //call parent
        super(Key.SM);

        //add our indicator objects
        INDEX_SMA_SHORT = addIndicator(new SMA(PERIODS_SMA_SHORT));
        INDEX_SMA_LONG = addIndicator(new SMA(PERIODS_SMA_LONG));
    }

    @Override
    public boolean hasBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //access our indicators
        SMA objShort = (SMA)getIndicator(INDEX_SMA_SHORT);
        SMA objLong = (SMA)getIndicator(INDEX_SMA_LONG);

        if (getRecent(objShort.getSma(), 2) < getRecent(objLong.getSma(), 2) &&
                getRecent(objShort.getSma()) > getRecent(objLong.getSma())) {
            return true;
        }

        //no signal
        return false;
    }

    @Override
    public boolean hasSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //access our indicators
        SMA objShort = (SMA)getIndicator(INDEX_SMA_SHORT);
        SMA objLong = (SMA)getIndicator(INDEX_SMA_LONG);

        if (getRecent(objShort.getSma()) < getRecent(objLong.getSma()))
            return true;

        //no signal
        return false;
    }
}