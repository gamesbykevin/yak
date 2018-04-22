package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;

import java.util.List;

public class HAE extends Strategy {

    //our indicators
    private HA haObj;
    private EMA emaObj;

    private final static int EMA_LONG = 12;
    private final static int EMA_SHORT = 5;

    public HAE() {
        this(EMA_LONG, EMA_SHORT);
    }

    public HAE(int emaLong, int emaShort) {

        //create our indicators
        this.haObj = new HA();
        this.emaObj = new EMA(emaLong, emaShort);
    }

    private Period getRecent() {
        return haObj.getHaPeriods().get(haObj.getHaPeriods().size() - 1);
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //get the most recent heikin-ashi candle
        Period period = getRecent();

        //if the latest candle is bullish and the closing price is above both short and long ema's lets buy
        if (haObj.isBullish(period) && period.close > getRecent(emaObj.getEmaShort()) && period.close > getRecent(emaObj.getEmaLong()))
            agent.setBuy(true);

        //display our data
        displayData(agent, agent.hasBuy());
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //if the candle close is below the ema short
        if (getRecent().close < getRecent(emaObj.getEmaShort()))
            agent.setReasonSell(ReasonSell.Reason_Strategy);

        //display our data
        displayData(agent, agent.getReasonSell() != null);
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display our information
        haObj.displayData(agent, write);
        emaObj.displayData(agent, write);
    }

    @Override
    public void calculate(List<Period> history) {

        //calculate
        haObj.calculate(history);
        emaObj.calculate(history);
    }
}