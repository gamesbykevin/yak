package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.Period.Fields;
import com.gamesbykevin.tradingbot.calculator.indicator.momentun.RSI;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.EMA;

import java.util.List;

import static com.gamesbykevin.tradingbot.calculator.strategy.StrategyHelper.hasTrendDownward;
import static com.gamesbykevin.tradingbot.calculator.strategy.StrategyHelper.hasTrendUpward;

/**
 * Multiple EMA / Relative Strength Index
 */
public class MER extends Strategy {

    //how to access our indicator objects
    private static int INDEX_EMA_1;
    private static int INDEX_EMA_2;
    private static int INDEX_EMA_3;
    private static int INDEX_EMA_4;
    private static int INDEX_EMA_5;
    private static int INDEX_RSI;

    //configurable values
    private static final int PERIODS_EMA_1 = 3;
    private static final int PERIODS_EMA_2 = 5;
    private static final int PERIODS_EMA_3 = 13;
    private static final int PERIODS_EMA_4 = 21;
    private static final int PERIODS_EMA_5 = 80;
    private static final int PERIODS_RSI = 14;
    private static final float RSI_LINE = 50.0f;

    public MER() {

        //call parent
        super(Key.MER);

        //add our indicators
        INDEX_EMA_1 = addIndicator(new EMA(PERIODS_EMA_1));
        INDEX_EMA_2 = addIndicator(new EMA(PERIODS_EMA_2));
        INDEX_EMA_3 = addIndicator(new EMA(PERIODS_EMA_3));
        INDEX_EMA_4 = addIndicator(new EMA(PERIODS_EMA_4));
        INDEX_EMA_5 = addIndicator(new EMA(PERIODS_EMA_5));
        INDEX_RSI = addIndicator(new RSI(PERIODS_RSI));
    }

    @Override
    public boolean hasBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //get the recent period
        Period period = history.get(history.size() - 1);

        EMA ema1 = (EMA)getIndicator(INDEX_EMA_1);
        EMA ema2 = (EMA)getIndicator(INDEX_EMA_2);
        EMA ema3 = (EMA)getIndicator(INDEX_EMA_3);
        EMA ema4 = (EMA)getIndicator(INDEX_EMA_4);
        EMA ema5 = (EMA)getIndicator(INDEX_EMA_5);
        RSI objRSI = (RSI)getIndicator(INDEX_RSI);

        //is the close > our 80 period ema then there is bullish trend
        if (period.close > getRecent(ema5)) {

            //if 13 period ema is > 21 period ema (minor bullish trend)
            if (getRecent(ema3) > getRecent(ema4)) {

                //if 3 period ema is > 5 period ema (minor bullish trend)
                if (getRecent(ema1) > getRecent(ema2)) {

                    //and the 3 period is constantly going upward
                    if (hasTrendUpward(ema1.getEma(), DEFAULT_PERIODS_CONFIRM_INCREASE)) {

                        //if the rsi line is above trend, we will buy
                        if (getRecent(objRSI.getValueRSI()) >= RSI_LINE)
                            return true;
                    }
                }
            }
        }

        //no signal
        return false;
    }

    @Override
    public boolean hasSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //get the recent period
        Period period = history.get(history.size() - 1);

        EMA ema1 = (EMA)getIndicator(INDEX_EMA_1);
        EMA ema2 = (EMA)getIndicator(INDEX_EMA_2);
        EMA ema3 = (EMA)getIndicator(INDEX_EMA_3);
        EMA ema4 = (EMA)getIndicator(INDEX_EMA_4);
        EMA ema5 = (EMA)getIndicator(INDEX_EMA_5);
        RSI objRSI = (RSI)getIndicator(INDEX_RSI);

        //if below trend sell immediately
        if (period.close < getRecent(ema5))
            return true;

        //if 13 period ema is < 21 period ema (minor bearish trend)
        if (getRecent(ema3) < getRecent(ema4))
            goShort(agent, getShortLow(history));

        //if 3 period ema is < 5 period ema (minor bearish trend)
        if (getRecent(ema1) < getRecent(ema2))
            goShort(agent, getShortLow(history));

        //if our fastest moving ema goes below a longer ema
        if (getRecent(ema1) < getRecent(ema4) || getRecent(ema1) < getRecent(ema5))
            return true;

        //if rsi drops below the line we will sell
        if (getRecent(objRSI.getValueRSI()) < RSI_LINE)
            return true;

        //if things are going down we should sell
        if (hasTrendDownward(ema1.getEma(), DEFAULT_PERIODS_CONFIRM_DECREASE) &&
                hasTrendDownward(ema2.getEma(), DEFAULT_PERIODS_CONFIRM_DECREASE))
            return true;

        //no signal
        return false;
    }
}