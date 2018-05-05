package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;

import java.util.List;

import static com.gamesbykevin.tradingbot.agent.AgentManagerHelper.displayMessage;

/**
 * Narrow Range
 */
public class NR extends Strategy {

    //the period with the smallest range
    private Period smallest;

    //list of configurable values
    public static int PERIODS_NR = 4;

    private final int periodsNR;

    public NR() {
        this(PERIODS_NR);
    }

    public NR(int periodsNR) {
        this.periodsNR = periodsNR;
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //if the current $ breaks above high, we will buy
        if (currentPrice > smallest.high)
            agent.setBuy(true);

        //display our data
        displayData(agent, agent.hasBuy());
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //make sure the current price is above the purchase price
        if (currentPrice > agent.getWallet().getPurchasePrice()) {

            //if the next period closes above our purchase price, sell!!!!
            if (history.get(history.size() - 1).close > agent.getWallet().getPurchasePrice())
                agent.setReasonSell(ReasonSell.Reason_Strategy);
        }

        //display our data
        displayData(agent, agent.getReasonSell() != null);
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display the information
        displayMessage(agent, "NR" + periodsNR + ": High $" + smallest.high + ", Low $" + smallest.low, write);
    }

    @Override
    public void calculate(List<Period> history) {

        //which period has the smallest range
        smallest = null;

        //check the previous # of periods to look for the smallest range
        for (int i = history.size() - periodsNR; i < history.size(); i++) {

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