package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.Period.Fields;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.EMA;
import com.gamesbykevin.tradingbot.calculator.indicator.momentun.SO;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;

import java.util.ArrayList;
import java.util.List;

import static com.gamesbykevin.tradingbot.calculator.CalculatorHelper.hasCrossover;

/**
 * stochastic oscillator / ema
 */
public class SOEMA extends Strategy {

    //our reference object(s)
    private SO objSoFast, objSoSlow;

    //our list of ema values
    private List<Double> ema;

    //list of configurable values
    private static final int PERIODS_EMA = 20;
    private static final int PERIODS_SO_SLOW = 21;
    private static final int PERIODS_SO_SLOW_SMA = 4;
    private static final int PERIODS_SO_FAST = 5;
    private static final int PERIODS_SO_FAST_SMA = 2;
    private static final double OVER_SOLD = 20.0d;
    private static final double OVER_BOUGHT = 80.0d;

    //how many periods do we calculate ema
    private final int periodsEMA;

    public SOEMA() {
        this(PERIODS_EMA, PERIODS_SO_SLOW, PERIODS_SO_SLOW_SMA, PERIODS_SO_FAST, PERIODS_SO_FAST_SMA);
    }

    public SOEMA(int periodsEMA, int periodsSoSlow, int periodsSoSlowSMA, int periodsSoFast, int periodsSoFastSMA) {

        //store our values
        this.periodsEMA = periodsEMA;

        //create new object(s)
        this.objSoSlow = new SO(periodsSoSlow, periodsSoSlowSMA, 1);
        this.objSoFast = new SO(periodsSoFast, periodsSoFastSMA, 1);
        this.ema = new ArrayList<>();
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //first we check that both indicators are at the extreme opposite of each other
        if (getRecent(objSoFast.getStochasticOscillator()) <= OVER_SOLD &&
            getRecent(objSoSlow.getStochasticOscillator()) >= OVER_BOUGHT) {

            //if the price is above the ema average
            if (getRecent(history, Fields.Close) > getRecent(ema))
                agent.setBuy(true);
        }

        //display our data
        displayData(agent, agent.hasBuy());
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //if the price is below the ema average
        if (getRecent(history, Fields.Close) < getRecent(ema)) {
            agent.setReasonSell(ReasonSell.Reason_Strategy);

            //adjust our hard stop price to protect our investment
            adjustHardStopPrice(agent, currentPrice);
        }

        //display our data
        displayData(agent, agent.getReasonSell() != null);
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display the information
        this.objSoSlow.displayData(agent, write);
        this.objSoFast.displayData(agent, write);
        display(agent, "EMA :", ema, write);
    }

    @Override
    public void calculate(List<Period> history) {

        //calculate our value(s)
        this.objSoSlow.calculate(history);
        this.objSoFast.calculate(history);
        EMA.calculateEMA(history, ema, periodsEMA);
    }
}