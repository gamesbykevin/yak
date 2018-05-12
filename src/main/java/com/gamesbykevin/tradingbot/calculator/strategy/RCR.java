package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.indicator.other.RC;
import com.gamesbykevin.tradingbot.calculator.indicator.momentun.RSI;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;

import java.util.List;

/**
 * Renko Charts / Relative Strength Index
 */
public class RCR extends Strategy {

    //how to access our indicator objects
    private static int INDEX_RC;
    private static int INDEX_RSI;

    //configurable values
    private static final int PERIODS_SMA = 10;
    private static final int PERIODS_RSI = 14;
    private static final int PERIODS_ATR = 14;
    private static final float OVERBOUGHT = 70.0f;
    private static final float OVERSOLD = 30.0f;

    public RCR() {
        this(PERIODS_SMA, PERIODS_RSI, PERIODS_ATR);
    }

    public RCR(int periodsSMA, int periodsRSI, int periodsATR) {

        //add our indicator objects
        INDEX_RC = addIndicator(new RC(periodsATR, periodsSMA));
        INDEX_RSI = addIndicator(new RSI(periodsRSI));
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        RC objRC = (RC)getIndicator(INDEX_RC);
        RSI objRSI = (RSI)getIndicator(INDEX_RSI);

        //if the current renko chart price is greater than sma we have bullish trend
        if (getRecent(objRC.getRenkoChart(),2) < getRecent(objRC.getRenkoChartSMA(),2) &&
                getRecent(objRC.getRenkoChart()) > getRecent(objRC.getRenkoChartSMA())) {

            //check if oversold before we buy
            if (getRecent(objRSI.getRsiVal()) < OVERSOLD)
                agent.setBuy(true);
        }
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        RC objRC = (RC)getIndicator(INDEX_RC);
        RSI objRSI = (RSI)getIndicator(INDEX_RSI);

        //if the current renko chart price is less than sma we have bearish trend
        if (getRecent(objRC.getRenkoChart()) < getRecent(objRC.getRenkoChartSMA()))
            adjustHardStopPrice(agent, currentPrice);

        //if we reached overbought, protect investment
        if (getRecent(objRSI.getRsiVal()) >= OVERBOUGHT)
            adjustHardStopPrice(agent, currentPrice);

        //if we were overbought and are now below overbought, it is time to sell
        if (getRecent(objRSI.getRsiVal(), 2) >= OVERBOUGHT && getRecent(objRSI.getRsiVal()) < OVERBOUGHT)
            agent.setReasonSell(ReasonSell.Reason_Strategy);
    }
}