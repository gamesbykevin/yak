package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;

import java.util.List;

/**
 * Negative volume index / Positive volume index
 */
public class NP extends Strategy {

    /**
     * How many periods do we calculate for our ema
     */
    private static final int PERIODS_EMA = 255;

    //our negative volume object
    private NVI nviObj;

    //our positive volume object
    private PVI pviObj;

    public NP() {
        this(PERIODS_EMA);
    }

    public NP(int periods) {

        //call default value
        super(periods);

        //create new objects
        this.nviObj = new NVI(PERIODS_EMA);
        this.pviObj = new PVI(PERIODS_EMA);
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