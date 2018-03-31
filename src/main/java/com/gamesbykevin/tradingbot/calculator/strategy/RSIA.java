package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;

import java.util.List;

import static com.gamesbykevin.tradingbot.calculator.CalculatorHelper.hasCrossover;

/**
 * RSI / Adx
 */
public class RSIA extends Strategy {

    //our adx object reference
    private ADX adxObj;

    //our rsi object reference
    private RSI rsiObj;

    //our list of variations
    protected static int[] LIST_PERIODS_ADX = {20};
    protected static double[] LIST_TREND_ADX = {20.0d};
    protected static int[] LIST_PERIODS_RSI = {7};
    protected static float[] LIST_SUPPORT_LINE = {30.0f};
    protected static float[] LIST_RESISTANCE_LINE = {70.0f};

    //list of configurable values
    protected static float SUPPORT_LINE = 30.0f;
    protected static float RESISTANCE_LINE = 70.0f;

    public RSIA() {

        //call parent
        super();

        //create objects
        this.adxObj = new ADX();
        this.rsiObj = new RSI();
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //start with rsi > resistance
        if (getRecent(rsiObj.getRsiVal()) > RESISTANCE_LINE) {

            //make sure dm+ crosses above dm-
            if (hasCrossover(true, adxObj.getDmPlusIndicator(), adxObj.getDmMinusIndicator()))
                agent.setBuy(true);
        }

        //display our data
        displayData(agent, agent.hasBuy());
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //start with rsi < support
        if (getRecent(rsiObj.getRsiVal()) < SUPPORT_LINE) {

            //make sure dm+ crosses below dm-
            if (hasCrossover(false, adxObj.getDmPlusIndicator(), adxObj.getDmMinusIndicator()))
                agent.setReasonSell(ReasonSell.Reason_Strategy);
        }

        //display our data
        displayData(agent, agent.getReasonSell() != null);
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display our information
        rsiObj.displayData(agent, write);
        adxObj.displayData(agent, write);
    }

    @Override
    public void calculate(List<Period> history) {

        //calculate
        rsiObj.calculate(history);
        adxObj.calculate(history);
    }
}