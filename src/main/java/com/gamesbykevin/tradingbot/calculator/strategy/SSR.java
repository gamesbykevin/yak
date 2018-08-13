package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.Period.Fields;
import com.gamesbykevin.tradingbot.calculator.indicator.momentun.RSI;
import com.gamesbykevin.tradingbot.calculator.indicator.momentun.SO;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.SMA;
import java.util.List;

/**
 * Stochastic Oscillator / Simple Moving Average / Relative Strength Index
 */
public class SSR extends Strategy {

    //how to access our indicator objects
    private static int INDEX_SO;
    private static int INDEX_SMA;
    private static int INDEX_RSI;

    //configurable values
    private static final int PERIODS_SMA = 150;
    private static final int PERIODS_RSI = 3;
    private static final float SO_OVERBOUGHT = 70.0f;
    private static final float SO_OVERSOLD = 30.0f;
    private static final float RSI_OVERBOUGHT = 80.0f;
    private static final float RSI_OVERSOLD = 20.0f;
    private static final int PERIODS_SO_MARKET_RATE = 6;
    private static final int PERIODS_SO_MARKET_RATE_SMA = 3;
    private static final int PERIODS_SO_STOCHASTIC_SMA = 3;

    public SSR() {

        //call parent
        super(Key.SSR);

        //add our indicators
        INDEX_SO = addIndicator(new SO(PERIODS_SO_MARKET_RATE, PERIODS_SO_MARKET_RATE_SMA, PERIODS_SO_STOCHASTIC_SMA));
        INDEX_RSI = addIndicator(new RSI(PERIODS_RSI));
        INDEX_SMA = addIndicator(new SMA(PERIODS_SMA));
    }

    @Override
    public boolean hasBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //get the recent period
        Period period = history.get(history.size() - 1);

        //get our indicator objects
        SMA objSMA = (SMA)getIndicator(INDEX_SMA);
        RSI objRSI = (RSI)getIndicator(INDEX_RSI);
        SO objSO = (SO)getIndicator(INDEX_SO);

        //if the close is above the sma we have a bullish trend
        if (period.close > getRecent(objSMA)) {

            //we also want the rsi to be over sold
            if (getRecent(objRSI.getValueRSI()) < RSI_OVERSOLD) {

                //the stochastic oscillator should be in oversold territory as well
                if (getRecent(objSO.getMarketRateFull()) < SO_OVERSOLD && getRecent(objSO.getStochasticOscillator()) < SO_OVERSOLD) {

                    //last thing we check is for the stochastic bullish crossover before we buy
                    if (getRecent(objSO.getMarketRateFull()) > getRecent(objSO.getStochasticOscillator()))
                        return true;
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

        //get our indicator objects
        SMA objSMA = (SMA)getIndicator(INDEX_SMA);
        RSI objRSI = (RSI)getIndicator(INDEX_RSI);
        SO objSO = (SO)getIndicator(INDEX_SO);

        //if the close is below the sma we have a bearish trend
        if (period.close < getRecent(objSMA))
            return true;

        //if the stock is overbought, adjust our hard stop price and sell
        if (getRecent(objRSI.getValueRSI()) > RSI_OVERBOUGHT && getRecent(objSO.getStochasticOscillator()) > SO_OVERBOUGHT)
            return true;

        //if bearish crossover we go short
        if (getRecent(objSO.getMarketRateFull()) < getRecent(objSO.getStochasticOscillator()))
            goShort(agent, getRecent(history, Fields.Low));

        //no signal
        return false;
    }
}