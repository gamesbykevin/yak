package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;

import java.util.ArrayList;
import java.util.List;

import static com.gamesbykevin.tradingbot.calculator.CalculatorHelper.hasDivergence;

/**
 * Accumulation Distribution Line
 */
public class ADL extends Strategy {

    //our data for each period
    private List<Double> accumulationDistributionLine;

    //this is here by default
    protected static int[] LIST_DEFAULT = {0};

    public ADL() {

        //call parent
        super();

        //create a new list
        this.accumulationDistributionLine = new ArrayList<>();
    }

    public List<Double> getAccumulationDistributionLine() {
        return this.accumulationDistributionLine;
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        double previous = getRecent(history, Period.Fields.Close, 2);
        double current = getRecent(history, Period.Fields.Close);

        double previousVal = getRecent(getAccumulationDistributionLine(), 2);
        double currentVal = getRecent(getAccumulationDistributionLine());

        if (previous > current && previousVal < currentVal)
            agent.setBuy(true);

        //if we have a bullish divergence, let's buy
        //if (hasDivergence(history, getPeriods(), true, getAccumulationDistributionLine()))
        //    agent.setBuy(true);

        //display our data
        displayData(agent, agent.hasBuy());
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        double previous = getRecent(history, Period.Fields.Close, 2);
        double current = getRecent(history, Period.Fields.Close);

        double previousVal = getRecent(getAccumulationDistributionLine(), 2);
        double currentVal = getRecent(getAccumulationDistributionLine());

        if (previous < current && previousVal > currentVal)
            agent.setReasonSell(ReasonSell.Reason_Strategy);

        //if we have a bearish divergence, let's sell
        //if (hasDivergence(history, getPeriods(), false, getAccumulationDistributionLine()))
        //    agent.setReasonSell(ReasonSell.Reason_Strategy);

        //display our data
        displayData(agent, agent.getReasonSell() != null);
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display the information
        display(agent, "ADL: ", getAccumulationDistributionLine(), write);
    }

    @Override
    public void calculate(List<Period> history) {

        //clear our list
        getAccumulationDistributionLine().clear();

        for (int i = 0; i < history.size(); i++) {

            //calculate our money flow multiplier
            double multiplier = getMultiplier(history.get(i));

            //calculate our money flow volume
            double volume = (history.get(i).volume * multiplier);

            //calculate Accumulation Distribution Line
            if (getAccumulationDistributionLine().isEmpty()) {

                //if no previous values, the volume will be the initial
                getAccumulationDistributionLine().add(volume);

            } else {

                //add the previous volume to the current
                double newVolume = getRecent(getAccumulationDistributionLine()) + volume;

                //add our new volume to the list
                getAccumulationDistributionLine().add(newVolume);
            }
        }
    }

    private double getMultiplier(Period period) {

        //simplify our calculations
        double value1 = period.close - period.low;
        double value2 = period.high - period.close;
        double value3 = period.high - period.low;

        //return 0 if 0
        if (value3 == 0)
            return 0;
        if (value1 - value2 == 0)
            return 0;

        //return our result
        return (value1 - value2) / value3;
    }
}