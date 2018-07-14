package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.Period.Fields;
import com.gamesbykevin.tradingbot.calculator.indicator.volatility.BB;
import com.gamesbykevin.tradingbot.calculator.indicator.momentun.RSI;
import com.gamesbykevin.tradingbot.trade.TradeHelper.ReasonSell;

import java.util.List;

import static com.gamesbykevin.tradingbot.agent.AgentManagerHelper.displayMessage;

public class BBR extends Strategy {

    //how to access our indicator objects
    private static int INDEX_BB;
    private static int INDEX_RSI;

    //list of configurable values
    protected static int PERIODS_BB = 10;
    protected static int PERIODS_RSI = 21;

    //multiplier for standard deviation
    private static final float MULTIPLIER = 2.0f;

    //what is the bollinger band squeeze ratio
    private static final float SQUEEZE_RATIO = .040f;

    //our rsi signal values
    private static final float RSI_TREND = 50.0f;
    private static final float RSI_OVERBOUGHT = 70.0f;

    public BBR() {
        this(PERIODS_BB, MULTIPLIER, PERIODS_RSI);
    }

    public BBR(int periodsBB, float multiplier, int periodsRSI) {

        //call parent
        super(Key.BBR);

        //add our indicator objects
        INDEX_BB = addIndicator(new BB(periodsBB, multiplier));
        INDEX_RSI = addIndicator(new RSI(periodsRSI));
    }

    public boolean hasBuySignal(Agent agent, List<Period> history, double currentPrice) {

        BB objBB = (BB)getIndicator(INDEX_BB);
        RSI objRSI = (RSI)getIndicator(INDEX_RSI);

        //what is the price percentage
        float percentage = (float)(getRecent(objBB.getWidth()) / getRecent(history, Fields.Close));

        //current closing price
        final double close = getRecent(history, Fields.Close);

        //current rsi value
        final double rsi = getRecent(objRSI.getValueRSI());

        //current upper band
        final double upper = getRecent(objBB.getUpper());

        //first make sure the rsi value goes above the trend
        if (getRecent(objRSI.getValueRSI(), 2) < RSI_TREND && rsi >= RSI_TREND) {

            //if the price is narrow and the close is above our upper band
            if (percentage <= SQUEEZE_RATIO && close > upper)
                return true;
        }

        //no signal
        return false;
    }

    @Override
    public boolean hasSellSignal(Agent agent, List<Period> history, double currentPrice) {

        BB objBB = (BB)getIndicator(INDEX_BB);
        RSI objRSI = (RSI)getIndicator(INDEX_RSI);

        //indicator values
        double rsi = getRecent(objRSI.getValueRSI());
        double middleCurr = getRecent(objBB.getMiddle().getSma());
        double middlePrev = getRecent(objBB.getMiddle().getSma(), 2);
        double close = getRecent(history, Fields.Close);

        //if the rsi is overbought ....
        if (rsi >= RSI_OVERBOUGHT) {

            //adjust our hard stop price to protect our investment
            goShort(agent, getShortLow(history));

            //if the middle band is not up-trending compared to previous we can exit our trade now
            if (middlePrev > middleCurr)
                return true;

        } else if (rsi < RSI_TREND) {

            //adjust our hard stop price to protect our investment
            goShort(agent, getShortLow(history));

            //if the rsi is going towards oversold territory, let's see if the close drops below our middle band or if the middle band is decreasing
            if (middlePrev > middleCurr || close < middleCurr)
                return true;
        }

        //adjust our hard stop price to protect our investment
        if (close < middleCurr)
            goShort(agent, getShortLow(history));

        return false;
    }
}