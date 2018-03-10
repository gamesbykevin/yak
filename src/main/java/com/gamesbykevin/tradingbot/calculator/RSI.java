package com.gamesbykevin.tradingbot.calculator;

import java.util.List;

import static com.gamesbykevin.tradingbot.calculator.Calculator.PERIODS_RSI;

public class RSI {

    /**
     * Calcuate the calculator value for the specified range
     * @param history Our historical data
     * @param startIndex Beginning period
     * @param endIndex Ending period
     * @param current Are we calculating the current calculator? if false we just want the historical calculator
     * @param currentPrice The current price when calculating the current calculator, otherwise this field is not used
     * @return The calculator value
     */
    protected static double calculateRsi(List<Period> history, int startIndex, int endIndex, boolean current, double currentPrice) {

        //the length of our calculation
        final int size = endIndex - startIndex;

        //track total gains and losses
        float gain = 0, loss = 0;
        float gainCurrent = 0, lossCurrent = 0;

        //go through the periods to calculate calculator
        for (int i = startIndex; i < endIndex - 1; i++) {

            //prevent index out of bounds exception
            if (i + 1 >= endIndex)
                break;

            //get the next and previous prices
            double previous = history.get(i).close;
            double next     = history.get(i + 1).close;

            if (next > previous) {

                //here we have a gain
                gain += (next - previous);

            } else {

                //here we have a loss
                loss += (previous - next);
            }
        }

        //calculate the average gain and loss
        float avgGain = (gain / size);
        float avgLoss = (loss / size);

        //if we don't want the current calculator we can do simple moving average (SMA)
        if (!current) {

            //calculate relative strength
            final float rs = avgGain / avgLoss;

            //calculate relative strength index
            float rsi = 100 - (100 / (1 + rs));

            //return our calculator value
            return rsi;

        } else {

            //get the latest price in our list so we can compare to the current price
            final double recentPrice = history.get(endIndex - 1).close;

            //check if the current price is a gain or loss
            if (currentPrice > recentPrice) {
                gainCurrent = (float)(currentPrice - recentPrice);
            } else {
                lossCurrent = (float)(recentPrice - currentPrice);
            }

            //smothered calculator including current gain loss
            float smotheredRS =
                    (((avgGain * (size - 1)) + gainCurrent) / size)
                            /
                            (((avgLoss * (size - 1)) + lossCurrent) / size);

            //calculate our calculator value
            final float rsi = 100 - (100 / (1 + smotheredRS));

            //return our calculator value
            return rsi;
        }
    }

    /**
     * Calculate the calculator values for each period
     */
    protected static void calculateRsi(List<Period> history, List<Double> rsi) {

        //clear our historical calculator list
        rsi.clear();

        //calculate the calculator for each period
        for (int i = PERIODS_RSI; i >= 0; i--) {

            //we need to go back the desired number of periods
            final int startIndex = history.size() - (PERIODS_RSI + i);

            //we only go the length of the desired periods
            final int endIndex = startIndex + PERIODS_RSI;

            //get the calculator for this period
            final double tmpRsi = calculateRsi(history, startIndex, endIndex, false, 0);

            //add the calculator calculation to the list
            rsi.add(tmpRsi);
        }
    }
}