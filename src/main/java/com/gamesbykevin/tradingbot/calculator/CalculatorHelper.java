package com.gamesbykevin.tradingbot.calculator;

import com.gamesbykevin.tradingbot.calculator.Period.Fields;

import java.util.List;

public class CalculatorHelper {

    public static void updateHistory(List<Period> history, double[][] data) {

        //parse each period from the data
        for (int row = data.length - 1; row >= 0; row--) {
            updateHistory(history, data[row]);
        }
    }

    public static void updateHistory(List<Period> history, double[] data) {

        //create and populate our period
        Period period = new Period();
        period.time = (long) data[0];
        period.low = data[1];
        period.high = data[2];
        period.open = data[3];
        period.close = data[4];
        period.volume = data[5];

        //check this period against our history and add if missing
        addHistory(history, period);
    }

    public static void addHistory(List<Period> history, Period period) {

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
    public static void sortHistory(List<Period> history) {

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

    public static synchronized double getValue(List<Period> data, Fields field, int index) {

        Period period = data.get(index);

        //which field do we want
        switch (field) {

            case Close:
                return period.close;

            case High:
                return period.high;

            case Open:
                return period.open;

            case Low:
                return period.low;

            case Time:
                return period.time;

            case Volume:
                return period.volume;

            default:
                throw new RuntimeException("Field not defined: " + field);
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
     * @param bullish True: Check if previousValue1 > previousValue2 then currentValue1 < currentValue2.
     *                False: Check if previousValue1 < previousValue2 then currentValue1 > currentValue2
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

    public static boolean hasTrendDownward(List<Period> data, Fields field, int periods) {

        //check for lower highs and lower lows
        Double high = null;
        Double low = null;

        //do we have success?
        boolean success = true;

        //now checking the rest of the data
        for (int index = data.size() - periods + 1; index < data.size(); index++) {

            //our current and previous values
            double curr, prev;

            switch (field) {

                case Volume:
                    curr = data.get(index).volume;
                    prev = data.get(index - 1).volume;
                    break;

                case Time:
                    curr = data.get(index).time;
                    prev = data.get(index - 1).time;
                    break;

                case Low:
                    curr = data.get(index).low;
                    prev = data.get(index - 1).low;
                    break;

                case Open:
                    curr = data.get(index).open;
                    prev = data.get(index - 1).open;
                    break;

                case Close:
                    curr = data.get(index).close;
                    prev = data.get(index - 1).close;
                    break;

                case High:
                    curr = data.get(index).high;
                    prev = data.get(index - 1).high;
                    break;

                default:
                    throw new RuntimeException("Field not managed " + field);
            }

            //if the current is > we are checking for lower gain
            if (curr > prev) {

                if (low != null && curr > low) {

                    //if the current loss is higher than the previous low, we don't have success
                    success = false;
                    break;

                } else {

                    //we have a new low
                    low = curr;

                }

            } else {

                if (high != null && curr > high) {

                    //if the current gain is higher than the previous high, we don't have success
                    success = false;
                    break;

                } else {

                    //we have a new high
                    high = curr;
                }
            }
        }

        //did we detect an downtrend
        return success;
    }

    public static boolean hasTrendDownward(List<Double> data, int periods) {

        //check for lower highs and lower lows
        Double high = null;
        Double low = null;

        //do we have success?
        boolean success = true;

        //now checking the rest of the data
        for (int index = data.size() - periods + 1; index < data.size(); index++) {

            double curr = data.get(index);
            double prev = data.get(index - 1);

            //if the current is > we are checking for lower gain
            if (curr > prev) {

                if (low != null && curr > low) {

                    //if the current loss is higher than the previous low, we don't have success
                    success = false;
                    break;

                } else {

                    //we have a new low
                    low = curr;

                }

            } else {

                if (high != null && curr > high) {

                    //if the current gain is higher than the previous high, we don't have success
                    success = false;
                    break;

                } else {

                    //we have a new high
                    high = curr;
                }
            }
        }

        //did we detect an downtrend
        return success;
    }

    public static boolean hasTrendUpward(List<Period> data, Fields field, int periods) {

        //check for lower highs and lower lows
        Double high = null;
        Double low = null;

        //do we have success?
        boolean success = true;

        //now checking the rest of the data
        for (int index = data.size() - periods + 1; index < data.size(); index++) {

            //our current and previous values
            double curr, prev;

            switch (field) {

                case Volume:
                    curr = data.get(index).volume;
                    prev = data.get(index - 1).volume;
                    break;

                case Time:
                    curr = data.get(index).time;
                    prev = data.get(index - 1).time;
                    break;

                case Low:
                    curr = data.get(index).low;
                    prev = data.get(index - 1).low;
                    break;

                case Open:
                    curr = data.get(index).open;
                    prev = data.get(index - 1).open;
                    break;

                case Close:
                    curr = data.get(index).close;
                    prev = data.get(index - 1).close;
                    break;

                case High:
                    curr = data.get(index).high;
                    prev = data.get(index - 1).high;
                    break;

                default:
                    throw new RuntimeException("Field not managed " + field);
            }

            //if current is > we are checking for a higher high
            if (curr > prev) {

                if (high != null && curr < high) {

                    //if the current gain is lower than the previous high, we don't have success
                    success = false;
                    break;

                } else {

                    //here is our new high
                    high = curr;
                }

            } else {

                if (low != null && curr < low) {

                    //if the current loss is lower than the previous high, we don't have success
                    success = false;
                    break;

                } else {

                    //here is our new low
                    low = curr;
                }
            }
        }

        //did we detect an uptrend
        return success;
    }

    public static boolean hasTrendUpward(List<Double> data, int periods) {

        //check for higher highs and higher lows
        Double high = null;
        Double low = null;

        //do we have success?
        boolean success = true;

        //now checking the rest of the data
        for (int index = data.size() - periods + 1; index < data.size(); index++) {

            double curr = data.get(index);
            double prev = data.get(index - 1);

            //if current is > we are checking for a higher high
            if (curr > prev) {

                if (high != null && curr < high) {

                    //if the current gain is lower than the previous high, we don't have success
                    success = false;
                    break;

                } else {

                    //here is our new high
                    high = curr;
                }

            } else {

                if (low != null && curr < low) {

                    //if the current loss is lower than the previous high, we don't have success
                    success = false;
                    break;

                } else {

                    //here is our new low
                    low = curr;
                }
            }
        }

        //did we detect an uptrend
        return success;
    }
}