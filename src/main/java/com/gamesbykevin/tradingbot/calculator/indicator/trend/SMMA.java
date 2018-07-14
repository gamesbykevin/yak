package com.gamesbykevin.tradingbot.calculator.indicator.trend;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.Period.Fields;
import com.gamesbykevin.tradingbot.calculator.indicator.Indicator;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

/**
 * Smoothed Moving Average
 */
public class SMMA extends Indicator {

    //list of our values
    private List<Double> smma;

    //the field we want to calculate
    private final List<Fields> fields;

    public SMMA(int periods) {

        //assume the close $ by default
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

    public SMMA(int periods, List<Fields> fields) {

        //call parent
        super(Indicator.Key.SMMA, periods);

        //what fields are we checking?
        this.fields = fields;

        //create new list
        this.smma = new ArrayList<>();
    }

    public List<Double> getSmma() {
        return this.smma;
    }

    public List<Fields> getFields() {
        return this.fields;
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
                final double sma = getSma(history, index + 1, getPeriods());

                //add the simple moving average to our list
                getSmma().add(sma);

            } else {

                //get the value of our desired fields
                double value = getSma(history, index + 1, 1);

                //our smoothed moving average value
                double smma;

                //get the previous smoothed value
                double previous = getRecent(getSmma());

                //the 2nd smoothed value is calculated differently
                if (getSmma().size() == 1) {

                    //calculate our smoothed moving average value accordingly
                    smma = (((previous * (float)(getPeriods() - 1)) + value) / (float) getPeriods());

                } else {

                    //additional smoothed moving averages are calculated differently
                    smma = (((previous * (float)getPeriods()) - previous) + value) / (float)getPeriods();

                }

                //add the value to our list
                getSmma().add(smma);
            }
        }
    }

    private double getSma(List<Period> history, int index, int periods) {

        //value we want to calculate
        double value = 0;

        for (int i = index - periods; i < index; i++) {

            //get the current period
            Period period = history.get(i);

            //calculate the current value
            double tmp = 0;

            for (int j = 0; j < getFields().size(); j++) {

                //figure out which field we need
                switch (getFields().get(j)) {

                    case Close:
                        tmp += period.close;
                        break;

                    case High:
                        tmp += period.high;
                        break;

                    case Low:
                        tmp += period.low;
                        break;

                    case Open:
                        tmp += period.open;
                        break;

                    case Time:
                        tmp += period.time;
                        break;

                    case Volume:
                        tmp += period.volume;
                        break;

                    default:
                        throw new RuntimeException("Field not handled: " + getFields().get(j));
                }
            }

            //divide by the number of fields to get the average and add to our total value
            value += (tmp / (float)getFields().size());
        }

        //return the average
        return (value / (float)periods);
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