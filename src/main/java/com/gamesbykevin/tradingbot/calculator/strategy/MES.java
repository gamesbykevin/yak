package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.Period.Fields;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.EMA;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.SMA;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.MACD;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;

import java.util.ArrayList;
import java.util.List;

import static com.gamesbykevin.tradingbot.calculator.indicator.trend.EMA.calculateEMA;

/**
 * MACD / EMA / SMA
 */
public class MES extends Strategy {

    //how to access our indicator objects
    private static int INDEX_SMA;
    private static int INDEX_EMA;
    private static int INDEX_MACD;

    //configurable values
    private static final int PERIODS_SMA_SHORT = 15;
    private static final int PERIODS_EMA_SHORT = 5;
    private static final int PERIODS_MACD_SHORT = 12;
    private static final int PERIODS_MACD_LONG = 26;
    private static final int PERIODS_MACD_SIGNAL = 9;

    //check recent periods to confirm recent crossover
    private static final int PERIOD_DISTANCE_REFERENCE = 3;

    public MES() {
        this(PERIODS_SMA_SHORT, PERIODS_EMA_SHORT, PERIODS_MACD_SHORT, PERIODS_MACD_LONG, PERIODS_MACD_SIGNAL);
    }

    public MES(int periodsSmaShort, int periodsEmaShort, int periodsMacdShort, int periodsMacdLong, int periodsMacdSignal) {

        //add our indicator objects
        INDEX_MACD = addIndicator(new MACD(periodsMacdLong, periodsMacdShort, periodsMacdSignal));
        INDEX_SMA = addIndicator(new SMA(periodsSmaShort));
        INDEX_EMA = addIndicator(new EMA(periodsEmaShort));
    }

    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        boolean confirmA = false, confirmB = false;

        EMA objEMA = (EMA)getIndicator(INDEX_EMA);
        MACD objMACD = (MACD)getIndicator(INDEX_MACD);
        SMA objSMA = (SMA)getIndicator(INDEX_SMA);

        //make sure we recently crossed
        for (int index = 0; index < PERIOD_DISTANCE_REFERENCE; index++) {

            if (!confirmA && getRecent(objMACD.getMacdLine(), index + 2) < getRecent(objMACD.getSignalLine(), index + 2))
                confirmA = true;
            if (!confirmB && getRecent(objEMA, index + 2) < getRecent(objSMA, index + 2))
                confirmB = true;
        }

        //if we confirm macd and ema were previously down
        if (confirmA && confirmB) {

            //first we make sure the macd line crossed above the signal line
            if (getRecent(objMACD.getMacdLine()) > getRecent(objMACD.getSignalLine())) {

                //make sure the fast ema crosses above the sma short, let's buy
                if (getRecent(objEMA) > getRecent(objSMA))
                    agent.setBuy(true);
            }
        }
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        EMA objEMA = (EMA)getIndicator(INDEX_EMA);
        MACD objMACD = (MACD)getIndicator(INDEX_MACD);
        SMA objSMA = (SMA)getIndicator(INDEX_SMA);

        //first we make sure the macd line crossed below the signal line
        if (getRecent(objMACD.getMacdLine()) < getRecent(objMACD.getSignalLine())) {

            //next make sure the fast ema crosses below the sma short
            if (getRecent(objEMA) < getRecent(objSMA))
                agent.setReasonSell(ReasonSell.Reason_Strategy);

        } else {

            double close = getRecent(history, Fields.Close);

            //if the close is less than the emas and the macd line is negative
            if (close < getRecent(objEMA) && close < getRecent(objSMA) && getRecent(objMACD.getMacdLine()) < 0)
                agent.setReasonSell(ReasonSell.Reason_Strategy);
        }

        //adjust our hard stop price to protect our investment
        if (objEMA.getPeriods() < objSMA.getPeriods() && getRecent(objEMA) < getRecent(objSMA))
            adjustHardStopPrice(agent, currentPrice);
    }
}