package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.Period.Fields;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.ADX;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.MACD;

import java.util.List;

/**
 * Moving Average Convergence Divergence / Average Directional Index
 */
public class MA extends Strategy {

    //how to access our indicator objects
    private static int INDEX_MACD;
    private static int INDEX_ADX;

    //configurable values
    private static final int PERIODS_ADX = 18;
    private static final int PERIODS_EMA_SHORT = 3;
    private static final int PERIODS_EMA_LONG = 10;
    private static final int PERIODS_EMA_SIGNAL = 18;

    public MA() {

        //call parent
        super(Key.MA);

        //add our indicator objects
        INDEX_ADX = addIndicator(new ADX(PERIODS_ADX));
        INDEX_MACD = addIndicator(new MACD(PERIODS_EMA_LONG, PERIODS_EMA_SHORT, PERIODS_EMA_SIGNAL));
    }

    @Override
    public boolean hasBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //access our indicators
        MACD objMacd = (MACD)getIndicator(INDEX_MACD);
        ADX objAdx = (ADX)getIndicator(INDEX_ADX);

        //if the macd is positive and the dm+ is > dm- enter long
        if (getRecent(objMacd.getMacdLine()) > 0 && getRecent(objAdx.getDmPlusIndicator()) > getRecent(objAdx.getDmMinusIndicator()))
            return true;

        //no signal
        return false;
    }

    @Override
    public boolean hasSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //access our indicators
        MACD objMacd = (MACD)getIndicator(INDEX_MACD);
        ADX objAdx = (ADX)getIndicator(INDEX_ADX);

        //if the macd is negative and the dm+ is < dm- go short
        if (getRecent(objMacd.getMacdLine()) < 0 && getRecent(objAdx.getDmPlusIndicator()) < getRecent(objAdx.getDmMinusIndicator()))
            goShort(agent, getShortLow(history));

        //no signal
        return false;
    }
}