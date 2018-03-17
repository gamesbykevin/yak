package com.gamesbykevin.tradingbot.calculator;

import java.util.List;

public class CalculatorHelper {

    /**
     * Check the history <br>
     * We will do 3 things<br>
     * 1) Add the period if it doesn't exist in the history
     * 2) Sort the list so the most recent period is at the end of the array list
     * 3) Verify the list to make sure the gap between each period matches our duration
     * @param history Our current list of history periods
     * @param period The period we want to check
     * @return true if no issues were found, false if there is a gap between periods
     */
    protected static void checkHistory(List<Period> history, Period period) {

        //did we find an existing period
        boolean match = false;

        //check every period
        for (int i = 0; i < history.size(); i++) {

            //if the time matches it already exists
            if (history.get(i).time == period.time) {

                //flag match
                match = true;

                //exit loop
                break;
            }
        }

        //if there was a match we don't need to add
        if (match)
            return;

        //since it wasn't found, add it to the list
        history.add(period);
    }

    /**
     * Sort the list so the most recent period is at the end of the array list
     * @param history Our current list of history periods
     */
    protected static void sortHistory(List<Period> history) {

        //sort so the periods are in order from oldest to newest
        for (int x = 0; x < history.size(); x++) {
            for (int y = x; y < history.size() - 1; y++) {

                //get the current and next period
                Period tmp1 = history.get(x);
                Period tmp2 = history.get(y + 1);

                //if the next object does not have a greater time, we need to swap
                if (tmp1.time > tmp2.time) {

                    //swap the values
                    history.set(x,     tmp2);
                    history.set(y + 1, tmp1);
                }
            }
        }
    }

    /**
     * Get the current trend
     * @param upwardTrend Are we checking for an upward trend
     * @param history Arraylist of historical periods
     * @param currentPrice Current price
     * @param periods Number of periods to check
     * @return If price is constantly upward "Trend.Upward", if price is constantly downward "Trend.Downward", else "Trend.None"
     */
    protected static boolean hasTrend(boolean upwardTrend, List<Period> history, double currentPrice, int periods) {

        //if not large enough skip, this shouldn't happen
        if (history.size() < periods || history.isEmpty())
            return false;

        //we want to start here
        Period begin = history.get(history.size() - periods);

        //if the first price is greater than the current price, there is no uptrend
        if (upwardTrend && begin.close > currentPrice)
            return false;

        //if the first price is less than the current price, there is no downtrend
        if (!upwardTrend && begin.close < currentPrice)
            return false;

        //our coordinates to calculate slope
        final double x1 = 0, y1, x2 = periods, y2;

        //are we detecting and upward or downward trend?
        if (upwardTrend) {
            y1 = begin.low;
            y2 = currentPrice;
        } else {
            y1 = begin.high;
            y2 = currentPrice;
        }

        //the value of y when x = 0
        final double yIntercept = y1;

        //calculate slope
        final double slope = (y2 - y1) / (x2 - x1);

        //check and see if every period is above the slope indicating an upward trend
        for (int i = history.size() - periods; i < history.size(); i++) {

            //get the current period
            Period current = history.get(i);

            //the current x-coordinate
            final double x = i - (history.size() - periods);

            //calculate the y-coordinate
            final double y = (slope * x) + yIntercept;

            //are we checking for an upward trend
            if (upwardTrend) {

                //if the current low is below the calculated y-coordinate slope we have a break
                if (current.low < y)
                    return false;

            } else {

                //if the current high is above the calculated y-coordinate slope we have a break
                if (current.high > y)
                    return false;
            }
        }

        //we have a trend
        return true;
    }

    protected static synchronized boolean hasDivergence(List<Period> history, boolean uptrend, int periods, List<Double> values, double currentValue) {

        //flag we will use to track if the price is following the desired trend
        boolean betterPrice = true;

        //the current price is the most recent closing price from our history
        double currentPrice = history.get(history.size() - 1).close;

        //check all recent periods
        for (int i = history.size() - (periods + 1); i < history.size() - 1; i++) {

            //get the current period
            Period period = history.get(i);

            if (uptrend) {

                //if we are checking for an uptrend we don't want any "high" price higher than our current price
                if (period.high > currentPrice) {
                    betterPrice = false;
                    break;
                }

            } else {

                //if we are checking for a downtrend we don't want any "low" price lower than our current price
                if (period.low < currentPrice) {
                    betterPrice = false;
                    break;
                }
            }
        }

        //if we don't have a better price, we don't have a divergence
        if (!betterPrice)
            return false;

        //is the current value the best whether it's an up or down trend?
        final boolean betterValue = isCurrentBest(values, periods, currentValue, uptrend);

        //if the price is better but the value isn't that means we have a divergence
        return (betterPrice && !betterValue);
    }

    private static boolean isCurrentBest(List<Double> list, int periods, double currentValue, boolean uptrend) {

        //look at all our periods
        for (int i = list.size() - periods; i < list.size(); i++) {

            if (uptrend) {

                //if checking uptrend we don't want any values higher
                if (list.get(i) > currentValue)
                    return false;

            } else {

                //if checking downtrend we don't want any values lower
                if (list.get(i) < currentValue)
                    return false;
            }
        }

        //yep the current value is the best
        return true;
    }
}