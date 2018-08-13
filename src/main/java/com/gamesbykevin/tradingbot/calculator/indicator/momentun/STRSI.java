package com.gamesbykevin.tradingbot.calculator.indicator.momentun;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.indicator.Indicator;

import java.util.ArrayList;
import java.util.List;

/**
 * Stoch RSI
 */
public class STRSI extends Indicator {

    //our rsi object
    private RSI objRsi;

    //list of stoch rsi values
    private List<Double> stochRsi;

    //list of configurable values
    public static final int PERIODS = 14;
    public static final double OVER_BOUGHT = .80d;
    public static final double OVER_SOLD = .20d;

    public STRSI() {
        this(PERIODS);
    }

    public STRSI(int periods) {

        //call parent
        super(Indicator.Key.STRSI, periods);

        //create our rsi object
        this.objRsi = new RSI(periods);

        //create new list
        this.stochRsi = new ArrayList<>();
    }

    private RSI getObjRsi() {
        return this.objRsi;
    }

    public List<Double> getStochRsi() {
        return this.stochRsi;
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display the information
        getObjRsi().displayData(agent, write);
        display(agent, "STOCH RSI: ", getStochRsi(), write);
    }

    @Override
    public void calculate(List<Period> history, int newPeriods) {

        //calculate
        getObjRsi().calculate(history, newPeriods);

        //where do we start?
        int start = getStochRsi().isEmpty() ? 0 : getObjRsi().getValueRSI().size() - newPeriods;

        //check every period
        for (int i = start; i < getObjRsi().getValueRSI().size(); i++) {

            //skip until we have enough data
            if (i < getPeriods())
                continue;

            double rsiHigh = -1, rsiLow = 101;

            //check the recent periods for our calculations
            for (int x = i - getPeriods(); x < i; x++) {

                //get the current rsi value
                double rsi = getObjRsi().getValueRSI().get(x);

                //locate our high and low
                if (rsi < rsiLow)
                    rsiLow = rsi;
                if (rsi > rsiHigh)
                    rsiHigh = rsi;
            }

            //calculate the numerator and the denominator
            double numerator = getObjRsi().getValueRSI().get(i) - rsiLow;
            double denominator = rsiHigh - rsiLow;
            double stochRsi = 0;

            if (numerator != 0 && denominator != 0)
                stochRsi = (numerator / denominator);

            //add our new value to the list
            getStochRsi().add(stochRsi);
        }
    }

    @Override
    public void cleanup() {
        cleanup(getStochRsi());
        getObjRsi().cleanup();
    }
}