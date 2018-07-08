package com.gamesbykevin.tradingbot.calculator;

import com.gamesbykevin.tradingbot.agent.AgentManager;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.SMA;
import com.gamesbykevin.tradingbot.calculator.Period.Fields;
import com.gamesbykevin.tradingbot.calculator.strategy.*;
import com.gamesbykevin.tradingbot.util.Email;
import com.gamesbykevin.tradingbot.util.GSon;
import com.gamesbykevin.tradingbot.util.History;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import static com.gamesbykevin.tradingbot.Main.ENDPOINT;
import static com.gamesbykevin.tradingbot.agent.AgentHelper.round;
import static com.gamesbykevin.tradingbot.calculator.Calculation.getRecent;
import static com.gamesbykevin.tradingbot.calculator.CalculatorHelper.*;
import static com.gamesbykevin.tradingbot.trade.TradeHelper.NEW_LINE;
import static com.gamesbykevin.tradingbot.util.JSon.getJsonResponse;
import static com.gamesbykevin.tradingbot.util.PropertyUtil.displayMessage;

public class Calculator {

    //track our historical data for every candle duration
    private List<Period> history;

    //list of periods for when we want to create custom candles
    private List<Period> historyTmp;

    //all of the strategies we are trading
    private List<Strategy> strategies;

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

    //endpoint to get the top 50 bid/ask
    public static final String ENDPOINT_ORDER_BOOK = ENDPOINT + "/products/%s/book?level=2";

    //the product id
    private final String productId;

    /**
     * Our list of chosen trading strategies
     */
    public static Strategy.Key[] MY_TRADING_STRATEGIES;

    public enum Candle {

        OneMinute(60, "one_minute", 6, null),                       //check every  10 seconds
        FiveMinutes(300, "five_minutes", 10, null),                 //check every  30 seconds
        FifteenMinutes(900, "fifteen_minutes", 10, null),           //check every  90 seconds (1 minute 30 seconds)
        ThirtyMinutes(1800, "thirty_minutes", 15, FifteenMinutes),  //check every 120 seconds (2 minutes)
        OneHour(3600, "one_hour", 24, null),                        //check every 150 seconds (2 minutes 30 seconds)
        FourHours(14400, "four_hours", 48, OneHour),                //check every 300 seconds (5 minutes)
        SixHours(21600, "six_hours", 60, null),                     //check every 360 seconds (6 minutes)
        TwentyFourHours(86400, "one_day", 120, null);               //check every 720 seconds (12 minutes)

        //how long (in seconds)
        public final long duration;

        //how often we check
        public final int frequency;

        //text description of this candle
        public final String description;

        //is this candle dependent on another? this is for creating a custom candle?
        public final Candle dependency;

        Candle(long duration, String description, int frequency, Candle dependency) {
            this.duration = duration;
            this.description = description;
            this.frequency = frequency;
            this.dependency = dependency;
        }
    }

    //which candle duration is this calculator for?
    private final Candle candle;

    //when was the last time we updated our data
    private long timestamp = 0;

    //object for calculating sma
    private SMA objSMA;

    //our sma will be 200 periods typically
    public static int PERIODS_SMA;

    public Calculator(Candle candle, String productId, PrintWriter writer) {

        //save the candle for this calculator
        this.candle = candle;

        //make sure we have the correct strategies
        populateStrategies();

        //the product this calculator is for
        this.productId = productId;

        //populate the list based on existing file data
        History.load(getHistory(), productId, candle, writer, false);

        //merge periods if this is a custom candle
        if (candle.dependency != null)
            merge(getHistory(), candle);

        //update the previous run time, so it runs immediately since we don't have data yet
        this.timestamp = System.currentTimeMillis() - (candle.duration * 1000);

        //create all strategies and add to our list
        for (int i = 0; i < MY_TRADING_STRATEGIES.length; i++) {

            //add the strategy to the list
            getStrategies().add(createStrategy(MY_TRADING_STRATEGIES[i]));

            //let's calculate the strategies so we can cleanup the data asap
            calculate(writer, getStrategies().get(getStrategies().size() - 1), 0);
        }

        //create our sma and do the first calculation
        this.objSMA = new SMA(PERIODS_SMA);
        getObjSMA().calculate(getHistory(), 0);

        //cleanup the history list
        cleanupHistory(writer);
    }

    public SMA getObjSMA() {
        return this.objSMA;
    }

