package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.Period.Fields;
import com.gamesbykevin.tradingbot.calculator.indicator.momentun.SO;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.EMA;

import java.util.List;

/**
 * Stochastic Oscillator / 2 EMA's
 */
public class SEMAS extends Strategy {

    //how to access our indicator objects
    private static int INDEX_SO;
    private static int INDEX_EMA_FAST;
    private static int INDEX_EMA_SLOW;

    //list of configurable values
    private static final int PERIODS_EMA_FAST = 2;
    private static final int PERIODS_EMA_SLOW = 4;
    private static final int PERIODS_SO_BASIC = 5;
    private static final int PERIODS_SO_FULL = 3;
    private static final int PERIODS_SO_STOCHASTIC = 3;
    private static final double LINE = 50.0d;
    private static final double OVER_BOUGHT = 70.0d;

    public SEMAS() {

        //call parent
        super(Key.SEMAS);

        //add our indicators
        INDEX_SO        = addIndicator(new SO(PERIODS_SO_BASIC, PERIODS_SO_FULL, PERIODS_SO_STOCHASTIC));
        INDEX_EMA_FAST  = addIndicator(new EMA(PERIODS_EMA_FAST));
        INDEX_EMA_SLOW  = addIndicator(new EMA(PERIODS_EMA_SLOW));
    }

    @Override
    public boolean hasBuySignal(Agent agent, List<Period> history, double price) {

        SO so       = (SO)getIndicator(INDEX_SO);
        EMA emaFast = (EMA)getIndicator(INDEX_EMA_FAST);
        EMA emaSlow = (EMA)getIndicator(INDEX_EMA_SLOW);

        //make sure indicator is below the line
        if (getRecent(so.getStochasticOscillator()) < LINE) {

            //let's also make sure there is a bullish trend
            if (getRecent(emaFast.getEma()) > getRecent(emaSlow.getEma()))
                return true;
        }

        //no signal found yet
        return false;
    }

    @Override
    public boolean hasSellSignal(Agent agent, List<Period> history, double price) {

        SO so       = (SO)getIndicator(INDEX_SO);
        EMA emaFast = (EMA)getIndicator(INDEX_EMA_FAST);
        EMA emaSlow = (EMA)getIndicator(INDEX_EMA_SLOW);

        //protect investment
        if (getRecent(so.getStochasticOscillator()) >= OVER_BOUGHT)
            goShort(agent, getRecent(history, Fields.Low));

        //if overbought and goes below
        if (getRecent(so.getStochasticOscillator(),2) > OVER_BOUGHT && getRecent(so.getStochasticOscillator(),1) < OVER_BOUGHT)
            return true;

        //if ema crosses and we are in over bought territory
        if (getRecent(emaFast.getEma()) < getRecent(emaSlow.getEma()) && getRecent(so.getStochasticOscillator()) > OVER_BOUGHT)
            return true;

        //no signal found yet
        return false;
    }
}