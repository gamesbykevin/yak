package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonBuy;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;

import java.util.List;

import static com.gamesbykevin.tradingbot.calculator.CalculatorHelper.hasCrossover;

/**
 * Stochastic Oscillator crossover
 */
public class SOC extends Strategy {

    //our reference object
    private SOD sodObj;

    public SOC() {

        //use default value
        super(0);

        //create new object
        this.sodObj = new SOD();
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //if we have a bullish crossover, let's buy
        if (hasCrossover(true, sodObj.getMarketRate(), sodObj.getStochasticOscillator()))
            agent.setReasonBuy(ReasonBuy.Reason_16);

        //display our data
        displayData(agent, agent.getReasonBuy() != null);
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //if we have a bearish crossover, let's sell
        if (hasCrossover(false, sodObj.getMarketRate(), sodObj.getStochasticOscillator()))
            agent.setReasonSell(ReasonSell.Reason_17);

        //display our data
        displayData(agent, agent.getReasonSell() != null);
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display the information
        this.sodObj.displayData(agent, write);
    }

    @Override
    public void calculate(List<Period> history) {

        //calculate our value(s)
        this.sodObj.calculate(history);
    }
}