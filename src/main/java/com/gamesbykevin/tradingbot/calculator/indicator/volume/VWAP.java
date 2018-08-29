package com.gamesbykevin.tradingbot.calculator.indicator.volume;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Calculator.Candle;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.indicator.Indicator;

import java.util.ArrayList;
import java.util.List;

/**
 * Volume weighted average price
 */
public class VWAP extends Indicator {

    //cumulative total
    private List<Double> cumulativeTotal;

    //cumulative volume
    private List<Double> cumulativeVolume;

    //our final result
    private List<Double> vwap;

    public VWAP() {

        //call parent
        super(Key.VWAP, 0);
    }

    private List<Double> getCumulativeTotal() {

        //instantiate if null
        if (this.cumulativeTotal == null)
            this.cumulativeTotal = new ArrayList<>();

        return this.cumulativeTotal;
    }

    private List<Double> getCumulativeVolume() {

        //instantiate if null
        if (this.cumulativeVolume == null)
            this.cumulativeVolume = new ArrayList<>();

        return this.cumulativeVolume;
    }

    public List<Double> getVwap() {

        //instantiate if null
        if (this.vwap == null)
            this.vwap = new ArrayList<>();

        return this.vwap;
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display the information
        display(agent, "VWAP Cum Total $: ", getCumulativeTotal(), write);
        display(agent, "VWAP Cum Vol   $: ", getCumulativeVolume(), write);
        display(agent, "VWAP           $: ", getVwap(), write);
    }

    @Override
    public void calculate(List<Period> history, int newPeriods) {

        //clear our lists
        getCumulativeVolume().clear();
        getCumulativeTotal().clear();
        getVwap().clear();

        //get the most recent time
        long time = history.get(history.size() - 1).time;

        for (int i = 0; i < history.size(); i++) {

            //get the current period
            Period period = history.get(i);

            //we only want to calculate the previous day
            if (period.time < time - Candle.TwentyFourHours.duration)
                continue;

            //calculate typical price
            double typicalPrice = ((period.high + period.low + period.close) / 3);

            //multiply by typical price
            double priceVolume = typicalPrice * period.volume;

            //add cumulative total price volume to our list
            if (getCumulativeTotal().isEmpty()) {

                //add current to list if empty
                getCumulativeTotal().add(priceVolume);

            } else {

                //we take the previous value and add the current for a new value
                getCumulativeTotal().add(priceVolume + getRecent(getCumulativeTotal()));

            }

            //add cumulative volume to the list
            if (getCumulativeVolume().isEmpty()) {

                //add current volume if empty
                getCumulativeVolume().add(period.volume);

            } else {

                //else we add it to the previous value
                getCumulativeVolume().add(period.volume + getRecent(getCumulativeVolume()));
            }

            //the (total price volume) divided by (cumulative volume) is our final result
            getVwap().add(getRecent(getCumulativeTotal()) / getRecent(getCumulativeVolume()));
        }
    }

    @Override
    public void cleanup() {
        cleanup(getCumulativeVolume());
        cleanup(getCumulativeTotal());
        cleanup(getVwap());
    }
}