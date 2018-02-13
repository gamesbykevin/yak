package com.gamesbykevin.tradingbot.rsi;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.util.GSon;

import java.util.ArrayList;
import java.util.List;

import static com.gamesbykevin.tradingbot.Main.ENDPOINT;
import static com.gamesbykevin.tradingbot.util.JSon.getJsonResponse;

public class Calculator {

    //list of our periods
    private List<Period> history;

    /**
     * How many periods to calculate rsi
     */
    public static int PERIODS;

    //endpoint to get the history
    public static final String ENDPOINT_HISTORIC = ENDPOINT + "/products/%s/candles?granularity=%s";

    //endpoint to get the history
    public static final String ENDPOINT_TICKER = ENDPOINT + "/products/%s/ticker";

    //our calculated rsi value
    private float rsi;

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

    /**
     * Get the RSI value
     * @return The RSI value based on our data
     */
    public synchronized void calculateRsi(final double currentPrice) {

        //create temp list
        List<Double> tmp = new ArrayList<>();

        //populate with historical feed
        for (int i = history.size() - PERIODS; i < history.size(); i++) {
            tmp.add(this.history.get(i).close);
        }

        //return our result
        calculateRsi(tmp, currentPrice);
    }

    private synchronized void calculateRsi(List<Double> periods, final double currentPrice) {

        //track total gains and losses
        float gain = 0, loss = 0;
        float gainCurrent = 0, lossCurrent = 0;

        //go through the periods to calculate rsi
        for (int i = 0; i < periods.size() - 1; i++) {

            //prevent index out of bounds exception
            if (i + 1 >= periods.size())
                break;

            //get the next and previous prices
            double previous = periods.get(i);
            double next = periods.get(i + 1);

            if (next > previous) {

                //here we have a gain
                gain += (next - previous);

            } else {

                //here we have a loss
                loss += (previous - next);
            }
        }

        //get the latest price in our list so we can compare to the current price
        final double recentPrice = periods.get(periods.size() - 1);

        //check if the current price is a gain or loss
        if (currentPrice > recentPrice) {
            gainCurrent = (float)(currentPrice - recentPrice);
        } else {
            lossCurrent = (float)(recentPrice - currentPrice);
        }

        //calculate the average gain and loss
        float avgGain = (gain / periods.size());
        float avgLoss = (loss / periods.size());

        //smothered rsi including current gain loss (more accurate)
        float smotheredRS =
            (((avgGain * (periods.size() - 1)) + gainCurrent) / periods.size())
            /
            (((avgLoss * (periods.size() - 1)) + lossCurrent) / periods.size());

        //calculate the new and improved more accurate rsi
        setRsi(100 - (100 / (1 + smotheredRS)));
    }

    private void setRsi(final float rsi) {
        this.rsi = rsi;
    }

    public float getRsi() {
        return this.rsi;
    }
}