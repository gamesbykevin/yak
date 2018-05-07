package com.gamesbykevin.tradingbot.calculator.indicator.volume;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.indicator.Indicator;

import java.util.ArrayList;
import java.util.List;

import static com.gamesbykevin.tradingbot.calculator.indicator.trend.SMA.calculateSMA;

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
    private List<Double> smaEmv, valEmv;

    private final int periods;

    public EMV() {
        this(PERIODS_EMV);
    }

    public EMV(int periods) {

        this.periods = periods;

        //create new lists
        this.smaEmv = new ArrayList<>();
        this.valEmv = new ArrayList<>();
    }

    public List<Double> getEmvSma() {
        return this.smaEmv;
    }

    public List<Double> getEmv() {
        return this.valEmv;
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display the information
        display(agent, "    EMV: ", getEmv(), write);
        display(agent, "SMA EMV: ", getEmvSma(), write);
    }

    @Override
    public void calculate(List<Period> history) {

        //clear our list
        getEmv().clear();

        for (int i = 0; i < history.size(); i++) {

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
        calculateSMA(getEmv(), getEmvSma(), periods);
    }
}