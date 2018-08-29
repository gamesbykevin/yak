package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.Period.Fields;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.EMA;
import com.gamesbykevin.tradingbot.calculator.indicator.momentun.RSI;

import java.util.List;

/**
 * Exponential Moving Average / Relative Strength Index
 */
public class EMAR extends Strategy {

    //how to access our indicator objects
    private static int INDEX_EMA_LONG;
    private static int INDEX_EMA_SHORT;
    private static int INDEX_RSI;

    //list of configurable values
    private static final int PERIODS_EMA_LONG = 12;
    private static final int PERIODS_EMA_SHORT = 5;
    private static final int PERIODS_RSI = 21;
    private static final float RSI_LINE = 50.0f;

    public EMAR() {
        this(PERIODS_EMA_LONG, PERIODS_EMA_SHORT, PERIODS_RSI);
    }

    public EMAR(int emaLong, int emaShort, int periodsRSI) {

        //call parent
        super(Key.EMAR);

        //add our indicators
        INDEX_EMA_SHORT = addIndicator(new EMA(emaShort));
        INDEX_EMA_LONG = addIndicator(new EMA(emaLong));
        INDEX_RSI = addIndicator(new RSI(periodsRSI));
    }

    @Override
    public boolean hasBuySignal(Agent agent, List<Period> history, double currentPrice) {

        RSI rsiObj = (RSI)getIndicator(INDEX_RSI);
        EMA emaShortObj = (EMA)getIndicator(INDEX_EMA_SHORT);
        EMA emaLongObj = (EMA)getIndicator(INDEX_EMA_LONG);

        double currRsi = getRecent(rsiObj.getValueRSI(), 1);
        double prevRsi = getRecent(rsiObj.getValueRSI(), 2);

        double currEmaS = getRecent(emaShortObj.getEma(), 1);
        double prevEmaS = getRecent(emaShortObj.getEma(), 2);

        double currEmaL = getRecent(emaLongObj.getEma(), 1);
        double prevEmaL = getRecent(emaLongObj.getEma(), 2);

        //if the rsi just crossed and our short ema is trending above
        if (prevRsi < RSI_LINE && currRsi >= RSI_LINE && currEmaS > currEmaL)
            return true;

        //if rsi is trending and short ema just crossed
        if (currRsi >= RSI_LINE && prevEmaS < prevEmaL && currEmaS > currEmaL)
            return true;

        //no signal
        return false;
    }

    @Override
    public boolean hasSellSignal(Agent agent, List<Period> history, double price) {

        RSI rsiObj = (RSI)getIndicator(INDEX_RSI);
        EMA emaShortObj = (EMA)getIndicator(INDEX_EMA_SHORT);
        EMA emaLongObj = (EMA)getIndicator(INDEX_EMA_LONG);

        //recent values
        double current = getRecent(rsiObj.getValueRSI());
        double emaShort = getRecent(emaShortObj.getEma());
        double emaLong = getRecent(emaLongObj.getEma());

        //if rsi is under the line and the fast line is below the slow long indicating a downward trend
        if (current < RSI_LINE && emaShort < emaLong)
            return true;

        //adjust our hard stop price to protect our investment
        if (emaShort < emaLong || current < RSI_LINE)
            goShort(agent, getShortLow(history));

        //no signal
        return false;
    }
}