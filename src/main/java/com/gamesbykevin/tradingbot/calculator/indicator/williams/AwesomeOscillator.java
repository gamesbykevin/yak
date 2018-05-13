package com.gamesbykevin.tradingbot.calculator.indicator.williams;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.indicator.Indicator;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.SMA;

import java.util.List;

public class AwesomeOscillator extends Indicator {

    //our simple moving average indicators
    private SMA smaShort, smaLong;

    //configurable values
    private static final int PERIODS_SMA_LONG = 34;
    private static final int PERIODS_SMA_SHORT = 5;

    public AwesomeOscillator() {
        this(PERIODS_SMA_SHORT, PERIODS_SMA_LONG);
    }

    public AwesomeOscillator(int periodsSmaShort, int periodsSmaLong) {

        //create our indicators
        this.smaShort = new SMA(periodsSmaShort);
        this.smaLong = new SMA(periodsSmaLong);
    }

    @Override
    public void calculate(List<Period> history, int newPeriods) {

        //smaLong.c
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display our data
        smaShort.displayData(agent, write);
        smaLong.displayData(agent, write);
    }

    @Override
    public void cleanup() {
        smaShort.cleanup();
        smaLong.cleanup();
    }
}