package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.EMA;
import com.gamesbykevin.tradingbot.calculator.indicator.volume.PVI;

import java.util.List;

import static com.gamesbykevin.tradingbot.calculator.strategy.StrategyHelper.hasTrendDownward;
import static com.gamesbykevin.tradingbot.calculator.strategy.StrategyHelper.hasTrendUpward;

/**
 * Positive Volume Index / Exponential Moving Average
 */
public class PVE extends Strategy {

    //how to access our indicator objects
    private static int INDEX_PVI;
    private static int INDEX_EMA;

    //configurable values
    private static final int PERIODS_CONFIRM_BULL = 3;
    private static final int PERIODS_CONFIRM_BEAR = 10;
    private static final int PERIODS_EMA = 100;

    public PVE() {

        //call parent
        super(Key.PVE);

        //add our indicator objects
        INDEX_PVI = addIndicator(new PVI());
        INDEX_EMA = addIndicator(new EMA(PERIODS_EMA));
    }

    @Override
    public boolean hasBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //get our indicator object(s)
        PVI pvi = (PVI)getIndicator(INDEX_PVI);
        EMA ema = (EMA)getIndicator(INDEX_EMA);

        Period period = history.get(history.size() - 1);

        //let's confirm bullish market
        if (getRecent(pvi.getCumulative()) > getRecent(pvi.getEma()) && period.close > getRecent(ema.getEma())) {

            //we want to make sure pvi is going upwards
            if (hasTrendUpward(pvi.getCumulative(), PERIODS_CONFIRM_BULL))
                return true;
        }

        //no signal
        return false;
    }

    @Override
    public boolean hasSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //get our indicator object(s)
        PVI pvi = (PVI)getIndicator(INDEX_PVI);

        //if bearish market we will exit
        if (getRecent(pvi.getCumulative()) < getRecent(pvi.getEma()))
            return true;

        //if pvi is going downwards this is not good
        if (hasTrendDownward(pvi.getCumulative(), PERIODS_CONFIRM_BEAR))
            return true;

        //no signal
        return false;
    }
}