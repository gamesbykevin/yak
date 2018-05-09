package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.indicator.momentun.RSI;
import com.gamesbykevin.tradingbot.calculator.indicator.volatility.ATR;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;

import java.util.List;

/**
 * Average True Range / Relative Strength Index
 */
public class AR extends Strategy {

    //our indicator objects
    private ATR objATR;
    private RSI objRSI;

    //configurable values
    private static final int PERIODS_ATR = 10;
    private static final int PERIODS_RSI = 7;

    private static final float OVERBOUGHT = 75.0f;
    private static final float OVERSOLD = 25.0f;

    public AR() {
        this(PERIODS_ATR, PERIODS_RSI);
    }

    public AR(int periodsATR, int periodsRSI) {

        //create our new indicator objects
        this.objATR = new ATR(periodsATR);
        this.objRSI = new RSI(periodsRSI);
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //make sure we just came out of oversold territory
        if (getRecent(objRSI.getRsiVal(), 2) < OVERSOLD && getRecent(objRSI.getRsiVal()) > OVERSOLD) {

            //let's buy
            agent.setBuy(true);

            //adjust our hard stop based on average true range
            adjustHardStopPrice(agent, history.get(history.size() - 1).close - getRecent(objATR.getTrueRangeAverage()));
        }

        //display our data
        displayData(agent, agent.hasBuy());
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //make sure we just came out of over bought territory before selling
        if (getRecent(objRSI.getRsiVal(), 2) > OVERBOUGHT && getRecent(objRSI.getRsiVal()) < OVERBOUGHT)
            agent.setReasonSell(ReasonSell.Reason_Strategy);

        //if over bought adjust our hard stop price
        if (getRecent(objRSI.getRsiVal()) > OVERBOUGHT)
            adjustHardStopPrice(agent, currentPrice);

        //display our data
        displayData(agent, agent.getReasonSell() != null);
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display the information
        objATR.displayData(agent, write);
        objRSI.displayData(agent, write);
    }

    @Override
    public void calculate(List<Period> history) {

        //do our calculations
        objATR.calculate(history);
        objRSI.calculate(history);
    }
}