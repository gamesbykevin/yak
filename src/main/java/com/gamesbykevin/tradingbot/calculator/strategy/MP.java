package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.Period.Fields;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.MACD;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.PS;

import java.util.List;

/**
 * MACD / Parabolic SAR
 */
public class MP extends Strategy {

    //how to access our indicator objects
    private static int INDEX_MACD;
    private static int INDEX_PSAR;

    //configurable values
    private static final int PERIODS_MACD_EMA_SLOW = 26;
    private static final int PERIODS_MACD_EMA_FAST = 12;
    private static final int PERIODS_MACD_SIGNAL = 9;

    public MP() {

        //call parent
        super(Key.MP);

        //add our indicator objects
        INDEX_MACD = addIndicator(new MACD(PERIODS_MACD_EMA_SLOW, PERIODS_MACD_EMA_FAST, PERIODS_MACD_SIGNAL));
        INDEX_PSAR = addIndicator(new PS());
    }

    @Override
    public boolean hasBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //access our indicators
        MACD objMACD = (MACD)getIndicator(INDEX_MACD);
        PS objPSAR = (PS)getIndicator(INDEX_PSAR);

        //make sure PSAR has buy signal
        if (getRecent(objPSAR.getSar()) < getRecent(history, Fields.Low)) {

            //when MACD crosses above signal line
            if (getRecent(objMACD.getMacdLine(), 2) < getRecent(objMACD.getSignalLine(), 2) &&
                    getRecent(objMACD.getMacdLine()) > getRecent(objMACD.getSignalLine())) {
                return true;
            }
        }

        //no signal
        return false;
    }

    @Override
    public boolean hasSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //access our indicators
        MACD objMACD = (MACD)getIndicator(INDEX_MACD);
        PS objPSAR = (PS)getIndicator(INDEX_PSAR);

        //make sure PSAR has a sell signal
        if (getRecent(objPSAR.getSar()) > getRecent(history, Fields.High)) {

            //when MACD crosses below
            if (getRecent(objMACD.getMacdLine()) < getRecent(objMACD.getSignalLine()))
                return true;
        }

        //no signal
        return false;
    }
}