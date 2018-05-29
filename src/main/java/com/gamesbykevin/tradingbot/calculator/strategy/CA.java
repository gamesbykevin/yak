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

        double adxCurr = getRecent(objADX.getAdx(), 1);
        double adxPrev = getRecent(objADX.getAdx(), 2);

        double cciCurr = getRecent(objCCI.getCCI(), 1);
        double cciPrev = getRecent(objCCI.getCCI(), 2);

        if (adxCurr < TREND && cciCurr < CCI_LOW)
            return true;

        /*
        //are things trending with momentum? we want to catch it when the switch just happens
        if ((adxCurr < TREND && cciPrev > CCI_LOW && cciCurr < CCI_LOW) ||
            (adxPrev > TREND && adxCurr < TREND && cciCurr < CCI_LOW)) {

            //get the current candle
            Period period = history.get(history.size() - 1);

            //if the candle is bullish we will buy
            if (period.open < period.close)
                return true;
        }
        */

        //no signal
        return false;
    }

    @Override
    public boolean hasSellSignal(Agent agent, List<Period> history, double currentPrice) {

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