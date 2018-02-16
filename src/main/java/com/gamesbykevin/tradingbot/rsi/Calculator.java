package com.gamesbykevin.tradingbot.rsi;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.util.GSon;

import java.util.ArrayList;
import java.util.List;

import static com.gamesbykevin.tradingbot.Main.ENDPOINT;
import static com.gamesbykevin.tradingbot.util.JSon.getJsonResponse;

public class Calculator {

    //keep a list of our periods
    private List<Period> history;

    //keep a historical list of the rsi so we can check for divergence
    private List<Float> rsi;

    /**
     * How many periods to calculate rsi
     */
    public static int PERIODS;

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

    public synchronized void update(Duration key) {

        //make our rest call and get the json response
        final String json = getJsonResponse(String.format(ENDPOINT_HISTORIC, productId, key.duration));

        //convert json text to multi array
        double[][] data = GSon.getGson().fromJson(json, double[][].class);

        //make sure we have data before we update
        if (data != null) {

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

            //calculate the rsi for all our specified periods now that we have new data
            calculateRsi();
        }
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

    public synchronized void calculateTrend() {

        //reset values
        setTrend(Trend.None);
        setBreaks(0);

        //if not large enough skip, this shouldn't happen
        if (history.size() < PERIODS || history.isEmpty())
            return;

        //we want to start here
        Period first = history.get(history.size() - PERIODS);

        //the first element is the most recent and is last
        Period last = history.get(PERIODS - 1);

        //our coordinates to calculate slope
        final double x1 = 0, y1, x2 = PERIODS - 1, y2;

        //are we checking an upward trend?
        if (first.close < last.close) {
            setTrend(Trend.Upward);
        } else if (first.close > last.close) {
            setTrend(Trend.Downward);
        } else {
            //no difference
            return;
        }

        //are we detecting and upward or downward trend?
        switch (getTrend()) {
            case Upward:
                y1 = first.low;
                y2 = last.low;
                break;

            case Downward:
                y1 = first.high;
                y2 = last.high;
                break;

            case None:
            default:
                return;
        }

        //the value of y when x = 0
        final double yIntercept = y1;

        //calculate slope
        final double slope = (y2 - y1) / (x2 - x1);

        //the start and end index
        final int start = history.size() - PERIODS + 1;
        final int end = history.size() - 1;

        //check and see if every period is above the slope indicating an upward trend
        for (int i = start; i < end; i++) {

            //get the current period
            Period current = history.get(i);

            //the current x-coordinate
            final double x = i - start;

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

    public boolean hasDivergence(final boolean uptrend, final double currentPrice, final float currentRsi) {

        //flag we will use to track if the price is following the desired trend
        boolean betterPrice = true;

        //check all recent periods
        for (int i = history.size() - PERIODS; i < history.size() - 1; i++) {

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

        //now that the price is good, let's look at the rsi
        boolean betterRsi = true;

        //look at all our rsi periods
        for (int i = 0; i < rsi.size() - 1; i++) {

            if (uptrend) {

                //if checking uptrend we don't want any rsi values higher
                if (rsi.get(i) > currentRsi) {
                    betterRsi = false;
                    break;
                }

            } else {

                //if checking downtrend we don't want any rsi values lower
                if (rsi.get(i) < currentRsi) {
                    betterRsi = false;
                    break;
                }

            }
        }

        //at this point we don't have a divergence in the uptrend
        if (betterRsi)
            return false;

        //if the price is better but the rsi isn't that means we have a divergence
        return (betterPrice && !betterRsi);
    }

    /**
     * Do we have a divergence in the uptrend?
     * @return true if the closing price ends with a higher high, but the indicator is not a higher high, false otherwise
     */
    private boolean hasDivergenceUptrend(final double currentPrice, final float currentRsi) {

        //is the high at the most recent period greater than all others in our period range
        boolean higherPrice = true;

        //track the high price for the latest period
        //final double highPrice = history.get(history.size() - 1).high;
        final double highPrice = currentPrice;

        //make sure all periods before the recent have a high price that is lower
        for (int i = history.size() - PERIODS; i < history.size() - 1; i++) {

            //get the current period
            Period period = history.get(i);

            //oops we have a high price that is higher than our latest high, so this is no good
            if (period.high > highPrice) {
                higherPrice = false;
                break;
            }
        }

        //at this point we don't have a divergence in the uptrend
        if (!higherPrice)
            return false;

        //is the rsi at the most recent period greater than all others in our period range
        boolean higherRsi = true;

        //track the high rsi for the latest period
        //final float highRsi = rsi.get(rsi.size() - 1);
        final float highRsi = currentRsi;

        for (int i = 0; i < rsi.size() - 1; i++) {

            //oops we have a rsi higher that the latest rsi, so this is no good
            if (rsi.get(i) > highRsi) {
                higherRsi = false;
                break;
            }
        }

        //at this point we don't have a divergence in the uptrend
        if (higherRsi)
            return false;

        //if the high is higher than all previous, but the indicator is not higher than all previous we will have a divergence
        return (higherPrice && !higherRsi);
    }

    /**
     * Do we have a divergence in the downtrend?
     @return true if the closing price ends with a lower low, but the indicator is not a lower low, false otherwise
     */
    private boolean hasDivergenceDowntrend(final double currentPrice, final float currentRsi) {

        //is the low at the most recent period lower  than all others in our period range
        boolean lowerPrice = true;

        //track the low price for the latest period
        //final double lowPrice = history.get(history.size() - 1).low;
        final double lowPrice = currentPrice;

        //make sure all periods before the recent have a low price that is greater
        for (int i = history.size() - PERIODS; i < history.size() - 1; i++) {

            //get the current period
            Period period = history.get(i);

            //oops we have a low price that is lower than our latest low, so this is no good
            if (period.low < lowPrice) {
                lowerPrice = false;
                break;
            }
        }

        //at this point we don't have a divergence in the downtrend
        if (!lowerPrice)
            return false;

        //is the rsi at the most recent period lower than all others in our period range
        boolean lowerRsi = true;

        //track the low rsi for the latest period
        //final float lowRsi = rsi.get(rsi.size() - 1);
        final float lowRsi = currentRsi;

        for (int i = 0; i < rsi.size() - 1; i++) {

            //oops we have a rsi lower that the latest rsi, so this is no good
            if (rsi.get(i) < lowRsi) {
                lowerRsi = false;
                break;
            }
        }

        //at this point we don't have a divergence in the uptrend
        if (lowerRsi)
            return false;

        //if the low is lower than all previous, but the indicator is not lower than all previous we will have a divergence
        return (lowerPrice && !lowerRsi);
    }

    /**
     * Calculate the rsi values for each period
     */
    private synchronized void calculateRsi() {

        //clear our historical rsi list
        this.rsi.clear();

        //calculate the rsi for each period
        for (int i = PERIODS; i >= 0; i--) {

            //we need to go back the desired number of periods
            final int startIndex = history.size() - (PERIODS + i);

            //we only go the length of the desired periods
            final int endIndex = startIndex + PERIODS;

            //get the rsi for this period
            final float rsi = calculateRsi(startIndex, endIndex, false, 0);

            //add the rsi calculation to the list
            this.rsi.add(rsi);
        }
    }

    /**
     * Calcuate the rsi value for the specified range
     * @param startIndex Begining period
     * @param endIndex Ending period
     * @param current Are we calculating the current rsi? if false we just want the historical rsi
     * @param currentPrice The current price when calculating the current rsi, otherwise this field is not used
     * @return The rsi value
     */
    private synchronized float calculateRsi(final int startIndex, final int endIndex, final boolean current, final double currentPrice) {

        //the length of our calculation
        final int size = endIndex - startIndex;

        //track total gains and losses
        float gain = 0, loss = 0;
        float gainCurrent = 0, lossCurrent = 0;

        //go through the periods to calculate rsi
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

        //if we don't want the current rsi we can do simple moving average (SMA)
        if (!current) {

            //calculate relative strength
            final float rs = avgGain / avgLoss;

            //calculate relative strength index
            final float rsi = 100 - (100 / (1 + rs));

            //return our rsi value
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

            //smothered rsi including current gain loss
            float smotheredRS =
                (((avgGain * (size - 1)) + gainCurrent) / size)
                /
                (((avgLoss * (size - 1)) + lossCurrent) / size);

            //calculate our rsi value
            final float rsi = 100 - (100 / (1 + smotheredRS));

            //return our rsi value
            return rsi;
        }
    }

    public float getRsiCurrent(final double currentPrice) {
        return calculateRsi(history.size() - PERIODS, history.size(),true, currentPrice);
    }
}