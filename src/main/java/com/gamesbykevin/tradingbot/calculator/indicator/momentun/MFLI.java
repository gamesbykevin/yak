package com.gamesbykevin.tradingbot.calculator.indicator.momentun;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.indicator.Indicator;

import java.util.ArrayList;
import java.util.List;

/**
 * Money Flow Index
 */
public class MFLI extends Indicator {

    //our list of data
    private List<Double> moneyFlowIndex;

    public MFLI(int periods) {

        //call parent
        super(Key.MFLI, periods);
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display the information
        display(agent, "Money Flow Index: ", getMoneyFlowIndex(), write);
    }

    public List<Double> getMoneyFlowIndex() {

        //instantiate if null
        if (this.moneyFlowIndex == null)
            this.moneyFlowIndex = new ArrayList<>();

        return this.moneyFlowIndex;
    }

    @Override
    public void calculate(List<Period> history, int newPeriods) {

        //where do we start
        int start = getMoneyFlowIndex().isEmpty() ? 0 : history.size() - newPeriods;

        //check every period for our calculations
        for (int index = start; index < history.size(); index++) {

            //we need enough data to calculate
            if (index < getPeriods())
                continue;

            //calculate and add our value to the list
            getMoneyFlowIndex().add(calculateMFI(history, index));
        }
    }

    /**
     * Steps
     * 1. Typical Price = (High + Low + Close) / 3
     * 2. Raw Money Flow = Typical Price x Volume
     * 3. Money Flow Ratio = (14-period Positive Money Flow)/(14-period Negative Money Flow)
     * 4. Money Flow Index = 100 - 100/(1 + Money Flow Ratio)
     *
     * @param history
     * @param index
     * @return
     */
    private double calculateMFI(List<Period> history, int index) {

        //our calculated result
        double moneyFlowIndex = 0;

        //needed to calculate our final result
        double moneyFlowPositive = 0;
        double moneyFlowNegative = 0;

        //where do we start
        int start = index - getPeriods() + 1;

        //what is the previous typical price
        double typicalPricePrevious = getTypicalPrice(history.get(start - 1));

        //look at these periods for our calculation
        for (int i = start; i <= index; i++) {

            //get the current period
            Period current = history.get(i);

            //determine the typical price
            double typicalPrice = getTypicalPrice(current);
            double rawMoneyFlow = (typicalPrice * current.volume);

            if (typicalPrice > typicalPricePrevious) {

                //if the period was a gain add it to the positive
                moneyFlowPositive += rawMoneyFlow;

            } else if (typicalPrice < typicalPricePrevious) {

                //if the period was a gain add it to the negative
                moneyFlowNegative += rawMoneyFlow;

            }

            //keep track of the previous price
            typicalPricePrevious = typicalPrice;
        }

        //what is our ratio
        float moneyFlowRatio = 0f;

        //we don't want to divide by 0
        if (moneyFlowPositive != 0 && moneyFlowNegative != 0)
            moneyFlowRatio = (float)(moneyFlowPositive / moneyFlowNegative);

        //calculate our money flow index
        moneyFlowIndex = 100.0d - (100.0d / (1.0f + moneyFlowRatio));

        //return our result
        return moneyFlowIndex;
    }

    private double getTypicalPrice(Period period) {
        return (period.close + period.high + period.low) / 3.0f;
    }

    @Override
    public void cleanup() {
        cleanup(getMoneyFlowIndex());
    }
}