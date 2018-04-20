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

    //list of configurable values
    private static int PERIODS_EMA_LONG = 2;
    private static int PERIODS_EMA_SHORT = 4;
    private static int SO_INDICATOR = 50;
    private static int PERIODS_SO = 5;
    private static int PERIODS_SMA_SO = 3;

    private final int soIndicator;

    public SOEMA() {
        this(PERIODS_EMA_LONG, PERIODS_EMA_SHORT, SO_INDICATOR, PERIODS_SO, PERIODS_SMA_SO);
    }

    public SOEMA(int emaLong, int emaShort, int soIndicator, int periodsSO, int periodsSMA) {

        //create new object
        this.emaObj = new EMA(emaLong, emaShort);
        this.soObj = new SO(periodsSO, periodsSMA, 1000, 1000);

        this.soIndicator = soIndicator;
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        double previousSO = getRecent(soObj.getStochasticOscillator(), 2);
        double currentSO = getRecent(soObj.getStochasticOscillator());

        if (currentSO < soIndicator && hasCrossover(true, emaObj.getEmaShort(), emaObj.getEmaLong())) {

            //make sure we are below the indicator and we have a bullish crossover
            agent.setBuy(true);

        } else if (previousSO > soIndicator && currentSO < soIndicator && getRecent(emaObj.getEmaShort()) > getRecent(emaObj.getEmaLong())) {

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

        if (currentSO > soIndicator && hasCrossover(false, emaObj.getEmaShort(), emaObj.getEmaLong())) {

            //make sure we are above the indicator and we have a bearish crossover
            agent.setReasonSell(ReasonSell.Reason_Strategy);

        } else if (previousSO < soIndicator && currentSO > soIndicator && getRecent(emaObj.getEmaShort()) < getRecent(emaObj.getEmaLong())) {

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