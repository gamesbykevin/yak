package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;

import java.util.ArrayList;
import java.util.List;

import static com.gamesbykevin.tradingbot.calculator.CalculatorHelper.hasDivergence;

public class OBV extends Strategy {

    //our list of variations
    protected static int[] LIST_PERIODS_OBV = {10, 20, 30, 50, 100};

    //list of configurable values
    protected static int PERIODS_OBV = 10;

    //keep a historical list of the volume so we can check for divergence
    private List<Double> volume;

    public OBV() {

        //call parent
        super();

        //create list
        this.volume = new ArrayList<>();
    }

    public List<Double> getVolume() {
        return this.volume;
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //if there is a bullish divergence let's buy
        if (hasDivergence(history, PERIODS_OBV, true, getVolume()))
            agent.setBuy(true);

        //display our data
        displayData(agent, agent.hasBuy());
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //if there is a bearish divergence let's sell
        if (hasDivergence(history, PERIODS_OBV, false, getVolume()))
            agent.setReasonSell(ReasonSell.Reason_Strategy);

        //display our data
        displayData(agent, agent.getReasonSell() != null);
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display the volume
        display(agent, "OBV: ", getVolume(), write);
    }

    @Override
    public void calculate(List<Period> history) {

        //clear our historical list
        getVolume().clear();

        //calculate the obv for each period
        for (int i = 0; i < history.size(); i++) {

            //skip if not enough info
            if (i < PERIODS_OBV)
                continue;

            //get the obv for this period
            final double tmpVolume = calculateOBV(history, i);

            //add the obv calculation to the list
            getVolume().add(tmpVolume);
        }
    }

    private double calculateOBV(List<Period> history, int currentPeriod) {

        //the total sum
        double sum = 0;

        //check every period
        for (int i = currentPeriod - PERIODS_OBV; i < currentPeriod - 1; i++) {

            Period prev = history.get(i);
            Period next = history.get(i + 1);

            if (next.close > prev.close) {

                //add to the total volume
                sum = sum + history.get(i).volume;

            } else if (next.close < prev.close) {

                //subtract from the total volume
                sum = sum - history.get(i).volume;
            }
        }

        //return the on balance volume
        return sum;
    }
}