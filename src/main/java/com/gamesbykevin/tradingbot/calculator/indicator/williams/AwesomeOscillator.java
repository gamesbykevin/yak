package com.gamesbykevin.tradingbot.calculator.indicator.williams;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.Period.Fields;
import com.gamesbykevin.tradingbot.calculator.indicator.Indicator;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.SMA;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

public class AwesomeOscillator extends Indicator {

    //our simple moving average indicators
    private SMA smaShort, smaLong;

    //our histogram list
    private List<Double> histogram;

    //configurable values
    private static final int PERIODS_SMA_LONG = 34;
    private static final int PERIODS_SMA_SHORT = 5;

    public AwesomeOscillator() {
        this(PERIODS_SMA_SHORT, PERIODS_SMA_LONG);
    }

    public AwesomeOscillator(int periodsSmaShort, int periodsSmaLong) {

        //create default fields list consisting of high / low
        this(periodsSmaShort, periodsSmaLong, new AbstractList<Fields>() {
            @Override
            public Fields get(int index) {
                if (index == 0) {
                    return Fields.High;
                } else if (index == 1) {
                    return Fields.Low;
                } else {
                    return null;
                }
            }

            @Override
            public int size() {
                return 2;
            }
        });
    }

    public AwesomeOscillator(int periodsSmaShort, int periodsSmaLong, List<Fields> fields) {

        //call parent
        super(Indicator.Key.AO,0);

        //create our indicators
        this.smaShort = new SMA(periodsSmaShort, fields);
        this.smaLong = new SMA(periodsSmaLong, fields);

        //create our histogram list
        this.histogram = new ArrayList<>();
    }

    public List<Double> getHistogram() {
        return this.histogram;
    }

    public SMA getSmaLong() {
        return this.smaLong;
    }

    public SMA getSmaShort() {
        return this.smaShort;
    }

    @Override
    public void calculate(List<Period> history, int newPeriods) {

        //perform our sma calculations first
        getSmaShort().calculate(history, newPeriods);
        getSmaLong().calculate(history, newPeriods);

        //now we can calculate our histogram
        calculateHistogram(newPeriods);
    }

    private void calculateHistogram(int newPeriods) {

        int start = (getHistogram().isEmpty()) ? 0 : getSmaLong().getSma().size() - newPeriods;

        //find the difference between the short and long lists
        int difference = getSmaShort().getSma().size() - getSmaLong().getSma().size();

        //subtract sma's to create the histogram
        for (int i = start; i < getSmaLong().getSma().size(); i++) {

            //get our short and long values
            double smaShort = getSmaShort().getSma().get(i + difference);
            double smaLong = getSmaLong().getSma().get(i);

            //add the difference to our histogram list
            getHistogram().add(smaShort - smaLong);
        }
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display our data
        getSmaShort().displayData(agent, write);
        getSmaLong().displayData(agent, write);
        display(agent, "Histogram: ", getHistogram(), write);
    }

    @Override
    public void cleanup() {

        //keep data storage minimal
        getSmaShort().cleanup();
        getSmaLong().cleanup();
        cleanup(getHistogram());
    }
}