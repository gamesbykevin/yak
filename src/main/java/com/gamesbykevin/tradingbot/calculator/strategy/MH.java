package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.indicator.momentun.MFLI;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.HA;

import java.util.List;

/**
 * Money Flow Index / Heiken Ashi
 */
public class MH extends Strategy {

    //how to access our indicator objects
    private static int INDEX_MF;
    private static int INDEX_HA;

    //configurable values
    private static final int PERIODS_MF = 14;
    private static final float OVERBOUGHT = 80;
    private static final float OVERSOLD = 20;

    public MH() {

        //call parent
        super(Key.MH);

        //add our indicator objects
        INDEX_MF = addIndicator(new MFLI(PERIODS_MF));
        INDEX_HA = addIndicator(new HA());
    }

    @Override
    public boolean hasBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //access our indicators
        MFLI mf = (MFLI)getIndicator(INDEX_MF);
        HA ha = (HA)getIndicator(INDEX_HA);

        //we need to be in oversold territory
        if (getRecent(mf.getMoneyFlowIndex()) <= OVERSOLD) {

            //look at 2 periods
            Period current = ha.getHaPeriods().get(ha.getHaPeriods().size() - 1);
            Period previous = ha.getHaPeriods().get(ha.getHaPeriods().size() - 2);

            //we need the previous candle to be red and the current candle green
            if (ha.isBearish(previous) && ha.isBullish(current))
                return true;
        }

        //no signal
        return false;
    }

    @Override
    public boolean hasSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //access our indicators
        MFLI mf = (MFLI)getIndicator(INDEX_MF);
        HA ha = (HA)getIndicator(INDEX_HA);

        //we need to be overbought
        if (getRecent(mf.getMoneyFlowIndex()) >= OVERBOUGHT) {

            //get the current heiken ashi candle
            Period current = ha.getHaPeriods().get(ha.getHaPeriods().size() - 1);

            if (ha.isBearish(current)) {

                //if overbought and red candle, sell
                return true;

            } else if (ha.isBullish(current)) {

                //if overbought and green candle, protect investment
                goShort(agent, current.low);

            }
        }

        //no signal
        return false;
    }
}