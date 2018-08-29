package com.gamesbykevin.tradingbot.calculator.indicator.williams;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.indicator.Indicator;

import java.util.ArrayList;
import java.util.List;

/**
 *Market Facilitation Index
 *
 * Volume	Index 	Terms
 *    +     + 	    Green
 *    -     - 	    Fade
 *    -     + 	    Fake
 *    +     - 	    Squat
 *
 */
public class MarketFacilitationIndex extends Indicator {

    //list of our index values
    private List<Double> marketFacilitationIndex;

    /**
     * Default constructor
     */
    public MarketFacilitationIndex() {

        //call parent
        super(Indicator.Key.MFI,0);

        //create a new list
        this.marketFacilitationIndex = new ArrayList<>();
    }

    public List<Double> getMarketFacilitationIndex() {
        return this.marketFacilitationIndex;
    }

    @Override
    public void calculate(List<Period> history, int newPeriods) {

        //where do we start?
        int start = getMarketFacilitationIndex().isEmpty() ? 0 : history.size() - newPeriods;

        //calculate our values
        for (int index = start; index < history.size(); index++) {

            //get the current period
            Period period = history.get(index);

            //calculate the num and den
            double numerator = period.high - period.low;
            double denominator = period.volume;

            //add the value to our list
            if (numerator <= 0 || denominator <= 0) {
                getMarketFacilitationIndex().add(0d);
            } else {
                getMarketFacilitationIndex().add(numerator / denominator);
            }
        }
    }

    @Override
    public void displayData(Agent agent, boolean write) {
        display(agent, "MFI: ", getMarketFacilitationIndex(), write);
    }

    @Override
    public void cleanup() {
        cleanup(getMarketFacilitationIndex());
    }
}