    public synchronized boolean update(AgentManager manager) {

        //were we successful
        boolean result = false;

        try {

            //how much time has passed
            long lapsed = System.currentTimeMillis() - this.timestamp;

            //time required to pass
            long minimum = (getCandle().duration / getCandle().frequency) * 1000L;

            //has enough time passed to retrieve more candle data
            if (lapsed >= minimum) {

                //display message as sometimes the call is not successful
                displayMessage("Making rest call to retrieve history " + productId + " (" + getCandle().description + ")", null);

                //our json response
                final String json;

                //make our rest call and get the json response
                if (getCandle().dependency == null) {
                    json = getJsonResponse(String.format(ENDPOINT_HISTORIC, productId, getCandle().duration));
                } else {
                    json = getJsonResponse(String.format(ENDPOINT_HISTORIC, productId, getCandle().dependency.duration));
                }

                //convert json text to multi array
                double[][] data = GSon.getGson().fromJson(json, double[][].class);

                //make sure we have data before we update
                if (data != null && data.length > 0) {

                    //store the size
                    final int size = getHistory().size();

                    //if there are no dependencies update as usual
                    if (getCandle().dependency == null) {

                        //update our history list with potential new data
                        updateHistory(getHistory(), data);

                    } else {

                        //else we will need to store the periods separately until we have enough to create our own custom candle(s);
                        updateHistory(getHistory(), getHistoryTmp(), getCandle(), data);
                    }

                    //sort the history
                    sortHistory(getHistory());

                    //let's see if the size changed
                    final int change = getHistory().size();

                    //if a new candle has been added re-calculate our strategies
                    if (size != change)
                        calculate(manager, size > change ? size - change : change - size);

                    //rest call is successful
                    displayMessage("Rest call successful. History size: " + change, (change != size) ? manager.getWriter() : null);

                    //we are successful
                    result = true;

                    //update the last successful run
                    this.timestamp = System.currentTimeMillis();

                } else {

                    //flag our result false
                    result = false;

                    //rest call isn't successful
                    displayMessage("Rest call is NOT successful.", manager.getWriter());
                }

                //we want to prevent too many request errors
                Thread.sleep(DELAY);
            }

        } catch (Exception e) {

            //display our message and write to the log file
            displayMessage(e, manager.getWriter());
        }

        //return our result
        return result;
    }

    private synchronized void calculate(PrintWriter writer, Strategy strategy, int newPeriods) {

        //display info
        displayMessage("Calculating " + getCandle().description + " " + strategy.getKey() + "...", writer);

        //if there are new periods we are no longer waiting to calculate
        strategy.setWait(false);

        //calculate indicator values based on the current strategy
        strategy.calculate(getHistory(), newPeriods);

        //cleanup data list(s) to keep it at a manageable size
        strategy.cleanup();

        //display info
        displayMessage("Calculating " + getCandle().description + " " + strategy.getKey() + " done", writer);
    }

    public synchronized void calculate(AgentManager manager, int newPeriods) {

        //calculate all strategies
        for (int i = 0; i < getStrategies().size(); i++) {

            //calculate the current strategy
            calculate(manager.getWriter(), getStrategies().get(i), newPeriods);
        }

        //calculate our values
        if (PERIODS_SMA > 0)
            getObjSMA().calculate(getHistory(), newPeriods);

        //cleanup the history list
        cleanupHistory(manager.getWriter());
    }

    private void cleanupHistory(PrintWriter writer) {

        //size before cleanup
        displayMessage("Cleaning up history: " + getHistory().size(), writer);

        //let's keep our historical list at a manageable size
        while (getHistory().size() > Calculator.HISTORICAL_PERIODS_MINIMUM) {
            getHistory().remove(0);
        }

        //keep object at a decent size
        getObjSMA().cleanup();

        //size after cleanup
        displayMessage("Cleaned: " + getHistory().size(), writer);
    }

    public List<Period> getHistoryTmp() {

        //instantiate if null
        if (this.historyTmp == null)
            this.historyTmp = new ArrayList<>();

        return this.historyTmp;
    }

    public List<Period> getHistory() {

        //instantiate if null
        if (this.history == null)
            this.history = new ArrayList<>();

        return this.history;
    }

    private List<Strategy> getStrategies() {

        //instantiate if null
        if (this.strategies == null)
            this.strategies = new ArrayList<>();

        return this.strategies;
    }

    public Strategy getStrategy(Strategy.Key key) {

        for (int i = 0; i < getStrategies().size(); i++) {

            //return if the key matches
            if (getStrategies().get(i).getKey() == key)
                return getStrategies().get(i);
        }

        //return null if nothing matched (this shouldn't happen)
        return null;
    }

    public Candle getCandle() {
        return this.candle;
    }
}