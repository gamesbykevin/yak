package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.Period.Fields;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.EMA;

import java.util.List;

import static com.gamesbykevin.tradingbot.calculator.strategy.StrategyHelper.hasTrendDownward;
import static com.gamesbykevin.tradingbot.calculator.strategy.StrategyHelper.hasTrendUpward;

/**
 * Moving average crossover strategy
 */
public class MACS extends Strategy {

    //how to access our indicator objects
    private static int INDEX_EMA_FAST;
    private static int INDEX_EMA_SLOW;
    private static int INDEX_EMA_TREND;

    //list of configurable values
    private static final int PERIODS_EMA_FAST = 5;
    private static final int PERIODS_EMA_SLOW = 10;
    private static final int PERIODS_EMA_TREND = 50;
    private static final int PERIODS_CONFIRM = 3;

    public MACS() {
        this(PERIODS_EMA_FAST, PERIODS_EMA_SLOW, PERIODS_EMA_TREND);
    }

    public MACS(int fast, int slow, int trend) {

        //call parent
        super(Key.MACS);

        //add our indicators
        INDEX_EMA_TREND = addIndicator(new EMA(trend));
        INDEX_EMA_SLOW = addIndicator(new EMA(slow));
        INDEX_EMA_FAST = addIndicator(new EMA(fast));
    }

    @Override
    public boolean hasBuySignal(Agent agent, List<Period> history, double currentPrice) {

        EMA emaSlow = (EMA)getIndicator(INDEX_EMA_SLOW);
        EMA emaFast = (EMA)getIndicator(INDEX_EMA_FAST);
        EMA emaTrend = (EMA)getIndicator(INDEX_EMA_TREND);

        //current values
        double currEmaSlow = getRecent(emaSlow);
        double currEmaFast = getRecent(emaFast);
        double currEmaTrend = getRecent(emaTrend);

        //if we are below the trend but the fast is above the slow
        if (currEmaFast > currEmaSlow && currEmaSlow <= currEmaTrend) {

            //if the fast ema has an upward trend
            if (hasTrendUpward(emaFast.getEma(), DEFAULT_PERIODS_CONFIRM_INCREASE + 1))
                return true;
        }

        //no signal
        return false;
    }

    @Override
    public boolean hasSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //did we confirm downtrend?
        boolean downtrend = true;

        EMA emaSlow = (EMA)getIndicator(INDEX_EMA_SLOW);
        EMA emaFast = (EMA)getIndicator(INDEX_EMA_FAST);
        EMA emaTrend = (EMA)getIndicator(INDEX_EMA_TREND);

        //all have to be in down trend to sell
        if (!hasTrendDownward(emaSlow.getEma(), PERIODS_CONFIRM) ||
            !hasTrendDownward(emaTrend.getEma(), PERIODS_CONFIRM) ||
            !hasTrendDownward(emaFast.getEma(), PERIODS_CONFIRM)) {
            downtrend = false;
        }

        //do we have a downtrend?
        if (downtrend)
            return true;

        //adjust our hard stop price to protect our investment
        if (getRecent(emaFast) < getRecent(emaSlow))
            goShort(agent, getShortLow(history));

        //no signal
        return false;
    }
}