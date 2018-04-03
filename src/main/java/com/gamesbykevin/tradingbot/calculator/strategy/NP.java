package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;

import java.util.List;

/**
 * Negative volume index / Positive volume index
 */
public class NP extends Strategy {

    //our list of variations
    protected static int[] LIST_PERIODS_EMA_NVI = {50};
    protected static int[] LIST_PERIODS_EMA_PVI = {50};

    //our negative volume object
    private NVI nviObj;

    //our positive volume object
    private PVI pviObj;

    public NP() {

        //call parent
        super();

        //create new objects
        this.nviObj = new NVI();
        this.pviObj = new PVI();
    }

    @Override
    public String getStrategyDesc() {
        return "PERIODS_EMA_NVI = " + LIST_PERIODS_EMA_NVI[getIndexStrategy()] + ", PERIODS_EMA_PVI = " + LIST_PERIODS_EMA_PVI[getIndexStrategy()];
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //check the NVI for bullish signals
        this.nviObj.checkBuySignal(agent, history, currentPrice);
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //check the PVI for bearish signals
        this.pviObj.checkSellSignal(agent, history, currentPrice);
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display the information (if needed)
        this.pviObj.displayData(agent, write);
        this.nviObj.displayData(agent, write);
    }

    @Override
    public void calculate(List<Period> history) {

        //perform our calculations
        this.pviObj.calculate(history);
        this.nviObj.calculate(history);
    }
}