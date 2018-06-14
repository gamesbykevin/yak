package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
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
    private static int INDEX_SMA_SHORT;

    //configurable values
    private static final int PERIODS_SMA_LONG = 200;
    private static final int PERIODS_SMA_SHORT = 5;
    private static final int PERIODS_RSI = 2;
    private static final float RSI_OVERBOUGHT = 95.0f;
    private static final float RSI_OVERSOLD = 5.0f;

    public TWOR() {

        //call parent
        super(Key.TWOR);

        //add our indicators
        INDEX_RSI = addIndicator(new RSI(PERIODS_RSI));
        INDEX_SMA_LONG = addIndicator(new SMA(PERIODS_SMA_LONG));
        INDEX_SMA_SHORT = addIndicator(new SMA(PERIODS_SMA_SHORT));
    }

    @Override
    public boolean hasBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //get indicators
        SMA objSmaLong = (SMA)getIndicator(INDEX_SMA_LONG);
        SMA objSmaShort = (SMA)getIndicator(INDEX_SMA_SHORT);
        RSI objRSI = (RSI)getIndicator(INDEX_RSI);

        //get the recent period
        Period period = history.get(history.size() - 1);

        //bullish signal when closing above sma and the rsi is oversold
        if (getRecent(objRSI.getValueRSI()) <= RSI_OVERSOLD) {

            //we want to close above the long sma, but below the short sma
            if (period.close > getRecent(objSmaLong.getSma()) && period.close < getRecent(objSmaShort.getSma()))
                return true;
        }

        //no signal
        return false;
    }

    @Override
    public boolean hasSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //get indicator(s)
        SMA objSmaShort = (SMA)getIndicator(INDEX_SMA_SHORT);

        //get the recent period
        Period period = history.get(history.size() - 1);

        //if we close above the short sma, let's sell while we are ahead
        if (period.close > getRecent(objSmaShort.getSma()))
            return true;

        //no signal
        return false;
    }
}