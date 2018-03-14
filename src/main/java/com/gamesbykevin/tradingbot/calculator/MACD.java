package com.gamesbykevin.tradingbot.calculator;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.agent.AgentHelper;

import java.util.List;

import static com.gamesbykevin.tradingbot.calculator.Calculator.EMA_CROSSOVER;
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

    protected static boolean hasMacdCrossover(boolean bullish, List<Double> signalLine, List<Double> macdLine) {

        if (bullish) {

            //if we cross above 0 this is good signal
            if (macdLine.get(macdLine.size() - 2) < 0 && macdLine.get(macdLine.size() - 1) > 0)
                return true;

            return false;
        }


        /*
        //where do we start checking
        int start = EMA_CROSSOVER + 1;

        //if we are checking bullish the macdLine is greater then the signal line
        if (bullish) {

            //to start we want the signal line to be greater than the macd line
            if (signalLine.get(signalLine.size() - start) > macdLine.get(macdLine.size() - start)) {

                //now we want the macd line to be greater than the signal line
                for (int index = start - 1; index > 0; index--) {

                    //if the macd line is the same or smaller, we can't confirm a crossover
                    if (macdLine.get(macdLine.size() - index) <= signalLine.get(signalLine.size() - index))
                        return false;
                }

                //we found everything as expected
                return true;
            }

        } else {

            //for bearish crossover the macd line needs to be less than the signal line
            if (macdLine.get(macdLine.size() - start) > signalLine.get(signalLine.size() - start)) {

                //now we want the macd line to be greater than the signal line
                for (int index = start - 1; index > 0; index--) {

                    //if the macd line is the same or greater, we can't confirm a crossover
                    if (macdLine.get(macdLine.size() - index) >= signalLine.get(signalLine.size() - index))
                        return false;
                }

                //we found everything as expected
                return true;
            }
        }
        */

        //no crossover detected
        return false;
    }
}