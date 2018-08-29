package com.gamesbykevin.tradingbot.calculator;

public class Period {
    public long time;
    public double low;
    public double high;
    public double open;
    public double close;
    public double volume;

    /**
     * When we get our json data array from gdax what is each attribute for?
     */
    public static final int PERIOD_INDEX_TIME = 0;
    public static final int PERIOD_INDEX_LOW = 1;
    public static final int PERIOD_INDEX_HIGH = 2;
    public static final int PERIOD_INDEX_OPEN = 3;
    public static final int PERIOD_INDEX_CLOSE = 4;
    public static final int PERIOD_INDEX_VOLUME = 5;

    public enum Fields {
        Time, Low, High, Open, Close, Volume
    }
}