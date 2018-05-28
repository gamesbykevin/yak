package com.gamesbykevin.tradingbot.calculator.indicator.volume;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.indicator.Indicator;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.EMA;

import java.util.ArrayList;
import java.util.List;

public class PVI extends Indicator {

    //our cumulative and ema lists
    private List<Double> cumulative;

    //our ema object
    private EMA objEMA;

    //list of configurable values
    private static final int PERIODS = 200;

    public PVI() {
        this(PERIODS);
    }

    public PVI(int periods) {

        //call parent
        super(Indicator.Key.PVI,0);

        //create new objects
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
        display(agent, "PVI Cum: ", getCumulative(), write);
        display(agent, "PVI Ema (" + objEMA.getPeriods() + "): ", objEMA.getEma(), write);
    }

    @Override
    public void calculate(List<Period> history, int newPeriods) {

        //start at 1,000
        double pvi = 1000;

        //where do we start?
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

            //we only update cumulative pvi if the volume increases
            if (changeVolume > 0) {
                pvi += changePrice;
            }

            //add the nvi value to our list
            getCumulative().add(pvi);
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
