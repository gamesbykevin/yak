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

        //check every period
        for (int i = 0; i < history.size(); i++) {

            //if the time matches it already exists and no need to continue
            if (history.get(i).time == period.time)
                return;
        }

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
     * Do we have divergence?
     *
     * What we do to detect bullish divergence:
     * a) The first period the price is highest and each period afterwards is lower
     * b) The first period our data is the lowest and each period afterwards is higher
     *
     * What we do to detect bearish divergence:
     * a) The first period the price is the lowest and each period afterwards is higher
     * b) The first period the data is the highest and each period afterwards is lower
     *
     * @param history Stock price history
     * @param periods How many periods do we check
     * @param bullish Are we looking for a bullish sign of divergence, if false we are looking for bearish
     * @param data Array data of our indicator
     * @return true if divergence is found, false otherwise
     */
    public static synchronized boolean hasDivergence(List<Period> history, int periods, boolean bullish, List<Double> data) {

        if (bullish) {

            for (int i = history.size() - periods; i < history.size(); i++) {

                //at this point we want prices that are lower than the starting price
                if (history.get(i).close > history.get(history.size() - periods).close)
                    return false;
            }

            for (int i = data.size() - periods; i < data.size(); i++) {

                //at this point we want every data point to be larger than the first
                if (data.get(i) < data.get(data.size() - periods))
                    return false;
            }

            //the prices are lower and the indicator data is higher
            return true;

        } else {

            for (int i = history.size() - periods; i < history.size(); i++) {

                //at this point we want prices that are higher than the starting price
                if (history.get(i).close < history.get(history.size() - periods).close)
                    return false;
            }

            for (int i = data.size() - periods; i < data.size(); i++) {

                //at this point we want every data point to be smaller than the first
                if (data.get(i) > data.get(data.size() - periods))
                    return false;
            }

            //the price are higher and the indicator data is lower
            return true;
        }
    }

    /**
     * Do we have crossover?
     *
     * If bullish we look for this:
     * a) We check the previous data where fast < slow
     * b) Then we check the current data where fast > slow
     *
     * If bearish we look for this:
     * a) We check the previous data where slow < fast
     * b) Then we check the current data where slow > fast
     *
     * @param bullish Are we checking bullish? if false we are checking bearish
     * @param fast Array of fast moving data
     * @param slow Array of slow moving data
     * @return true if crossover, false otherwise
     */
    public static boolean hasCrossover(boolean bullish, List<Double> fast, List<Double> slow) {

        //our previous slow and fast values
        double previousSlow = slow.get(slow.size() - 2);
        double previousFast = fast.get(fast.size() - 2);

        //our current slow and fast values
        double currentSlow = slow.get(slow.size() - 1);
        double currentFast = fast.get(fast.size() - 1);

        //now check if we have a crossover
        return hasCrossover(bullish, previousSlow, previousFast, currentSlow, currentFast);
    }

    /**
     * Do we have a crossover?
     * @param bullish True: Check if value1 > value2 then value1 < value2, False value1 < value2 then value1 > value2
     * @param previousValue1
     * @param previousValue2
     * @param currentValue1
     * @param currentValue2
     * @return true if the specified bullish or bearish check mentioned above is verified, false otherwise
     */
    public static boolean hasCrossover(boolean bullish, double previousValue1, double previousValue2, double currentValue1, double currentValue2) {

        if (bullish) {

            //if the previous value1 > previous value2 and now the current value1 < current value2 we have a crossover
            if (previousValue1 > previousValue2 && currentValue1 < currentValue2)
                return true;

        } else {

            //if the previous value1 < previous value2 and now the current value1 > current value2 we have a crossover
            if (previousValue1 < previousValue2 && currentValue1 > currentValue2)
                return true;

        }

        //we did not find a crossover
        return false;
    }
}