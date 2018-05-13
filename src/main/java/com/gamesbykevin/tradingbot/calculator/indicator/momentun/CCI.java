package com.gamesbykevin.tradingbot.calculator.indicator.momentun;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.indicator.Indicator;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.SMA;

import java.util.ArrayList;
import java.util.List;

/**
 * Commodity Channel Index
 */
public class CCI extends Indicator {

    //our list of data
    private List<Double> cci;

    //list of typical price values
    private List<Double> typicalPrice;

    //our sma of typical price
    private SMA objSMA;

    /**
     * The number of periods to calculate our typical price sma
     */
    private static final int PERIODS = 20;

    /**
     * What is our constant that we will multiple by the mean deviation
     */
    private static final float CONSTANT_VALUE = .015f;

    //the constant value
    private final float constantValue;

    /**
     * Default Constructor
     */
    public CCI() {
        this(PERIODS, CONSTANT_VALUE);
    }

    public CCI(int periods) {
        this(periods, CONSTANT_VALUE);
    }

    public CCI(int periods, float constantValue) {

        //store our constant value
        this.constantValue = constantValue;

        //create new list for our data
        this.cci = new ArrayList<>();

        //create new objects
        this.typicalPrice = new ArrayList<>();
        this.objSMA = new SMA(periods);
    }

    public float getConstantValue() {
        return this.constantValue;
    }

    public List<Double> getTypicalPriceSma() {
        return this.objSMA.getSma();
    }

    public List<Double> getTypicalPrice() {
        return this.typicalPrice;
    }

    public List<Double> getCCI() {
        return this.cci;
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display the information
        display(agent, "Typical $", getTypicalPrice(), write);
        display(agent, "Typical SMA (" + objSMA.getPeriods() + ") $", getTypicalPrice(), write);
        display(agent, "CC Index: ", getCCI(), write);
    }

    @Override
    public void calculate(List<Period> history, int newPeriods) {

        //where do we start
        int start = getTypicalPrice().isEmpty() ? 0 : history.size() - newPeriods;

        //check every period for our calculations
        for (int i = start; i < history.size(); i++) {

            //get the current period
            Period current = history.get(i);

            //determine the typical price
            double typicalPrice = (current.close + current.high + current.low) / 3.0f;

            //add the typical price to our list
            getTypicalPrice().add(typicalPrice);
        }

        //calculate the simple moving average
        objSMA.calculateSMA(getTypicalPrice(), newPeriods);

        //where do we start
        start = getCCI().isEmpty() ? objSMA.getPeriods() : getTypicalPrice().size() - newPeriods;

        //calculate cci for every value in this list
        for (int i = start; i < getTypicalPrice().size(); i++) {

            //get the sma typical price
            double sma = getTypicalPriceSma().get(i - objSMA.getPeriods());

            //the sum of the difference
            double sum = 0;

            //subtract the typical price from the sma typical price and add the absolute value
            for (int j = i - objSMA.getPeriods(); j < i; j++) {
                sum += Math.abs(getTypicalPrice().get(j) - sma);
            }

            //what is our mean deviation?
            double meanDeviation = sum / (double)objSMA.getPeriods();

            //calculate the commodity channel index value
            double cci = (getTypicalPrice().get(i) - sma) / (getConstantValue() * meanDeviation);

            //add the value to our list
            getCCI().add(cci);
        }
    }

    @Override
    public void cleanup() {
        cleanup(getCCI());
        cleanup(getTypicalPrice());
        objSMA.cleanup();
    }
}