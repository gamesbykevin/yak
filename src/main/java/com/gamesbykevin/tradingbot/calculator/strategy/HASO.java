package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.Period.Fields;
import com.gamesbykevin.tradingbot.calculator.indicator.momentun.SO;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.HA;

import java.util.List;

/**
 * Heiken Ashi / Stochastic Oscillator
 */
public class HASO extends Strategy {

    //how to access our indicator objects
    private static int INDEX_HA;
    private static int INDEX_SO;

    //configurable values
    private static final int PERIODS_MARKET_RATE = 14;
    private static final int PERIODS_MARKET_RATE_SMA = 7;
    private static final int PERIODS_STOCHASTIC_SMA = 3;
    private static final int HA_PERIODS = 10;
    private static final float STOCHASTIC_MAX = 70.0f;
    private static final float STOCHASTIC_MIN = 30.0f;

    public HASO() {
        this(PERIODS_MARKET_RATE, PERIODS_MARKET_RATE_SMA, PERIODS_STOCHASTIC_SMA, HA_PERIODS);
    }

    public HASO(int periodsMarketRate, int periodsMarketRateSma, int periodsStochasticSma, int periodsHa) {

        //call parent
        super(Key.HASO);

        //add our indicators
        INDEX_HA = addIndicator(new HA());
        INDEX_SO = addIndicator(new SO(periodsMarketRate, periodsMarketRateSma, periodsStochasticSma));
    }

    @Override
    public boolean hasBuySignal(Agent agent, List<Period> history, double currentPrice) {

        HA objHA = (HA)getIndicator(INDEX_HA);
        SO objSO = (SO)getIndicator(INDEX_SO);

        //get the most recent ha candles
        Period curr = objHA.getHaPeriods().get(objHA.getHaPeriods().size() - 1);
        Period prev = objHA.getHaPeriods().get(objHA.getHaPeriods().size() - 2);

        //if the 2 recent candles are bullish
        if (objHA.isBullish(prev) && objHA.isBullish(curr)) {

            //if our stochastic indicator is low, let's buy
            if (getRecent(objSO.getStochasticOscillator()) <= STOCHASTIC_MIN)
                return true;
        }

        //no signal
        return false;
    }

    @Override
    public boolean hasSellSignal(Agent agent, List<Period> history, double currentPrice) {

        HA objHA = (HA)getIndicator(INDEX_HA);
        SO objSO = (SO)getIndicator(INDEX_SO);

        //get the most recent ha candles
        Period curr = objHA.getHaPeriods().get(objHA.getHaPeriods().size() - 1);
        Period prev = objHA.getHaPeriods().get(objHA.getHaPeriods().size() - 2);

        //if the last 2 candles are bearish
        if (objHA.isBearish(prev) && objHA.isBearish(curr)) {

            //protect investment
            goShort(agent, getShortLow(history));

            //if our stochastic indicator is high, let's sell
            if (getRecent(objSO.getStochasticOscillator()) >= STOCHASTIC_MAX)
                return true;
        }

        //no signal
        return false;
    }
}