package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.Period.Fields;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.SMA;
import com.gamesbykevin.tradingbot.calculator.indicator.volatility.ATR;

import java.util.List;

import static com.gamesbykevin.tradingbot.calculator.strategy.StrategyHelper.hasTrendDownward;
import static com.gamesbykevin.tradingbot.calculator.strategy.StrategyHelper.hasTrendUpward;

/**
 * Average True Range / Simple Moving Average
 */
public class AS extends Strategy {

    //how to access our indicator objects
    private static int INDEX_ATR;
    private static int INDEX_SMA;

    //configurable values
    private static final int PERIODS_ATR = 14;
    private static final int PERIODS_SMA = 50;

    public AS() {

        //call parent
        super(Key.AS);

        //add our indicator objects
        INDEX_ATR = addIndicator(new ATR(PERIODS_ATR));
        INDEX_SMA = addIndicator(new SMA(PERIODS_SMA));
    }

    @Override
    public boolean hasBuySignal(Agent agent, List<Period> history, double currentPrice) {

        ATR objATR = (ATR)getIndicator(INDEX_ATR);
        SMA objSMA = (SMA)getIndicator(INDEX_SMA);

        //if the candle close is above the sma
        if (getRecent(history, Fields.Close) > getRecent(objSMA.getSma())) {

            //if atr going do this is a sign of a trend
            if (hasTrendDownward(objATR.getAverageTrueRange(), DEFAULT_PERIODS_CONFIRM_DECREASE))
                return true;
        }

        //no signal
        return false;
    }

    @Override
    public boolean hasSellSignal(Agent agent, List<Period> history, double currentPrice) {

        ATR objATR = (ATR)getIndicator(INDEX_ATR);
        SMA objSMA = (SMA)getIndicator(INDEX_SMA);

        //if we are above the sma
        if (getRecent(history, Fields.Close) > getRecent(objSMA.getSma())) {

            //if the atr is going up, it is sign of a trend change
            if (hasTrendUpward(objATR.getAverageTrueRange(), DEFAULT_PERIODS_CONFIRM_INCREASE))
                goShort(agent, getShortLow(history));

        } else if (getRecent(history, Fields.Close) < getRecent(objSMA.getSma())) {

            //if we are below the sma go short
            goShort(agent, getShortLow(history));
        }

        //no signal
        return false;
    }
}