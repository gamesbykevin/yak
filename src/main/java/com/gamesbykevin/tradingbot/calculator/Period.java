package com.gamesbykevin.tradingbot.calculator;

public class Period {
    public long time;
    public double low;
    public double high;
    public double open;
    public double close;
    public double volume;

    public enum Fields {
        Time, Low, High, Open, Close, Volume
    }
}
