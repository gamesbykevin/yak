package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.EMA;
import com.gamesbykevin.tradingbot.calculator.indicator.volume.NVI;

import java.util.List;

import static com.gamesbykevin.tradingbot.calculator.strategy.StrategyHelper.hasTrendDownward;
import static com.gamesbykevin.tradingbot.calculator.strategy.StrategyHelper.hasTrendUpward;

/**
 * Negative Volume Index / Exponential Moving Average
 */
public class NVE extends Strategy {

    //how to access our indicator objects
    private static int INDEX_NVI;
    private static int INDEX_EMA;

    //configurable values
    private static final int PERIODS_CONFIRM_BULL = 3;
    private static final int PERIODS_CONFIRM_BEAR = 10;
    private static final int PERIODS_EMA = 100;

    public NVE() {

        //call parent
        super(Key.NVE);

        //add our indicator objects
        INDEX_NVI = addIndicator(new NVI());
        INDEX_EMA = addIndicator(new EMA(PERIODS_EMA));
    }

    @Override
    public boolean hasBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //get our indicator object(s)
        NVI nvi = (NVI)getIndicator(INDEX_NVI);
        EMA ema = (EMA)getIndicator(INDEX_EMA);

        Period period = history.get(history.size() - 1);

        //let's confirm bullish market
        if (getRecent(nvi.getCumulative()) > getRecent(nvi.getEma()) && period.close > getRecent(ema.getEma())) {

            //we want to make sure nvi is going upwards
            if (hasTrendUpward(nvi.getCumulative(), PERIODS_CONFIRM_BULL))
                return true;
        }

        //no signal
        return false;
    }

    @Override
    public boolean hasSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //get our indicator object(s)
        NVI nvi = (NVI)getIndicator(INDEX_NVI);

        //if bearish market we will exit
        if (getRecent(nvi.getCumulative()) < getRecent(nvi.getEma()))
            return true;

        //if nvi is going downwards this is not good
        if (hasTrendDownward(nvi.getCumulative(), PERIODS_CONFIRM_BEAR))
            return true;

        //no signal
        return false;
    }
}