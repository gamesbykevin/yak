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

    //list of sma closing prices
    private List<Double> smaPrice;

    //our ema object
    private EMA emaObj;

    /**
     * How many periods do we calculate ema from macd line
     */
    public static final int PERIODS_MACD = 9;

    /**
     * How many periods do we calculate the sma trend line
     */
    public static final int PERIODS_SMA_TREND = 200;

    public MACD() {
        this(PERIODS_MACD);
    }

    public MACD(int periods) {

        //call parent
        super(periods);

        this.macdLine = new ArrayList<>();
        this.signalLine = new ArrayList<>();
        this.emaObj = new EMA();
        this.histogram = new ArrayList<>();
        this.smaPrice = new ArrayList<>();
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

    public List<Double> getSmaPrice() {
        return this.smaPrice;
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //first we want the close price to be greater than our sma price for the most recent period
        if (getRecent(history, Fields.Close) > getRecent(getSmaPrice())) {

            //then we want our macd line less than 0
            if (getRecent(getMacdLine()) < 0) {

                //last we want the macd line to cross above the signal line
                if (hasCrossover(true, getMacdLine(), getSignalLine()))
                    agent.setBuy(true);
            }
        }


        //display our data
        displayData(agent, agent.hasBuy());
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //first we want the close price to be less than our sma price for the most recent period
        if (getRecent(history, Fields.Close) < getRecent(getSmaPrice())) {

            //then we want our macd line greater than 0
            if (getRecent(getMacdLine()) > 0) {

                //last we want the macd line to cross below the signal line
                if (hasCrossover(false, getMacdLine(), getSignalLine()))
                    agent.setReasonSell(ReasonSell.Reason_Strategy);
            }
        }

        //display our data
        displayData(agent, agent.getReasonSell() != null);
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display the recent MACD values which we use as a signal
        display(agent, "MACD Line: ", getMacdLine(), getPeriods(), write);
        display(agent, "Signal Line: ", getSignalLine(), getPeriods(), write);
        display(agent, "SMA Price: ", getSmaPrice(), getPeriods(), write);

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
        calculateEmaList(getSignalLine(), getMacdLine(), getPeriods());

        //last we can calculate the histogram
        calculateHistogram(getMacdLine(), getSignalLine(), getHistogram());

        //calculate our sma price
        calculateSMA(history, getSmaPrice(), PERIODS_SMA_TREND, Fields.Close);
    }

    protected static void calculateMacdLine(List<Double> emaShort, List<Double> emaLong, List<Double> macdLine) {

        //clear the list
        macdLine.clear();

        //we need to start at the right index
        int difference = emaShort.size() - emaLong.size();

        //calculate for every value possible
        for (int i = 0; i < emaLong.size(); i++) {

            //the macd line is the 12 day ema - 26 day ema
            macdLine.add(emaShort.get(difference + i) - emaLong.get(i));
        }
    }

    protected static void calculateHistogram(List<Double> macdLine, List<Double> signalLine, List<Double> histogram) {

        //clear list
        histogram.clear();

        //determine how long back we can calculate the histogram
        int length = (macdLine.size() > signalLine.size()) ? signalLine.size() - 1 : macdLine.size() - 1;

        //loop through and calculate the histogram
        for (int i = length; i > 0; i--) {
            histogram.add(macdLine.get(i) - signalLine.get(i));
        }
    }
}