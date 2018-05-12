package com.gamesbykevin.tradingbot.calculator.indicator.trend;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.Period.Fields;
import com.gamesbykevin.tradingbot.calculator.indicator.Indicator;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple Moving Average
 */
public class SMA extends Indicator {

    //our list of sma values
    private final List<Double> sma;

    //how many periods so we cover
    private final int periods;

    //the field we want to calculate
    private final Fields field;

    public SMA(int periods) {
        this(periods, Fields.Close);
    }

    public SMA(int periods, Fields field) {

        //save the periods
        this.periods = periods;

        //which field are we calculating
        this.field = field;

        //create new list
        this.sma = new ArrayList<>();
    }

    public int getPeriods() {
        return this.periods;
    }

    public Fields getField() {
        return this.field;
    }

    public List<Double> getSma() {
        return this.sma;
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display our data
        display(agent, "SMA (" + getPeriods() + ") :", getSma(), write);
    }

    @Override
    public void calculate(List<Period> history, int newPeriods) {

        //calculate the sma
        calculateSMA(history, getSma(), newPeriods, getPeriods(), getField());
    }

    public static void calculateSMA(List<Double> data, List<Double> populate, int newPeriods, int periods) {

        //where do we start
        int start = (populate.isEmpty()) ? 0 : data.size() - newPeriods;

        for (int i = start; i <= data.size(); i++) {

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

    public static void calculateSMA(List<Period> history, List<Double> populate, int newPeriods, int periods, Fields field) {

        //where do we start
        int start = populate.isEmpty() ? 0 : history.size() - newPeriods;

        //check all data when calculating
        for (int i = start; i <= history.size(); i++) {

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
    public static double calculateSMA(List<Period> history, int currentPeriod, int periods, Fields field) {

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

    @Override
    public void cleanup() {
        cleanup(getSma());
    }
}