package com.gamesbykevin.tradingbot.calculator;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.agent.AgentManager.TradingStrategy;
import com.gamesbykevin.tradingbot.calculator.strategy.*;
import com.gamesbykevin.tradingbot.util.GSon;
import com.gamesbykevin.tradingbot.util.History;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.gamesbykevin.tradingbot.Main.ENDPOINT;
import static com.gamesbykevin.tradingbot.Main.TRADING_STRATEGIES;
import static com.gamesbykevin.tradingbot.calculator.CalculatorHelper.*;
import static com.gamesbykevin.tradingbot.util.JSon.getJsonResponse;

public class Calculator {

    //keep a list of our periods
    private List<Period> history;

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
     * How long is each period?
     */
    public static int PERIOD_DURATION = 0;

    //our json response string
    private String json;

    public enum Duration {

        OneMinute(60, "one_minute", 6),             //check every 10 seconds
        FiveMinutes(300, "five_minutes", 20),       //check every 15 seconds
        FifteenMinutes(900, "fifteen_minutes", 30), //check every 30 seconds
        OneHour(3600, "one_hour", 90),              //check every 40 seconds
        SixHours(21600, "six_hours", 240),          //check every 90 seconds
        TwentyFourHours(86400, "one_day", 720);     //check every 120 seconds

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

    public Calculator() {

        //create new list(s)
        this.history = new ArrayList<>();
        this.strategies = new HashMap<>();

        //create an object for each strategy that we have specified
        for (int i = 0; i < MY_TRADING_STRATEGIES.length; i++) {

            //what is our strategy?
            Strategy strategy;

            switch (MY_TRADING_STRATEGIES[i]) {

                case ADL:
                    strategy = new ADL();
                    break;

                case ADX:
                    strategy = new ADX();
                    break;

                case BBER:
                    strategy = new BBER();
                    break;

                case EMA:
                    strategy = new EMA();
                    break;

                case EMAR:
                    strategy = new EMAR();
                    break;

                case EMAS:
                    strategy = new EMAS();
                    break;

                case EMV:
                    strategy = new EMV();
                    break;

                case HAE:
                    strategy = new HAE();
                    break;

                case MACD:
                    strategy = new MACD();
                    break;

                case MACS:
                    strategy = new MACS();
                    break;

                case MARS:
                    strategy = new MARS();
                    break;

                case NR:
                    strategy = new NR();
                    break;

                case NVI:
                    strategy = new NVI();
                    break;

                case OBV:
                    strategy = new OBV();
                    break;

                case PVI:
                    strategy = new PVI();
                    break;

                case RSI:
                    strategy = new RSI();
                    break;

                case SO:
                    strategy = new SO();
                    break;

                case SOEMA:
                    strategy = new SOEMA();
                    break;

                case SR:
                    strategy = new SR();
                    break;

                case TWO_RSI:
                    strategy = new TWO_RSI();
                    break;

                default:
                    throw new RuntimeException("Strategy not found: " + MY_TRADING_STRATEGIES[i]);
            }

            //add to hash map
            getStrategies().put(MY_TRADING_STRATEGIES[i], strategy);
        }
    }

    public synchronized boolean update(Duration key, String productId) {

        //were we successful
        boolean result = false;

        try {

            //make our rest call and get the json response
            setJson(getJsonResponse(String.format(ENDPOINT_HISTORIC, productId, key.duration)));

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

            } else {

                result = false;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        //return our result
        return result;
    }

    public synchronized void calculate() {

        //recalculate all our strategies
        for (int i = 0; i < MY_TRADING_STRATEGIES.length; i++) {
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