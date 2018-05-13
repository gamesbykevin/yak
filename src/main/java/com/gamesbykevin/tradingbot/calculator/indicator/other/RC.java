package com.gamesbykevin.tradingbot.calculator.indicator.other;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.indicator.Indicator;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.SMA;
import com.gamesbykevin.tradingbot.calculator.indicator.volatility.ATR;

import java.util.ArrayList;
import java.util.List;

/**
 * Renko Charts
 */
public class RC extends Indicator {

    //our average true range object
    private ATR objATR;

    //the price of each period
    private List<Double> renkoChart;

    //our simple moving average
    private SMA objSMA;

    //number of periods for our sma
    private static final int PERIODS_SMA = 10;

    //list of configurable values
    public static int PERIODS_ATR = 14;

    public RC() {
        this(PERIODS_ATR, PERIODS_SMA);
    }

    public RC(int periodsATR, int periodsSMA) {

        //create our object(s)
        this.objATR = new ATR(periodsATR);
        this.objSMA = new SMA(periodsSMA);

        //create new list
        this.renkoChart = new ArrayList<>();
    }

    public List<Double> getRenkoChart() {
        return this.renkoChart;
    }

    public SMA getRenkoChartSMA() {
        return this.objSMA;
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display the information
        objATR.displayData(agent, write);
        objSMA.displayData(agent, write);
        display(agent, "Renko Chart $", getRenkoChart(), write);
    }

    @Override
    public void calculate(List<Period> history, int newPeriods) {

        //calculate average true range
        objATR.calculate(history, newPeriods);

        //find the offset
        int difference = (history.size() - objATR.getAverageTrueRange().size());

        //where do we start?
        int start = getRenkoChart().isEmpty() ? 0 : history.size() - newPeriods;

        for (int i = start; i < history.size(); i++) {

            //get the current average true range
            double atr = objATR.getAverageTrueRange().get(i - difference);

            //get the current period
            Period period = history.get(i);

            if (getRenkoChart().isEmpty()) {

                //if the list is empty add our initial value
                getRenkoChart().add(period.close > period.open ? period.close + atr : period.close - atr);

            } else {

                //get the previous renko value
                double previous = getRenkoChart().get(getRenkoChart().size() - 1);

                //adjust based on a bearish or bullish close
                if (period.close > period.open) {
                    previous += atr;
                } else {
                    previous -= atr;
                }

                //add the adjusted value to our list
                getRenkoChart().add(previous);
            }
        }

        //calculate our simple moving average
        objSMA.calculateSMA(getRenkoChart(), newPeriods);
    }

    @Override
    public void cleanup() {
        cleanup(getRenkoChart());
        objATR.cleanup();
        objSMA.cleanup();
    }
}