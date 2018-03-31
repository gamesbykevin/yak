package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;

import java.util.List;

import static com.gamesbykevin.tradingbot.calculator.CalculatorHelper.hasDivergence;

/**
 * Divergence between MACD Histogram and Price
 */
public class MACDD extends Strategy {

    //our macd object
    private MACD macdObj;

    //our list of variations
    protected static int[] LIST_PERIODS_MACD = {9};
    protected static int[] LIST_PERIODS_SMA_TREND = {50};
    protected static int[] LIST_PERIODS_EMA_LONG = {26};
    protected static int[] LIST_PERIODS_EMA_SHORT = {12};

    //list of configurable values
    protected static int PERIODS_MACD = 9;

    public MACDD() {

        //call parent
        super();

        //create obj
        this.macdObj = new MACD();
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //if bullish divergence, buy
        if (hasDivergence(history, PERIODS_MACD, true, macdObj.getHistogram()))
            agent.setBuy(true);

        //display our data
        displayData(agent, agent.hasBuy());
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //if we have a bearish divergence, we expect price to go down
        if (hasDivergence(history, PERIODS_MACD, false, macdObj.getHistogram()))
            agent.setReasonSell(ReasonSell.Reason_Strategy);

        //display our data
        displayData(agent, agent.getReasonSell() != null);
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display the histogram values which we use as a signal
        display(agent, "Histogram: ", macdObj.getHistogram(), write);

        //display values
        this.macdObj.displayData(agent, write);
    }

    @Override
    public void calculate(List<Period> history) {

        //make macd calculations
        this.macdObj.calculate(history);
    }
}