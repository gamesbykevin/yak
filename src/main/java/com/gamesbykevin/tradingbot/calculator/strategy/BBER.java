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
    private static final float RSI_LINE = 50.0f;
    private static final int PERIODS_EMA_LONG = 45;
    private static final int PERIODS_EMA_SHORT = 9;
    private static final int PERIODS_RSI = 12;
    private static final int PERIODS_BB = 18;
    private static final float MULTIPLIER_BB = 2.0f;

    //ema object
    private EMA emaObj;

    //bollinger bands object
    private BB bbObj;

    //our rsi object
    private RSI rsiObj;

    private final float rsiLine;

    public BBER() {
        this(PERIODS_EMA_LONG, PERIODS_EMA_SHORT, PERIODS_BB, MULTIPLIER_BB, PERIODS_RSI, RSI_LINE);
    }

    public BBER(int emaLong, int emaShort, int periodsBB, float multiplierBB, int periodsRSI, float rsiLine) {

        this.rsiLine = rsiLine;

        this.emaObj = new EMA(emaLong, emaShort);
        this.bbObj = new BB(periodsBB, multiplierBB);
        this.rsiObj = new RSI(1, periodsRSI, 0, 0);
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //get the current values
        double emaLongCurrent = getRecent(emaObj.getEmaLong());
        double middleCurrent = getRecent(bbObj.getMiddle());
        double rsiCurrent = getRecent(rsiObj.getRsiVal());

        //get the previous values
        double emaLongPrevious = getRecent(emaObj.getEmaLong(), 2);
        double middlePrevious = getRecent(bbObj.getMiddle(), 2);
        double rsiPrevious = getRecent(rsiObj.getRsiVal(), 2);

        //is the current price above our ema long?
        boolean aboveEmaLong = currentPrice > emaLongCurrent;

        //is the current price above our bollinger bands middle line?
        boolean aboveBbMiddle = currentPrice > middleCurrent;

        //is the rsi value above the rsi line
        boolean aboveRsiSupport = rsiCurrent > rsiLine;

        //if all are true, let's buy
        if (aboveEmaLong && aboveBbMiddle && aboveRsiSupport) {

            //but wait let's make sure we enter at the start of a trend before we buy
            if (rsiPrevious <= rsiLine || currentPrice < middlePrevious || currentPrice < emaLongPrevious)
                agent.setBuy(true);
        }

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
        boolean belowRsiSupport = getRecent(rsiObj.getRsiVal()) < rsiLine;

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