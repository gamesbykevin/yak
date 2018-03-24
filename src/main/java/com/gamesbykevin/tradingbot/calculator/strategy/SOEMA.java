package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonBuy;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;

import java.util.List;

import static com.gamesbykevin.tradingbot.calculator.CalculatorHelper.hasCrossover;

/**
 * stochastic oscillator and ema
 */
public class SOEMA extends SO {

    //our reference object
    private EMA emaObj;

    /**
     * Number of periods used to calculate ema short
     */
    public static final int PERIODS_EMA_SHORT = 2;

    /**
     * Number of periods used to calculate ema long
     */
    public static final int PERIODS_EMA_LONG = 4;

    /**
     * Additional indicator value we use for trading
     */
    public static final int SO_INDICATOR = 50;

    public SOEMA() {

        //use default value
        super();

        //create new object
        this.emaObj = new EMA(PERIODS_EMA_LONG, PERIODS_EMA_SHORT);
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //if we have a bullish crossover and the so indicator is below 50, let's buy
        if (hasCrossover(true, emaObj.getEmaShort(), emaObj.getEmaLong()) && getRecent(getStochasticOscillator()) < SO_INDICATOR)
            agent.setReasonBuy(ReasonBuy.Reason_17);

        //display our data
        displayData(agent, agent.getReasonBuy() != null);
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //if we have a bearish crossover and the so indicator is above 50, let's sell
        if (hasCrossover(false, emaObj.getEmaShort(), emaObj.getEmaLong()) && getRecent(getStochasticOscillator()) > SO_INDICATOR)
            agent.setReasonSell(ReasonSell.Reason_18);

        //display our data
        displayData(agent, agent.getReasonSell() != null);
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display the information
        super.displayData(agent, write);
        this.emaObj.displayData(agent, write);
    }

    @Override
    public void calculate(List<Period> history) {

        //calculate our value(s)
        super.calculate(history);
        this.emaObj.calculate(history);
    }
}