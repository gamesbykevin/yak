package com.gamesbykevin.tradingbot.calculator;

import java.util.List;

import static com.gamesbykevin.tradingbot.calculator.Calculator.PERIODS_RSI;

public class RSI {

    /**
     * Calcuate the rsi value for the specified range
     * @param history Our historical data
     * @param startIndex Beginning period
     * @param endIndex Ending period
     * @return The rsi value
     */
    protected static double calculateRsi(List<Period> history, int startIndex, int endIndex) {

        //track total gains and losses
        float gain = 0, loss = 0;
        float gainCurrent = 0, lossCurrent = 0;

        //count the periods
        final int size = (endIndex - startIndex) - 1;

        //go through the periods to calculate rsi
        for (int i = startIndex; i < endIndex; i++) {

            //get the close prices to compare
            double previous = history.get(i - 1).close;
            double next     = history.get(i).close;

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

        //get the latest price in our list so we can compare to the current price
        final double recentPrice = history.get(endIndex - 1).close;

        //the recent period will be the current price
        final double currentPrice = history.get(endIndex).close;

        //check if the current price is a gain or loss
        if (currentPrice > recentPrice) {
            gainCurrent = (float)(currentPrice - recentPrice);
        } else {
            lossCurrent = (float)(recentPrice - currentPrice);
        }

        //smothered rsi including current gain loss
        float smotheredRS = (
                ((avgGain * size) + gainCurrent) / (size + 1)
        ) / (
                ((avgLoss * size) + lossCurrent) / (size + 1)
        );

        //calculate our rsi value
        final float rsi = 100 - (100 / (1 + smotheredRS));

        //return our rsi value
        return rsi;
    }

    /**
     * Calculate the rsi values for each period
     */
    protected static void calculateRsi(List<Period> history, List<Double> rsi) {

        //clear our historical rsi list
        rsi.clear();

        //calculate as many periods as we can
        for (int i = 0; i < history.size() - PERIODS_RSI; i++) {

            //skip if we don't have enough data
            if (i <= PERIODS_RSI)
                continue;

            //find the start and end periods
            final int start = i;
            final int end = start + PERIODS_RSI;

            //calculate the rsi for the given periods
            final double tmpRsi = calculateRsi(history, start, end);

            //add the rsi value to our list
            rsi.add(tmpRsi);
        }
    }
}