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
    private static int INDEX_EMA_1;
    private static int INDEX_EMA_2;
    private static int INDEX_EMA_3;
    private static int INDEX_EMA_4;
    private static int INDEX_SMA_VOLUME;

    //list of configurable values
    private static final int PERIODS_EMA_1 = 9;
    private static final int PERIODS_EMA_2 = 13;
    private static final int PERIODS_EMA_3 = 21;
    private static final int PERIODS_EMA_4 = 55;
    private static final int PERIODS_SMA_VOLUME = 50;
    private static final int PERIODS_CONFIRM_DECREASE = 5;

    public EMAC() {
        this(PERIODS_EMA_1, PERIODS_EMA_2, PERIODS_EMA_3, PERIODS_EMA_4);
    }

    public EMAC(int ema1, int ema2, int ema3, int ema4) {

        //call parent
        super(Key.EMAC);

        //add our indicators
        INDEX_EMA_1 = addIndicator(new EMA(ema1));
        INDEX_EMA_2 = addIndicator(new EMA(ema2));
        INDEX_EMA_3 = addIndicator(new EMA(ema3));
        INDEX_EMA_4 = addIndicator(new EMA(ema4));
        INDEX_SMA_VOLUME = addIndicator(new SMA(PERIODS_SMA_VOLUME, Fields.Volume));
    }

    @Override
    public boolean hasBuySignal(Agent agent, List<Period> history, double currentPrice) {

        EMA ema1 = (EMA)getIndicator(INDEX_EMA_1);
        EMA ema2 = (EMA)getIndicator(INDEX_EMA_2);
        EMA ema3 = (EMA)getIndicator(INDEX_EMA_3);
        EMA ema4 = (EMA)getIndicator(INDEX_EMA_4);
        SMA smaVolume = (SMA)getIndicator(INDEX_SMA_VOLUME);

        double d1 = getRecent(ema1.getEma());
        double d2 = getRecent(ema2.getEma());
        double d3 = getRecent(ema3.getEma());
        double d4 = getRecent(ema4.getEma());

        //get the previous candle
        Period period = history.get(history.size() - 2);

        //we want the candle to engulf all the averages
        if (period.low < d4 && period.low < d3 && period.low < d2 && period.low < d1) {
            if (period.high > d4 && period.high > d3 && period.high > d2 && period.high > d1) {

                //our candle also needs wicks so the high is above open/close and low is below open/close
                if (period.high > period.close && period.high > period.open && period.low < period.open && period.low < period.close) {

                    //we also need volume
                    if (period.volume > getRecent(smaVolume.getSma())) {

                        //get the current candle
                        Period current = history.get(history.size() - 1);

                        //now the latest candle needs to close higher and we need volume
                        if (current.close > current.open && current.volume > getRecent(smaVolume.getSma()))
                            return true;
                    }
                }
            }
        }

        /*
        //if the short is above we are in an uptrend
        if (getRecent(emaShortObj) > getRecent(emaLongObj))
            return true;

        //if the short ema is in an upward trend, we will buy
        if (hasTrendUpward(emaShortObj.getEma(), PERIODS_CONFIRM_INCREASE))
            return true;
        */

        //no signal
        return false;
    }

    @Override
    public boolean hasSellSignal(Agent agent, List<Period> history, double currentPrice) {

        EMA ema1 = (EMA)getIndicator(INDEX_EMA_1);
        EMA ema2 = (EMA)getIndicator(INDEX_EMA_2);
        EMA ema3 = (EMA)getIndicator(INDEX_EMA_3);
        EMA ema4 = (EMA)getIndicator(INDEX_EMA_4);

        double d1 = getRecent(ema1.getEma());
        double d2 = getRecent(ema2.getEma());
        double d3 = getRecent(ema3.getEma());
        double d4 = getRecent(ema4.getEma());

        //if constantly going downward protect our investment
        if (hasTrendDownward(ema1.getEma(), PERIODS_CONFIRM_DECREASE))
            adjustHardStopPrice(agent, currentPrice);

        //if the candle closes below all ema's
        double close = history.get(history.size() - 1).close;
        if (close < d1 && close < d2 && close < d3 && close < d4)
            return true;

        //if longest ema is the least, let's exit
        if (d4 < d3 && d4 < d2 && d4 < d1)
            return true;

        /*
        EMA emaShortObj = (EMA)getIndicator(INDEX_EMA_SHORT);
        EMA emaLongObj = (EMA)getIndicator(INDEX_EMA_LONG);

        //if the short is below we should sell
        if (getRecent(emaShortObj) < getRecent(emaLongObj))
            return true;

        //if the short ema is in a downward trend, we will sell
        if (hasTrendDownward(emaShortObj.getEma(), PERIODS_CONFIRM_DECREASE))
            return true;

        //if the short is declining adjust our hard stop price
        if (getRecent(emaShortObj, 1) < getRecent(emaShortObj, 2))
            adjustHardStopPrice(agent, currentPrice);
        */

        //no signal
        return false;
    }
}