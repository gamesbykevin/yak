package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonBuy;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;

import java.util.List;

import static com.gamesbykevin.tradingbot.calculator.CalculatorHelper.hasDivergence;
import static com.gamesbykevin.tradingbot.calculator.strategy.MACD.PERIODS_MACD;
import static com.gamesbykevin.tradingbot.calculator.strategy.RSI.PERIODS_RSI;
import static com.gamesbykevin.tradingbot.calculator.strategy.RSI.RESISTANCE_LINE;
import static com.gamesbykevin.tradingbot.calculator.strategy.RSI.SUPPORT_LINE;

/**
 * RSI MACD
 */
public class RSIM extends Strategy {

    //our adx object reference
    private MACD macdObj;

    //our rsi object reference
    private RSI rsiObj;

    public RSIM() {

        //call parent
        super(PERIODS_RSI);

        //create objects
        this.macdObj = new MACD(PERIODS_MACD);
        this.rsiObj = new RSI(PERIODS_RSI);
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //get the most recent rsi value
        double rsi = getRecent(rsiObj.getRsi());

        //if we are at or below the support line, let's check if we are in a good place to buy
        if (rsi <= SUPPORT_LINE) {

            //if bullish divergence in macd and price
            if (hasDivergence(history, macdObj.getPeriods(), true, macdObj.getMacdLine()))
                agent.setReasonBuy(ReasonBuy.Reason_12);
        }

        //display our data
        displayData(agent, agent.getReasonBuy() != null);
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //get the most recent rsi value
        double rsi = getRecent(rsiObj.getRsi());

        //let's see if we are above resistance line before selling
        if (rsi >= RESISTANCE_LINE) {

            //if bearish divergence in macd and price
            if (hasDivergence(history, macdObj.getPeriods(), false, macdObj.getMacdLine()))
                agent.setReasonSell(ReasonSell.Reason_13);
        }

        //display our data
        displayData(agent, agent.getReasonSell() != null);
    }

    @Override
    public void displayData(Agent agent, boolean write) {

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