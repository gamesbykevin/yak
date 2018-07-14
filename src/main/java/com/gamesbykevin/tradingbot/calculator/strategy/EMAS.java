package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.Period.Fields;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.EMA;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.SMA;

import java.util.List;

/**
 * Exponential Moving Average / Simple Moving Average
 */
public class EMAS extends Strategy {

    //how to access our indicator objects
    private static int INDEX_EMA_SHORT;
    private static int INDEX_EMA_LONG;
    private static int INDEX_SMA_SHORT;
    private static int INDEX_SMA_LONG;

    //our list of variations
    private static final int PERIODS_EMA_SHORT = 144;
    private static final int PERIODS_EMA_LONG = 169;
    private static final int PERIODS_SMA_SHORT = 5;
    private static final int PERIODS_SMA_LONG = 14;

    public EMAS() {

        //call parent
        super(Key.EMAS);

        //add our indicators
        INDEX_EMA_SHORT = addIndicator(new EMA(PERIODS_EMA_SHORT));
        INDEX_EMA_LONG = addIndicator(new EMA(PERIODS_EMA_LONG));
        INDEX_SMA_SHORT = addIndicator(new SMA(PERIODS_SMA_SHORT));
        INDEX_SMA_LONG = addIndicator(new SMA(PERIODS_SMA_LONG));
    }

    @Override
    public boolean hasBuySignal(Agent agent, List<Period> history, double currentPrice) {

        EMA emaShortObj = (EMA)getIndicator(INDEX_EMA_SHORT);
        EMA emaLongObj = (EMA)getIndicator(INDEX_EMA_LONG);
        SMA smaObjShort = (SMA)getIndicator(INDEX_SMA_SHORT);
        SMA smaObjLong = (SMA)getIndicator(INDEX_SMA_LONG);

        //make sure the short ema is above the long ema
        if (getRecent(emaShortObj) > getRecent(emaLongObj)) {

            //if the period closes above the short sma let's buy
            if (getRecent(history, Fields.Close) > getRecent(smaObjShort) && getRecent(smaObjShort) > getRecent(smaObjLong))
                return true;

        } else {

            //if the period closes above the short sma let's buy
            if (getRecent(history, Fields.Close) > getRecent(smaObjShort) && getRecent(smaObjShort) > getRecent(smaObjLong))
                return true;

        }

        //no signal
        return false;
    }

    @Override
    public boolean hasSellSignal(Agent agent, List<Period> history, double currentPrice) {

        EMA emaShortObj = (EMA)getIndicator(INDEX_EMA_SHORT);
        EMA emaLongObj = (EMA)getIndicator(INDEX_EMA_LONG);
        SMA smaObjShort = (SMA)getIndicator(INDEX_SMA_SHORT);
        SMA smaObjLong = (SMA)getIndicator(INDEX_SMA_LONG);

        //make sure the short ema is above the long ema
        if (getRecent(emaShortObj) > getRecent(emaLongObj)) {

            //if the candle closes below the long sma let's sell
            if (getRecent(history, Fields.Close) < getRecent(smaObjLong))
                return true;

        } else {

            //if the candle closes below the short sma let's go short
            if (getRecent(history, Fields.Close) < getRecent(smaObjShort))
                goShort(agent, getShortLow(history));
        }

        //no signal
        return false;
    }
}