package com.gamesbykevin.tradingbot.calculator;

import com.gamesbykevin.tradingbot.agent.AgentManager.TradingStrategy;
import com.gamesbykevin.tradingbot.calculator.strategy.*;
import com.gamesbykevin.tradingbot.util.GSon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.gamesbykevin.tradingbot.Main.ENDPOINT;
import static com.gamesbykevin.tradingbot.Main.PERIOD_DURATIONS;
import static com.gamesbykevin.tradingbot.Main.TRADING_STRATEGIES;
import static com.gamesbykevin.tradingbot.calculator.CalculatorHelper.*;
import static com.gamesbykevin.tradingbot.util.JSon.getJsonResponse;

public class Calculator {

    //keep a list of our periods
    private List<Period> history;

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

    //create an indicator class for each trading strategy
    private HashMap<TradingStrategy, Strategy> strategies;

    /**
     * Our list of chosen trading strategies
     */
    public static TradingStrategy[] MY_TRADING_STRATEGIES;

    /**
     * How long is each period we want to trade?
     */
    public static Duration[] MY_PERIOD_DURATIONS;

    //our json response string
    private String json;

    //last time calculator was updated
    private long previous;

    //the chosen duration
    private final Duration duration;

    public enum Duration {

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

        public final String description;

        Duration(long duration, String description, int frequency) {
            this.duration = duration;
            this.description = description;
            this.frequency = frequency;
        }
    }

    public Calculator(Duration duration) {

        //store the selected duration
        this.duration = duration;

        //create new list(s)
        this.history = new ArrayList<>();
        this.strategies = new HashMap<>();

        //create an object for each strategy that we have specified
        for (int i = 0; i < MY_TRADING_STRATEGIES.length; i++) {

            //what is our strategy?
            Strategy strategy;

            switch (MY_TRADING_STRATEGIES[i]) {

                case AE:
                    strategy = new AE();
                    break;

                case BBAR:
                    strategy = new BBAR();
                    break;

                case BBER:
                    strategy = new BBER();
                    break;

                case BBR:
                    strategy = new BBR();
                    break;

                case EMAR:
                    strategy = new EMAR();
                    break;

                case EMAS:
                    strategy = new EMAS();
                    break;

                case HASO:
                    strategy = new HASO();
                    break;

                case MACS:
                    strategy = new MACS();
                    break;

                case MARS:
                    strategy = new MARS();
                    break;

                case MES:
                    strategy = new MES();
                    break;

                case MSL:
                    strategy = new MSL();
                    break;

                case NR:
                    strategy = new NR();
                    break;

                case NVI:
                    strategy = new NVI();
                    break;

                case PVI:
                    strategy = new PVI();
                    break;

                case SOEMA:
                    strategy = new SOEMA();
                    break;

                default:
                    throw new RuntimeException("Strategy not found: " + MY_TRADING_STRATEGIES[i]);
            }

            //add to hash map
            getStrategies().put(MY_TRADING_STRATEGIES[i], strategy);
        }

        //update the previous run time, so it runs immediately since we don't have data yet
        this.previous = System.currentTimeMillis() - (getDuration().duration * 1000);
    }

    public Duration getDuration() {
        return this.duration;
    }

    public boolean canExecute() {
        return System.currentTimeMillis() - previous >= (getDuration().duration / getDuration().frequency) * 1000;
    }

    public synchronized boolean update(String productId) {

        //were we successful
        boolean result = false;

        try {

            //make our rest call and get the json response
            setJson(getJsonResponse(String.format(ENDPOINT_HISTORIC, productId, getDuration().duration)));

            //convert json text to multi array
            double[][] data = GSon.getGson().fromJson(getJson(), double[][].class);

            //make sure we have data before we update
            if (data != null && data.length > 0) {

                //update our history list
                updateHistory(getHistory(), data);

                //sort the history
                sortHistory(getHistory());

                //we are successful
                result = true;

                //update the last successful run
                this.previous = System.currentTimeMillis();

            } else {

                result = false;
            }

            //we want to limit the time between each service call to prevent too many request errors
            Thread.sleep(DELAY);

        } catch (Exception e) {
            e.printStackTrace();
        }

        //return our result
        return result;
    }

