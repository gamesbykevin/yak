package com.gamesbykevin.tradingbot.calculator;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonBuy;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;

import java.util.List;

import static com.gamesbykevin.tradingbot.calculator.CalculatorHelper.hasCrossover;
import static com.gamesbykevin.tradingbot.calculator.CalculatorHelper.hasDivergence;
import static com.gamesbykevin.tradingbot.calculator.MACD.PERIODS_MACD;
import static com.gamesbykevin.tradingbot.calculator.RSI.PERIODS_RSI;
import static com.gamesbykevin.tradingbot.calculator.RSI.RESISTANCE_LINE;
import static com.gamesbykevin.tradingbot.calculator.RSI.SUPPORT_LINE;

/**
 * RSI MACD
 */
public class RSIM extends Indicator {

    //our adx object reference
    private MACD macdObj;

    //our rsi object reference
    private RSI rsiObj;

    public RSIM() {

        //call parent
        super(PERIODS_RSI);

        //create objects
        this.macdObj = new MACD();
        this.rsiObj = new RSI();
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //get the most recent rsi value
        double rsi = rsiObj.getRsi().get(rsiObj.getRsi().size() - 1);

        //if we are at or below the support line, let's check if we are in a good place to buy
        if (rsi <= SUPPORT_LINE) {

            //if bullish divergence in macd and price
            if (hasDivergence(history, PERIODS_MACD, true, macdObj.getMacdLine()))
                agent.setReasonBuy(ReasonBuy.Reason_13);
        }

        //display our data
        displayData(agent, agent.getReasonBuy() != null);
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //get the most recent rsi value
        double rsi = rsiObj.getRsi().get(rsiObj.getRsi().size() - 1);

        //let's see if we are above resistance line before selling
        if (rsi >= RESISTANCE_LINE) {

            //if bearish divergence in macd and price
            if (hasDivergence(history, PERIODS_MACD, false, macdObj.getMacdLine()))
                agent.setReasonSell(ReasonSell.Reason_16);
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