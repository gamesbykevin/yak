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
    private static final int PERIODS_EMA_FAST = 12;
    private static final int PERIODS_EMA_SLOW = 26;

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

        //let's check to confirm
        for (int i = 1; i <= DEFAULT_PERIODS_CONFIRM_DECREASE; i++) {

            //get the current fast & slow ema lines
            double fast1 = getRecent(emaFast, i);
            double slow1 = getRecent(emaSlow, i);

            //make sure ema fast is below ema slow
            if (fast1 < slow1) {

                //look at the previous period's values
                double fast2 = getRecent(emaFast, i + 1);
                double slow2 = getRecent(emaSlow, i + 1);

                //if the difference of the current ema's is greater than the previous then the downtrend is getting stronger
                if (slow1 - fast1 > slow2 - fast2)
                    return false;

            } else {

                //if fast is above slow we are too late
                return false;
            }
        }

        //we have signal
        return true;
    }

    @Override
    public boolean hasSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //get our indicators
        EMA emaFast = (EMA)getIndicator(INDEX_EMA_FAST);
        EMA emaSlow = (EMA)getIndicator(INDEX_EMA_SLOW);

        if (getRecent(emaFast.getEma()) > getRecent(emaSlow.getEma())) {

            //if ema is bullish, but heading downward, let's sell
            if (hasTrendDownward(emaFast.getEma(), DEFAULT_PERIODS_CONFIRM_DECREASE))
                return true;

        } else {

            //if the previous was above and now we are below let's go short
            if (getRecent(emaFast.getEma(), 2) > getRecent(emaSlow.getEma(), 2))
                goShort(agent);

        }

        //no signal
        return false;
    }
}