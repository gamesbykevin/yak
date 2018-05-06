package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;

import java.util.ArrayList;
import java.util.List;

/**
 * Bollinger Bands, EMA, & RSI
 */
public class BBER extends Strategy {

    //our list of variations
    private static final float RSI_LINE = 50.0f;
    private static final int PERIODS_EMA = 75;
    private static final int PERIODS_RSI = 14;
    private static final int PERIODS_BB = 20;
    private static final float MULTIPLIER_BB = 2.0f;

    //bollinger bands object
    private BB bbObj;

    //our rsi object
    private RSI rsiObj;

    //our rsi line
    private final float rsiLine;

    //# of periods to calculate ema
    private final int periodsEMA;

    //list of ema values
    private List<Double> emaList;

    public BBER() {
        this(PERIODS_EMA, PERIODS_BB, MULTIPLIER_BB, PERIODS_RSI, RSI_LINE);
    }

    public BBER(int periodsEMA, int periodsBB, float multiplierBB, int periodsRSI, float rsiLine) {

        this.rsiLine = rsiLine;
        this.periodsEMA = periodsEMA;

        this.bbObj = new BB(periodsBB, multiplierBB);
        this.rsiObj = new RSI(periodsRSI);
        this.emaList = new ArrayList<>();
    }

    private List<Double> getEmaList() {
        return this.emaList;
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //get the current values
        double close = getRecent(history, Period.Fields.Close);
        double ema = getRecent(getEmaList());
        double middle = getRecent(bbObj.getMiddle());
        double rsi = getRecent(rsiObj.getRsiVal());

        //if the close price is above our long ema, middle bollinger band, and the rsi is above the trend
        if (close > ema && close > middle && rsi >= rsiLine)
            agent.setBuy(true);

        //display our data
        displayData(agent, agent.hasBuy());
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //get the current values
        double close = getRecent(history, Period.Fields.Close);
        double ema = getRecent(getEmaList());
        double middle = getRecent(bbObj.getMiddle());
        double rsi = getRecent(rsiObj.getRsiVal());

        //if the close price is below our long ema, middle bollinger band, and the rsi is below the trend
        if (close < ema && close < middle && rsi <= rsiLine)
            agent.setReasonSell(ReasonSell.Reason_Strategy);

        //display our data
        displayData(agent, agent.getReasonSell() != null);
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display the information
        display(agent, "EMA : ", getEmaList(), write);
        this.bbObj.displayData(agent, write);
        this.rsiObj.displayData(agent, write);
    }

    @Override
    public void calculate(List<Period> history) {

        //do our calculations
        EMA.calculateEMA(history, getEmaList(), periodsEMA);
        this.bbObj.calculate(history);
        this.rsiObj.calculate(history);
    }
}