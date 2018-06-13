package com.gamesbykevin.tradingbot.calculator.indicator;

import com.gamesbykevin.tradingbot.calculator.Calculation;
import com.gamesbykevin.tradingbot.calculator.Calculator;
import com.gamesbykevin.tradingbot.calculator.Period;

import java.util.List;

public abstract class Indicator extends Calculation {

    //we need a way to identify this indicator
    private final Key key;

    public enum Key {

        //momentum
        CCI, RSI, SO, STRSI,

        //trend
        ADX, EMA, HA, LR, MACD, SMA, SMMA, SR,

        //volatility
        ATR, BB,

        //volume
        ADL, EMV, NVI, OBV, PVI,

        //bill williams
        ADO, A, AO, F, MFI,
    }

    //each indicator is checking a number of periods
    private final int periods;

    protected Indicator(Key key, int periods) {
        this.key = key;
        this.periods = periods;

        //make sure we are retaining enough data
        if (getPeriods() > Calculator.HISTORICAL_PERIODS_MINIMUM)
            throw new RuntimeException("Indicator (" + getKey() + ") is calculating more than (" + Calculator.HISTORICAL_PERIODS_MINIMUM + ") periods: " + getPeriods());
    }

    public int getPeriods() {
        return this.periods;
    }

    public Key getKey() {
        return this.key;
    }

    //any common elements here that all indicators have that isn't part of Strategy?
    public abstract void calculate(List<Period> history, int newPeriods);
}