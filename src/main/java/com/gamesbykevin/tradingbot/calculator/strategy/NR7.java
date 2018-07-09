package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.Period.Fields;
import com.gamesbykevin.tradingbot.calculator.indicator.momentun.RSI;
import com.gamesbykevin.tradingbot.calculator.indicator.volatility.NR;
import com.gamesbykevin.tradingbot.trade.Trade;

import java.util.List;

public class NR7 extends Strategy {

    //how we access our indicator(s)
    private static int INDEX_NR;
    private static int INDEX_RSI;

    //configurable values
    private static final int PERIODS = 7;

    //if the price goes below this we will sell
    private double sellBreak = 0;

    //track the time of the current candle
    private long candleTime;

    //is the stock oversold
    private static final float OVERSOLD = 30.0f;

    public NR7() {

        //call parent
        super(Key.NR7);

        //add indicator(s)
        INDEX_NR = addIndicator(new NR(PERIODS));
        INDEX_RSI = addIndicator(new RSI(PERIODS));
    }

    @Override
    public boolean hasBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //obtain our indicator
        NR nr = (NR)getIndicator(INDEX_NR);
        RSI rsi = (RSI)getIndicator(INDEX_RSI);

        //we want the rsi level to be oversold
        if (getRecent(rsi.getValueRSI()) <= OVERSOLD) {

            //when the price breaks out above the high, we will buy
            if (currentPrice > nr.getNarrowRangeCandle().high) {
                candleTime = history.get(history.size() - 1).time;
                sellBreak = nr.getNarrowRangeCandle().low;
                return true;
            }
        }

        //no signal yet
        return false;
    }

    @Override
    public boolean hasSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //if the candle does not match the period has ended and we sell
        if (candleTime != history.get(history.size() - 1).time)
            return true;

        //get the current trade
        Trade trade = agent.getTrade();

        //confirm the $ is below the sell break $
        boolean confirm = true;

        //make sure enough $'s are below the sell break to trigger a sell
        for (int index = trade.getPriceHistory().length - 5; index < trade.getPriceHistory().length; index++) {

            if (trade.getPriceHistory()[index] == 0 || trade.getPriceHistory()[index] > sellBreak)
                confirm = false;
        }

        //make sure we confirmed the price
        if (confirm)
            return true;

        //no signal yet
        return false;
    }
}