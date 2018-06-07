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

    private static final int PERIODS_SO_SLOW_RATE_BASIC = 21;
    private static final int PERIODS_SO_SLOW_RATE_FULL = 4;
    private static final int PERIODS_SO_SLOW_STOCHASTIC = 10;

    private static final int PERIODS_SO_FAST_RATE_BASIC = 5;
    private static final int PERIODS_SO_FAST_RATE_FULL = 2;
    private static final int PERIODS_SO_FAST_STOCHASTIC = 1;

    private static final double OVER_SOLD = 20.0d;
    private static final double OVER_BOUGHT = 80.0d;

    public SOEMA() {
        this(PERIODS_EMA,
            PERIODS_SO_SLOW_RATE_BASIC, PERIODS_SO_SLOW_RATE_FULL, PERIODS_SO_SLOW_STOCHASTIC,
            PERIODS_SO_FAST_RATE_BASIC, PERIODS_SO_FAST_RATE_FULL, PERIODS_SO_FAST_STOCHASTIC
        );
    }

    public SOEMA(int periodsEMA, int slowBasic, int slowFull, int slowSO, int fastBasic, int fastFull, int fastSO) {

        //call parent
        super(Key.SOEMA);

        //add our indicators
        INDEX_SO_SLOW = addIndicator(new SO(slowBasic, slowFull, slowSO));
        INDEX_SO_FAST = addIndicator(new SO(fastBasic, fastFull, fastSO));
        INDEX_EMA = addIndicator(new EMA(periodsEMA));
    }

    @Override
    public boolean hasBuySignal(Agent agent, List<Period> history, double price) {

        SO objSoFast = (SO)getIndicator(INDEX_SO_FAST);
        SO objSoSlow = (SO)getIndicator(INDEX_SO_SLOW);
        EMA objEMA = (EMA)getIndicator(INDEX_EMA);

        //latest values
        double fastSO = getRecent(objSoFast.getStochasticOscillator());
        double slowSO = getRecent(objSoSlow.getStochasticOscillator());

        //first we check that both indicators are at the extreme opposite of each other
        if (fastSO <= OVER_SOLD && slowSO >= OVER_BOUGHT || slowSO <= OVER_SOLD && fastSO >= OVER_BOUGHT) {

            double close1 = getRecent(history, Fields.Close, 1);
            double close2 = getRecent(history, Fields.Close, 2);
            double low3   = getRecent(history, Fields.Low, 3);
            double close4 = getRecent(history, Fields.Close, 4);

            double ema1 = getRecent(objEMA, 1);
            double ema2 = getRecent(objEMA, 2);
            double ema3 = getRecent(objEMA, 3);
            double ema4 = getRecent(objEMA, 4);

            //let's check to see if the close $ bounces off the ema
            if (close4 > ema4 && low3 < ema3 && close2 > ema2 && close1 > ema1)
                return true;
        }

        //no signal found yet
        return false;
    }

    @Override
    public boolean hasSellSignal(Agent agent, List<Period> history, double price) {

        EMA objEMA = (EMA)getIndicator(INDEX_EMA);
        SO objSoFast = (SO)getIndicator(INDEX_SO_FAST);
        SO objSoSlow = (SO)getIndicator(INDEX_SO_SLOW);

        //if the price is below the ema average we have a signal
        if (getRecent(history, Fields.Close) < getRecent(objEMA))
            adjustHardStopPrice(agent, price);

        //if both indicators are over sold, let's help protect our investment
        if (getRecent(objSoFast.getStochasticOscillator()) <= OVER_SOLD && getRecent(objSoSlow.getStochasticOscillator()) <= OVER_SOLD)
            adjustHardStopPrice(agent, price);

        //if both indicators are over bought, let's sell
        if (getRecent(objSoFast.getStochasticOscillator()) >= OVER_BOUGHT && getRecent(objSoSlow.getStochasticOscillator()) >= OVER_BOUGHT)
            return true;

        //no signal found yet
        return false;
    }
}