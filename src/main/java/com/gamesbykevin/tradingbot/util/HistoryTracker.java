package com.gamesbykevin.tradingbot.util;

import com.gamesbykevin.tradingbot.calculator.Calculator.Duration;
import com.gamesbykevin.tradingbot.calculator.Period;

import java.util.ArrayList;
import java.util.List;

import static com.gamesbykevin.tradingbot.calculator.Calculator.ENDPOINT_HISTORIC;
import static com.gamesbykevin.tradingbot.calculator.CalculatorHelper.sortHistory;
import static com.gamesbykevin.tradingbot.calculator.CalculatorHelper.updateHistory;
import static com.gamesbykevin.tradingbot.util.JSon.getJsonResponse;
import static com.gamesbykevin.tradingbot.util.PropertyUtil.displayMessage;

/**
 * This class is responsible to track all the candles for all the products on GDAX
 */
public class HistoryTracker implements Runnable {

    /**
     * List of products we are trading
     */
    public static final String[] PRODUCT_IDS = {"LTC-USD", "ETH-USD", "BTC-USD", "BCH-USD"};

    //list of trackers
    private List<Tracker> trackers;

    //individual thread to track the history
    private Thread thread;

    /**
     * How long do we wait between our json calls
     */
    private static final long DELAY = (120L * 1000L);

    /**
     * Default constructor
     */
    public HistoryTracker() {

        //create new thread and start it
        this.thread = new Thread(this);
        this.thread.start();
    }

    private List<Tracker> getTrackers() {

        //create new list of trackers
        if (this.trackers == null)
            this.trackers = new ArrayList<>();

        return this.trackers;
    }

    @Override
    public void run() {

        //loop through each product
        for (String productId : PRODUCT_IDS) {

            //loop through each duration
            for (Duration duration : Duration.values()) {

                //add a tracker for every product/duration combination
                getTrackers().add(new Tracker(productId, duration));
            }
        }

        //load the history for each tracker, before we start tracking
        for (Tracker tracker : getTrackers()) {
            History.load(tracker.history, tracker.productId, tracker.duration, null);
        }

        while (true) {

            try {

                //check every tracker
                for (int i = 0; i < getTrackers().size(); i++) {

                    try {

                        //get the current tracker
                        Tracker tracker = getTrackers().get(i);

                        //get the current size to check for changes
                        int size = tracker.history.size();

                        //display message
                        displayMessage("Checking history: " + tracker.productId + ", " + tracker.duration.description + ", Size: " + tracker.history.size());

                        //format our endpoint
                        String endpoint = String.format(ENDPOINT_HISTORIC, tracker.productId, tracker.duration.duration);

                        //display endpoint
                        displayMessage("Endpoint: " + endpoint);

                        //make historic candle call and get json response
                        String json = getJsonResponse(endpoint);

                        //convert json text to multi array
                        double[][] data = GSon.getGson().fromJson(json, double[][].class);

                        //make sure we have data before we update and sort
                        if (data != null && data.length > 0) {
                            updateHistory(tracker.history, data);
                            sortHistory(tracker.history);
                        }

                        //if the history changed, write it to local storage
                        if (tracker.history.size() != size) {
                            displayMessage("Writing history: " + tracker.productId + ", " + tracker.duration.description + ", Size: " + tracker.history.size());
                            History.write(tracker.history, tracker.productId, tracker.duration);
                        }

                    } catch (Exception ex) {

                        ex.printStackTrace();

                    } finally {

                        //sleep the thread for a short time
                        Thread.sleep(DELAY);
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * This class will track the candle history for a particular product and particular duration
     */
    private class Tracker {

        private final String productId;
        private final Duration duration;
        private final List<Period> history;

        private Tracker(String productId, Duration duration) {
            this.productId = productId;
            this.duration = duration;
            this.history = new ArrayList<>();
        }
    }
}