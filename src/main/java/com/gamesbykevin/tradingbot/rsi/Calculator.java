package com.gamesbykevin.tradingbot.rsi;

import com.gamesbykevin.tradingbot.Main;
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

    //our calculated rsi value
    private float rsi;

    //the product we are tracking
    private String productId;

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
            for (int row = 0; row < data.length; row++) {

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

    /**
     * Get the RSI value
     * @return The RSI value based on our data
     */
    public synchronized void calculateRsi(final Agent agent, final double currentPrice) {

        //create temp list
        List<Double> tmp = new ArrayList<>();

        //populate with historical feed
        for (int i = 0; i < PERIODS; i++) {
            tmp.add(this.history.get(i).close);
        }

        //return our result
        calculateRsi(tmp, PERIODS, agent, currentPrice);
    }

    private synchronized void calculateRsi(List<Double> periods, final int periodTotal, final Agent agent, final double currentPrice) {

        //track total gains and losses
        float gain = 0, loss = 0;
        float gainCurrent = 0, lossCurrent = 0;

        //go through the periods to calculate rsi
        for (int i = 0; i < periodTotal - 1; i++) {

            //prevent index out of bounds exception
            if (i + 1 >= periods.size())
                break;

            //get the next and previous prices
            double next = periods.get(i);
            double previous = periods.get(i + 1);

            if (next > previous) {

                //here we have a gain
                gain += (next - previous);

            } else {

                //here we have a loss
                loss += (previous - next);
            }
        }

        //check if the current price is a gain or loss
        if (currentPrice > periods.get(0)) {
            gainCurrent = (float)(currentPrice - periods.get(0));
        } else {
            lossCurrent = (float)(periods.get(0) - currentPrice);
        }

        //calculate the average gain and loss
        float avgGain = (gain / periodTotal);
        float avgLoss = (loss / periodTotal);

        //smothered rsi including current gain loss (more accurate)
        float smotheredRS =
            (((avgGain * (periodTotal - 1)) + gainCurrent) / periodTotal)
            /
            (((avgLoss * (periodTotal - 1)) + lossCurrent) / periodTotal);

        //calculate the new and improved more accurate rsi
        setRsi(100 - (100 / (1 + smotheredRS)));

        //print the rsi value
        agent.displayMessage("Product (" + productId + ") RSI = " + getRsi() + ", Stock Price $" + currentPrice, true);
    }

    private void setRsi(final float rsi) {
        this.rsi = rsi;
    }

    public float getRsi() {
        return this.rsi;
    }
}