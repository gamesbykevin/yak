package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;

import java.util.List;

/**
 * 2 period RSI
 */
public class TWO_RSI extends Strategy {

    //our rsi object
    private RSI rsiObj;

    //list of configurable values
    private static final double MIN_RSI = 5.0d;
    private static final double MAX_RSI = 95.0d;
    private static final int PERIODS_RSI = 2;

    private final double minRSI, maxRSI;

    public TWO_RSI() {
        this(MIN_RSI, MAX_RSI, PERIODS_RSI);
    }

    public TWO_RSI(double minRSI, double maxRSI, int periodsRSI) {
        this.minRSI = minRSI;
        this.maxRSI = maxRSI;

        //create new list
        this.rsiObj = new RSI(periodsRSI);
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //buy
        if (getRecent(rsiObj.getRsiVal()) < minRSI)
            agent.setBuy(true);

        //display our data
        displayData(agent, agent.hasBuy());
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //sell
        if (getRecent(rsiObj.getRsiVal()) > maxRSI)
            agent.setReasonSell(ReasonSell.Reason_Strategy);

        //display our data
        displayData(agent, agent.getReasonSell() != null);
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display the information
        rsiObj.displayData(agent, write);
    }

    @Override
    public void calculate(List<Period> history) {

        //perform our calculations
        rsiObj.calculate(history);
    }
}