package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;

import java.util.List;

import static com.gamesbykevin.tradingbot.agent.AgentManagerHelper.displayMessage;

public class NR extends Strategy {

    //the period with the smallest range
    private Period smallest;

    //our list of variations
    protected static int[] LIST_PERIODS_NR = {4, 7};

    //list of configurable values
    public static int PERIODS_NR = 4;

    public NR() {

        //call parent
        super();
    }

    @Override
    public String getStrategyDesc() {
        return "PERIODS_NR = " + LIST_PERIODS_NR[getIndexStrategy()];
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //if the current $ breaks above high, we will buy
        if (currentPrice > smallest.high) {

            //assign our reason to buy
            agent.setBuy(true);

            //set our hard stop amount
            //agent.setHardStop(smallest.low);
        }

        //display our data
        displayData(agent, agent.hasBuy());
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //make sure the current price is above the purchase price
        if (currentPrice > agent.getWallet().getPurchasePrice()) {

            //if the next period closes above our purchase price, sell!!!!
            //if (history.get(history.size() - 1).close > agent.getWallet().getPurchasePrice())
            //    agent.setReasonSell(ReasonSell.Reason_9);
        }

        //display our data
        displayData(agent, agent.getReasonSell() != null);
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display the information
        displayMessage(agent, "NR" + PERIODS_NR + ": High $" + smallest.high + ", Low $" + smallest.low, write);
    }

    @Override
    public void calculate(List<Period> history) {

        //which period has the smallest range
        smallest = null;

        //check the previous # of periods to look for the smallest range
        for (int i = history.size() - PERIODS_NR; i < history.size(); i++) {

            //get the current period
            Period current = history.get(i);

            //we are always looking for a period with the smallest range
            if (smallest == null)
                smallest = current;
            if (current.high - current.low < smallest.high - smallest.low)
                smallest = current;
        }
    }
}