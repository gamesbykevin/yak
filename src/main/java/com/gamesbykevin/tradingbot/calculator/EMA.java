package com.gamesbykevin.tradingbot.calculator;

import java.util.List;

import static com.gamesbykevin.tradingbot.calculator.Calculator.PERIODS_EMA_LONG;
import static com.gamesbykevin.tradingbot.calculator.Calculator.PERIODS_EMA_SHORT;

public class EMA {

    /**
     * Calculate the SMA (simple moving average)
     * @param currentPeriod The desired period of the SMA we want
     * @param periods The number of periods to check
     * @return The average of the sum of closing prices within the specified period
     */
    private static double calculateSMA(List<Period> history, int currentPeriod, int periods) {

        //the total sum
        double sum = 0;

        //number of prices we add together
        int count = 0;

        //check every period
        for (int i = currentPeriod - periods; i < currentPeriod; i++) {

            //add to the total sum
            sum += history.get(i).close;

            //keep track of how many we add
            count++;
        }

        //return the average of the sum
        return (sum / (double)count);
    }

    private static double calculateEMA(List<Period> history, int current, int periods, double emaPrevious) {

        //what is our multiplier
        final float multiplier = ((float)2 / ((float)periods + 1));

        //calculate simple moving average
        final double sma = calculateSMA(history, current - 1, periods);

        //this close price is the current price
        final double currentPrice = history.get(current).close;

        //calculate our ema
        final double ema;

        if (emaPrevious != 0) {
            ema = ((currentPrice - emaPrevious) * multiplier) + emaPrevious;
        } else {
            ema = ((currentPrice - sma) * multiplier) + sma;
        }

        //return our result
        return ema;
    }

    protected static void calculateEMA(List<Period> history, List<Double> emaList, int periods) {

        //clear our list
        emaList.clear();

        //populate the list
        for (int i = 0; i < periods; i++) {

            final double ema;

            if (i == 0) {
                ema = calculateEMA(history, history.size() - periods - i, periods, 0);
            } else {
                ema = calculateEMA(history, history.size() - periods - i, periods, emaList.get(emaList.size() - 1));
            }

            //add it to the list
            emaList.add(ema);
        }
    }
}