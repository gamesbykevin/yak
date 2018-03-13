package com.gamesbykevin.tradingbot.calculator;

import java.util.List;

import static com.gamesbykevin.tradingbot.calculator.Calculator.PERIODS_EMA_LONG;
import static com.gamesbykevin.tradingbot.calculator.Calculator.PERIODS_OBV;

public class OBV {

    /**
     * Calculate the calculator values for each period
     */
    protected static void calculateOBV(List<Period> history, List<Double> volume) {

        //clear our historical list
        volume.clear();

        //calculate the obv for each period
        for (int i = 0; i < history.size(); i++) {

            //skip if not enough info
            if (i <= PERIODS_OBV)
                continue;

            //get the obv for this period
            final double tmpVolume = calculateOBV(history, i, PERIODS_OBV);

            //add the obv calculation to the list
            volume.add(tmpVolume);
        }
    }

    private static double calculateOBV(List<Period> history, int currentPeriod, int periods) {

        //the total sum
        double sum = 0;

        //check every period
        for (int i = currentPeriod - periods; i < currentPeriod - 1; i++) {

            Period prev = history.get(i);
            Period next = history.get(i + 1);

            if (next.close > prev.close) {

                //add to the total volume
                sum = sum + history.get(i).volume;

            } else if (next.close < prev.close) {

                //subtract from the total volume
                sum = sum - history.get(i).volume;
            }
        }

        //return the on balance volume
        return sum;
    }
}