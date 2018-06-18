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

        //did we confirm fast below slow
        boolean confirm = true;

        //let's check to confirm
        for (int i = 1; i <= DEFAULT_PERIODS_CONFIRM_DECREASE; i++) {

            //if the fast is above then things aren't down
            if (getRecent(emaFast, i) > getRecent(emaSlow, i)) {
                confirm = false;
                break;
            }
        }

        //if we confirm fast is below slow
        if (confirm) {

            //if the fast ema is downward let's try to buy now before it goes back up
            if (hasTrendDownward(emaFast.getEma(), DEFAULT_PERIODS_CONFIRM_DECREASE))
                return true;
        }

        //no signal
        return false;
    }

    @Override
    public boolean hasSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //get our indicators
        EMA emaFast = (EMA)getIndicator(INDEX_EMA_FAST);
        EMA emaSlow = (EMA)getIndicator(INDEX_EMA_SLOW);

        //if things are up
        if (getRecent(emaFast.getEma()) > getRecent(emaSlow.getEma())) {

            //but ema is heading downward, let's sell
            if (hasTrendDownward(emaFast.getEma(), DEFAULT_PERIODS_CONFIRM_DECREASE))
                return true;

        } else if (hasTrendUpward(emaFast.getEma(), DEFAULT_PERIODS_CONFIRM_INCREASE)) {

            //if the ema fast is constantly going up let's sell
            return true;
        }

        //no signal
        return false;
    }
}