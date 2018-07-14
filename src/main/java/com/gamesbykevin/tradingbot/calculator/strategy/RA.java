package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.Period.Fields;
import com.gamesbykevin.tradingbot.calculator.indicator.momentun.RSI;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.ADX;

import java.util.List;

/**
 * Relative Strength Index / Average Directional Index
 */
public class RA extends Strategy {

    //how to access our indicator objects
    private static int INDEX_RSI;
    private static int INDEX_ADX;

    //configurable values
    private static final int PERIODS_RSI = 7;
    private static final int PERIODS_ADX = 20;

    public static final float RSI_HIGH = 70.0f;
    public static final float RSI_LOW = 30.0f;

    public RA() {

        //call parent
        super(Key.RA);

        //add our indicator objects
        INDEX_RSI = addIndicator(new RSI(PERIODS_RSI));
        INDEX_ADX = addIndicator(new ADX(PERIODS_ADX));
    }

    @Override
    public boolean hasBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //get our indicators
        RSI rsi = (RSI)getIndicator(INDEX_RSI);
        ADX adx = (ADX)getIndicator(INDEX_ADX);

        //make sure rsi is high enough
        if (getRecent(rsi.getValueRSI()) > RSI_HIGH) {

            //if + is above -
            if (getRecent(adx.getDmPlusIndicator()) > getRecent(adx.getDmMinusIndicator()))
                return true;
        }

        //no signal
        return false;
    }

    @Override
    public boolean hasSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //get our indicators
        RSI rsi = (RSI)getIndicator(INDEX_RSI);
        ADX adx = (ADX)getIndicator(INDEX_ADX);

        //make sure rsi is low enough
        if (getRecent(rsi.getValueRSI()) < RSI_LOW) {

            //if + is below -
            if (getRecent(adx.getDmPlusIndicator()) < getRecent(adx.getDmMinusIndicator()))
                return true;
        }

        //if trend is going down, protect our investment
        if (getRecent(rsi.getValueRSI()) < RSI_LOW)
            goShort(agent, getRecent(history, Fields.Low));
        if (getRecent(adx.getDmPlusIndicator()) < getRecent(adx.getDmMinusIndicator()))
            goShort(agent, getRecent(history, Fields.Low));

        //no signal
        return false;
    }
}