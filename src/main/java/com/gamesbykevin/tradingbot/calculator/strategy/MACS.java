package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;

import java.util.ArrayList;
import java.util.List;

import static com.gamesbykevin.tradingbot.agent.AgentManagerHelper.displayMessage;
import static com.gamesbykevin.tradingbot.calculator.CalculatorHelper.hasCrossover;
import static com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;

/**
 * Moving average crossover strategy
 */
public class MACS extends Strategy {

    //our list of fast, slow, trending values
    private List<Double> emaFast, emaSlow, emaTrend;

    //list of configurable values
    private static final int PERIODS_MACS_FAST = 5;
    private static final int PERIODS_MACS_SLOW = 10;
    private static final int PERIODS_MACS_TREND = 20;

    private final int fast, slow, trend;

    public MACS() {
        this(PERIODS_MACS_FAST, PERIODS_MACS_SLOW, PERIODS_MACS_TREND);
    }

    public MACS(int fast, int slow, int trend) {

        this.fast = fast;
        this.slow = slow;
        this.trend = trend;

        //create new list(s)
        this.emaFast = new ArrayList<>();
        this.emaSlow = new ArrayList<>();
        this.emaTrend = new ArrayList<>();
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        if (hasCrossover(true, emaFast, emaSlow) && getRecent(emaSlow) > getRecent(emaTrend)) {
            agent.setBuy(true);
            displayMessage(agent, "hasCrossover(true, emaFast, emaSlow) && getRecent(emaSlow) > getRecent(emaTrend)", true);
        } else if (hasCrossover(true, emaFast, emaTrend) && getRecent(emaFast) > getRecent(emaSlow)) {
            agent.setBuy(true);
            displayMessage(agent, "hasCrossover(true, emaFast, emaTrend) && getRecent(emaFast) > getRecent(emaSlow)", true);
        }

        //display our data for what it is worth
        displayData(agent, agent.hasBuy());
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        if (getRecent(emaFast) < getRecent(emaSlow) && getRecent(emaFast) < getRecent(emaTrend))
            agent.setReasonSell(ReasonSell.Reason_Strategy);

        //display our data for what it is worth
        displayData(agent, agent.getReasonSell() != null);
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display values
        display(agent, "EMA Fast: ", emaFast, write);
        display(agent, "EMA Slow: ", emaSlow, write);
        display(agent, "EMA Trend: ", emaTrend, write);
    }

    @Override
    public void calculate(List<Period> history) {

        //calculate the different ema values
        EMA.calculateEMA(history, emaFast, fast);
        EMA.calculateEMA(history, emaSlow, slow);
        EMA.calculateEMA(history, emaTrend, trend);
    }
}