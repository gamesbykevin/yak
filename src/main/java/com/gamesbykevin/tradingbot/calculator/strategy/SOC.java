package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;

import java.util.List;

import static com.gamesbykevin.tradingbot.calculator.CalculatorHelper.hasCrossover;

/**
 * Stochastic Oscillator crossover
 */
public class SOC extends SO {

    public SOC() {

        //use default value
        super();
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //if we have a bullish crossover, let's buy
        if (hasCrossover(true, getMarketRate(), getStochasticOscillator()))
            agent.setBuy(true);

        //display our data
        displayData(agent, agent.hasBuy());
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //if we have a bearish crossover, let's sell
        if (hasCrossover(false, getMarketRate(), getStochasticOscillator()))
            agent.setReasonSell(ReasonSell.Reason_Strategy);

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

        //calculate our value(s)
        super.calculate(history);
    }
}