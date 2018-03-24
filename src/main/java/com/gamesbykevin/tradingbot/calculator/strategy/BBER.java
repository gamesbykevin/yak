package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonBuy;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;

import java.util.List;

import static com.gamesbykevin.tradingbot.calculator.strategy.BB.PERIODS_BB;
import static com.gamesbykevin.tradingbot.calculator.strategy.RSI.PERIODS_RSI;

/**
 * Bollinger Bands, EMA, & RSI
 */
public class BBER extends Strategy {

    /**
     * How many periods to calculate long ema
     */
    public static final int PERIODS_EMA_LONG = 75;

    /**
     * How many periods to calculate short ema
     */
    public static final int PERIODS_EMA_SHORT = 5;

    /**
     * What is our rsi line to detect bullish / bearish trends
     */
    public static final double RSI_LINE = 50.0d;

    //ema object
    private EMA emaObj;

    //bollinger bands object
    private BB bbObj;

    //our rsi object
    private RSI rsiObj;

    public BBER(int periodsRSI, int periodsBB, int periodsEmaShort, int periodsEmaLong) {

        //call parent with default volume
        super(0);

        this.emaObj = new EMA(periodsEmaLong, periodsEmaShort);
        this.bbObj = new BB(periodsBB);
        this.rsiObj = new RSI(periodsRSI);
    }

    public BBER() {
        this(PERIODS_RSI, PERIODS_BB, PERIODS_EMA_SHORT, PERIODS_EMA_LONG);
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //is the current price below our ema long?
        boolean belowEmaLong = currentPrice < getRecent(emaObj.getEmaLong());

        //is the current price below our bollinger bands middle line?
        boolean belowBbMiddle = currentPrice < getRecent(bbObj.getMiddle());

        //is the rsi value below the mid line suggesting oversold
        boolean belowRsiSupport = getRecent(rsiObj.getRsi()) < RSI_LINE;

        //if all are true, let's buy
        if (belowEmaLong && belowBbMiddle && belowRsiSupport)
            agent.setReasonBuy(ReasonBuy.Reason_14);

        //display our data
        displayData(agent, agent.getReasonBuy() != null);
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //is the current price below our ema long?
        boolean aboveEmaLong = currentPrice > getRecent(emaObj.getEmaLong());

        //is the current price below our bollinger bands middle line?
        boolean aboveBbMiddle = currentPrice > getRecent(bbObj.getMiddle());

        //is the rsi value below the mid line suggesting oversold
        boolean aboveRsiSupport = getRecent(rsiObj.getRsi()) > RSI_LINE;

        //if all are true, let's sell
        if (aboveEmaLong && aboveBbMiddle && aboveRsiSupport)
            agent.setReasonSell(ReasonSell.Reason_15);

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