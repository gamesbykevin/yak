package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;

import java.util.List;

import static com.gamesbykevin.tradingbot.calculator.CalculatorHelper.hasCrossover;

/**
 * Stochastic Oscillator crossover
 */
public class SOC extends Strategy {

    //our object reference
    private SO soObj;

    //our list of variations
    protected static int[] LIST_PERIODS_SO = {12, 14, 14};
    protected static int[] LIST_PERIODS_SMA = {3, 3, 3};
    protected static int[] LIST_PERIODS_SMA_PRICE_LONG = {50, 50, 100};
    protected static int[] LIST_PERIODS_SMA_PRICE_SHORT = {10, 10, 20};

    public SOC() {

        //call parent
        super();

        //create a new object
        this.soObj = new SO();
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //if we have a bullish crossover, let's buy
        if (hasCrossover(true, soObj.getMarketRate(), soObj.getStochasticOscillator()))
            agent.setBuy(true);

        //display our data
        displayData(agent, agent.hasBuy());
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //if we have a bearish crossover, let's sell
        if (hasCrossover(false, soObj.getMarketRate(), soObj.getStochasticOscillator()))
            agent.setReasonSell(ReasonSell.Reason_Strategy);

        //display our data
        displayData(agent, agent.getReasonSell() != null);
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display the information
        this.soObj.displayData(agent, write);
    }

    @Override
    public void calculate(List<Period> history) {

        //calculate our value(s)
        this.soObj.calculate(history);
    }
}