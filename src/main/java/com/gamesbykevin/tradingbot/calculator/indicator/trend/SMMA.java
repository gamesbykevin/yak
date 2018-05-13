package com.gamesbykevin.tradingbot.calculator.indicator.trend;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.Period.Fields;
import com.gamesbykevin.tradingbot.calculator.indicator.Indicator;

import java.util.ArrayList;
import java.util.List;

import static com.gamesbykevin.tradingbot.calculator.indicator.trend.SMA.calculateSMA;

/**
 * Smoothed Moving Average
 */
public class SMMA extends Indicator {

    //list of our values
    private List<Double> smma;

    //how many periods do we calculate
    private final int periods;

    //the field we want to calculate
    private final Fields field;

    public SMMA(int periods) {
        this(periods, Fields.Close);
    }

    public SMMA(int periods, Fields field) {

        //store our periods
        this.periods = periods;

        //save the field
        this.field = field;

        //create new list
        this.smma = new ArrayList<>();
    }

    public List<Double> getSmma() {
        return this.smma;
    }

    public int getPeriods() {
        return this.periods;
    }

    public Fields getField() {
        return this.field;
    }

    @Override
    public void calculate(List<Period> history, int newPeriods) {

        //where do we start?
        int start = getSmma().isEmpty() ? 0 : history.size() - newPeriods;

        //check our periods
        for (int index = start; index < history.size(); index++) {

            //skip until we have enough data
            if (index < getPeriods())
                continue;

            //if the list is empty first value will be the simple moving average
            if (getSmma().isEmpty()) {

                //calculate our simple moving average
                final double sma = calculateSMA(history, index, getPeriods(), getField());

                //add the simple moving average to our list
                getSmma().add(sma);

            } else {

                //get the current period
                Period period = history.get(index);

                //value we want to calculate
                double value;

                //figure out which field we need
                switch (getField()) {

                    case Close:
                        value = period.close;
                        break;

                    case High:
                        value = period.high;
                        break;

                    case Low:
                        value = period.low;
                        break;

                    case Open:
                        value = period.open;
                        break;

                    case Time:
                        value = period.time;
                        break;

                    case Volume:
                        value = period.volume;
                        break;

                    default:
                        throw new RuntimeException("Field not handled: " + getField());
                }

                //our smoothed moving average value
                double smma;

                //get the previous smoothed value
                double previous = getRecent(getSmma());

                //the 2nd smoothed value is calculated differently
                if (getSmma().size() == 1) {

                    //calculate our smoothed moving average value accordingly
                    smma = (((previous * (float) (getPeriods() - 1)) + value) / (float) getPeriods());

                } else {

                    //additional smoothed moving averages are calculated differently
                    smma = (((previous * (float)getPeriods()) - previous) + value) / (float)getPeriods();

                }

                //add the value to our list
                getSmma().add(smma);
            }
        }
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display our information
        display(agent, "SMMA (" + getPeriods() + "): ", getSmma(), write);
    }

    @Override
    public void cleanup() {
        cleanup(getSmma());
    }
}