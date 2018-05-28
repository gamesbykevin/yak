package com.gamesbykevin.tradingbot.calculator.indicator.volume;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.indicator.Indicator;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.EMA;

import java.util.ArrayList;
import java.util.List;

public class NVI extends Indicator {

    //our cumulative and ema lists
    private List<Double> cumulative;

    //our ema object
    private EMA objEMA;

    //list of configurable values
    protected static int PERIODS = 200;

    public NVI() {
        this(PERIODS);
    }

    public NVI(int periods) {

        //call parent
        super(Indicator.Key.NVI, 0);

        //create new lists
        this.cumulative = new ArrayList<>();
        this.objEMA = new EMA(periods);
    }

    public List<Double> getCumulative() {
        return this.cumulative;
    }

    public List<Double> getEma() {
        return this.objEMA.getEma();
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display the information
        display(agent, "NVI Cum: ", getCumulative(), write);
        display(agent, "NVI Ema (" + objEMA.getPeriods() + "): ", getEma(), write);
    }

    @Override
    public void calculate(List<Period> history, int newPeriods) {

        //start at 1,000
        double nvi = 1000;

        //where do we start
        int start = getCumulative().isEmpty() ? 1 : history.size() - newPeriods;

        //check all of our periods
        for (int i = start; i < history.size(); i++) {

            //get the current and previous periods
            Period current = history.get(i);
            Period previous = history.get(i - 1);

            //calculate the percent volume change between the periods
            double changeVolume = ((current.volume - previous.volume) / previous.volume) * 100d;

            //calculate the percent price change between the periods
            double changePrice = ((current.close - previous.close) / previous.close) * 100.0d;

            //we only update cumulative nvi if the volume decreases
            if (changeVolume < 0) {
                nvi += changePrice;
            }

            //add the nvi value to our list
            getCumulative().add(nvi);
        }

        //now that we have our standard list, let's calculate ema
        objEMA.calculateEma(getCumulative(), newPeriods);
    }

    @Override
    public void cleanup() {
        cleanup(getCumulative());
        objEMA.cleanup();
    }
}
