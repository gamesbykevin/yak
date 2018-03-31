package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;

import java.util.List;

/**
 * Bollinger Bands, EMA, & RSI
 */
public class BBER extends Strategy {

    //our list of variations
    protected static float[] LIST_RSI_LINE = {50.0f};
    protected static int[] LIST_PERIODS_EMA_LONG = {60};
    protected static int[] LIST_PERIODS_EMA_SHORT = {10};
    protected static int[] LIST_PERIODS_RSI = {14};
    protected static int[] LIST_PERIODS_BB = {20};

    //list of configurable values
    protected static float RSI_LINE = 50.0f;

    //ema object
    private EMA emaObj;

    //bollinger bands object
    private BB bbObj;

    //our rsi object
    private RSI rsiObj;

    public BBER() {

        //call parent
        super();

        this.emaObj = new EMA();
        this.bbObj = new BB();
        this.rsiObj = new RSI();
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //is the current price above our ema long?
        boolean aboveEmaLong = currentPrice > getRecent(emaObj.getEmaLong());

        //is the current price above our bollinger bands middle line?
        boolean aboveBbMiddle = currentPrice > getRecent(bbObj.getMiddle());

        //is the rsi value above the rsi line
        boolean aboveRsiSupport = getRecent(rsiObj.getRsiVal()) > RSI_LINE;

        //if all are true, let's buy
        if (aboveEmaLong && aboveBbMiddle && aboveRsiSupport)
            agent.setBuy(true);

        //display our data
        displayData(agent, agent.hasBuy());
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //is the current price below our ema long?
        boolean belowEmaLong = currentPrice < getRecent(emaObj.getEmaLong());

        //is the current price below our bollinger bands middle line?
        boolean belowBbMiddle = currentPrice < getRecent(bbObj.getMiddle());

        //is the rsi value below the rsi line
        boolean belowRsiSupport = getRecent(rsiObj.getRsiVal()) < RSI_LINE;

        //if all are true, let's sell
        if (belowEmaLong && belowBbMiddle && belowRsiSupport)
            agent.setReasonSell(ReasonSell.Reason_Strategy);

        //display our data
        displayData(agent, agent.getReasonSell() != null);
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display the information
        this.emaObj.displayData(agent, write);
        this.bbObj.displayData(agent, write);
        this.rsiObj.displayData(agent, write);
    }

    @Override
    public void calculate(List<Period> history) {

        //do our calculations
        this.emaObj.calculate(history);
        this.bbObj.calculate(history);
        this.rsiObj.calculate(history);
    }
}