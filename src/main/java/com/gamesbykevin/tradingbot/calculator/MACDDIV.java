package com.gamesbykevin.tradingbot.calculator;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonBuy;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;

import java.util.ArrayList;
import java.util.List;

import static com.gamesbykevin.tradingbot.calculator.CalculatorHelper.hasDivergence;
import static com.gamesbykevin.tradingbot.calculator.EMA.PERIODS_EMA_LONG;
import static com.gamesbykevin.tradingbot.calculator.EMA.PERIODS_EMA_SHORT;
import static com.gamesbykevin.tradingbot.calculator.MACD.*;

public class MACDDIV extends Indicator {

    //macdLine values
    private List<Double> macdLine;

    //list of ema values from the macd line
    private List<Double> signalLine;

    //our ema object
    private EMA emaObj;

    //our histogram (macdLine - signalLine)
    private List<Double> histogram;

    public MACDDIV() {

        //call parent
        super(PERIODS_MACD);

        this.macdLine = new ArrayList<>();
        this.signalLine = new ArrayList<>();
        this.emaObj = new EMA();
        this.histogram = new ArrayList<>();
    }

    private List<Double> getHistogram() {
        return this.histogram;
    }

    private List<Double> getMacdLine() {
        return this.macdLine;
    }

    private List<Double> getSignalLine() {
        return this.signalLine;
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //if bullish divergence, buy
        if (hasDivergence(history, getPeriods(), true, getHistogram()))
            agent.setReasonBuy(ReasonBuy.Reason_9);

        //display our data
        displayData(agent, agent.getReasonBuy() != null);
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //if we have a bearish divergence, we expect price to go down
        if (hasDivergence(history, getPeriods(), false, getHistogram()))
            agent.setReasonSell(ReasonSell.Reason_12);

        //display our data
        displayData(agent, agent.getReasonSell() != null);
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display the recent MACD values which we use as a signal
        display(agent, "MACD Line: ", getMacdLine(), getPeriods(), write);
        display(agent, "Signal Line: ", getSignalLine(), getPeriods(), write);
        display(agent, "Histogram: ", getHistogram(), getPeriods(), write);

        //display values
        this.emaObj.displayData(agent, write);
    }

    @Override
    public void calculate(List<Period> history) {

        //calculate our short and long ema values first
        this.emaObj.calculate(history);

        //now we can calculate our macd line
        calculateMacdLine(this.emaObj.getEmaShort(), this.emaObj.getEmaLong(), getMacdLine());

        //then we can calculate our signal line
        calculateSignalLine(getSignalLine(), getMacdLine(), getPeriods());

        //now let's calculate our histogram so we can check for divergence
        calculateHistogram(getMacdLine(), getSignalLine(), getHistogram());
    }
}