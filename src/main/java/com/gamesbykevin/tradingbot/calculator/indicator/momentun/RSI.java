package com.gamesbykevin.tradingbot.calculator.indicator.momentun;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.indicator.Indicator;

import java.util.ArrayList;
import java.util.List;

/**
 * Relative Strength Index
 */
public class RSI extends Indicator {

    //keep a list of the rsi values
    private List<Double> valueRSI;

    //our average gain and loss
    private List<Double> avgGain, avgLoss;

    //list of configurable values
    private static final int PERIODS = 14;

    public RSI() {
        this(PERIODS);
    }

    public RSI(int periods) {

        //call parent
        super(Indicator.Key.RSI, periods);

        //create new list(s)
        this.valueRSI = new ArrayList<>();
        this.avgGain = new ArrayList<>();
        this.avgLoss = new ArrayList<>();
    }

    public List<Double> getValueRSI() {
        return this.valueRSI;
    }

    private List<Double> getAvgGain() {
        return this.avgGain;
    }

    private List<Double> getAvgLoss() {
        return this.avgLoss;
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display the volume
        display(agent, "RSI (" + getPeriods() + "): ", getValueRSI(), write);
    }

    @Override
    public void calculate(List<Period> history, int newPeriods) {

        //where do we start
        int start = (getValueRSI().isEmpty() ? 0 : history.size() - newPeriods);

        //calculate as many periods as we need
        for (int index = start; index < history.size(); index++) {

            //skip if we don't have enough data
            if (index < getPeriods())
                continue;

            //we need to find out the average gain / loss
            double avgGain, avgLoss;

            //add up our gain and losses
            double sumGain = 0, sumLoss = 0;

            //first value is calculated differently
            if (getValueRSI().isEmpty()) {

                //check our recent periods
                for (int i = (index + 1) - getPeriods(); i <= index; i++) {

                    //get the current and previous periods
                    Period curr = history.get(i);
                    Period prev = history.get(i - 1);

                    //what is the difference between periods?
                    double diff = Math.abs(curr.close - prev.close);

                    //determine if this was a gain or loss
                    if (curr.close > prev.close) {
                        sumGain += diff;
                    } else {
                        sumLoss += diff;
                    }
                }

                //figure out our averages
                avgGain = sumGain / (float)getPeriods();
                avgLoss = sumLoss / (float)getPeriods();

            } else {

                //get the current and previous periods
                Period curr = history.get(index);
                Period prev = history.get(index - 1);

                //what is the difference between periods?
                double diff = Math.abs(curr.close - prev.close);

                //determine if this was a gain or loss
                if (curr.close > prev.close) {
                    sumGain += diff;
                } else {
                    sumLoss += diff;
                }

                //figure out our averages, this calculation will smooth out the value
                avgGain = ((getRecent(getAvgGain()) * (float)(getPeriods() - 1) + sumGain) / (float)getPeriods());
                avgLoss = ((getRecent(getAvgLoss()) * (float)(getPeriods() - 1) + sumLoss) / (float)getPeriods());
            }

            //add our average gain / loss to the list
            getAvgGain().add(avgGain);
            getAvgLoss().add(avgLoss);

            //calculate the relative strength
            double relativeStrength = (avgGain / avgLoss);

            //finally add our relative strength index
            if (avgGain <= 0) {

                //if the gain is 0 our rsi will be 0
                getValueRSI().add(0.0d);

            } else if (avgLoss <= 0) {

                //if the loss is 0 our rsi will be 100
                getValueRSI().add(100.0d);

            } else {

                //else calculate the relative strength index
                getValueRSI().add(100.0f - (100.0f / (1.0f + relativeStrength)));
            }
        }
    }

    @Override
    public void cleanup() {
        cleanup(getValueRSI());
        cleanup(getAvgGain());
        cleanup(getAvgLoss());
    }
}