package com.gamesbykevin.tradingbot.calculator.indicator.volume;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.indicator.Indicator;
import com.gamesbykevin.tradingbot.calculator.strategy.Strategy;

import java.util.ArrayList;
import java.util.List;

/**
 * On balance volume
 */
public class OBV extends Indicator {

    //keep a historical list of the indicator
    private List<Double> volume;

    public OBV() {

        //create list
        this.volume = new ArrayList<>();
    }

    public List<Double> getVolume() {
        return this.volume;
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display the volume
        display(agent, "OBV: ", getVolume(), write);
    }

    @Override
    public void calculate(List<Period> history) {

        //clear our historical list
        getVolume().clear();

        //calculate the obv for each period
        for (int i = 1; i < history.size(); i++) {

            //get the latest volume value
            double volume = getVolume().isEmpty() ? history.get(i).volume : getRecent(getVolume());

            //what is the current volume change
            double change;

            if (history.get(i).close > history.get(i - 1).close) {

                //if the current $ is greater than previous $ change is positive
                change = history.get(i).volume;

            } else if (history.get(i).close < history.get(i - 1).close) {

                //if the current $ is less than previous $ change is negative
                change = -history.get(i).volume;

            } else {

                //no change
                change = 0;
            }

            //add the change to the latest volume value
            getVolume().add(volume + change);
        }
    }
}