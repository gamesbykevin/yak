package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonBuy;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;

import java.util.ArrayList;
import java.util.List;

import static com.gamesbykevin.tradingbot.calculator.CalculatorHelper.hasDivergence;

/**
 * Stochastic Oscillator divergence
 */
public class SOD extends SO {

    public SOD(int periodsSMA, int periodsSO) {

        //calling our parent with a default value
        super(periodsSMA, periodsSO);
    }

    public SOD() {
        this(PERIODS_SMA, PERIODS_SO);
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //if we have a bullish divergence, let's buy
        if (hasDivergence(history, periodsSO, true, getStochasticOscillator()))
            agent.setReasonBuy(ReasonBuy.Reason_15);

        //display our data
        displayData(agent, agent.getReasonBuy() != null);
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //if we have a bearish divergence, let's sell
        if (hasDivergence(history, periodsSO, false, getStochasticOscillator()))
            agent.setReasonSell(ReasonSell.Reason_16);

        //display our data
        displayData(agent, agent.getReasonSell() != null);
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display the information
        super.displayData(agent, write);
    }

    @Override
    public void calculate(List<Period> history) {
        super.calculate(history);
    }
}