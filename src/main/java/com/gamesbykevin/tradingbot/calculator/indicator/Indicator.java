package com.gamesbykevin.tradingbot.calculator.indicator;

import com.gamesbykevin.tradingbot.calculator.Calculation;
import com.gamesbykevin.tradingbot.calculator.Period;

import java.util.List;

public abstract class Indicator extends Calculation {

    private final Key key;

    public enum Key {

        //momentum
        CCI, RSI, SO, SR,

        //other
        RC,

        //trend
        ADX, EMA, HA, LR, MACD, SMA, SMMA,

        //volatility
        ATR, BB,

        //volume
        ADL, EMV, NVI, OBV, PVI,

        //bill williams
        ADO, A, AO, F, MFI
    }

    protected Indicator(Key key) {
        this.key = key;
    }

    public Key getKey() {
        return this.key;
    }

    //any common elements here that all indicators have that isn't part of Strategy?
    public abstract void calculate(List<Period> history, int newPeriods);
}