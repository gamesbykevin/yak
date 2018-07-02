package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.Period.Fields;
import com.gamesbykevin.tradingbot.calculator.indicator.momentun.CCI;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.ADX;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.EMA;

import java.util.List;

/**
 * Commodity Channel Index / Average Directional Index
 */
public class CA extends Strategy {

    //how to access our indicator objects
    private static int INDEX_CCI;
    private static int INDEX_ADX;
    private static int INDEX_EMA_FAST;
    private static int INDEX_EMA_SLOW;

    //configurable values
    private static final int PERIODS_EMA_FAST = 12;
    private static final int PERIODS_EMA_SLOW = 26;
    private static final int PERIODS_CCI = 4;
    private static final int PERIODS_ADX = 50;
    private static final double TREND = 15.0d;
    private static final float CCI_LOW = -100;
    private static final float CCI_HIGH = 100;

    public CA() {
        this(PERIODS_CCI, PERIODS_ADX);
    }

    public CA(int periodsCCI, int periodsADX) {

        //call parent
        super(Key.CA);

        //add our indicators
        INDEX_CCI = addIndicator(new CCI(periodsCCI));
        INDEX_ADX = addIndicator(new ADX(periodsADX));
        INDEX_EMA_FAST = addIndicator(new EMA(PERIODS_EMA_FAST));
        INDEX_EMA_SLOW = addIndicator(new EMA(PERIODS_EMA_SLOW));
    }

    @Override
    public boolean hasBuySignal(Agent agent, List<Period> history, double currentPrice) {

        ADX objADX = (ADX)getIndicator(INDEX_ADX);
        CCI objCCI = (CCI)getIndicator(INDEX_CCI);
        EMA objEmaFast = (EMA)getIndicator(INDEX_EMA_FAST);
        EMA objEmaSlow = (EMA)getIndicator(INDEX_EMA_SLOW);

        //let's time this right
        if (getRecent(objADX.getAdx()) < TREND && getRecent(objCCI.getCCI()) < CCI_LOW) {

            double emaFastCurr = getRecent(objEmaFast.getEma());
            double emaFastPrev = getRecent(objEmaFast.getEma(), 2);
            double emaSlowCurr = getRecent(objEmaSlow.getEma());

            //if things are trending up buy asap, else we buy at the end of the current period
            if (emaFastCurr > emaSlowCurr && emaFastCurr > emaFastPrev) {

                //we have signal to buy now
                return true;

            } else {

                //setup the time to trade (if we haven't already)
                if (!hasSetupTimeTrade())
                    setupTimeTrade(agent.getCandle());

                //if enough time passed
                if (hasTimeTrade())
                    return true;
            }

        } else {

            //start over for our next buy signal
            resetTimeTrade();
        }

        //no signal
        return false;
    }

    @Override
    public boolean hasSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //start over for our next buy signal
        resetTimeTrade();

        ADX objADX = (ADX)getIndicator(INDEX_ADX);
        CCI objCCI = (CCI)getIndicator(INDEX_CCI);

        //get the current candle
        Period period = history.get(history.size() - 1);

        //if adx is below the trend and cci is above 100
        if (getRecent(objADX.getAdx()) < TREND && getRecent(objCCI.getCCI()) > CCI_HIGH) {

            //if the candle is bearish we will sell
            if (period.open > period.close)
                return true;
        }

        //if adx is trending
        if (getRecent(objADX.getAdx()) > TREND) {

            //if dm+ is below dm-
            if (getRecent(objADX.getDmPlusIndicator()) < getRecent(objADX.getDmMinusIndicator())) {

                //cci is below -100 and the current period closed bullish
                if (getRecent(objCCI.getCCI()) <= CCI_LOW && period.open < period.close)
                    goShort(agent, getShortLow(history));

            } else if (getRecent(objADX.getDmPlusIndicator()) > getRecent(objADX.getDmMinusIndicator())) {

                //cci is above 100 and the current period closed bearish
                if (getRecent(objCCI.getCCI()) >= CCI_HIGH && period.open > period.close)
                    goShort(agent, getShortLow(history));
            }
        }

        //no signal
        return false;
    }
}