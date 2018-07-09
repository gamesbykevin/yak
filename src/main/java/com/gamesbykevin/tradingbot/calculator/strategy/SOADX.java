package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.Period.Fields;
import com.gamesbykevin.tradingbot.calculator.indicator.momentun.SO;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.ADX;

import java.util.List;

/**
 * Stochastic Oscillator / Average Directional Index
 */
public class SOADX extends Strategy {

    //how to access our indicator objects
    private static int INDEX_SO;
    private static int INDEX_ADX;

    //list of configurable values
    private static final int PERIODS_SO_MARKET = 5;
    private static final int PERIODS_SO_MARKET_SMA = 3;
    private static final int PERIODS_SO_OSCILLATOR = 3;
    private static final int PERIODS_ADX = 20;
    private static final double OVERBOUGHT = 70.0d;
    private static final double OVERSOLD = 30.0d;

    public SOADX() {

        //call parent
        super(Key.SOADX);

        //add our indicators
        INDEX_ADX = addIndicator(new ADX(PERIODS_ADX));
        INDEX_SO = addIndicator(new SO(PERIODS_SO_MARKET, PERIODS_SO_MARKET_SMA, PERIODS_SO_OSCILLATOR));
    }

    @Override
    public boolean hasBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //get our indicator objects
        SO so = (SO)getIndicator(INDEX_SO);
        ADX adx = (ADX)getIndicator(INDEX_ADX);

        if (getRecent(so.getStochasticOscillator()) > OVERSOLD && getRecent(so.getStochasticOscillator()) < OVERBOUGHT) {
            if (getRecent(adx.getDmPlusIndicator()) > getRecent(adx.getDmMinusIndicator()))
                return true;
        }

        //no signal
        return false;
    }

    @Override
    public boolean hasSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //get our indicator objects
        SO so = (SO)getIndicator(INDEX_SO);
        ADX adx = (ADX)getIndicator(INDEX_ADX);

        //if overbought, get ready to sell
        if (getRecent(so.getStochasticOscillator()) >= OVERBOUGHT)
            goShort(agent, getRecent(history, Fields.Low));

        //if overbought and the adx crosses, we will sell
        if (getRecent(so.getStochasticOscillator()) >= OVERBOUGHT) {
            if (getRecent(adx.getDmMinusIndicator()) > getRecent(adx.getDmPlusIndicator()))
                return true;
        }

        //no signal
        return false;
    }
}