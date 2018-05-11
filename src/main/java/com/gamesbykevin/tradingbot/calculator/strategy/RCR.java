package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.indicator.RC;
import com.gamesbykevin.tradingbot.calculator.indicator.momentun.RSI;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;

import java.util.ArrayList;
import java.util.List;

import static com.gamesbykevin.tradingbot.calculator.indicator.trend.SMA.calculateSMA;

/**
 * Renko Charts / Relative Strength Index
 */
public class RCR extends Strategy {

    //our indicator objects
    private RC objRC;
    private RSI objRSI;
    private List<Double> sma;

    //configurable values
    private static final int PERIODS_SMA = 10;
    private static final int PERIODS_RSI = 14;
    private static final int PERIODS_ATR = 14;
    private static final float OVERBOUGHT = 70.0f;
    private static final float OVERSOLD = 30.0f;

    //how many sma periods
    private final int periodsSMA;

    public RCR() {
        this(PERIODS_SMA, PERIODS_RSI, PERIODS_ATR);
    }

    public RCR(int periodsSMA, int periodsRSI, int periodsATR) {

        //save the sma periods
        this.periodsSMA = periodsSMA;

        //create our objects
        this.objRC = new RC(periodsATR);
        this.objRSI = new RSI(periodsRSI);
        this.sma = new ArrayList<>();
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //if the current renko chart price is greater than sma we have bullish trend
        if (getRecent(objRC.getRenkoChart(),2) < getRecent(sma,2) &&
                getRecent(objRC.getRenkoChart()) > getRecent(sma)) {

            //check if oversold before we buy
            if (getRecent(objRSI.getRsiVal()) < OVERSOLD)
                agent.setBuy(true);
        }

        //display our data
        displayData(agent, agent.hasBuy());
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //if the current renko chart price is less than sma we have bearish trend
        if (getRecent(objRC.getRenkoChart()) < getRecent(sma))
            adjustHardStopPrice(agent, currentPrice);

        //if we reached overbought, protect investment
        if (getRecent(objRSI.getRsiVal()) >= OVERBOUGHT)
            adjustHardStopPrice(agent, currentPrice);

        //if we were overbought and are now below overbought, it is time to sell
        if (getRecent(objRSI.getRsiVal(), 2) >= OVERBOUGHT && getRecent(objRSI.getRsiVal()) < OVERBOUGHT)
            agent.setReasonSell(ReasonSell.Reason_Strategy);

        //display our data
        displayData(agent, agent.getReasonSell() != null);
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display info
        objRSI.displayData(agent, write);
        objRC.displayData(agent, write);
    }

    @Override
    public void calculate(List<Period> history, int newPeriods) {

        //perform calculations
        objRSI.calculate(history, newPeriods);
        objRC.calculate(history, newPeriods);
        calculateSMA(objRC.getRenkoChart(), sma, newPeriods, periodsSMA);
    }
}