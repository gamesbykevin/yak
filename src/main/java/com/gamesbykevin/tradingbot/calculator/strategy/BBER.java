package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;

import java.util.List;

/**
 * Bollinger Bands, EMA, & RSI
 */
public class BBER extends Strategy {

    /**
     * How many periods to calculate long ema
     */
    private static final int PERIODS_EMA_LONG = 60;

    /**
     * How many periods to calculate short ema
     */
    private static final int PERIODS_EMA_SHORT = 10;

    /**
     * What is our rsi line to detect bullish / bearish trends
     */
    private static final float RSI_LINE = 50.0f;

    /**
     * How many RSI periods we are calculating
     */
    private static final int PERIODS_RSI = 14;

    /**
     * How many BB periods we are calculating
     */
    private static final int PERIODS_BB = 20;

    //ema object
    private EMA emaObj;

    //bollinger bands object
    private BB bbObj;

    //our rsi object
    private RSI rsiObj;

    //what is our rsi line
    private final float rsiLine;

    public BBER(int periodsRSI, int periodsBB, int periodsEmaShort, int periodsEmaLong, float rsiLine) {

        //call parent with default volume
        super(0);

        this.emaObj = new EMA(periodsEmaLong, periodsEmaShort);
        this.bbObj = new BB(periodsBB);
        this.rsiObj = new RSI(periodsRSI, 1, rsiLine, rsiLine);
        this.rsiLine = rsiLine;
    }

    public BBER() {
        this(PERIODS_RSI, PERIODS_BB, PERIODS_EMA_SHORT, PERIODS_EMA_LONG, RSI_LINE);
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //is the current price above our ema long?
        boolean aboveEmaLong = currentPrice > getRecent(emaObj.getEmaLong());

        //is the current price above our bollinger bands middle line?
        boolean aboveBbMiddle = currentPrice > getRecent(bbObj.getMiddle());

        //is the rsi value above the rsi line
        boolean aboveRsiSupport = getRecent(rsiObj.getRsiVal()) > rsiLine;

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