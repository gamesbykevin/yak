package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.Period.Fields;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.ADX;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.EMA;

import java.util.List;

import static com.gamesbykevin.tradingbot.calculator.strategy.StrategyHelper.hasTrendDownward;
import static com.gamesbykevin.tradingbot.calculator.strategy.StrategyHelper.hasTrendUpward;

/**
 * Average Directional Index / Exponential Moving Average
 */
public class AE extends Strategy {

    //how to access our indicator objects
    private static int INDEX_EMA_SHORT;
    private static int INDEX_EMA_LONG;
    private static int INDEX_ADX;

    //configurable values
    private static final int PERIODS_EMA_LONG = 10;
    private static final int PERIODS_EMA_SHORT = 3;
    private static final int PERIODS_ADX = 14;
    private static final double ADX_TREND = 20.0d;

    public AE() {
        this(PERIODS_EMA_LONG, PERIODS_EMA_SHORT, PERIODS_ADX);
    }

    public AE(int periodsEmaLong, int periodsEmaShort, int periodsAdx) {

        //call parent
        super(Key.AE);

        //add our indicators
        INDEX_EMA_SHORT = addIndicator(new EMA(periodsEmaShort));
        INDEX_EMA_LONG = addIndicator(new EMA(periodsEmaLong));
        INDEX_ADX = addIndicator(new ADX(periodsAdx));
    }

    @Override
    public boolean hasBuySignal(Agent agent, List<Period> history, double currentPrice) {

        ADX objADX = (ADX)getIndicator(INDEX_ADX);
        EMA objShortEMA = (EMA)getIndicator(INDEX_EMA_SHORT);
        EMA objLongEMA = (EMA)getIndicator(INDEX_EMA_LONG);

        //make sure adx is trending
        if (getRecent(objADX.getAdx()) > ADX_TREND) {

            //check to ensure adx is not declining
            if (hasTrendDownward(objADX.getAdx(), DEFAULT_PERIODS_CONFIRM_DECREASE))
                return false;

            //if the short ema is not trending upwards
            if (!hasTrendUpward(objShortEMA.getEma(), DEFAULT_PERIODS_CONFIRM_INCREASE))
                return false;

            //if the short crosses above the long let's buy and enter the trade
            if (getRecent(objShortEMA.getEma(), 2) < getRecent(objLongEMA.getEma(), 2) &&
                    getRecent(objShortEMA.getEma()) > getRecent(objLongEMA.getEma()))
                return true;
        }

        //we didn't find a buy signal
        return false;
    }

    @Override
    public boolean hasSellSignal(Agent agent, List<Period> history, double currentPrice) {

        ADX objADX = (ADX)getIndicator(INDEX_ADX);
        EMA objShortEMA = (EMA)getIndicator(INDEX_EMA_SHORT);
        EMA objLongEMA = (EMA)getIndicator(INDEX_EMA_LONG);

        //if the ema short crosses below the ema long, it is time to sell
        if (getRecent(objShortEMA.getEma()) < getRecent(objLongEMA.getEma()) || getRecent(objADX.getAdx()) < ADX_TREND)
            return true;

        //if going downward, protect investment
        if (hasTrendDownward(objShortEMA.getEma(), DEFAULT_PERIODS_CONFIRM_DECREASE))
            goShort(agent, getShortLow(history));

        //no signal yet
        return false;
    }
}