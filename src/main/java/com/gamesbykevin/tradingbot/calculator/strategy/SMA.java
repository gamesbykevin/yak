package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.Period.Fields;

import java.util.List;

public class SMA {

    protected static void calculateSMA(List<Double> data, List<Double> populate, int periods) {

        //clear the list
        populate.clear();

        for (int i = 0; i <= data.size(); i++) {

            //skip if we don't have enough data
            if (i < periods)
                continue;

            double sum = 0;

            //go back our desired number of periods
            for (int x = i - periods; x < i; x++) {
                sum += data.get(x);
            }

            //add the average to our list
            populate.add(sum / (float)periods);
        }
    }

    protected static void calculateSMA(List<Period> history, List<Double> populate, int periods, Fields field) {

        //clear the list
        populate.clear();

        //check all data when calculating
        for (int i = 0; i <= history.size(); i++) {

            //skip until we have enough data to calculate
            if (i < periods)
                continue;

            //calculate the sma value with the given values
            double sma = calculateSMA(history, i, periods, field);

            //add the sma value to our list
            populate.add(sma);
        }
    }

    /**
     * Calculate the SMA (simple moving average)
     * @param history Our trading data
     * @param currentPeriod The desired start period of the SMA we want
     * @param periods The number of periods to check back
     * @param field The field we want to calculate
     * @return The average of the sum of the specified field within the specified period
     */
    protected static double calculateSMA(List<Period> history, int currentPeriod, int periods, Fields field) {

        //the total sum
        double sum = 0;

        //check every period
        for (int i = currentPeriod - periods; i < currentPeriod; i++) {

            //get the period
            Period period = history.get(i);

            //what field are we adding to our sum
            switch (field) {

                case Open:
                    sum += period.open;
                    break;

                case Close:
                    sum += period.close;
                    break;

                case Low:
                    sum += period.low;
                    break;

                case High:
                    sum += period.high;
                    break;

                case Time:
                    sum += period.time;
                    break;

                case Volume:
                    sum += period.volume;
                    break;

                default:
                    throw new RuntimeException("Field not handled: " + field);
            }
        }

        //return the average of the sum
        return (sum / (double)periods);
    }
}