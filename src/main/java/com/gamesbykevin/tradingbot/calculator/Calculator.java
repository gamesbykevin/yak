package com.gamesbykevin.tradingbot.calculator;

import com.gamesbykevin.tradingbot.util.GSon;

import java.util.ArrayList;
import java.util.List;

import static com.gamesbykevin.tradingbot.Main.ENDPOINT;
import static com.gamesbykevin.tradingbot.calculator.CalculatorHelper.*;
import static com.gamesbykevin.tradingbot.calculator.EMA.calculateEMA;
import static com.gamesbykevin.tradingbot.calculator.MACD.calculateMacdLine;
import static com.gamesbykevin.tradingbot.calculator.MACD.calculateSignalLine;
import static com.gamesbykevin.tradingbot.calculator.OBV.calculateOBV;
import static com.gamesbykevin.tradingbot.calculator.RSI.calculateRsi;
import static com.gamesbykevin.tradingbot.util.JSon.getJsonResponse;

public class Calculator {

    //keep a list of our periods
    private List<Period> history;

    //keep a historical list of the rsi so we can check for divergence
    private List<Double> rsi;

    //keep a historical list of the volume so we can check for divergence
    private List<Double> volume;

    //list of ema values for our long period
    private List<Double> emaLong;

    //list of ema values for our short period
    private List<Double> emaShort;

    //macdLine values
    private List<Double> macdLine;

    //list of ema values from the macd line
    private List<Double> signalLine;

    /**
     * How many periods to calculate rsi
     */
    public static int PERIODS_RSI;

    /**
     * How many periods to calculate long ema
     */
    public static int PERIODS_EMA_LONG;

    /**
     * How many periods to calculate short ema
     */
    public static int PERIODS_EMA_SHORT;

    /**
     * How many periods to calculate the on balance volume
     */
    public static int PERIODS_OBV;

    /**
     * How many periods do we calculate ema from macd line
     */
    public static int PERIODS_MACD;

    /**
     * How many periods do we check to confirm a crossover?
     */
    public static int EMA_CROSSOVER;

    /**
     * How many historical periods do we need in order to start trading
     */
    public static int HISTORICAL_PERIODS_MINIMUM;

    //endpoint to get the history
    public static final String ENDPOINT_HISTORIC = ENDPOINT + "/products/%s/candles?granularity=%s";

    //endpoint to get the history
    public static final String ENDPOINT_TICKER = ENDPOINT + "/products/%s/ticker";

    /**
     * How long is each period?
     */
    public static int PERIOD_DURATION = 0;

    public enum Duration {

        OneMinute(60),
        FiveMinutes(300),
        FifteenMinutes(900),
        OneHour(3600),
        SixHours(21600),
        TwentyFourHours(86400);

        public final long duration;

        Duration(long duration) {
            this.duration = duration;
        }
    }

    public Calculator() {
        this.history = new ArrayList<>();
        this.rsi = new ArrayList<>();
        this.volume = new ArrayList<>();
        this.emaShort = new ArrayList<>();
        this.emaLong = new ArrayList<>();
        this.macdLine = new ArrayList<>();
        this.signalLine = new ArrayList<>();
    }

    public synchronized boolean update(Duration key, String productId) {

        //were we successful
        boolean result = false;

        try {

            //make our rest call and get the json response
            final String json = getJsonResponse(String.format(ENDPOINT_HISTORIC, productId, key.duration));

            //convert json text to multi array
            double[][] data = GSon.getGson().fromJson(json, double[][].class);

            //make sure we have data before we update
            if (data != null && data.length > 0) {

                //parse each period from the data
                for (int row = data.length - 1; row >= 0; row--) {

                    //create and populate our period
                    Period period = new Period();
                    period.time = (long) data[row][0];
                    period.low = data[row][1];
                    period.high = data[row][2];
                    period.open = data[row][3];
                    period.close = data[row][4];
                    period.volume = data[row][5];

                    //check this period against our history and add if missing
                    checkHistory(getHistory(), period);
                }

                //sort the history
                sortHistory(getHistory());

                //make sure the history is long enough
                if (getHistory().size() < PERIODS_OBV)
                    throw new RuntimeException("History not long enough to calculate OBV");
                if (getHistory().size() < PERIODS_RSI)
                    throw new RuntimeException("History not long enough to calculate RSI");
                if (getHistory().size() < PERIODS_EMA_SHORT)
                    throw new RuntimeException("History not long enough to calculate EMA (short)");
                if (getHistory().size() < PERIODS_EMA_LONG)
                    throw new RuntimeException("History not long enough to calculate EMA (long)");

                //calculate the rsi for all our specified periods now that we have new data
                calculateRsi(getHistory(), getRsi());

                //calculate the on balance volume for all our specified periods now that we have new data
                calculateOBV(getHistory(), getVolume());

                //calculate the ema for the long period now that we have new data
                calculateEMA(getHistory(), getEmaLong(), PERIODS_EMA_LONG);

                //calculate the ema for the short period now that we have new data
                calculateEMA(getHistory(), getEmaShort(), PERIODS_EMA_SHORT);

                //calculate the macd line
                calculateMacdLine(getMacdLine(), getEmaShort(), getEmaLong());

                //calculate the signal line
                calculateSignalLine(getSignalLine(), getMacdLine());

                //we are successful
                result = true;

            } else {
                result = false;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        //return our result
        return result;
    }

    public synchronized boolean hasDivergenceRsi(boolean uptrend) {
        return hasDivergence(getHistory(), uptrend, PERIODS_RSI, getRsi(), getRsi().get(getRsi().size() - 1));
    }

    public synchronized boolean hasDivergenceObv(boolean uptrend) {
        return hasDivergence(getHistory(), uptrend, PERIODS_OBV, getVolume(), getVolume().get(getVolume().size() - 1));
    }

    public boolean hasEmaCrossover(boolean bullish) {
        return EMA.hasEmaCrossover(bullish, getEmaShort(), getEmaLong());
    }

    public boolean hasMacdCrossover(boolean bullish) {
        return MACD.hasMacdCrossover(bullish, getSignalLine(), getMacdLine());
    }

    public boolean hasMacdConvergenceDivergence(boolean uptrend, int periods, double currentPrice) {

        //if uptrend price is in downtrend, and macd line is uptrend
        if (uptrend) {

            //if the price is in a downtrend, but the macd line is in uptrend
            return hasTrend(false, getHistory(), currentPrice, periods) && MACD.hasConvergenceDivergenceTrend(true, getMacdLine(), periods);

        } else {

            //if the price is in uptrend, but the macd line is in downtrend
            return hasTrend(true, getHistory(), currentPrice, periods) && MACD.hasConvergenceDivergenceTrend(false, getMacdLine(), periods);
        }
    }

    public List<Double> getEmaShort() {
        return this.emaShort;
    }

    public List<Double> getEmaLong() {
        return this.emaLong;
    }

    public List<Period> getHistory() {
        return this.history;
    }

    protected List<Double> getVolume() {
        return this.volume;
    }

    protected List<Double> getRsi() {
        return this.rsi;
    }

    public List<Double> getMacdLine() {
        return this.macdLine;
    }

    public List<Double> getSignalLine() {
        return this.signalLine;
    }

    public double getCurrentRsi() {
        return getRsi().get(getRsi().size() - 1);
    }
}