package com.gamesbykevin.tradingbot.calculator.indicator.volume;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.indicator.Indicator;

import java.util.ArrayList;
import java.util.List;

/**
 * Accumulation Distribution Line
 */
public class ADL extends Indicator {

    //our data for each period
    private List<Double> accumulationDistributionLine;

    public ADL() {

        //call parent
        super(Indicator.Key.ADL,0);

        //create a new list
        this.accumulationDistributionLine = new ArrayList<>();
    }

    public List<Double> getVolume() {
        return this.accumulationDistributionLine;
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display the information
        display(agent, "ADL: ", getVolume(), write);
    }

    @Override
    public void calculate(List<Period> history, int newPeriods) {

        //where do we start?
        int start = getVolume().isEmpty() ? 0 : history.size() - newPeriods;

        for (int i = start; i < history.size(); i++) {

            //calculate our money flow volume
            double volume = getMultiplier(history.get(i)) * history.get(i).volume;

            //calculate Accumulation Distribution Line
            if (getVolume().isEmpty()) {

                //if no previous values, the volume will be the initial
                getVolume().add(volume);

            } else {

                //add the previous volume to the current
                double newVolume = getRecent(getVolume()) + volume;

                //add our new volume to the list
                getVolume().add(newVolume);
            }
        }
    }

    private double getMultiplier(Period period) {

        //simplify our calculations
        double value1 = period.close - period.low;
        double value2 = period.high - period.close;
        double value3 = period.high - period.low;

        //return 0 if 0
        if (value3 == 0)
            return 0;
        if (value1 - value2 == 0)
            return 0;

        //return our result
        return (value1 - value2) / value3;
    }

    @Override
    public void cleanup() {
        cleanup(getVolume());
    }
}