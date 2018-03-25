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

    private BB bbObj;
    private RSI rsiObj;

    /**
     * RSI Resistance line indicating our stock is overbought
     */
    public static final double RESISTANCE_LINE = 55.0d;

    /**
     * RSI Support line indicating our stock is oversold
     */
    public static final double SUPPORT_LINE = 45.0d;

    /**
     * How many RSI periods we are calculating
     */
    public static final int PERIODS_RSI = 16;

    /**
     * How many BB periods we are calculating
     */
    public static final int PERIODS_BB = 20;

    public BBR() {

        //call parent with default value
        super(0);

        //create new objects
        this.bbObj = new BB(PERIODS_BB);
        this.rsiObj = new RSI(PERIODS_RSI);
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //we won't try to buy unless we are below the support line
        if (getRecent(rsiObj.getRsi()) < SUPPORT_LINE) {

            //check the previous 2 lower values
            double previous = getRecent(bbObj.getLower(), 2);
            double current = getRecent(bbObj.getLower());

            //if the price was below the previous lower value, then crosses above it
            if (hasCrossover(true, previous, currentPrice, current, current))
                agent.setBuy(true);
        }

        //display our data
        displayData(agent, agent.hasBuy());
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //we won't try to sell unless we are above the resistance line
        if (getRecent(rsiObj.getRsi()) > RESISTANCE_LINE) {

            //check the previous 2 upper values
            double previous = getRecent(bbObj.getUpper(), 2);
            double current = getRecent(bbObj.getUpper());

            //if the price was above the previous upper value, then crosses below it
            if (hasCrossover(false, previous, currentPrice, current, current))
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