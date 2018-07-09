package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.Period.Fields;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.ADX;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.EMA;

import java.util.List;

/**
 * Two period ADX (Average Directional Index)
 */
public class TWOA extends Strategy {

    //how to access our indicator objects
    private static int INDEX_ADX;
    private static int INDEX_EMA;

    //configurable values
    private static final int PERIODS_ADX = 2;
    private static final int PERIODS_EMA = 20;
    private static final float ADX_TREND = 25.0f;

    public TWOA() {

        //call parent
        super(Key.TWOA);

        //add our indicators
        INDEX_ADX = addIndicator(new ADX(PERIODS_ADX));
        INDEX_EMA = addIndicator(new EMA(PERIODS_EMA));
    }

    @Override
    public boolean hasBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //get indicators
        ADX adx = (ADX)getIndicator(INDEX_ADX);
        EMA ema = (EMA)getIndicator(INDEX_EMA);

        if (getRecent(adx.getAdx()) < ADX_TREND) {

            //make sure price is above the sma
            if (getRecent(history, Fields.Close) > getRecent(ema.getEma()))
                return true;
        }

        //no signal
        return false;
    }

    @Override
    public boolean hasSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //get indicators
        ADX adx = (ADX)getIndicator(INDEX_ADX);
        EMA ema = (EMA)getIndicator(INDEX_EMA);

        //get the recent period
        double closeCurr = history.get(history.size() - 1).close;

        if (getRecent(adx.getAdx()) < ADX_TREND) {

            //if price is below the ema
            if (closeCurr < getRecent(ema.getEma()))
                return true;
        }

        //no signal
        return false;
    }
}