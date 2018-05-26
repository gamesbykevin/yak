package com.gamesbykevin.tradingbot.calculator;

import com.gamesbykevin.tradingbot.agent.AgentManager;
import com.gamesbykevin.tradingbot.agent.AgentManager.TradingStrategy;
import com.gamesbykevin.tradingbot.calculator.strategy.*;
import com.gamesbykevin.tradingbot.util.GSon;
import com.gamesbykevin.tradingbot.util.History;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.gamesbykevin.tradingbot.Main.ENDPOINT;
import static com.gamesbykevin.tradingbot.calculator.Calculation.PERIODS_RETAIN;
import static com.gamesbykevin.tradingbot.calculator.CalculatorHelper.createStrategy;
import static com.gamesbykevin.tradingbot.calculator.CalculatorHelper.sortHistory;
import static com.gamesbykevin.tradingbot.calculator.CalculatorHelper.updateHistory;
import static com.gamesbykevin.tradingbot.util.JSon.getJsonResponse;
import static com.gamesbykevin.tradingbot.util.PropertyUtil.displayMessage;

public class Calculator {

    //track our historical data for every candle duration
    private HashMap<Candle, List<Period>> history;

    //how long do we pause after our service call, this is to help prevent 429 errors (rate limit exceeded)
    private static final long DELAY = 500L;

    /**
     * How many historical periods do we need in order to start trading
     */
    public static int HISTORICAL_PERIODS_MINIMUM;

    //endpoint to get the history
    public static final String ENDPOINT_HISTORIC = ENDPOINT + "/products/%s/candles?granularity=%s";

    //endpoint to get the history
    public static final String ENDPOINT_TICKER = ENDPOINT + "/products/%s/ticker";

    //we will have the same strategy for each candle
    private List<Strategy> strategies;

    //the product id
    private final String productId;

    /**
     * Our list of chosen trading strategies
     */
    public static TradingStrategy[] MY_TRADING_STRATEGIES;

    public enum Candle {

        OneMinute(60, "one_minute", 6),             //check every 10 seconds
        FiveMinutes(300, "five_minutes", 10),       //check every 30 seconds
        FifteenMinutes(900, "fifteen_minutes", 10), //check every 90 seconds  (1 minute 30 seconds)
        OneHour(3600, "one_hour", 24),              //check every 150 seconds (2 minutes 30 seconds)
        SixHours(21600, "six_hours", 45),          //check every 480 seconds  (8 minutes)
        TwentyFourHours(86400, "one_day", 60);     //check every 1440 seconds (24 minutes)

        //how long (in seconds)
        public final long duration;

        //how often we check
        public final int frequency;

        //text description of this candle
        public final String description;

        //when is the last time this candle has been updated
        public long timestamp = 0;

        Candle(long duration, String description, int frequency) {
            this.duration = duration;
            this.description = description;
            this.frequency = frequency;
        }
    }

    public Calculator(String productId, PrintWriter writer) {

        //the product this calculator is for
        this.productId = productId;

        //we want to track all durations
        for (int j = 0; j < Candle.values().length; j++) {

            //what is the current candle
            Candle duration = Candle.values()[j];

            //we need to add an empty list for the candle duration before we populate it
            getHistory().put(duration, new ArrayList<>());

            //populate the list based on existing file data
            History.load(getHistory().get(duration), productId, duration, writer);

            //update the previous run time, so it runs immediately since we don't have data yet
            duration.timestamp = System.currentTimeMillis() - (duration.duration * 1000);
        }

        //create all strategies and add to our list
        for (int i = 0; i < MY_TRADING_STRATEGIES.length; i++) {
            getStrategies().add(createStrategy(MY_TRADING_STRATEGIES[i]));
        }
    }

    public synchronized boolean update(AgentManager manager) {

        //were we successful
        boolean result = false;

        //we want to track all durations
        for (int j = 0; j < Candle.values().length; j++) {

            //the current candle duration
            Candle duration = Candle.values()[j];

            try {

                //how much time has passed
                long lapsed = System.currentTimeMillis() - duration.timestamp;

                //time required to pass
                long minimum = (duration.duration / duration.frequency) * 1000L;

                //has enough time passed to retrieve more candle data
                if (lapsed >= minimum) {

                    //display message as sometimes the call is not successful
                    displayMessage("Making rest call to retrieve history " + productId + " (" + duration.description + ")", null);

                    //make our rest call and get the json response
                    String json = getJsonResponse(String.format(ENDPOINT_HISTORIC, productId, duration.duration));

                    //convert json text to multi array
                    double[][] data = GSon.getGson().fromJson(json, double[][].class);

                    //make sure we have data before we update
                    if (data != null && data.length > 0) {

                        //get data for the current candle
                        List<Period> tmpHistory = getHistory().get(duration);

                        //store the size
                        final int size = tmpHistory.size();

                        //update our history list with potential new data
                        updateHistory(tmpHistory, data);

                        //sort the history
                        sortHistory(tmpHistory);

                        //let's see if the size changed
                        final int change = tmpHistory.size();

                        //if a new candle has been added re-calculate our strategies
                        if (size != change)
                            calculate(manager, size > change ? size - change : change - size);

                        //rest call is successful
                        displayMessage("Rest call successful. History size: " + change, (change != size) ? manager.getWriter() : null);

                        //we are successful
                        result = true;

                        //update the last successful run
                        duration.timestamp = System.currentTimeMillis();

                    } else {

                        //flag our result false
                        result = false;

                        //rest call isn't successful
                        displayMessage("Rest call is NOT successful.", manager.getWriter());
                    }

                    //we want to limit the time between each service call to prevent too many request errors
                    Thread.sleep(DELAY);
                }

            } catch (Exception e) {

                //display our message and write to the log file
                displayMessage(e, manager.getWriter());
            }
        }

        //return our result
        return result;
    }

    public synchronized void calculate(AgentManager manager, int newPeriods) {

        //calculate all strategies
        for (int i = 0; i < getStrategies().size(); i++) {

            //get the current strategy
            Strategy strategy = getStrategies().get(i);

            //display info
            displayMessage("Calculating " + getStrategies().get(i).getClass() + "...", manager.getWriter());

            //flag the strategy as no longer waiting for new candle data
            strategy.setWait(false);

            //calculate indicator values based on the current strategy
            strategy.calculate(getHistory(), newPeriods);

            //cleanup data list(s) to keep it at a manageable size
            strategy.cleanup();

            //display info
            displayMessage("Calculating " + getStrategies().get(i).getClass() + " Done", manager.getWriter());
        }

        //let's keep our list at a manageable size
        for (int i = 0; i < Candle.values().length; i++) {

            //get the current history list
            List<Period> tmpHistory = getHistory().get(Candle.values()[i]);

            while (tmpHistory.size() > PERIODS_RETAIN) {
                tmpHistory.remove(0);
            }
        }
    }

    private HashMap<Candle, List<Period>> getHistory() {

        //instantiate if null
        if (this.history == null)
            this.history = new HashMap<>();

        return this.history;
    }

    private List<Strategy> getStrategies() {

        //instantiate if null
        if (this.strategies == null)
            this.strategies = new ArrayList<>();

        return this.strategies;
    }
}