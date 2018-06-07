package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.Period.Fields;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.EMA;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.SMA;

import java.util.List;

import static com.gamesbykevin.tradingbot.calculator.strategy.StrategyHelper.hasTrendDownward;
import static com.gamesbykevin.tradingbot.calculator.strategy.StrategyHelper.hasTrendUpward;

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
    private static final int PERIODS_EMA_LONG = 34;
    private static final int PERIODS_EMA_SHORT = 13;
    private static final int PERIODS_SMA_LONG = 200;
    private static final int PERIODS_SMA_SHORT = 50;

    public EMAS() {
        this(PERIODS_EMA_LONG, PERIODS_EMA_SHORT, PERIODS_SMA_LONG, PERIODS_SMA_SHORT);
    }

    public EMAS(int emaLong, int emaShort, int smaLong, int smaShort) {

        //call parent
        super(Key.EMAS);

        //add our indicators
        INDEX_EMA_SHORT = addIndicator(new EMA(emaShort));
        INDEX_EMA_LONG = addIndicator(new EMA(emaLong));
        INDEX_SMA_SHORT = addIndicator(new SMA(smaShort));
        INDEX_SMA_LONG = addIndicator(new SMA(smaLong));
    }

    @Override
    public boolean hasBuySignal(Agent agent, List<Period> history, double currentPrice) {

        EMA emaShortObj = (EMA)getIndicator(INDEX_EMA_SHORT);
        EMA emaLongObj = (EMA)getIndicator(INDEX_EMA_LONG);
        SMA smaObjShort = (SMA)getIndicator(INDEX_SMA_SHORT);
        SMA smaObjLong = (SMA)getIndicator(INDEX_SMA_LONG);

        //our current values
        double currEmaShort = getRecent(emaShortObj.getEma());
        double currEmaLong = getRecent(emaLongObj.getEma());
        double currSmaShort = getRecent(smaObjShort.getSma());
        double currSmaLong = getRecent(smaObjLong.getSma());

        //the short ema needs to be above the long ema
        if (currEmaShort > currEmaLong) {

            //we also need the short sma to be above the long sma
            if (currSmaShort > currSmaLong)
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

        //recent closing $
        final double close = getRecent(history, Fields.Close);

        //our current values
        double currEmaShort = getRecent(emaShortObj.getEma());
        double currEmaLong = getRecent(emaLongObj.getEma());
        double currSmaShort = getRecent(smaObjShort.getSma());
        double currSmaLong = getRecent(smaObjLong.getSma());

        //if the ema short went below the ema long
        if (currEmaShort < currEmaLong)
            return true;

        //if we confirmed there is a downward trend, we should sell
        if (hasTrendDownward(emaShortObj.getEma(), DEFAULT_PERIODS_CONFIRM_DECREASE))
            return true;

        //no signal
        return false;
    }
}