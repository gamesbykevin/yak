package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.Period.Fields;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;

import java.util.ArrayList;
import java.util.List;

import static com.gamesbykevin.tradingbot.agent.AgentManagerHelper.displayMessage;
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

    //what is our slope line
    private float slope = 0f;

    //the index where our crossover happens
    private int x1, x2;

    //our ema object
    private EMA emaObj;

    //our list of variations
    private static final int PERIODS_MACD = 9;
    private static final int PERIODS_EMA_LONG = 26;
    private static final int PERIODS_EMA_SHORT = 12;
    private static final int PERIODS_CONFIRM_TREND = 3;

    public MACD() {
        this(PERIODS_EMA_LONG, PERIODS_EMA_SHORT, PERIODS_MACD, PERIODS_CONFIRM_TREND);
    }

    //how many periods to calculate our macd line, and how many periods do we confirm the trend
    private final int periodsMacd, periodsConfirmTrend;

    public MACD(int emaLong, int emaShort, int periodsMacd, int periodsConfirmTrend) {

        //store our settings
        this.periodsMacd = periodsMacd;
        this.periodsConfirmTrend = periodsConfirmTrend;

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

        //find the latest crossover
        for (int i = getHistogram().size() - 1; i >= 0; i--) {

            //if the current is greater than 0 and the previous is below we have found the crossover
            if (getHistogram().get(i) > 0 && getHistogram().get(i - 1) < 0) {
                setX2(i);
                break;
            }
        }

        //find the crossover before the latest
        for (int i = getX2() - 1; i >= 0; i--) {

            //if the current is greater than 0 and the previous is below we have found the crossover
            if (getHistogram().get(i) > 0 && getHistogram().get(i - 1) < 0) {
                setX1(i);
                break;
            }
        }

        //we are checking the close price for slope
        final double y1 = history.get(getX1()).close;
        final double y2 = history.get(getX2()).close;

        //the slope between the 2 recent crossovers will be our support line
        setSlope( (float)(y2 - y1) / (float)(getX2() - getX1()) );

        //our value when x = 0
        final double yIntercept = history.get(getX1()).close;

        //macd line crosses above signal line and both values are below 0
        final boolean crossBelow = getRecent(getMacdLine()) > getRecent(getSignalLine()) && (getRecent(getMacdLine()) < 0 && getRecent(getSignalLine()) < 0);

        //macd line crosses above signal line and both values are above 0
        final boolean crossAbove = getRecent(getMacdLine()) > getRecent(getSignalLine()) && (getRecent(getMacdLine()) > 0 && getRecent(getSignalLine()) > 0);

        //ensure previous 2 histogram values are increasing
        final boolean increase = getRecent(getHistogram(), 1) > getRecent(getHistogram(), 2) && getRecent(getHistogram(), 2) >  getRecent(getHistogram(), 3);

        //get the latest period
        final Period period = history.get(history.size() - 1);

        //based on our slope that is the support line for closing price
        final double slopePrice = (getSlope() * (getHistogram().size() - x1)) + yIntercept;

        //here are our buy signals
        if (getSlope() > 0) {
            if (increase && crossBelow && period.close > period.open) {
                agent.setBuy(true);
            } else if (increase && crossAbove && period.close > period.open && period.close >= slopePrice) {
                agent.setBuy(true);
            }
        }

        //display our data
        displayMessage(agent,"Period Close $" + period.close + ", Slope $" + slopePrice, agent.hasBuy());
        displayData(agent, agent.hasBuy());
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //get the latest period
        Period period = history.get(history.size() - 1);

        //our value when x = 0
        final double yIntercept = history.get(getX1()).close;

        //based on our slope that is the support line for closing price
        final double slopePrice = (getSlope() * (getHistogram().size() - getX1())) + yIntercept;

        //if macd goes below the signal line and our close is below the slope price it is time to sell
        if (getRecent(getHistogram()) < 0 && period.close < slopePrice)
            agent.setReasonSell(ReasonSell.Reason_Strategy);

        //display our data
        displayData(agent, agent.getReasonSell() != null);
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display the recent MACD values which we use as a signal
        display(agent, "MACD Line: ",   getMacdLine(),   write);
        display(agent, "Signal Line: ", getSignalLine(), write);
        display(agent, "Histogram: ", getHistogram(), write);
        displayMessage(agent, "Slope: " + getSlope(), write);

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

        //where do we start
        int length = (emaShort.size() > emaLong.size()) ? emaLong.size() - 1 : emaShort.size() - 1;

        //calculate the macd line
        for (int i = length; i > 0; i--) {
            macdLine.add(emaShort.get(emaShort.size() - i) - emaLong.get(emaLong.size() - i));
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

    public float getSlope() {
        return this.slope;
    }

    public void setSlope(float slope) {
        this.slope = slope;
    }

    public int getX1() {
        return this.x1;
    }

    public void setX1(int x1) {
        this.x1 = x1;
    }

    public int getX2() {
        return this.x2;
    }

    public void setX2(int x2) {
        this.x2 = x2;
    }
}