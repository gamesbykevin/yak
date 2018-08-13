package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.Period.Fields;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.EMA;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.SMA;
import sun.management.counter.perf.PerfInstrumentation;

import java.util.List;

import static com.gamesbykevin.tradingbot.calculator.strategy.StrategyHelper.hasTrendDownward;
import static com.gamesbykevin.tradingbot.calculator.strategy.StrategyHelper.hasTrendUpward;

/**
 * Exponential Moving Average / Cross
 */
public class EMAC extends Strategy {

    //how to access our indicator objects
    private static int INDEX_EMA_FAST;
    private static int INDEX_EMA_SLOW;

    //list of configurable values
    private static final int PERIODS_EMA_FAST = 50;
    private static final int PERIODS_EMA_SLOW = 200;

    public EMAC() {
        this(PERIODS_EMA_FAST, PERIODS_EMA_SLOW);
    }

    public EMAC(int emaFast, int emaSlow) {

        //call parent
        super(Key.EMAC);

        //add our indicators
        INDEX_EMA_FAST = addIndicator(new EMA(emaFast));
        INDEX_EMA_SLOW = addIndicator(new EMA(emaSlow));
    }

    @Override
    public boolean hasBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //get our indicators
        EMA emaFast = (EMA)getIndicator(INDEX_EMA_FAST);
        EMA emaSlow = (EMA)getIndicator(INDEX_EMA_SLOW);

        if (getRecent(emaFast, 2) < getRecent(emaSlow, 2) && getRecent(emaFast) > getRecent(emaSlow))
            return true;

        //no signal
        return false;
    }

    @Override
    public boolean hasSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //get our indicators
        EMA emaFast = (EMA)getIndicator(INDEX_EMA_FAST);
        EMA emaSlow = (EMA)getIndicator(INDEX_EMA_SLOW);

        //if fast ema is constantly going down, let's short
        if (hasTrendDownward(emaFast.getEma(), DEFAULT_PERIODS_CONFIRM_DECREASE))
            goShort(agent, getShortLow(history));

        if (getRecent(emaFast) < getRecent(emaSlow))
            return true;

        //no signal
        return false;
    }
}