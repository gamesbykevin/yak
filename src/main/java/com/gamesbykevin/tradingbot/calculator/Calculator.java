package com.gamesbykevin.tradingbot.calculator;

import com.gamesbykevin.tradingbot.util.GSon;

import java.util.ArrayList;
import java.util.List;

import static com.gamesbykevin.tradingbot.Main.ENDPOINT;
import static com.gamesbykevin.tradingbot.util.JSon.getJsonResponse;

public class Calculator {

    //keep a list of our periods
    private List<Period> history;

    //keep a historical list of the calculator so we can check for divergence
    private List<Float> rsi;

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

    //endpoint to get the history
    public static final String ENDPOINT_HISTORIC = ENDPOINT + "/products/%s/candles?granularity=%s";

    //endpoint to get the history
    public static final String ENDPOINT_TICKER = ENDPOINT + "/products/%s/ticker";

    //the product we are tracking
    private String productId;

    public enum Trend {
        Upward, Downward, None
    }

    //what is the current trend?
    private Trend trend = Trend.None;

    //total number of breaks
    private int breaks = 0;

    public enum Duration {

        OneMinute(60);

        /*
        FiveMinutes(300),
        FifteenMinutes(900),
        OneHour(3600),
        SixHours(21600),
        TwentyFourHours(86400);
        */

        public final long duration;

        Duration(long duration) {
            this.duration = duration;
        }
    }

    public Calculator(final String productId) {
        this.productId = productId;
        this.history = new ArrayList<>();
        this.rsi = new ArrayList<>();
    }

    public synchronized boolean update(Duration key) {

        //were we successful
        boolean result = false;

        //make our rest call and get the json response
        final String json = getJsonResponse(String.format(ENDPOINT_HISTORIC, productId, 60));//key.duration));

        //convert json text to multi array
        double[][] data = GSon.getGson().fromJson(json, double[][].class);

        //make sure we have data before we update
        if (data != null && data.length > 0) {

            //clear the current list
            this.history.clear();

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

                //add to array list
                this.history.add(period);
            }

            //calculate the calculator for all our specified periods now that we have new data
            calculateRsi();

            //we are successful
            result = true;

        } else {
            result = false;
        }

