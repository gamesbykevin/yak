package com.gamesbykevin.tradingbot.calculator.indicator.trend;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.Period.Fields;
import com.gamesbykevin.tradingbot.calculator.indicator.Indicator;

import java.util.*;

/**
 * Simple Moving Average
 */
public class SMA extends Indicator {

    //our list of sma values
    private final List<Double> sma;

    //the fields we want to calculate
    private final List<Fields> fields;

    public SMA(int periods) {

        //assume close field default
        this(periods, new AbstractList<Fields>() {
            @Override
            public Fields get(int index) {
                return Fields.Close;
            }

            @Override
            public int size() {
                return 1;
            }
        });
    }

    public SMA(int periods, final Fields field) {

        this(periods, new AbstractList<Fields>() {
            @Override
            public Fields get(int index) {
                return field;
            }

            @Override
            public int size() {
                return 1;
            }
        });
    }

    public SMA(int periods, List<Fields> fields) {

        //call parent
        super(Indicator.Key.SMA, periods);

        //which fields are we calculating
        this.fields = fields;

        //create new list
        this.sma = new ArrayList<>();
    }

    public List<Fields> getFields() {
        return this.fields;
    }

    public List<Double> getSma() {
        return this.sma;
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display our data
        display(agent, "SMA (" + getPeriods() + "): ", getSma(), write);
    }

    @Override
    public void calculate(List<Period> history, int newPeriods) {

        //where do we start
        int start = getSma().isEmpty() ? 0 : history.size() - newPeriods;

        //check all data when calculating
        for (int i = start; i <= history.size(); i++) {

            //skip until we have enough data to calculate
            if (i < getPeriods())
                continue;

            //calculate the sma value with the given values
            double sma = calculateSMA(history, i, getPeriods(), getFields());

            //add the sma value to our list
            getSma().add(sma);
        }
    }

    public void calculateSMA(List<Double> data, int newPeriods) {

        //where do we start
        int start = (getSma().isEmpty()) ? 0 : data.size() - newPeriods;

        for (int i = start; i <= data.size(); i++) {

            //skip if we don't have enough data
            if (i < getPeriods())
                continue;

            double sum = 0;

            //go back our desired number of periods
            for (int x = i - getPeriods(); x < i; x++) {
                sum += data.get(x);
            }

            //add the average to our list
            getSma().add(sum / (float)getPeriods());
        }
    }

    /**
     * Calculate the SMA (simple moving average)
     * @param history Our trading data
     * @param currentPeriod The desired start period of the SMA we want
     * @param periods The number of periods to check back
     * @param fields The fields we want to use to calculate
     * @return The average of the sum of the specified field within the specified period
     */
    public static double calculateSMA(List<Period> history, int currentPeriod, int periods, List<Fields> fields) {

        //our final result
        double result = 0;

        //check every period
        for (int i = currentPeriod - periods; i < currentPeriod; i++) {

            //get the period
            Period period = history.get(i);

            //the total sum
            double sum = 0;

            //loop through each desired field
            for (int j = 0; j < fields.size(); j++) {

                //what field are we adding to our sum
                switch (fields.get(j)) {

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
                        throw new RuntimeException("Field not handled: " + fields.get(j));
                }
            }

            //add the average sum to our result
            result += (sum / (float)fields.size());
        }

        //return the average of the sum
        return (result / (double)periods);
    }

    @Override
    public void cleanup() {
        cleanup(getSma());
    }
}