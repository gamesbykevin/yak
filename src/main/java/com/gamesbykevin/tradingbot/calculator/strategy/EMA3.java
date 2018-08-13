package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.Period.Fields;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.EMA;

import java.util.List;

/**
 * 3 EMA's
 */
public class EMA3 extends Strategy {

    //how to access our indicator objects
    private static int INDEX_EMA_1;
    private static int INDEX_EMA_2;
    private static int INDEX_EMA_3;

    //configurable values
    private static final int PERIODS_EMA_1 = 10;
    private static final int PERIODS_EMA_2 = 25;
    private static final int PERIODS_EMA_3 = 50;

    public EMA3() {

        //call parent
        super(Key.EMA3);

        //add our indicator objects
        INDEX_EMA_1 = addIndicator(new EMA(PERIODS_EMA_1));
        INDEX_EMA_2 = addIndicator(new EMA(PERIODS_EMA_2));
        INDEX_EMA_3 = addIndicator(new EMA(PERIODS_EMA_3));
    }

    @Override
    public boolean hasBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //access our indicators
        EMA objEma1 = (EMA)getIndicator(INDEX_EMA_1);
        EMA objEma2 = (EMA)getIndicator(INDEX_EMA_2);
        EMA objEma3 = (EMA)getIndicator(INDEX_EMA_3);

        //if the fast ema is above the longer ema's
        if (getRecent(objEma1) > getRecent(objEma2) && getRecent(objEma1) > getRecent(objEma3)) {

            //if the recent candle closes above the longest ema
            if (getRecent(history, Fields.Close) > getRecent(objEma3))
                return true;
        }

        //no signal
        return false;
    }

    @Override
    public boolean hasSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //access our indicators
        EMA objEma1 = (EMA)getIndicator(INDEX_EMA_1);
        EMA objEma2 = (EMA)getIndicator(INDEX_EMA_2);
        EMA objEma3 = (EMA)getIndicator(INDEX_EMA_3);

        //if we go below we sell
        if (getRecent(objEma1) < getRecent(objEma2) || getRecent(objEma1) < getRecent(objEma3))
            return true;

        //no signal
        return false;
    }
}