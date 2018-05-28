package com.gamesbykevin.tradingbot.calculator.indicator.trend;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.indicator.Indicator;

import java.util.ArrayList;
import java.util.List;

/**
 * Average Directional Index
 */
public class ADX extends Indicator {

    //the average directional index
    private List<Double> adx;

    //our +- indicators to calculate adx and signal trades
    private List<Double> dmPlusIndicator;
    private List<Double> dmMinusIndicator;

    //list of configurable values
    public static final int PERIODS = 14;
    public static final double TREND = 20.0d;

    //our lists for calculation
    private List<Double> dmPlus;
    private List<Double> dmMinus;
    private List<Double> dmIndex;
    private List<Double> trueRange;

    //our temp lists
    private List<Double> tmpDmPlus;
    private List<Double> tmpDmMinus;
    private List<Double> tmpTrueRange;

    public ADX() {
        this(PERIODS);
    }

    public ADX(int periods) {

        //call parent
        super(Indicator.Key.ADX, periods);

        //create our lists
        this.adx = new ArrayList<>();
        this.dmPlusIndicator = new ArrayList<>();
        this.dmMinusIndicator = new ArrayList<>();
        this.dmPlus = new ArrayList<>();
        this.dmMinus = new ArrayList<>();
        this.trueRange = new ArrayList<>();
        this.tmpDmPlus = new ArrayList<>();
        this.tmpDmMinus = new ArrayList<>();
        this.tmpTrueRange = new ArrayList<>();
        this.dmIndex = new ArrayList<>();
    }

    public List<Double> getAdx() {
        return this.adx;
    }

    public List<Double> getDmPlusIndicator() {
        return this.dmPlusIndicator;
    }

    public List<Double> getDmMinusIndicator() {
        return this.dmMinusIndicator;
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display the recent values which we use as a signal
        display(agent, "+DI: ", getDmPlusIndicator(), write);
        display(agent, "-DI: ", getDmMinusIndicator(), write);
        display(agent, "ADX: ", getAdx(), write);
    }

    @Override
    public void calculate(List<Period> history, int newPeriods) {

        //where do we start
        int start = getAdx().isEmpty() ? 0 : history.size() - newPeriods;

        //calculate for the entire history that we have
        for (int i = start; i < history.size(); i++) {

            //we can't check the previous period here
            if (i <= 0)
                continue;

            //get the current and previous periods
            Period previous = history.get(i - 1);
            Period current = history.get(i);

            //calculate the high difference
            double highDiff = current.high - previous.high;

            //calculate the low difference
            double lowDiff = current.low - previous.low;

            //which values do we set
            if (highDiff > lowDiff) {

                //if less than 0, zero will be assigned
                if (highDiff < 0)
                    highDiff = 0d;

                tmpDmPlus.add(highDiff);
                tmpDmMinus.add(0d);

            } else {

                //if less than 0, zero will be assigned
                if (lowDiff < 0)
                    lowDiff = 0d;

                tmpDmPlus.add(0d);
                tmpDmMinus.add(lowDiff);
            }

            //current high minus current low
            double method1 = current.high - current.low;

            //if we have the previous period, current high minus previous period close (absolute value)
            double method2 = (previous == null) ? 0d : Math.abs(current.high - previous.close);

            //if we have the previous period, current low minus previous period close (absolute value)
            double method3 = (previous == null) ? 0d : Math.abs(current.low - previous.close);

            //the true range will be the greatest value of the 3 methods
            if (method1 >= method2 && method1 >= method3) {
                tmpTrueRange.add(method1);
            } else if (method2 >= method1 && method2 >= method3) {
                tmpTrueRange.add(method2);
            } else if (method3 >= method1 && method3 >= method2) {
                tmpTrueRange.add(method3);
            } else {
                tmpTrueRange.add(method1);
            }
        }

        //smooth the values
        smooth(tmpDmMinus,   dmMinus,   getPeriods(), newPeriods);
        smooth(tmpDmPlus,    dmPlus,    getPeriods(), newPeriods);
        smooth(tmpTrueRange, trueRange, getPeriods(), newPeriods);

        //where do we start?
        start = getAdx().isEmpty() ? 0 : dmPlus.size() - newPeriods;

        for (int i = start; i < dmPlus.size(); i++) {

            //calculate the +- indicators
            double newPlus = (dmPlus.get(i) / trueRange.get(i)) * 100.0d;
            double newMinus = (dmMinus.get(i) / trueRange.get(i)) * 100.0d;

            //add it to the list
            getDmPlusIndicator().add(newPlus);
            getDmMinusIndicator().add(newMinus);
        }

        //where do we start?
        start = getAdx().isEmpty() ? 0 : dmPlus.size() - newPeriods;

        //calculate each dm index
        for (int i = start; i < dmPlus.size(); i++) {
            double result1 = Math.abs(getDmPlusIndicator().get(i) - getDmMinusIndicator().get(i));
            double result2 = getDmPlusIndicator().get(i) + getDmMinusIndicator().get(i);
            dmIndex.add((result1 / result2) * 100.0d);
        }

        //where do we start
        start = getPeriods();

        //if the list is empty calculate our first value
        if (getAdx().isEmpty()) {

            double sum = 0;

            //get the average for the first value
            for (int i = 0; i < getPeriods(); i++) {
                sum += dmIndex.get(i);
            }

            //our first value is the average
            getAdx().add(sum / (double) getPeriods());
        } else {
            start = dmIndex.size() - newPeriods;
        }

        //calculate the remaining average directional index values
        for (int i = start; i < dmIndex.size(); i++) {

            //get the most recent adx
            double previousAdx = getRecent(getAdx());

            //calculate the new adx value
            double newAdx = ((previousAdx * (double)(getPeriods() - 1)) + dmIndex.get(i)) / (double)getPeriods();

            //add new value to our list
            getAdx().add(newAdx);
        }
    }

    /**
     * Smooth out the values
     * @param tmp Our temp list of values
     * @param result Our final result of smoothed values
     */
    private void smooth(List<Double> tmp, List<Double> result, int periods, int newPeriods) {

        //where do we start?
        int start = periods;

        //if the list is empty we need to get our first value
        if (result.isEmpty()) {

            double sum = 0;

            //add the sum of the first x periods to get the first value
            for (int i = 0; i < periods; i++) {
                sum += tmp.get(i);
            }

            //add first result to our list as a sum
            result.add(sum);

        } else {

            //adjust the start index
            start = tmp.size() - newPeriods;

        }

        //now lets smooth the values for the remaining
        for (int i = start; i < tmp.size(); i++) {

            //calculate our current
            double currentSum = 0;

            //calculate the sum of the current period
            for (int x = i - periods + 1; x <= i; x++) {
                currentSum += tmp.get(x);
            }

            //get the previous value
            double previousValue = result.get(result.size() - 1);

            //calculate the new smoothed value
            double newValue = previousValue - (previousValue / (double)periods) + currentSum;

            //add the smoothed value to our list
            result.add(newValue);
        }
    }

    @Override
    public void cleanup() {
        cleanup(adx);
        cleanup(dmPlusIndicator);
        cleanup(dmMinusIndicator);
        cleanup(dmPlus);
        cleanup(dmMinus);
        cleanup(dmIndex);
        cleanup(trueRange);
        cleanup(tmpDmPlus);
        cleanup(tmpDmMinus);
        cleanup(tmpTrueRange);
    }
}