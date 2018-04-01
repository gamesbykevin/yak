package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;

import java.util.ArrayList;
import java.util.List;

import static com.gamesbykevin.tradingbot.calculator.CalculatorHelper.hasCrossover;
import static com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;

/**
 * Moving average crossover strategy
 */
public class MACS extends Strategy {

    //our list of fast, slow, trending values
    private List<Double> emaFast, emaSlow, emaTrend;

    //our list of variations
    protected static int[] LIST_PERIODS_MACS_FAST = {10, 20, 5, 10, 5};
    protected static int[] LIST_PERIODS_MACS_SLOW = {20, 60, 10, 30, 10};
    protected static int[] LIST_PERIODS_MACS_TREND = {50, 100, 25, 150, 20};

    //list of configurable values
    protected static int PERIODS_MACS_FAST = 10;
    protected static int PERIODS_MACS_SLOW = 20;
    protected static int PERIODS_MACS_TREND = 50;

    public MACS() {

        //call parent
        super();

        //create new list(s)
        this.emaFast = new ArrayList<>();
        this.emaSlow = new ArrayList<>();
        this.emaTrend = new ArrayList<>();
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //we want a crossover where the fast is greater than slow
        if (hasCrossover(true, emaFast, emaSlow)) {

            //lets also check that the current price is above the trending data
            if (currentPrice > getRecent(emaTrend))
                agent.setBuy(true);
        }

        //display our data for what it is worth
        displayData(agent, agent.hasBuy());
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //we want a crossover where the slow is greater than fast
        if (hasCrossover(false, emaFast, emaSlow)) {

            //lets also check that the current price is below the trending data
            if (currentPrice < getRecent(emaTrend))
                agent.setReasonSell(ReasonSell.Reason_Strategy);
        }

        //display our data for what it is worth
        displayData(agent, agent.getReasonSell() != null);
    }

    @Override
    protected void displayData(Agent agent, boolean write) {

        //display values
        display(agent, "EMA Fast: ", emaFast, write);
        display(agent, "EMA Slow: ", emaSlow, write);
        display(agent, "EMA Trend: ", emaTrend, write);
    }

    @Override
    public void calculate(List<Period> history) {

        //calculate the different ema values
        EMA.calculateEMA(history, emaFast, PERIODS_MACS_FAST);
        EMA.calculateEMA(history, emaSlow, PERIODS_MACS_SLOW);
        EMA.calculateEMA(history, emaTrend, PERIODS_MACS_TREND);
    }
}