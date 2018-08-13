package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.indicator.momentun.SO;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.MACD;

import java.util.List;

/**
 * MACD / Stochastic Oscillator
 */
public class MS extends Strategy {

    //how to access our indicator objects
    private static int INDEX_MACD;
    private static int INDEX_SO;

    //configurable values
    private static final int PERIODS_MACD_EMA_FAST = 12;
    private static final int PERIODS_MACD_EMA_SLOW = 26;
    private static final int PERIODS_MACD_SIGNAL = 2;

    private static final int PERIODS_SO_BASIC = 5;
    private static final int PERIODS_SO_FULL = 3;
    private static final int PERIODS_SO_STOCHASTIC = 3;

    private static final float STOCHASTIC_BUY = 20.0f;
    private static final float STOCHASTIC_SELL = 80.0f;

    public MS() {

        //call parent
        super(Key.MS);

        //add our indicator objects
        INDEX_MACD = addIndicator(new MACD(PERIODS_MACD_EMA_SLOW, PERIODS_MACD_EMA_FAST, PERIODS_MACD_SIGNAL));
        INDEX_SO = addIndicator(new SO(PERIODS_SO_BASIC, PERIODS_SO_FULL, PERIODS_SO_STOCHASTIC));
    }

    @Override
    public boolean hasBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //access our indicators
        MACD objMACD = (MACD)getIndicator(INDEX_MACD);
        SO objSO = (SO)getIndicator(INDEX_SO);

        //MACD above 0
        if (getRecent(objMACD.getMacdLine()) > 0) {

            //if we are below 20 and go above it, we have a buy signal
            if (getRecent(objSO.getStochasticOscillator(), 2) < STOCHASTIC_BUY &&
                    getRecent(objSO.getStochasticOscillator()) > STOCHASTIC_BUY) {
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
        SO objSO = (SO)getIndicator(INDEX_SO);

        //MACD below 0
        if (getRecent(objMACD.getMacdLine()) < 0) {

            //if we are above 80 and go below it, we have a sell signal
            if (getRecent(objSO.getStochasticOscillator(), 2) > STOCHASTIC_SELL &&
                    getRecent(objSO.getStochasticOscillator()) > STOCHASTIC_SELL) {
                return true;
            }
        }

        //no signal
        return false;
    }
}