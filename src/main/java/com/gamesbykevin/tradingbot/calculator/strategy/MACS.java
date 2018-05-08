package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.EMA;

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
    private static final int PERIODS_CONFIRM = 3;
    private final int fast, slow, trend, confirm;

    public MACS() {
        this(PERIODS_MACS_FAST, PERIODS_MACS_SLOW, PERIODS_MACS_TREND, PERIODS_CONFIRM);
    }

    public MACS(int fast, int slow, int trend, int confirm) {

        this.fast = fast;
        this.slow = slow;
        this.trend = trend;
        this.confirm = confirm;

        //create new list(s)
        this.emaFast = new ArrayList<>();
        this.emaSlow = new ArrayList<>();
        this.emaTrend = new ArrayList<>();
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        if (getRecent(emaFast) > getRecent(emaSlow) && getRecent(emaSlow) > getRecent(emaTrend)) {

            //let's also confirm our values are all going in the correct direction
            if (getRecent(emaSlow, 2) < getRecent(emaSlow) &&
                getRecent(emaTrend, 2) < getRecent(emaTrend) &&
                getRecent(emaFast, 2) < getRecent(emaFast)) {
                    agent.setBuy(true);
            }
        }

        //display our data for what it is worth
        displayData(agent, agent.hasBuy());
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //if our fast value is below the slow and trend let's sell
        if (getRecent(emaFast) < getRecent(emaSlow) && getRecent(emaFast) < getRecent(emaTrend))
            agent.setReasonSell(ReasonSell.Reason_Strategy);

        //we should sell if every value is trending down even if they haven't crossed
        for (int count = 1; count <= PERIODS_CONFIRM; count++) {
            //TODO UPDATE THIS
        }


        if (getRecent(emaSlow, 3) > getRecent(emaSlow, 2) && getRecent(emaSlow, 2) > getRecent(emaSlow) &&
            getRecent(emaTrend, 3) > getRecent(emaTrend, 2) && getRecent(emaTrend, 2) > getRecent(emaTrend) &&
            getRecent(emaFast, 3) > getRecent(emaFast, 2) && getRecent(emaFast, 2) > getRecent(emaFast)) {

           agent.setReasonSell(ReasonSell.Reason_Strategy);

            //adjust our hard stop price to protect our investment
            adjustHardStopPrice(agent, currentPrice);
        }

        //adjust our hard stop price to protect our investment
        if (getRecent(emaFast) < getRecent(emaSlow) || getRecent(emaFast) < getRecent(emaTrend))
            adjustHardStopPrice(agent, currentPrice);

        //display our data for what it is worth
        displayData(agent, agent.getReasonSell() != null);
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display values
        display(agent, "EMA Fast :", emaFast, write);
        display(agent, "EMA Slow :", emaSlow, write);
        display(agent, "EMA Trend:", emaTrend, write);
    }

    @Override
    public void calculate(List<Period> history) {

        //calculate the different ema values
        EMA.calculateEMA(history, emaFast, fast);
        EMA.calculateEMA(history, emaSlow, slow);
        EMA.calculateEMA(history, emaTrend, trend);
    }
}