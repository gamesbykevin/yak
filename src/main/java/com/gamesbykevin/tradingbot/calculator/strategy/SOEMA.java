package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;

import java.util.List;

import static com.gamesbykevin.tradingbot.calculator.CalculatorHelper.hasCrossover;

/**
 * stochastic oscillator / ema
 */
public class SOEMA extends Strategy {

    //our reference object(s)
    private EMA emaObj;
    private SO soObj;

    //our list of variations
    protected static int[] LIST_PERIODS_EMA_LONG = {2};
    protected static int[] LIST_PERIODS_EMA_SHORT = {4};
    protected static int[] LIST_SO_INDICATOR = {50};
    protected static int[] LIST_PERIODS_SO = {5};
    protected static int[] LIST_PERIODS_SMA_SO = {3};

    //list of configurable values
    protected static int SO_INDICATOR = 50;
    protected static int PERIODS_SO = 5;
    protected static int PERIODS_SMA_SO = 3;

    public SOEMA() {

        //call parent
        super();

        //create new object
        this.emaObj = new EMA();
        this.soObj = new SO();
    }

    @Override
    public String getStrategyDesc() {
        return "PERIODS_EMA_LONG = " + LIST_PERIODS_EMA_LONG[getIndexStrategy()] + ", PERIODS_EMA_SHORT = " + LIST_PERIODS_EMA_SHORT[getIndexStrategy()] + ", SO_INDICATOR = " + LIST_SO_INDICATOR[getIndexStrategy()] + ", PERIODS_SO = " + LIST_PERIODS_SO[getIndexStrategy()] + ", PERIODS_SMA_SO = " + LIST_PERIODS_SMA_SO[getIndexStrategy()];
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        double previousSO = getRecent(soObj.getStochasticOscillator(), 2);
        double currentSO = getRecent(soObj.getStochasticOscillator());

        if (currentSO < SO_INDICATOR && hasCrossover(true, emaObj.getEmaShort(), emaObj.getEmaLong())) {

            //make sure we are below the indicator and we have a bullish crossover
            agent.setBuy(true);

        } else if (previousSO > SO_INDICATOR && currentSO < SO_INDICATOR && getRecent(emaObj.getEmaShort()) > getRecent(emaObj.getEmaLong())) {

            //confirm we just crossed below the indicator and the ema short is > ema long
            agent.setBuy(true);
        }

        //display our data
        displayData(agent, agent.hasBuy());
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        double previousSO = getRecent(soObj.getStochasticOscillator(), 2);
        double currentSO = getRecent(soObj.getStochasticOscillator());

        if (currentSO > SO_INDICATOR && hasCrossover(false, emaObj.getEmaShort(), emaObj.getEmaLong())) {

            //make sure we are above the indicator and we have a bearish crossover
            agent.setReasonSell(ReasonSell.Reason_Strategy);

        } else if (previousSO < SO_INDICATOR && currentSO > SO_INDICATOR && getRecent(emaObj.getEmaShort()) < getRecent(emaObj.getEmaLong())) {

            //confirm we just crossed above the indicator and the ema short is < ema long
            agent.setReasonSell(ReasonSell.Reason_Strategy);
        }

        //display our data
        displayData(agent, agent.getReasonSell() != null);
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display the information
        this.soObj.displayData(agent, write);
        this.emaObj.displayData(agent, write);
    }

    @Override
    public void calculate(List<Period> history) {

        //calculate our value(s)
        this.soObj.calculate(history);
        this.emaObj.calculate(history);
    }
}