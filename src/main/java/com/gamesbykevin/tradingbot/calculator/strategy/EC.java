package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.Period.Fields;
import com.gamesbykevin.tradingbot.calculator.indicator.momentun.CCI;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.EMA;

import java.util.List;

/**
 * Exponential Moving Average / Commodity Channel Index
 */
public class EC extends Strategy {

    //how to access our indicator objects
    private static int INDEX_EMA;
    private static int INDEX_CCI;

    //configurable values
    private static final int PERIODS_EMA = 60;
    private static final int PERIODS_CCI = 14;
    private static final int CCI_HIGH = 100;


    public EC() {

        //call parent
        super(Key.EC);

        //add our indicator objects
        INDEX_EMA = addIndicator(new EMA(PERIODS_EMA));
        INDEX_CCI = addIndicator(new CCI(PERIODS_CCI));
    }

    @Override
    public boolean hasBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //access our indicators
        EMA objEMA = (EMA)getIndicator(INDEX_EMA);
        CCI objCCI = (CCI)getIndicator(INDEX_CCI);

        if (getRecent(history, Fields.Close) > getRecent(objEMA) && getRecent(objCCI.getCCI()) > CCI_HIGH)
            return true;

        //no signal
        return false;
    }

    @Override
    public boolean hasSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //access our indicators
        EMA objEMA = (EMA)getIndicator(INDEX_EMA);
        CCI objCCI = (CCI)getIndicator(INDEX_CCI);

        //set our short right at the ema line
        goShort(agent, getRecent(objEMA));

        //if we close below the ema, definitely sell
        if (getRecent(history, Fields.Close) < getRecent(objEMA))
            return true;

        //no signal
        return false;
    }

}