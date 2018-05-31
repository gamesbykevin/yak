package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.EMA;

import java.util.List;

import static com.gamesbykevin.tradingbot.calculator.strategy.StrategyHelper.hasTrendDownward;
import static com.gamesbykevin.tradingbot.calculator.strategy.StrategyHelper.hasTrendUpward;

/**
 * Exponential Moving Average / Cross
 */
public class EMAC extends Strategy {

    //how to access our indicator objects
    private static int INDEX_EMA_SHORT;
    private static int INDEX_EMA_LONG;

    //list of configurable values
    private static final int PERIODS_EMA_SHORT = 12;
    private static final int PERIODS_EMA_LONG = 26;
    private static final int PERIODS_CONFIRM_INCREASE = 4;
    private static final int PERIODS_CONFIRM_DECREASE = 3;

    public EMAC() {
        this(PERIODS_EMA_LONG, PERIODS_EMA_SHORT);
    }

    public EMAC(int emaPeriodsLong, int emaPeriodsShort) {

        //call parent
        super(Key.EMAC);

        //add our indicators
        INDEX_EMA_SHORT = addIndicator(new EMA(emaPeriodsShort));
        INDEX_EMA_LONG = addIndicator(new EMA(emaPeriodsLong));
    }

    @Override
    public boolean hasBuySignal(Agent agent, List<Period> history, double currentPrice) {

        EMA emaShortObj = (EMA)getIndicator(INDEX_EMA_SHORT);
        EMA emaLongObj = (EMA)getIndicator(INDEX_EMA_LONG);

        //if the short is above we are in an uptrend
        if (getRecent(emaShortObj) > getRecent(emaLongObj))
            return true;

        //if the short ema is in an upward trend we will buy
        if (hasTrendUpward(emaShortObj.getEma(), PERIODS_CONFIRM_INCREASE))
            return true;

        //no signal
        return false;
    }

    @Override
    public boolean hasSellSignal(Agent agent, List<Period> history, double currentPrice) {

        EMA emaShortObj = (EMA)getIndicator(INDEX_EMA_SHORT);
        EMA emaLongObj = (EMA)getIndicator(INDEX_EMA_LONG);

        //if the short is below we should sell
        if (getRecent(emaShortObj) < getRecent(emaLongObj))
            return true;

        //if the short ema is in a downward trend, we will sell
        if (hasTrendDownward(emaShortObj.getEma(), PERIODS_CONFIRM_DECREASE))
            return true;

        //if the short is declining adjust our hard stop price
        if (getRecent(emaShortObj, 1) < getRecent(emaShortObj, 2))
            adjustHardStopPrice(agent, currentPrice);

        //no signal
        return false;
    }
}