package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.Period.Fields;
import com.gamesbykevin.tradingbot.calculator.indicator.momentun.SO;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.EMA;

import java.util.List;


/**
 * stochastic oscillator / ema
 */
public class SOEMA extends Strategy {

    //how to access our indicator objects
    private static int INDEX_SO_SLOW;
    private static int INDEX_SO_FAST;
    private static int INDEX_EMA;

    //list of configurable values
    private static final int PERIODS_EMA = 20;
    private static final int PERIODS_SO_SLOW = 21;
    private static final int PERIODS_SO_SLOW_SMA = 4;
    private static final int PERIODS_SO_FAST = 5;
    private static final int PERIODS_SO_FAST_SMA = 2;
    private static final double OVER_SOLD = 20.0d;
    private static final double OVER_BOUGHT = 80.0d;

    public SOEMA() {
        this(PERIODS_EMA, PERIODS_SO_SLOW, PERIODS_SO_SLOW_SMA, PERIODS_SO_FAST, PERIODS_SO_FAST_SMA);
    }

    public SOEMA(int periodsEMA, int periodsSoSlow, int periodsSoSlowSMA, int periodsSoFast, int periodsSoFastSMA) {

        //call parent
        super(Key.SOEMA);

        //add our indicators
        INDEX_SO_SLOW = addIndicator(new SO(periodsSoSlow, periodsSoSlowSMA, 1));
        INDEX_SO_FAST = addIndicator(new SO(periodsSoFast, periodsSoFastSMA, 1));
        INDEX_EMA = addIndicator(new EMA(periodsEMA));
    }

    @Override
    public boolean hasBuySignal(Agent agent, List<Period> history, double price) {

        SO objSoFast = (SO)getIndicator(INDEX_SO_FAST);
        SO objSoSlow = (SO)getIndicator(INDEX_SO_SLOW);
        EMA objEMA = (EMA)getIndicator(INDEX_EMA);

        //first we check that both indicators are at the extreme opposite of each other
        if (getRecent(objSoFast.getStochasticOscillator()) <= OVER_SOLD &&
                getRecent(objSoSlow.getStochasticOscillator()) >= OVER_BOUGHT) {

            //if the price is above the ema average
            if (getRecent(history, Fields.Close) > getRecent(objEMA))
                return true;
        }

        //no signal found yet
        return false;
    }

    @Override
    public boolean hasSellSignal(Agent agent, List<Period> history, double price) {

        EMA objEMA = (EMA)getIndicator(INDEX_EMA);

        //if the price is below the ema average we have a signal
        if (getRecent(history, Fields.Close) < getRecent(objEMA))
            return true;

        //no signal found yet
        return false;
    }
}