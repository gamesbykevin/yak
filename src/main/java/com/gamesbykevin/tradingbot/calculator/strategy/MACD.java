package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.Period.Fields;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;

import java.util.ArrayList;
import java.util.List;

import static com.gamesbykevin.tradingbot.calculator.CalculatorHelper.hasCrossover;
import static com.gamesbykevin.tradingbot.calculator.strategy.EMA.calculateEmaList;
import static com.gamesbykevin.tradingbot.calculator.strategy.SMA.calculateSMA;

public class MACD extends Strategy {

    //macdLine values
    private List<Double> macdLine;

    //list of ema values from the macd line
    private List<Double> signalLine;

    //our histogram (macdLine - signalLine)
    private List<Double> histogram;

    //our ema object
    private EMA emaObj;

    //our list of variations
    private static final int PERIODS_MACD = 7;
    private static final int PERIODS_EMA_LONG = 25;
    private static final int PERIODS_EMA_SHORT = 4;

    public MACD() {
        this(PERIODS_EMA_LONG, PERIODS_EMA_SHORT, PERIODS_MACD);
    }

    private final int periodsMacd;

    public MACD(int emaLong, int emaShort, int periodsMacd) {

        this.periodsMacd = periodsMacd;

        //create lists and objects
        this.macdLine = new ArrayList<>();
        this.signalLine = new ArrayList<>();
        this.histogram = new ArrayList<>();
        this.emaObj = new EMA(emaLong, emaShort);
    }

    public List<Double> getMacdLine() {
        return this.macdLine;
    }

    public List<Double> getSignalLine() {
        return this.signalLine;
    }

    public List<Double> getHistogram() {
        return this.histogram;
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //if the histogram crossed from below 0 to above 0
        if (getRecent(getHistogram(), 2) < 0 && getRecent(getHistogram()) > 0)
            agent.setBuy(true);

        //display our data
        displayData(agent, agent.hasBuy());
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //if macd goes below the signal line then it is time to sell
        if (getRecent(getMacdLine()) < getRecent(getSignalLine()))
            agent.setReasonSell(ReasonSell.Reason_Strategy);

        //display our data
        displayData(agent, agent.getReasonSell() != null);
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display the recent MACD values which we use as a signal
        display(agent, "MACD Line: ",   getMacdLine(),   write);
        display(agent, "Signal Line: ", getSignalLine(), write);

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
        calculateEmaList(getSignalLine(), getMacdLine(), periodsMacd);

        //last we can calculate the histogram
        calculateHistogram(getMacdLine(), getSignalLine(), getHistogram());
    }

    protected static void calculateMacdLine(List<Double> emaShort, List<Double> emaLong, List<Double> macdLine) {

        //clear the list
        macdLine.clear();

        //we need to start at the right index
        int difference = emaShort.size() - emaLong.size();

        //calculate for every value possible
        for (int i = 0; i < emaLong.size(); i++) {

            //the macd line
            macdLine.add(emaShort.get(difference + i) - emaLong.get(i));
        }
    }

    protected static void calculateHistogram(List<Double> macdLine, List<Double> signalLine, List<Double> histogram) {

        //clear list
        histogram.clear();

        //determine how long back we can calculate the histogram since the list sizes may vary
        int length = (macdLine.size() > signalLine.size()) ? signalLine.size() - 1 : macdLine.size() - 1;

        //loop through and calculate the histogram
        for (int i = length; i > 0; i--) {
            histogram.add(macdLine.get(macdLine.size() - i) - signalLine.get(signalLine.size() - i));
        }
    }
}