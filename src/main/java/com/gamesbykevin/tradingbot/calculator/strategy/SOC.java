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

    public SOC(int periodsSMA, int periodsSO, int periodsSmaPriceLong, int periodsSmaPriceShort) {

        //call parent with a default value
        super(periodsSO);

        //create a new object
        this.soObj = new SO(periodsSMA, periodsSO, periodsSmaPriceLong, periodsSmaPriceShort);
    }

    public SOC() {
        this(PERIODS_SMA, PERIODS_SO, PERIODS_SMA_PRICE_LONG, PERIODS_SMA_PRICE_SHORT);
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