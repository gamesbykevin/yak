package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.indicator.momentun.RSI;
import com.gamesbykevin.tradingbot.calculator.indicator.volatility.ATR;

import java.util.List;

/**
 * Average True Range / Relative Strength Index
 */
public class AR extends Strategy {

    //how to access our indicator objects
    private static int INDEX_ATR;
    private static int INDEX_RSI;

    //configurable values
    private static final int PERIODS_ATR = 10;
    private static final int PERIODS_RSI = 7;

    private static final float OVERBOUGHT = 75.0f;
    private static final float OVERSOLD = 25.0f;

    public AR() {
        this(PERIODS_ATR, PERIODS_RSI);
    }

    public AR(int periodsATR, int periodsRSI) {

        //call parent
        super(Key.AR);

        //add our indicator objects
        INDEX_ATR = addIndicator(new ATR(periodsATR));
        INDEX_RSI = addIndicator(new RSI(periodsRSI));
    }

    @Override
    public boolean hasBuySignal(Agent agent, List<Period> history, double currentPrice) {

        ATR objATR = (ATR)getIndicator(INDEX_ATR);
        RSI objRSI = (RSI)getIndicator(INDEX_RSI);

        //make sure we just came out of oversold territory
        if (getRecent(objRSI.getValueRSI(), 2) < OVERSOLD && getRecent(objRSI.getValueRSI()) > OVERSOLD) {

            //adjust our hard stop based on average true range
            adjustHardStopPrice(agent, history.get(history.size() - 1).close - getRecent(objATR.getAverageTrueRange()));

            //we have a signal
            return true;
        }

        //no signal
        return false;
    }

    @Override
    public boolean hasSellSignal(Agent agent, List<Period> history, double currentPrice) {

        RSI objRSI = (RSI)getIndicator(INDEX_RSI);

        //if over bought adjust our hard stop price
        if (getRecent(objRSI.getValueRSI()) > OVERBOUGHT)
            adjustHardStopPrice(agent, currentPrice);

        //make sure we just came out of over bought territory before selling
        if (getRecent(objRSI.getValueRSI(), 2) > OVERBOUGHT && getRecent(objRSI.getValueRSI()) < OVERBOUGHT)
            return true;

        //no signal
        return false;
    }
}