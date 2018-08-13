package com.gamesbykevin.tradingbot.calculator.indicator.williams;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.indicator.Indicator;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.SMA;

import java.util.ArrayList;
import java.util.List;

public class AccelerationDecelerationOscillator extends Indicator {

    //our awesome oscillator indicator
    private AwesomeOscillator objAwesomeOscillator;

    //our simple moving average indicator
    private SMA objSimpleMovingAverage;

    //our oscillator value
    private List<Double> oscillator;

    /**
     * How many periods is our simple moving average
     */
    private static final int PERIODS_SMA = 5;

    public AccelerationDecelerationOscillator() {
        this(PERIODS_SMA);
    }

    public AccelerationDecelerationOscillator(int periods) {

        //call parent
        super(Indicator.Key.ADO,0);

        //create new indicators
        this.objAwesomeOscillator = new AwesomeOscillator();
        this.objSimpleMovingAverage = new SMA(periods);

        //create new list
        this.oscillator = new ArrayList<>();
    }

    public List<Double> getOscillator() {
        return this.oscillator;
    }

    @Override
    public void calculate(List<Period> history, int newPeriods) {

        //perform our calculations
        objAwesomeOscillator.calculate(history, newPeriods);
        objSimpleMovingAverage.calculateSMA(objAwesomeOscillator.getHistogram(), newPeriods);

        //subtract the 2 values to get our oscillator
        calculateOscillator(newPeriods);
    }

    private void calculateOscillator(int newPeriods) {

        //where do we start?
        int start = getOscillator().isEmpty() ? 0 : objSimpleMovingAverage.getSma().size() - newPeriods;

        //what is the difference between the 2
        int difference = objAwesomeOscillator.getHistogram().size() - objSimpleMovingAverage.getSma().size();

        //look at every value for our calculation
        for (int i = start; i < objSimpleMovingAverage.getSma().size(); i++) {

            //get our indicator values
            double ao = objAwesomeOscillator.getHistogram().get(i + difference);
            double aoSMA = objSimpleMovingAverage.getSma().get(i);

            //subtract to get our oscillator value
            getOscillator().add(ao - aoSMA);
        }
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display our information
        objAwesomeOscillator.displayData(agent, write);
        objSimpleMovingAverage.displayData(agent, write);
        display(agent, "AD Oscillator: ", getOscillator(), write);
    }

    @Override
    public void cleanup() {
        objAwesomeOscillator.cleanup();
        objSimpleMovingAverage.cleanup();
        cleanup(getOscillator());
    }
}