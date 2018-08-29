package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.Period.Fields;
import com.gamesbykevin.tradingbot.calculator.indicator.momentun.RSI;
import com.gamesbykevin.tradingbot.calculator.indicator.momentun.SO;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.EMA;

import java.util.List;

/**
 * Exponential Moving Average / Relative Strength Index / Stochastic Oscillator
 */
public class ERS extends Strategy {

    //how to access our indicator objects
    private static int INDEX_EMA_SHORT;
    private static int INDEX_EMA_LONG;
    private static int INDEX_SO;
    private static int INDEX_RSI;

    //configurable values
    private static final int PERIODS_EMA_SHORT = 5;
    private static final int PERIODS_EMA_LONG = 10;
    private static final int PERIODS_RSI = 14;
    private static final float OVERBOUGHT = 75.0f;
    private static final float OVERSOLD = 25.0f;
    private static final float RSI_LINE = 50.0f;
    private static final int PERIODS_SO_MARKET_RATE = 14;
    private static final int PERIODS_SO_MARKET_RATE_SMA = 3;
    private static final int PERIODS_SO_STOCHASTIC_SMA = 3;

    public ERS() {

        //call parent
        super(Key.ERS);

        //add our indicator objects
        INDEX_EMA_SHORT = addIndicator(new EMA(PERIODS_EMA_SHORT));
        INDEX_EMA_LONG = addIndicator(new EMA(PERIODS_EMA_LONG));
        INDEX_SO = addIndicator(new SO(PERIODS_SO_MARKET_RATE, PERIODS_SO_MARKET_RATE_SMA, PERIODS_SO_STOCHASTIC_SMA));
        INDEX_RSI = addIndicator(new RSI(PERIODS_RSI));
    }

    @Override
    public boolean hasBuySignal(Agent agent, List<Period> history, double currentPrice) {

        EMA objShortEMA = (EMA)getIndicator(INDEX_EMA_SHORT);
        EMA objLongEMA = (EMA)getIndicator(INDEX_EMA_LONG);
        SO objSO = (SO)getIndicator(INDEX_SO);
        RSI objRSI = (RSI)getIndicator(INDEX_RSI);

        //the short period has crossed the long period
        if (getRecent(objShortEMA.getEma()) > getRecent(objLongEMA.getEma())) {

            //rsi is above 50
            if (getRecent(objRSI.getValueRSI()) > RSI_LINE) {

                //get the current market rate and stochastic values
                double marketRate = getRecent(objSO.getMarketRateFull());
                double stochastic = getRecent(objSO.getStochasticOscillator());

                //make sure stochastic lines are below overbought and above oversold
                if (marketRate < OVERBOUGHT && marketRate > OVERSOLD &&
                        stochastic < OVERBOUGHT && stochastic > OVERSOLD) {

                    //make sure both stochastic and market rate values are increasing
                    if (marketRate > getRecent(objSO.getMarketRateFull(), 2) &&
                            stochastic > getRecent(objSO.getStochasticOscillator(), 2)) {
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

        EMA objShortEMA = (EMA)getIndicator(INDEX_EMA_SHORT);
        EMA objLongEMA = (EMA)getIndicator(INDEX_EMA_LONG);
        RSI objRSI = (RSI)getIndicator(INDEX_RSI);

        //if the fast went below the long
        if (getRecent(objShortEMA.getEma()) < getRecent(objLongEMA.getEma())) {

            //protect our investment
            goShort(agent, getShortLow(history));

            //if rsi is lower then we have confirmation to sell
            if (getRecent(objRSI.getValueRSI()) < RSI_LINE)
                return true;
        }

        //no signal
        return false;
    }
}