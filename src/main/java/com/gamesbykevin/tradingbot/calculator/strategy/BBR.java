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

    //our list of variations
    protected static float[] LIST_RESISTANCE_LINE = {55.0f};
    protected static float[] LIST_SUPPORT_LINE = {45.0f};
    protected static int[] LIST_PERIODS_RSI = {12};
    protected static int[] LIST_PERIODS_BB = {20};

    //list of configurable values
    protected static float RESISTANCE_LINE = 55.0f;
    protected static float SUPPORT_LINE = 45.0f;

    public BBR() {

        //call parent
        super();

        //create new objects
        this.bbObj = new BB();
        this.rsiObj = new RSI();
    }

    @Override
    public String getStrategyDesc() {
        return "RESISTANCE_LINE = " + LIST_RESISTANCE_LINE[getIndexStrategy()] + ", SUPPORT_LINE = " + LIST_SUPPORT_LINE[getIndexStrategy()] + ", PERIODS_RSI = " + LIST_PERIODS_RSI[getIndexStrategy()] + ", PERIODS_BB = " + LIST_PERIODS_BB[getIndexStrategy()];
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //we won't try to buy unless we are below the support line
        if (getRecent(rsiObj.getRsiVal()) < SUPPORT_LINE) {

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
        if (getRecent(rsiObj.getRsiVal()) > RESISTANCE_LINE) {

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