package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;

import java.util.ArrayList;
import java.util.List;

/**
 * Accumulation Distribution Line
 */
public class ADL extends Strategy {

    //our data for each period
    private List<Double> accumulationDistributionLine;

    public ADL() {

        //create a new list
        this.accumulationDistributionLine = new ArrayList<>();
    }

    public List<Double> getVolume() {
        return this.accumulationDistributionLine;
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //display our data
        displayData(agent, agent.hasBuy());
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //display our data
        displayData(agent, agent.getReasonSell() != null);
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display the information
        display(agent, "ADL: ", getVolume(), write);
    }

    @Override
    public void calculate(List<Period> history) {

        //clear our list
        getVolume().clear();

        for (int i = 0; i < history.size(); i++) {

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
}