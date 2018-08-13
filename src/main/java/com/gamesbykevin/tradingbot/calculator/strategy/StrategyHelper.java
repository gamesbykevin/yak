package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.Period.Fields;

import java.util.List;

public class StrategyHelper {

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

                if (low != null && curr >= low) {

                    //if the current loss is higher than the previous low, we don't have success
                    success = false;
                    break;

                } else {

                    //we have a new low
                    low = curr;

                }

            } else {

                if (high != null && curr >= high) {

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

                if (low != null && curr >= low) {

                    //if the current loss is higher than the previous low, we don't have success
                    success = false;
                    break;

                } else {

                    //we have a new low
                    low = curr;

                }

            } else {

                if (high != null && curr >= high) {

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

                if (high != null && curr <= high) {

                    //if the current gain is lower than the previous high, we don't have success
                    success = false;
                    break;

                } else {

                    //here is our new high
                    high = curr;
                }

            } else {

                if (low != null && curr <= low) {

                    //if the current loss is lower than the previous low, we don't have success
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

                if (high != null && curr <= high) {

                    //if the current gain is lower than the previous high, we don't have success
                    success = false;
                    break;

                } else {

                    //here is our new high
                    high = curr;
                }

            } else {

                if (low != null && curr <= low) {

                    //if the current low is lower than the previous low, we don't have success
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