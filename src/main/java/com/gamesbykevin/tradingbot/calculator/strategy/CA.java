package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.indicator.momentun.CCI;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.ADX;
import com.gamesbykevin.tradingbot.trade.TradeHelper.ReasonSell;

import java.util.List;

/**
 * Commodity Channel Index / Average Directional Index
 */
public class CA extends Strategy {

    //how to access our indicator objects
    private static int INDEX_CCI;
    private static int INDEX_ADX;

    //configurable values
    private static final int PERIODS_CCI = 4;
    private static final int PERIODS_ADX = 50;
    private static final double TREND = 15.0d;
    private static final float CCI_LOW = -100;
    private static final float CCI_HIGH = 100;

    //we need to wait until the end of the candle when buying
    private boolean wait = false;

    //track time so we know when we are close to the end of the period
    private long time = 0, end = 0;

    public CA() {
        this(PERIODS_CCI, PERIODS_ADX);
    }

    public CA(int periodsCCI, int periodsADX) {

        //call parent
        super(Key.CA);

        //add our indicators
        INDEX_CCI = addIndicator(new CCI(periodsCCI));
        INDEX_ADX = addIndicator(new ADX(periodsADX));
    }

    @Override
    public boolean hasBuySignal(Agent agent, List<Period> history, double currentPrice) {

        ADX objADX = (ADX)getIndicator(INDEX_ADX);
        CCI objCCI = (CCI)getIndicator(INDEX_CCI);

        if (getRecent(objADX.getAdx()) < TREND && getRecent(objCCI.getCCI()) < CCI_LOW) {

            //get the recent period
            Period period = history.get(history.size() - 1);

            //we need to track time so we know when to buy
            if (!wait) {

                //track the current time
                time = System.currentTimeMillis();

                //calculate the end time when we can buy stock
                end = (long)(period.time + (agent.getCandle().duration * .75)) * 1000L;

                //flag that we setup our wait time
                wait = true;

            } else {

                //how much time has passed
                long lapsed = (System.currentTimeMillis() - time);

                //add to the candle to get the current time
                long current = (period.time * 1000L) + lapsed;

                //if we are close to the end of the candle we can now buy
                if (current >= end)
                    return true;
            }

        } else {

            //flag setup false
            wait = false;
        }

        //no signal
        return false;
    }

    @Override
    public boolean hasSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //flag wait false for the next trade
        this.wait = false;

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
                    adjustHardStopPrice(agent, currentPrice);

            } else if (getRecent(objADX.getDmPlusIndicator()) > getRecent(objADX.getDmMinusIndicator())) {

                //cci is above 100 and the current period closed bearish
                if (getRecent(objCCI.getCCI()) >= CCI_HIGH && period.open > period.close)
                    adjustHardStopPrice(agent, currentPrice);
            }
        }

        //no signal
        return false;
    }
}