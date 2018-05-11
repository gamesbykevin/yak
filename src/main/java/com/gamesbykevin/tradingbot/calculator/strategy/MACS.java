package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.EMA;

import java.util.ArrayList;
import java.util.List;

import static com.gamesbykevin.tradingbot.agent.AgentManagerHelper.displayMessage;
import static com.gamesbykevin.tradingbot.calculator.CalculatorHelper.hasCrossover;
import static com.gamesbykevin.tradingbot.calculator.indicator.trend.EMA.calculateEMA;
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

        //current values
        double currEmaSlow = getRecent(emaSlow);
        double currEmaFast = getRecent(emaFast);
        double currEmaTrend = getRecent(emaTrend);

        //previous values
        double prevEmaSlow = getRecent(emaSlow, 2);
        double prevEmaFast = getRecent(emaFast, 2);
        double prevEmaTrend = getRecent(emaTrend, 2);

        //make sure the fast just crossed above the slow
        if (prevEmaFast < prevEmaSlow && currEmaFast > currEmaSlow) {

            //we also want the slow to be above the trend
            if (currEmaSlow > currEmaTrend) {

                //last thing we check is that all ema values are going in the correct direction
                if (prevEmaSlow < currEmaSlow && prevEmaFast  < currEmaFast && prevEmaTrend < currEmaTrend)
                    agent.setBuy(true);
            }
        }

        //display our data for what it is worth
        displayData(agent, agent.hasBuy());
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //did we confirm downtrend?
        boolean downtrend = true;

        //we should sell if every value is trending down even if they haven't crossed
        for (int count = 1; count <= confirm; count++) {

            //if the previous ema period is less than the current we can't confirm downtrend
            if (getRecent(emaSlow, count + 1) < getRecent(emaSlow, count)) {
                downtrend = false;
                break;
            } else if (getRecent(emaTrend, count + 1) < getRecent(emaTrend, count)) {
                downtrend = false;
                break;
            } else if (getRecent(emaFast, count + 1) < getRecent(emaFast, count)) {
                downtrend = false;
                break;
            }
        }

        //do we have a downtrend
        if (downtrend) {

            //we have a reason to sell
            agent.setReasonSell(ReasonSell.Reason_Strategy);

            //adjust our hard stop price to protect our investment
            adjustHardStopPrice(agent, currentPrice);
        }

        //if our fast value is below the slow and trend let's sell
        if (getRecent(emaFast) < getRecent(emaSlow) && getRecent(emaFast) < getRecent(emaTrend))
            agent.setReasonSell(ReasonSell.Reason_Strategy);

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
    public void calculate(List<Period> history, int newPeriods) {

        //calculate the different ema values
        calculateEMA(history, emaFast, newPeriods, fast);
        calculateEMA(history, emaSlow, newPeriods, slow);
        calculateEMA(history, emaTrend, newPeriods, trend);
    }
}