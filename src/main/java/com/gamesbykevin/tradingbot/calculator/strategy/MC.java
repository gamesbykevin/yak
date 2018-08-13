package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.MACD;

import java.util.List;

/**
 * MACD
 */
public class MC extends Strategy {

    //how to access our indicator objects
    private static int INDEX_MACD;

    //configurable values
    private static final int PERIODS_EMA_SHORT = 12;
    private static final int PERIODS_EMA_LONG = 26;
    private static final int PERIODS_EMA_SIGNAL = 9;

    public MC() {

        //call parent
        super(Key.MC);

        //add our indicator objects
        INDEX_MACD = addIndicator(new MACD(PERIODS_EMA_LONG, PERIODS_EMA_SHORT, PERIODS_EMA_SIGNAL));
    }

    @Override
    public boolean hasBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //access our indicators
        MACD objMacd = (MACD)getIndicator(INDEX_MACD);

        //if macd line goes below signal line
        if (getRecent(objMacd.getMacdLine(), 2) > getRecent(objMacd.getSignalLine(), 2) &&
                getRecent(objMacd.getMacdLine()) < getRecent(objMacd.getSignalLine())) {

            //if macd line below 0
            if (getRecent(objMacd.getMacdLine()) < 0)
                return true;
        }

        return false;
    }

    @Override
    public boolean hasSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //access our indicators
        MACD objMacd = (MACD)getIndicator(INDEX_MACD);

        //if macd line goes above signal line
        if (getRecent(objMacd.getMacdLine(), 2) < getRecent(objMacd.getSignalLine(), 2) &&
                getRecent(objMacd.getMacdLine()) > getRecent(objMacd.getSignalLine())) {

            //if macd line above 0
            if (getRecent(objMacd.getMacdLine()) > 0)
                return true;
        }

        return false;
    }
}