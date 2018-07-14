package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.SMA;
import com.gamesbykevin.tradingbot.calculator.indicator.volume.VWAP;

import java.util.List;

/**
 * Volume weighted average price / Simple Moving Average
 */
public class VS extends Strategy {

    //how to access our indicator objects
    private static int INDEX_VWAP;
    private static int INDEX_SMA;

    //configurable values
    private static final int PERIODS_SMA = 20;

    public VS() {

        //call parent
        super(Key.VS);

        //add our indicators
        INDEX_VWAP = addIndicator(new VWAP());
        INDEX_SMA = addIndicator(new SMA(PERIODS_SMA));
    }

    @Override
    public boolean hasBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //get our indicators
        VWAP vwap = (VWAP)getIndicator(INDEX_VWAP);
        SMA sma = (SMA)getIndicator(INDEX_SMA);

        //get the most recent period
        Period current = history.get(history.size() - 1);

        //if price is above the average price this is good
        if (current.close > getRecent(vwap.getVwap())) {

            //let's also make sure it closes above the sma as well
            if (current.close > getRecent(sma.getSma()))
                return true;
        }

        //no signal
        return false;
    }

    @Override
    public boolean hasSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //get our indicators
        VWAP vwap = (VWAP)getIndicator(INDEX_VWAP);
        SMA sma = (SMA)getIndicator(INDEX_SMA);

        //recent period
        Period current = history.get(history.size() - 1);

        //go short if closing below the sma
        if (current.close < getRecent(sma.getSma()))
            goShort(agent, current.low);

        //if it closes below the line, now is a good time to exit the trade
        if (current.close < getRecent(vwap.getVwap()))
            return true;

        //no signal
        return false;
    }
}