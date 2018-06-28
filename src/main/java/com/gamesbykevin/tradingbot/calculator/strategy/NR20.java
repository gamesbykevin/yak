package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.Period.Fields;
import com.gamesbykevin.tradingbot.calculator.indicator.momentun.RSI;
import com.gamesbykevin.tradingbot.calculator.indicator.volatility.NR;

import java.util.List;

public class NR20 extends Strategy {

    //how we access our indicator(s)
    private static int INDEX_NR;
    private static int INDEX_RSI;

    //configurable values
    private static final int PERIODS = 20;

    //if the price goes below this we will sell
    private double sellBreak = 0;

    //track the time of the current candle
    private long candleTime;

    //is the stock oversold
    private static final float OVERSOLD = 30.0f;

    public NR20() {

        //call parent
        super(Key.NR20);

        //add indicator(s)
        INDEX_NR = addIndicator(new NR(PERIODS));
        INDEX_RSI = addIndicator(new RSI(PERIODS));
    }

    @Override
    public boolean hasBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //obtain our indicator
        NR nr = (NR)getIndicator(INDEX_NR);
        RSI rsi = (RSI)getIndicator(INDEX_RSI);

        //obtain the timestamp of the recent candle
        long time = (long)getRecent(history, Fields.Time);


        //we want the rsi level to be oversold
        if (getRecent(rsi.getValueRSI()) <= OVERSOLD) {

            //make sure the narrow range candle is the most recent
            if (nr.getNarrowRangeCandle().time == time) {

                //when the price breaks out above the high, we will buy
                if (currentPrice > nr.getNarrowRangeCandle().high) {
                    candleTime = time;
                    sellBreak = nr.getNarrowRangeCandle().low;
                    return true;
                }
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