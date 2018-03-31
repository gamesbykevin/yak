package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;

import java.util.List;

import static com.gamesbykevin.tradingbot.calculator.CalculatorHelper.hasDivergence;

/**
 * RSI / MACD
 */
public class RSIM extends Strategy {

    //our adx object reference
    private MACD macdObj;

    //our rsi object reference
    private RSI rsiObj;

    //our list of variations
    protected static int[] LIST_PERIODS_RSI = {12};
    protected static int[] LIST_PERIODS_MACD = {9};
    protected static float[] LIST_SUPPORT_LINE = {30.0f};
    protected static float[] LIST_RESISTANCE_LINE = {70.0f};
    protected static int[] LIST_PERIODS_SMA_TREND = {50};
    protected static int[] LIST_PERIODS_EMA_LONG = {26};
    protected static int[] LIST_PERIODS_EMA_SHORT = {12};

    //list of configurable values
    protected static int PERIODS_MACD = 9;
    protected static float SUPPORT_LINE = 30.0f;
    protected static float RESISTANCE_LINE = 70.0f;

    public RSIM() {

        //call parent
        super();

        //create objects
        this.macdObj = new MACD();
        this.rsiObj = new RSI();
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //if we are at or below the support line, let's check if we are in a good place to buy
        if (getRecent(rsiObj.getRsiVal()) <= SUPPORT_LINE) {

            //if bullish divergence in macd and price
            if (hasDivergence(history, PERIODS_MACD, true, macdObj.getMacdLine()))
                agent.setBuy(true);
        }

        //display our data
        displayData(agent, agent.hasBuy());
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //let's see if we are above resistance line before selling
        if (getRecent(rsiObj.getRsiVal()) >= RESISTANCE_LINE) {

            //if bearish divergence in macd and price
            if (hasDivergence(history, PERIODS_MACD, false, macdObj.getMacdLine()))
                agent.setReasonSell(ReasonSell.Reason_Strategy);
        }

        //display our data
        displayData(agent, agent.getReasonSell() != null);
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display information
        rsiObj.displayData(agent, write);
        macdObj.displayData(agent, write);
    }

    @Override
    public void calculate(List<Period> history) {

        //calculate
        rsiObj.calculate(history);
        macdObj.calculate(history);
    }
}