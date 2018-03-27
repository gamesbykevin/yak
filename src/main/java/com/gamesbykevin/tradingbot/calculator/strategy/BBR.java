package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;

import java.util.List;

import static com.gamesbykevin.tradingbot.calculator.CalculatorHelper.hasCrossover;

/**
 * Bollinger Bands / RSI
 */
public class BBR extends Strategy {

    //our bollinger bands and rsi objects
    private BB bbObj;
    private RSI rsiObj;

    /**
     * RSI Resistance line indicating our stock is overbought
     */
    private static final double RESISTANCE_LINE = 55.0d;

    /**
     * RSI Support line indicating our stock is oversold
     */
    private static final double SUPPORT_LINE = 45.0d;

    /**
     * How many RSI periods we are calculating
     */
    private static final int PERIODS_RSI = 16;

    /**
     * How many BB periods we are calculating
     */
    private static final int PERIODS_BB = 20;

    //our resistance and support lines
    private final double resistance, support;

    public BBR() {
        this(PERIODS_BB, PERIODS_RSI, RESISTANCE_LINE, SUPPORT_LINE);
    }

    public BBR(int periodsBB, int periodsRSI, double resistance, double support) {

        //call parent with default value
        super(0);

        //store our data
        this.resistance = resistance;
        this.support = support;

        //create new objects
        this.bbObj = new BB(periodsBB);
        this.rsiObj = new RSI(periodsRSI);
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //we won't try to buy unless we are below the support line
        if (getRecent(rsiObj.getRsiVal()) < support) {

            //check the previous 2 lower values
            double previous = getRecent(bbObj.getLower(), 2);
            double current = getRecent(bbObj.getLower());

            //if the current price was below the previous, then current price crosses above the current
            if (hasCrossover(true, previous, currentPrice, current, currentPrice))
                agent.setBuy(true);
        }

        //display our data
        displayData(agent, agent.hasBuy());
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //we won't try to sell unless we are above the resistance line
        if (getRecent(rsiObj.getRsiVal()) > resistance) {

            //check the previous 2 upper values
            double previous = getRecent(bbObj.getUpper(), 2);
            double current = getRecent(bbObj.getUpper());

            //if the price was above the previous upper value, then crosses below it
            if (hasCrossover(false, previous, currentPrice, current, currentPrice))
                agent.setReasonSell(ReasonSell.Reason_Strategy);
        }

        //display our data
        displayData(agent, agent.getReasonSell() != null);
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display the information
        this.bbObj.displayData(agent, write);
        this.rsiObj.displayData(agent, write);
    }

    @Override
    public void calculate(List<Period> history) {

        //do our calculations
        this.bbObj.calculate(history);
        this.rsiObj.calculate(history);
    }
}