    public synchronized void calculate() {

        //recalculate all our strategies
        for (int i = 0; i < MY_TRADING_STRATEGIES.length; i++) {

            //flag the strategy as no longer waiting for new candle data
            getStrategies().get(MY_TRADING_STRATEGIES[i]).setWait(false);

            //calculate based on the current strategy
            getStrategies().get(MY_TRADING_STRATEGIES[i]).calculate(getHistory());
        }
    }

    public List<Period> getHistory() {
        return this.history;
    }

    public Strategy getStrategy(TradingStrategy tradingStrategy) {
        return getStrategies().get(tradingStrategy);
    }

    private HashMap<TradingStrategy, Strategy> getStrategies() {
        return this.strategies;
    }

    public static void populateDurations() {

        //create a new array which will contain our candle durations
        if (MY_PERIOD_DURATIONS == null) {

            //make sure we aren't using duplicate durations
            for (int i = 0; i < PERIOD_DURATIONS.length; i++) {
                for (int j = 0; j < PERIOD_DURATIONS.length; j++) {

                    //don't check the same element
                    if (i == j)
                        continue;

                    //if the value already exists we have duplicate strategies
                    if (PERIOD_DURATIONS[i] == PERIOD_DURATIONS[j])
                        throw new RuntimeException("Duplicate period durations in your property file \"" + PERIOD_DURATIONS[i] + "\"");
                }
            }

            //create new list
            MY_PERIOD_DURATIONS = new Duration[PERIOD_DURATIONS.length];

            //populate the array list
            for (int i = 0; i < PERIOD_DURATIONS.length; i++) {

                //we must have a match
                boolean match = false;

                for (Duration duration : Duration.values()) {

                    //check if we have a match
                    if (duration.duration == PERIOD_DURATIONS[i]) {
                        MY_PERIOD_DURATIONS[i] = duration;
                        match = true;
                        break;
                    }
                }

                //throw exception if no match
                if (!match)
                    throw new RuntimeException("Duration not valid: " + PERIOD_DURATIONS[i]);
            }
        }
    }

    /**
     * Create our array containing our trading strategies
     */
    public static void populateStrategies() {

        //create a new array which will contain our trading strategies
        if (MY_TRADING_STRATEGIES == null) {

            //make sure we aren't using duplicate strategies
            for (int i = 0; i < TRADING_STRATEGIES.length; i++) {
                for (int j = 0; j < TRADING_STRATEGIES.length; j++) {

                    //don't check the same element
                    if (i == j)
                        continue;

                    //if the value already exists we have duplicate strategies
                    if (TRADING_STRATEGIES[i].trim().equalsIgnoreCase(TRADING_STRATEGIES[j].trim()))
                        throw new RuntimeException("Duplicate trading strategy in your property file \"" + TRADING_STRATEGIES[i] + "\"");
                }
            }

            //create our trading array
            MY_TRADING_STRATEGIES = new TradingStrategy[TRADING_STRATEGIES.length];

            //temp list of all values so we can check for a match
            TradingStrategy[] tmp = TradingStrategy.values();

            //make sure the specified strategies exist
            for (int i = 0; i < TRADING_STRATEGIES.length; i++) {

                //check each strategy for a match
                for (int j = 0; j < tmp.length; j++) {

                    //if the spelling matches we have found our strategy
                    if (tmp[j].toString().trim().equalsIgnoreCase(TRADING_STRATEGIES[i].trim())) {

                        //assign our strategy
                        MY_TRADING_STRATEGIES[i] = tmp[j];

                        //exit the loop
                        break;
                    }
                }

                //no matching strategy was found throw exception
                if (MY_TRADING_STRATEGIES[i] == null)
                    throw new RuntimeException("Strategy not found \"" + TRADING_STRATEGIES[i] + "\"");
            }
        }
    }

    private String getJson() {
        return this.json;
    }

    private void setJson(String json) {
        this.json = json;
    }
}