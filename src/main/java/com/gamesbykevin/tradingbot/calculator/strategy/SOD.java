package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;

import java.util.List;

import static com.gamesbykevin.tradingbot.calculator.CalculatorHelper.hasDivergence;

/**
 * Stochastic Oscillator divergence
 */
public class SOD extends Strategy {

    //our object reference
    private SO soObj;

    /**
     * Number of periods for stochastic oscillator
     */
    private static final int PERIODS_SO = 12;

    /**
     * Number of periods we calculate sma to get our indicator
     */
    private static final int PERIODS_SMA = 3;

    /**
     * Number of periods we calculate sma to get our indicator
     */
    private static final int PERIODS_SMA_PRICE_LONG = 50;

    /**
     * Number of periods we calculate sma to get our indicator
     */
    private static final int PERIODS_SMA_PRICE_SHORT = 10;


    public SOD(int periodsSMA, int periodsSO, int periodsSmaPriceLong, int periodsSmaPriceShort) {

        //call parent with a default value
        super(periodsSO);

        //create a new object
        this.soObj = new SO(periodsSMA, periodsSO, periodsSmaPriceLong, periodsSmaPriceShort);
    }

    public SOD() {
        this(PERIODS_SMA, PERIODS_SO, PERIODS_SMA_PRICE_LONG, PERIODS_SMA_PRICE_SHORT);
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //if we have a bullish divergence, let's buy
        if (hasDivergence(history, getPeriods(), true, soObj.getStochasticOscillator()))
            agent.setBuy(true);

        //display our data
        displayData(agent, agent.hasBuy());
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //if we have a bearish divergence, let's sell
        if (hasDivergence(history, getPeriods(), false, soObj.getStochasticOscillator()))
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

        //perform calculations
        this.soObj.calculate(history);
    }
}