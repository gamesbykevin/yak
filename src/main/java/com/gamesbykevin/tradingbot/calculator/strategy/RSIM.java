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

    /**
     * How many RSI periods we are calculating
     */
    private static final int PERIODS_RSI = 12;

    /**
     * How many RSI periods we are calculating
     */
    private static final int PERIODS_MACD = 9;

    /**
     * The support line meaning the stock is oversold
     */
    private static final float SUPPORT_LINE = 30.0f;

    /**
     * The resistance line meaning the stock is overbought
     */
    private static final float RESISTANCE_LINE = 70.0f;

    /**
     * How many periods do we calculate the sma trend line
     */
    private static final int PERIODS_SMA_TREND = 50;

    /**
     * How many periods to calculate long ema
     */
    private static final int PERIODS_EMA_LONG = 26;

    /**
     * How many periods to calculate short ema
     */
    private static final int PERIODS_EMA_SHORT = 12;

    public RSIM() {
        this(PERIODS_MACD, PERIODS_SMA_TREND, PERIODS_RSI, PERIODS_EMA_LONG, PERIODS_EMA_SHORT);
    }

    public RSIM(int periodsMacd, int periodsSmaTrend, int periodsRsi, int periodsEmaLong, int periodsEmaShort) {

        //call parent
        super(periodsRsi);

        //create objects
        this.macdObj = new MACD(periodsMacd, periodsSmaTrend, periodsEmaLong, periodsEmaShort);
        this.rsiObj = new RSI(periodsRsi, 1, RESISTANCE_LINE, SUPPORT_LINE);
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //if we are at or below the support line, let's check if we are in a good place to buy
        if (getRecent(rsiObj.getRsiVal()) <= SUPPORT_LINE) {

            //if bullish divergence in macd and price
            if (hasDivergence(history, macdObj.getPeriods(), true, macdObj.getMacdLine()))
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
            if (hasDivergence(history, macdObj.getPeriods(), false, macdObj.getMacdLine()))
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