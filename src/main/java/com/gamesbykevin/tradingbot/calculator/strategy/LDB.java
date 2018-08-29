package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.Period.Fields;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.EMA;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.MACD;

import java.util.List;

import static com.gamesbykevin.tradingbot.trade.TradeHelper.createTrade;

/**
 * London Daybreak Strategy (MACD / 50 Period EMA)
 */
public class LDB extends Strategy {

    //how to access our indicator objects
    private static int INDEX_MACD;
    private static int INDEX_EMA;

    //configurable values
    private static final int PERIODS_MACD_SLOW = 26;
    private static final int PERIODS_MACD_FAST = 12;
    private static final int PERIODS_MACD_SIGNAL = 9;
    private static final int PERIODS_EMA_TREND = 50;

    public LDB() {

        //call parent
        super(Key.LDB);

        //add our indicator objects
        INDEX_MACD = addIndicator(new MACD(PERIODS_MACD_SLOW, PERIODS_MACD_FAST, PERIODS_MACD_SIGNAL));
        INDEX_EMA = addIndicator(new EMA(PERIODS_EMA_TREND));
    }

    @Override
    public boolean hasBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //access our indicators
        MACD objMACD = (MACD)getIndicator(INDEX_MACD);
        EMA objEMA = (EMA)getIndicator(INDEX_EMA);

        //make sure we are trading above ema
        if (getRecent(history, Fields.Close) > getRecent(objEMA.getEma())) {

            //when MACD crosses above signal line
            if (getRecent(objMACD.getMacdLine(), 2) < getRecent(objMACD.getSignalLine(), 2) &&
                    getRecent(objMACD.getMacdLine()) > getRecent(objMACD.getSignalLine())) {

                //set the protective stop at the low of the recent candles
                createTrade(agent);
                agent.getTrade().setHardStopPrice(getShortLow(history));

                //we have a signal
                return true;
            }
        }

        //no signal
        return false;
    }

    @Override
    public boolean hasSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //access our indicators
        MACD objMACD = (MACD)getIndicator(INDEX_MACD);
        EMA objEMA = (EMA)getIndicator(INDEX_EMA);

        //make sure we are trading below ema
        if (getRecent(history, Fields.Close) < getRecent(objEMA.getEma())) {

            //when MACD crosses below
            if (getRecent(objMACD.getMacdLine()) < getRecent(objMACD.getSignalLine()))
                return true;
        }

        //no signal
        return false;
    }
}