        //return our result
        return result;
    }

    private void setTrend(final Trend trend) {
        this.trend = trend;
    }

    private void setBreaks(final int breaks) {
        this.breaks = breaks;
    }

    public Trend getTrend() {
        return this.trend;
    }

    public int getBreaks() {
        return this.breaks;
    }

    public synchronized void calculateTrend(final double currentPrice) {

        //reset values
        setTrend(Trend.None);
        setBreaks(0);

        //if not large enough skip, this shouldn't happen
        if (history.size() < PERIODS_RSI || history.isEmpty())
            return;

        //we want to start here
        Period begin = history.get(history.size() - PERIODS_RSI);

        //are we checking an upward trend?
        if (begin.close < currentPrice) {
            setTrend(Trend.Upward);
        } else if (begin.close > currentPrice) {
            setTrend(Trend.Downward);
        } else {
            //no difference
            return;
        }

        //our coordinates to calculate slope
        final double x1 = 0, y1, x2 = PERIODS_RSI, y2;

        //are we detecting and upward or downward trend?
        switch (getTrend()) {
            case Upward:
                y1 = begin.low;
                y2 = currentPrice;
                break;

            case Downward:
                y1 = begin.high;
                y2 = currentPrice;
                break;

            case None:
            default:
                return;
        }

        //the value of y when x = 0
        final double yIntercept = y1;

        //calculate slope
        final double slope = (y2 - y1) / (x2 - x1);

        //check and see if every period is above the slope indicating an upward trend
        for (int i = history.size() - PERIODS_RSI; i < history.size(); i++) {

            //get the current period
            Period current = history.get(i);

            //the current x-coordinate
            final double x = i - (history.size() - PERIODS_RSI);

            //calculate the y-coordinate
            final double y = (slope * x) + yIntercept;

            //are we checking for an upward trend
            switch (getTrend()) {

                case Upward:

                    //if the current low is below the calculated y-coordinate slope we have a break
                    if (current.low < y)
                        setBreaks(getBreaks() + 1);
                    break;

                case Downward:

                    //if the current high is above the calculated y-coordinate slope we have a break
                    if (current.high > y)
                        setBreaks(getBreaks() + 1);
                    break;
            }
        }
    }

    public synchronized boolean hasDivergence(final boolean uptrend, final double currentPrice, final float currentRsi) {

        //flag we will use to track if the price is following the desired trend
        boolean betterPrice = true;

        //check all recent periods
        for (int i = history.size() - PERIODS_RSI; i < history.size(); i++) {

            //get the current period
            Period period = history.get(i);

            if (uptrend) {

                //if we are checking for an uptrend we don't want any "high" price higher than our current price
                if (period.high > currentPrice) {
                    betterPrice = false;
                    break;
                }

            } else {

                //if we are checking for a downtrend we don't want any "low" price lower than our current price
                if (period.low < currentPrice) {
                    betterPrice = false;
                    break;
                }
            }
        }

        //if we don't have a better price, we don't have a divergence
        if (!betterPrice)
            return false;

        //now that the price is good, let's look at the calculator
        boolean betterRsi = true;

        //look at all our calculator periods
        for (int i = 0; i < rsi.size(); i++) {

            if (uptrend) {

                //if checking uptrend we don't want any calculator values higher
                if (rsi.get(i) > currentRsi) {
                    betterRsi = false;
                    break;
                }

            } else {

                //if checking downtrend we don't want any calculator values lower
                if (rsi.get(i) < currentRsi) {
                    betterRsi = false;
                    break;
                }

            }
        }

        //at this point we don't have a divergence in the uptrend
        if (betterRsi)
            return false;

        //if the price is better but the calculator isn't that means we have a divergence
        return (betterPrice && !betterRsi);
    }

    /**
     * Calculate the SMA (simple moving average)
     * @param currentPeriod The desired period of the SMA we want
     * @param periods The number of periods to check
     * @return The average of the sum of closing prices within the specified period
     */
    public double calculateSMA(final int currentPeriod, final int periods) {

        //the total sum
        double sum = 0;

        //number of prices we add together
        int count = 0;

        //check every period
        for (int i = currentPeriod - periods; i < currentPeriod; i++) {

            //add to the total sum
            sum += history.get(i).close;

            //keep track of how many we add
            count++;
        }

        //return the average of the sum
        return (sum / (double)count);
    }

    public double calculateEMA(final int periods, final double currentPrice, final double previousEMA) {

        //the total sum
        double sum = 0;

        //number of prices we add together
        int count = 0;

        //check every period
        for (int i = history.size() - periods + 1; i < history.size(); i++) {

            //add to the total sum
            sum += history.get(i).close;

            //keep track of how many we add
            count++;
        }

        //include current price
        sum += currentPrice;
        count++;

        //return the average of the sum
        return (sum / (double)count);

        /*
        //what is our multiplier
        final float multiplier = (2 / (periods + 1));

        //what is our ema value
        final double ema;

        //calculate ema
        if (previousEMA == 0) {

            //calculate simple moving average
            final double sma = calculateSMA(history.size(), periods);

            ema = ((currentPrice - sma) * multiplier) + sma;
        } else {
            ema = ((currentPrice - previousEMA) * multiplier) + previousEMA;
        }

        //return our result
        return ema;
        */
    }

    /**
     * Calculate the calculator values for each period
     */
    private void calculateRsi() {

        //clear our historical calculator list
        this.rsi.clear();

        //calculate the calculator for each period
        for (int i = PERIODS_RSI; i >= 0; i--) {

            //we need to go back the desired number of periods
            final int startIndex = history.size() - (PERIODS_RSI + i);

            //we only go the length of the desired periods
            final int endIndex = startIndex + PERIODS_RSI;

            //get the calculator for this period
            final float rsi = calculateRsi(startIndex, endIndex, false, 0);

            //add the calculator calculation to the list
            this.rsi.add(rsi);
        }
    }

    /**
     * Calcuate the calculator value for the specified range
     * @param startIndex Begining period
     * @param endIndex Ending period
     * @param current Are we calculating the current calculator? if false we just want the historical calculator
     * @param currentPrice The current price when calculating the current calculator, otherwise this field is not used
     * @return The calculator value
     */
    private float calculateRsi(final int startIndex, final int endIndex, final boolean current, final double currentPrice) {

        //the length of our calculation
        final int size = endIndex - startIndex;

        //track total gains and losses
        float gain = 0, loss = 0;
        float gainCurrent = 0, lossCurrent = 0;

        //go through the periods to calculate calculator
        for (int i = startIndex; i < endIndex - 1; i++) {

            //prevent index out of bounds exception
            if (i + 1 >= endIndex)
                break;

            //get the next and previous prices
            double previous = history.get(i).close;
            double next     = history.get(i + 1).close;

            if (next > previous) {

                //here we have a gain
                gain += (next - previous);

            } else {

                //here we have a loss
                loss += (previous - next);
            }
        }

        //calculate the average gain and loss
        float avgGain = (gain / size);
        float avgLoss = (loss / size);

        //if we don't want the current calculator we can do simple moving average (SMA)
        if (!current) {

            //calculate relative strength
            final float rs = avgGain / avgLoss;

            //calculate relative strength index
            float rsi = 100 - (100 / (1 + rs));

            //return our calculator value
            return rsi;

        } else {

            //get the latest price in our list so we can compare to the current price
            final double recentPrice = history.get(endIndex - 1).close;

            //check if the current price is a gain or loss
            if (currentPrice > recentPrice) {
                gainCurrent = (float)(currentPrice - recentPrice);
            } else {
                lossCurrent = (float)(recentPrice - currentPrice);
            }

            //smothered calculator including current gain loss
            float smotheredRS =
                (((avgGain * (size - 1)) + gainCurrent) / size)
                /
                (((avgLoss * (size - 1)) + lossCurrent) / size);

            //calculate our calculator value
            final float rsi = 100 - (100 / (1 + smotheredRS));

            //return our calculator value
            return rsi;
        }
    }

    public float getRsiCurrent(final double currentPrice) {
        return calculateRsi(history.size() - PERIODS_RSI, history.size(),true, currentPrice);
    }
}