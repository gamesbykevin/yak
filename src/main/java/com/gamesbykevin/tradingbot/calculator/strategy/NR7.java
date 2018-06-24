package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.Period.Fields;
import com.gamesbykevin.tradingbot.calculator.indicator.volatility.NR;

import java.util.List;

public class NR7 extends Strategy {

    //how we access our indicator(s)
    private static int INDEX_NR;

    //configurable values
    private static final int PERIODS = 7;

    //if the price goes below this we will sell
    private double sellBreak = 0;

    //track the time of the current candle
    private long candleTime;

    public NR7() {

        //call parent
        super(Key.NR7);

        //add indicator(s)
        INDEX_NR = addIndicator(new NR(PERIODS));
    }

    @Override
    public boolean hasBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //obtain our indicator
        NR nr = (NR)getIndicator(INDEX_NR);

        long time = (long)getRecent(history, Fields.Time);

        //make sure the narrow range candle is the most recent
        if (nr.getNarrowRangeCandle().time == time) {

            //when the price breaks out above the high, we will buy
            if (currentPrice > nr.getNarrowRangeCandle().high) {
                candleTime = time;
                sellBreak = nr.getNarrowRangeCandle().low;
                return true;
            }
        }

        //no signal yet
        return false;
    }

    @Override
    public boolean hasSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //if we hit our sell price we will sell
        if (currentPrice <= sellBreak)
            return true;

        //if the candle does not match the period has ended and we sell
        if (candleTime != (long)getRecent(history, Fields.Time))
            return true;

        //no signal yet
        return false;
    }
}