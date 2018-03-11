package com.gamesbykevin.tradingbot.calculator;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.agent.AgentHelper;

import java.util.List;

import static com.gamesbykevin.tradingbot.calculator.Calculator.EMA_CROSSOVER;

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

        //for an accurate ema we want to calculate as many data points as possible
        for (int i = 0; i < history.size(); i++) {

            //skip if we can't go back far enough
            if (i <= periods)
                continue;

            //either the ema will be 0 or we get the most recent
            final double previousEma = (emaList.isEmpty()) ? 0 : emaList.get(emaList.size() - 1);

            //calculate the ema for the current period
            final double ema = calculateEMA(history, i, periods, previousEma);

            //add it to the list
            emaList.add(ema);
        }
    }

    protected static boolean hasEmaCrossover(boolean bullish, List<Double> emaShort, List<Double> emaLong) {

        //where do we start checking
        int start = EMA_CROSSOVER + 1;

        //if we are checking bullish the long is greater then the short is greater
        if (bullish) {

            //to start we want the long to be greater than the short
            if (emaLong.get(emaLong.size() - start) > emaShort.get(emaShort.size() - start)) {

                //now we want the short to be greater than the long
                for (int index = start - 1; index > 0; index--) {

                    //if the short is less, we can't confirm a crossover
                    if (emaShort.get(emaShort.size() - index) < emaLong.get(emaLong.size() - index))
                        return false;
                }

                //we found everything as expected
                return true;
            }

        } else {

            //to start we want the short to be greater than the long
            if (emaLong.get(emaLong.size() - start) < emaShort.get(emaShort.size() - start)) {

                //now we want the long to be greater than the short
                for (int index = start - 1; index > 0; index--) {

                    //if the long is less, we can't confirm a crossover
                    if (emaShort.get(emaShort.size() - index) > emaLong.get(emaLong.size() - index))
                        return false;
                }

                //we found everything as expected
                return true;
            }
        }

        /*
        //where do we start checking
        int start = EMA_CROSSOVER + 1;

        //if we are checking bullish the long is greater then the short is greater
        if (bullish) {

            //to start we want the long to be greater than the short
            if (emaLong.get(emaLong.size() - start) > emaShort.get(emaShort.size() - start)) {

                //now we want the short to be greater than the long
                for (int index = start - 1; index > 0; index--) {

                    //if the short is less, we can't confirm a crossover
                    if (emaShort.get(emaShort.size() - index) < emaLong.get(emaLong.size() - index))
                        return false;
                }

                //lets also make sure the ema short line is constantly increasing
                for (int index = emaShort.size() - 1; index >= emaShort.size() - EMA_CROSSOVER + 1; index--) {

                    //if the previous ema value is greater return false
                    if (emaShort.get(index) < emaShort.get(index - 1))
                        return false;
                }

                //lets also make sure the ema long line is constantly decreasing
                for (int index = emaLong.size() - 1; index >= emaLong.size() - EMA_CROSSOVER + 1; index--) {

                    //if the previous ema value is less return false
                    if (emaLong.get(index) > emaLong.get(index - 1))
                        return false;
                }

                //we found everything as expected
                return true;
            }

        } else {

            //to start we want the short to be greater than the long
            if (emaLong.get(emaLong.size() - start) < emaShort.get(emaShort.size() - start)) {

                //now we want the long to be greater than the short
                for (int index = start - 1; index > 0; index--) {

                    //if the long is less, we can't confirm a crossover
                    if (emaShort.get(emaShort.size() - index) > emaLong.get(emaLong.size() - index))
                        return false;
                }

                //lets also make sure the ema short line is constantly decreasing
                for (int index = emaShort.size() - 1; index >= emaShort.size() - EMA_CROSSOVER + 1; index--) {

                    //if the previous ema value is less return false
                    if (emaShort.get(index) > emaShort.get(index - 1))
                        return false;
                }

                //lets also make sure the ema long line is constantly increasing
                for (int index = emaLong.size() - 1; index >= emaLong.size() - EMA_CROSSOVER + 1; index--) {

                    //if the previous ema value is greater return false
                    if (emaLong.get(index) < emaLong.get(index - 1))
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

    public static void displayEma(Agent agent, String desc, List<Double> emaList, boolean write) {

        String info = "";
        for (int i = emaList.size() - (EMA_CROSSOVER + 1); i < emaList.size(); i++) {

            if (info != null && info.length() > 0)
                info += ", ";

            info += AgentHelper.formatValue(2, emaList.get(i));
        }

        agent.displayMessage(desc + info, write);
    }
}