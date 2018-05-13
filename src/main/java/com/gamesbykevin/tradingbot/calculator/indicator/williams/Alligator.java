package com.gamesbykevin.tradingbot.calculator.indicator.williams;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.indicator.Indicator;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.SMMA;

import java.util.List;

public class Alligator extends Indicator {

    //our configurable values
    public static final int PERIODS_JAW = 13;
    public static final int PERIODS_TEETH = 8;
    public static final int PERIODS_LIPS = 5;

    public static final int PERIODS_JAW_OFFSET = 8;
    public static final int PERIODS_TEETH_OFFSET = 5;
    public static final int PERIODS_LIPS_OFFSET = 3;

    //lists that make up the alligator
    private SMMA jaw, teeth, lips;

    public Alligator() {

        //create our smoothed moving average objects
        this.jaw = new SMMA(PERIODS_JAW);
        this.teeth = new SMMA(PERIODS_TEETH);
        this.lips = new SMMA(PERIODS_LIPS);
    }

    public SMMA getJaw() {
        return this.jaw;
    }

    public SMMA getLips() {
        return this.lips;
    }

    public SMMA getTeeth() {
        return this.teeth;
    }

    @Override
    public void calculate(List<Period> history, int newPeriods) {

        //perform our calculations
        getJaw().calculate(history, newPeriods);
        getLips().calculate(history, newPeriods);
        getTeeth().calculate(history, newPeriods);
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display our info
        getJaw().displayData(agent, write);
        getLips().displayData(agent, write);
        getTeeth().displayData(agent, write);
    }

    @Override
    public void cleanup() {

        //prevent our lists from growing too large
        getJaw().cleanup();
        getLips().cleanup();
        getTeeth().cleanup();
    }
}