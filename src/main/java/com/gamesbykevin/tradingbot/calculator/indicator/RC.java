package com.gamesbykevin.tradingbot.calculator.indicator;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
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

    //list of configurable values
    public static int PERIODS_ATR = 14;

    public RC() {
        this(PERIODS_ATR);
    }

    public RC(int periodsATR) {

        //create our object
        this.objATR = new ATR(periodsATR);

        //create new list
        this.renkoChart = new ArrayList<>();
    }

    public List<Double> getRenkoChart() {
        return this.renkoChart;
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display the information
        objATR.displayData(agent, write);
        display(agent, "Renko Chart $", getRenkoChart(), write);
    }

    @Override
    public void calculate(List<Period> history) {

        //clear the list
        getRenkoChart().clear();

        //calculate average true range
        objATR.calculate(history);

        //find the offset
        int difference = (history.size() - objATR.getAverageTrueRange().size());

        for (int i = difference; i < history.size(); i++) {

            //get the current average true range
            double atr = objATR.getAverageTrueRange().get(i - difference);

            //get the current period
            Period period = history.get(i);

            if (getRenkoChart().isEmpty()) {
                getRenkoChart().add(period.close > period.open ? period.close + atr : period.close - atr);
            } else {

                //get the previous value
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
    }
}