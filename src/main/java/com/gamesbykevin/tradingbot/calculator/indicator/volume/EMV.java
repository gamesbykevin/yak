package com.gamesbykevin.tradingbot.calculator.indicator.volume;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.indicator.Indicator;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.SMA;

import java.util.ArrayList;
import java.util.List;

/**
 * Ease of Movement
 */
public class EMV extends Indicator {

    //list of configurable values
    private static final int PERIODS_EMV = 14;

    /**
     * What is the default volume we used to calculate the box ratio
     */
    protected static double VOLUME_DEFAULT = 100000000d;

    //list of emv values
    private List<Double> emvVal;

    //our simple moving average
    private SMA objSMA;

    public EMV() {
        this(PERIODS_EMV);
    }

    public EMV(int periods) {

        //call parent
        super(Indicator.Key.EMV,0);

        //create our objects
        this.objSMA = new SMA(periods);

        //create new lists
        this.emvVal = new ArrayList<>();
    }

    public List<Double> getEmvSma() {
        return this.objSMA.getSma();
    }

    public List<Double> getEmv() {
        return this.emvVal;
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display the information
        display(agent, "EMV: ", getEmv(), write);
        display(agent, "SMA EMV (" + objSMA.getPeriods() + "): ", getEmvSma(), write);
    }

    @Override
    public void calculate(List<Period> history, int newPeriods) {

        //where do we start
        int start = getEmv().isEmpty() ? 0 : history.size() - newPeriods;

        for (int i = start; i < history.size(); i++) {

            //we need to be able to look at the previous value
            if (i < 1)
                continue;

            //get the current and previous periods
            Period current = history.get(i);
            Period previous = history.get(i - 1);

            //calculate distance moved
            double distance = ((current.high + current.low) / 2.0d) - ((previous.high + previous.low) / 2.0d);

            //calculate the box ratio
            double ratio = (getRecent(history, Period.Fields.Volume) / VOLUME_DEFAULT) / (current.high - current.low);

            //calculate our emv value
            double emv = (distance / ratio);

            //add the new value to our list
            getEmv().add(emv);
        }

        //now that we have our list of emv values, calculate sma
        objSMA.calculateSMA(getEmv(), newPeriods);
    }

    @Override
    public void cleanup() {
        cleanup(getEmv());
        objSMA.cleanup();
    }
}