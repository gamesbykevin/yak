package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonBuy;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;

import java.util.List;

public class TWO_RSI extends Strategy {

    //our rsi object
    private RSI rsiObj;

    /**
     * Minimum required rsi value
     */
    public static final double MIN_RSI = 10.0d;

    /**
     * Maximum required rsi value
     */
    public static final double MAX_RSI = 90.0d;

    /**
     * The two rsi will always be 2 periods
     */
    public static final int TWO_RSI = 2;

    public TWO_RSI() {

        //call parent
        super(TWO_RSI);

        //create new list
        this.rsiObj = new RSI(TWO_RSI);
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //get the most recent rsi value
        double rsi = getRecent(rsiObj.getRsi());

        //if above the max we have a buy signal
        if (rsi > MAX_RSI)
            agent.setReasonBuy(ReasonBuy.Reason_7);

        //display our data
        displayData(agent, agent.getReasonBuy() != null);
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //get the most recent rsi value
        double rsi = getRecent(rsiObj.getRsi());

        //if below the min we have a sell signal
        if (rsi < MIN_RSI)
            agent.setReasonSell(ReasonSell.Reason_8);

        //display our data
        displayData(agent, agent.getReasonSell() != null);
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display the information
        rsiObj.displayData(agent, write);
    }

    @Override
    public void calculate(List<Period> history) {
        rsiObj.calculate(history);
    }
}