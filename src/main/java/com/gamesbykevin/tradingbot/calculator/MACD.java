package com.gamesbykevin.tradingbot.calculator;

import java.util.List;

import static com.gamesbykevin.tradingbot.calculator.Calculator.PERIODS_MACD;

public class MACD {

    protected static void calculateMacdLine(List<Double> macdLine, List<Double> emaShort, List<Double> emaLong) {

        //clear the list
        macdLine.clear();

        //we need to start at the right index
        int difference = emaShort.size() - emaLong.size();

        //calculate for every value possible
        for (int i = 0; i < emaLong.size(); i++) {

            //the macd line is the 12 day ema - 26 day ema
            macdLine.add(emaShort.get(difference + i) - emaLong.get(i));
        }
    }

    protected static void calculateSignalLine(List<Double> signalLine, List<Double> macdLine) {

        //clear list
        signalLine.clear();

        //we add the sum to get the sma (simple moving average)
        double sum = 0;

        //calculate sma first
        for (int i = 0; i < PERIODS_MACD; i++) {
            sum += macdLine.get(i);
        }

        //we now have the sma as a start
        final double sma = sum / (float)PERIODS_MACD;

        //here is our multiplier
        final double multiplier = ((float)2 / ((float)PERIODS_MACD + 1));

        //calculate our first ema
        final double ema = ((macdLine.get(PERIODS_MACD - 1) - sma) * multiplier) + sma;

        //add the 9 day ema to our list
        signalLine.add(ema);

        //now let's calculate the remaining periods for ema
        for (int i = PERIODS_MACD; i < macdLine.size(); i++) {

            //get our previous ema
            final double previousEma = signalLine.get(signalLine.size() - 1);

            //get our close value
            final double close = macdLine.get(i);

            //calculate our new ema
            final double newEma = ((close - previousEma) * multiplier) + previousEma;

            //add our new ema value to the list
            signalLine.add(newEma);
        }
    }

    protected static boolean hasConvergenceDivergenceTrend(boolean uptrend, List<Double> macdLine, int periods) {

        //if not large enough skip, this shouldn't happen
        if (macdLine.size() < periods || macdLine.isEmpty())
            return false;

        //do a quick check to see if there is a trend
        if (uptrend) {

            //if the first value is greater than current, return false
            if (macdLine.get(macdLine.size() - periods) > macdLine.get(macdLine.size() - 1))
                return false;

        } else {

            //if the first value is less than current, return false
            if (macdLine.get(macdLine.size() - periods) < macdLine.get(macdLine.size() - 1))
                return false;
        }

        //our coordinates to calculate slope
        final double x1 = 0, y1 = macdLine.get(macdLine.size() - periods), x2 = periods, y2 = macdLine.get(macdLine.size() - 1);

        //the value of y when x = 0
        final double yIntercept = y1;

        //calculate slope
        final double slope = (y2 - y1) / (x2 - x1);

        //check every specified period
        for (int i = macdLine.size() - periods; i < macdLine.size(); i++) {

            //the current x-coordinate
            final double x = i - (macdLine.size() - periods);

            //calculate the y-coordinate
            final double y = (slope * x) + yIntercept;

            if (uptrend) {

                //if less than the slope there is no uptrend
                if (macdLine.get(i) < y)
                    return false;

            } else {

                //if greater than the slope there is no uptrend
                if (macdLine.get(i) > y)
                    return false;
            }
        }

        //we confirmed trend
        return true;
    }
}