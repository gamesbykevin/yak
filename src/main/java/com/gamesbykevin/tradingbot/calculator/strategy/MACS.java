package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.EMA;

import java.util.List;
import static com.gamesbykevin.tradingbot.trade.TradeHelper.ReasonSell;

/**
 * Moving average crossover strategy
 */
public class MACS extends Strategy {

    //how to access our indicator objects
    private static int INDEX_EMA_FAST;
    private static int INDEX_EMA_SLOW;
    private static int INDEX_EMA_TREND;

    //list of configurable values
    private static final int PERIODS_MACS_FAST = 5;
    private static final int PERIODS_MACS_SLOW = 10;
    private static final int PERIODS_MACS_TREND = 50;
    private static final int PERIODS_CONFIRM = 3;

    private final int confirm;

    public MACS() {
        this(PERIODS_MACS_FAST, PERIODS_MACS_SLOW, PERIODS_MACS_TREND, PERIODS_CONFIRM);
    }

    public MACS(int fast, int slow, int trend, int confirm) {

        //call parent
        super(Key.MACS);

        //add our indicators
        INDEX_EMA_TREND = addIndicator(new EMA(trend));
        INDEX_EMA_SLOW = addIndicator(new EMA(slow));
        INDEX_EMA_FAST = addIndicator(new EMA(fast));

        //store our value
        this.confirm = confirm;
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

        /*
        //make sure the fast just crossed above the slow
        if (prevEmaFast < prevEmaSlow && currEmaFast > currEmaSlow) {

            //we also want the slow to be above the trend
            if (currEmaSlow > currEmaTrend) {

                //last thing we check is that all ema values are going in the correct direction
                if (prevEmaSlow < currEmaSlow && prevEmaFast  < currEmaFast && prevEmaTrend < currEmaTrend)
                    return true;
            }
        }
        */

        if (currEmaFast > currEmaSlow && currEmaSlow > currEmaTrend)
            return true;

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

        //we should sell if every value is trending down even if they haven't crossed
        for (int count = 1; count <= confirm; count++) {

            //if the previous ema period is less than the current we can't confirm downtrend
            if (getRecent(emaSlow, count + 1) < getRecent(emaSlow, count)) {
                downtrend = false;
                break;
            } else if (getRecent(emaTrend, count + 1) < getRecent(emaTrend, count)) {
                downtrend = false;
                break;
            } else if (getRecent(emaFast, count + 1) < getRecent(emaFast, count)) {
                downtrend = false;
                break;
            }
        }

        //do we have a downtrend?
        if (downtrend)
            return true;

        //if our fast value is below the slow and trend let's sell
        if (getRecent(emaFast) < getRecent(emaSlow) && getRecent(emaFast) < getRecent(emaTrend))
            return true;

        //adjust our hard stop price to protect our investment
        if (getRecent(emaFast) < getRecent(emaSlow) || getRecent(emaFast) < getRecent(emaTrend))
            adjustHardStopPrice(agent, currentPrice);

        //no signal
        return false;
    }
}