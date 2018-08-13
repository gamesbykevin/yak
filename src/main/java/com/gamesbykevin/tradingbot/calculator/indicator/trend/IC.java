package com.gamesbykevin.tradingbot.calculator.indicator.trend;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.indicator.Indicator;

import java.util.ArrayList;
import java.util.List;

/**
 * Ichimoku Cloud
 */
public class IC extends Indicator {

    //list of indicator values
    private List<Double> tenkanSen;
    private List<Double> kijunSen;
    private List<Double> senkouSpanA;
    private List<Double> senkouSpanB;
    private List<Double> chikouSpan;

    //configurable values
    private static final int PERIODS_TENKAN_SEN = 9;
    private static final int PERIODS_KIJUN_SEN = 26;
    private static final int PERIODS_CHIKOU_SPAN = 26;
    private static final int PERIODS_SENKOU_SPAN_A_FUTURE = 26;
    private static final int PERIODS_SENKOU_SPAN_B = 52;
    private static final int PERIODS_SENKOU_SPAN_B_FUTURE = 22;

    public IC() {

        //call parent
        super(Key.IC, 0);
    }

    @Override
    public void displayData(Agent agent, boolean write) {
        display(agent, "Tenkan Sen   : ", getTenkanSen(), write);
        display(agent, "Kijun Sen    : ", getKijunSen(), write);
        display(agent, "Senkou Span A: ", getSenkouSpanA(), write);
        display(agent, "Senkou Span B: ", getSenkouSpanB(), write);
        display(agent, "Chikou Span  : ", getChikouSpan(), write);
    }

    @Override
    public void calculate(List<Period> history, int newPeriods) {

        //where do we start
        int start = (getTenkanSen().isEmpty()) ? 0 : history.size() - newPeriods;

        //start with the most recent period and go backwards
        for (int index = start; index < history.size(); index++) {

            //calculate tenkan sen (conversion line)
            if (index >= PERIODS_TENKAN_SEN)
                getTenkanSen().add(calculateAverage(history, PERIODS_TENKAN_SEN, index));

            //calculate kiju sen (base line)
            if (index >= PERIODS_KIJUN_SEN)
                getKijunSen().add(calculateAverage(history, PERIODS_KIJUN_SEN, index));

            //calculate chikou span (lagging span)
            if (index >= PERIODS_CHIKOU_SPAN)
                getChikouSpan().add(history.get(index - PERIODS_CHIKOU_SPAN).close);

            //make sure we have both values when calculating senkou span a
            if (index >= PERIODS_KIJUN_SEN && index >= PERIODS_TENKAN_SEN) {

                //the values are also plotted in the future so the current value will come from the past
                if (getKijunSen().size() >= PERIODS_SENKOU_SPAN_A_FUTURE && getTenkanSen().size() >= PERIODS_SENKOU_SPAN_A_FUTURE) {
                    double senkouSpanA = (getRecent(getTenkanSen(), PERIODS_SENKOU_SPAN_A_FUTURE) +
                                            getRecent(getKijunSen(), PERIODS_SENKOU_SPAN_A_FUTURE)) / 2;
                    getSenkouSpanA().add(senkouSpanA);
                }
            }

            //make sure we have enough data for senkou span b
            if (index >= PERIODS_SENKOU_SPAN_B + PERIODS_SENKOU_SPAN_B_FUTURE)
                getSenkouSpanB().add(calculateAverage(history, PERIODS_SENKOU_SPAN_B, index - PERIODS_SENKOU_SPAN_B_FUTURE));
        }
    }

    @Override
    public void cleanup() {
        cleanup(getTenkanSen());
        cleanup(getKijunSen());
        cleanup(getSenkouSpanA());
        cleanup(getSenkouSpanB());
        cleanup(getChikouSpan());
    }

    private double calculateAverage(List<Period> history, int periods, int end) {

        double high = 0;
        double low = 0;

        for (int index = end - periods + 1; index <= end; index++) {

            //get the current period
            Period period = history.get(index);

            //search for the highest and lowest values
            if (high == 0 || period.high > high)
                high = period.high;
            if (low == 0 || period.low < low)
                low = period.low;
        }

        //return the average
        return ((high + low) / 2);
    }

    private List<Double> getTenkanSen() {

        //instantiate if null
        if (this.tenkanSen == null)
            this.tenkanSen = new ArrayList<>();

        return this.tenkanSen;
    }

    private List<Double> getKijunSen() {

        //instantiate if null
        if (this.kijunSen == null)
            this.kijunSen = new ArrayList<>();

        return this.kijunSen;
    }

    private List<Double> getSenkouSpanA() {

        //instantiate if null
        if (this.senkouSpanA == null)
            this.senkouSpanA = new ArrayList<>();

        return this.senkouSpanA;
    }

    private List<Double> getSenkouSpanB() {

        //instantiate if null
        if (this.senkouSpanB == null)
            this.senkouSpanB = new ArrayList<>();

        return this.senkouSpanB;
    }

    private List<Double> getChikouSpan() {

        //instantiate if null
        if (this.chikouSpan == null)
            this.chikouSpan = new ArrayList<>();

        return this.chikouSpan;
    }
}