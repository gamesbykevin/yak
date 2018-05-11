package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.indicator.volatility.BB;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.EMA;
import com.gamesbykevin.tradingbot.calculator.indicator.momentun.RSI;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;

import java.util.ArrayList;
import java.util.List;

import static com.gamesbykevin.tradingbot.agent.AgentManagerHelper.displayMessage;

/**
 * Bollinger Bands, EMA, & RSI
 */
public class BBER extends Strategy {

    //our list of variations
    private static final float RSI_LINE = 50.0f;
    private static final int PERIODS_EMA_LONG = 75;
    private static final int PERIODS_EMA_SHORT = 5;
    private static final int PERIODS_RSI = 14;
    private static final int PERIODS_BB = 20;
    private static final float MULTIPLIER_BB = 2.0f;

    //bollinger bands object
    private BB bbObj;

    //our rsi object
    private RSI rsiObj;

    //our ema object
    private EMA emaObj;

    //our rsi line
    private final float rsiLine;

    public BBER() {
        this(PERIODS_EMA_LONG, PERIODS_EMA_SHORT, PERIODS_BB, MULTIPLIER_BB, PERIODS_RSI, RSI_LINE);
    }

    public BBER(int periodsEmaLong, int periodsEmaShort, int periodsBB, float multiplierBB, int periodsRSI, float rsiLine) {

        this.rsiLine = rsiLine;

        this.bbObj = new BB(periodsBB, multiplierBB);
        this.rsiObj = new RSI(periodsRSI);
        this.emaObj = new EMA(periodsEmaLong, periodsEmaShort);
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //get the current values
        double close = getRecent(history, Period.Fields.Close);
        double ema = getRecent(emaObj.getEmaLong());
        double middle = getRecent(bbObj.getMiddle().getSma());
        double rsi = getRecent(rsiObj.getRsiVal());

        //if the candle closed above our long ema and above the bollinger bands middle line, and our rsi is above the line
        if (close > ema && close > middle && rsi >= RSI_LINE)
                agent.setBuy(true);

        //display our data
        displayMessage(agent, "Close $" + close, agent.hasBuy());
        displayData(agent, agent.hasBuy());
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //get the current values
        double close = getRecent(history, Period.Fields.Close);
        double ema = getRecent(emaObj.getEmaLong());
        double middle = getRecent(bbObj.getMiddle().getSma());
        double rsi = getRecent(rsiObj.getRsiVal());

        //if at least one of our values are below trending set the hard stop $
        if (close < ema || close < middle || rsi <= RSI_LINE)
            adjustHardStopPrice(agent, currentPrice);

        //if the close is below the ema long and bb middle and rsi is heading towards oversold
        if (close < ema && close < middle && rsi <= RSI_LINE)
            agent.setReasonSell(ReasonSell.Reason_Strategy);

        //display our data
        displayMessage(agent, "Close $" + close, agent.hasBuy());
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
    public void calculate(List<Period> history, int newPeriods) {

        //do our calculations
        this.emaObj.calculate(history, newPeriods);
        this.bbObj.calculate(history, newPeriods);
        this.rsiObj.calculate(history, newPeriods);
    }
}