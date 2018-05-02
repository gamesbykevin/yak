package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.calculator.Period;

import java.util.List;

/**
 * Slope
 */
public class SL {

    public static float getSlope(double x1, double x2, double y1, double y2) {
        return (float)(y2 - y1) / (float)(x2 - x1);
    }

    protected static void calculateSlope(List<Double> data, List<Float> populate) {

        //clear the list before we populate it
        populate.clear();

        //calculate the slope between each data point
        for (int i = 0; i < data.size() - 1; i++) {

            //we need this data to get our slope
            double x1 = i;
            double x2 = i + 1;
            double y1 = data.get(i);
            double y2 = data.get(i + 1);

            //add the slope to our list
            populate.add(getSlope(x1, x2, y1, y2));
        }
    }
}