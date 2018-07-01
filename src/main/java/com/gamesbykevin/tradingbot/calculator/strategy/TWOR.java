package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.Period.Fields;
import com.gamesbykevin.tradingbot.calculator.indicator.momentun.RSI;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.SMA;

import java.util.List;

/**
 * Two period RSI (Relative Strength Index)
 */
public class TWOR extends Strategy {

    //how to access our indicator objects
    private static int INDEX_RSI;
    private static int INDEX_SMA_LONG;

    //configurable values
    private static final int PERIODS_SMA_LONG = 50;
    private static final int PERIODS_RSI = 2;
    private static final float RSI_OVERBOUGHT = 90.0f;
    private static final float RSI_OVERSOLD = 10.0f;

    public TWOR() {

        //call parent
        super(Key.TWOR);

        //add our indicators
        INDEX_RSI = addIndicator(new RSI(PERIODS_RSI));
        INDEX_SMA_LONG = addIndicator(new SMA(PERIODS_SMA_LONG));
    }

    @Override
    public boolean hasBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //get indicators
        SMA objSmaLong = (SMA)getIndicator(INDEX_SMA_LONG);
        RSI objRSI = (RSI)getIndicator(INDEX_RSI);

        //bullish signal when closing above sma and the rsi is oversold
        if (getRecent(objRSI.getValueRSI()) <= RSI_OVERSOLD) {

            //if we are above the long sma let's buy
            if (getRecent(history, Fields.Close) > getRecent(objSmaLong))
                return true;
        }

        //no signal
        return false;
    }

    @Override
    public boolean hasSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //get indicator(s)
        SMA objSmaLong = (SMA)getIndicator(INDEX_SMA_LONG);
        RSI objRSI = (RSI)getIndicator(INDEX_RSI);

        //if the rsi is overbought, now is our time to sell
        if (getRecent(objRSI.getValueRSI()) >= RSI_OVERBOUGHT)
            return true;

        //if we are below the long sma let's go short
        if (getRecent(history, Fields.Close) < getRecent(objSmaLong))
            goShort(agent, getRecent(history, Fields.Low));

        //no signal
        return false;
    }
}