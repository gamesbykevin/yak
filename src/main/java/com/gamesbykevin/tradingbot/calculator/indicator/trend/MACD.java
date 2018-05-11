package com.gamesbykevin.tradingbot.calculator.indicator.trend;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.indicator.Indicator;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.EMA;

import java.util.ArrayList;
import java.util.List;

import static com.gamesbykevin.tradingbot.calculator.indicator.trend.EMA.calculateEma;

/**
 * Moving Average Crossover Divergence
 */
public class MACD extends Indicator {

    //macdLine values
    private List<Double> macdLine;

    //list of ema values from the macd line
    private List<Double> signalLine;

    //our histogram (macdLine - signalLine)
    private List<Double> histogram;

    //our ema object
    private EMA objEMA;

    //our list of variations
    private static final int PERIODS_MACD_SIGNAL = 9;
    private static final int PERIODS_EMA_LONG = 26;
    private static final int PERIODS_EMA_SHORT = 12;

    public MACD() {
        this(PERIODS_EMA_LONG, PERIODS_EMA_SHORT, PERIODS_MACD_SIGNAL);
    }

    //how many periods to calculate our signal line
    private final int periods;

    public MACD(int periodsEmaLong, int periodsEmaShort, int periods) {

        //store our settings
        this.periods = periods;

        //create lists and objects
        this.macdLine = new ArrayList<>();
        this.signalLine = new ArrayList<>();
        this.histogram = new ArrayList<>();
        this.objEMA = new EMA(periodsEmaLong, periodsEmaShort);
    }

    private EMA getObjEMA() {
        return this.objEMA;
    }

    private int getPeriods() {
        return this.periods;
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
    public void displayData(Agent agent, boolean write) {

        //display the recent MSL values which we use as a signal
        display(agent, "MSL Line: ",   getMacdLine(),   write);
        display(agent, "Signal Line: ", getSignalLine(), write);
        display(agent, "Histogram: ", getHistogram(), write);

        //display values
        getObjEMA().displayData(agent, write);
    }

    @Override
    public void calculate(List<Period> history, int newPeriods) {

        //calculate our short and long ema values first
        getObjEMA().calculate(history, newPeriods);

        //now we can calculate our macd line
        calculateMacdLine(getObjEMA().getEmaShort(), getObjEMA().getEmaLong(), getMacdLine(), newPeriods);

        //then we can calculate our signal line
        calculateEma(getSignalLine(), getMacdLine(), newPeriods, getPeriods());

        //last we can calculate the histogram
        calculateHistogram(getMacdLine(), getSignalLine(), getHistogram(), newPeriods);
    }

    private void calculateMacdLine(List<Double> emaShort, List<Double> emaLong, List<Double> macdLine, int newPeriods) {

        //where do we start
        int length = (emaShort.size() > emaLong.size()) ? emaLong.size() - 1 : emaShort.size() - 1;

        //where is the first index
        int start = macdLine.isEmpty() ? length : newPeriods;

        //calculate the macd line
        for (int i = start; i > 0; i--) {
            macdLine.add(emaShort.get(emaShort.size() - i) - emaLong.get(emaLong.size() - i));
        }
    }

    private void calculateHistogram(List<Double> macdLine, List<Double> signalLine, List<Double> histogram, int newPeriods) {

        //determine how long back we can calculate the histogram since the list sizes may vary
        int length = (macdLine.size() > signalLine.size()) ? signalLine.size() - 1 : macdLine.size() - 1;

        //where do we start
        int start = histogram.isEmpty() ? length : newPeriods;

        //loop through and calculate the histogram
        for (int i = start; i > 0; i--) {
            histogram.add(macdLine.get(macdLine.size() - i) - signalLine.get(signalLine.size() - i));
        }
    }
}