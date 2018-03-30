package com.gamesbykevin.tradingbot.calculator;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.agent.AgentManager.TradingStrategy;
import com.gamesbykevin.tradingbot.calculator.strategy.*;
import com.gamesbykevin.tradingbot.util.GSon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.gamesbykevin.tradingbot.Main.ENDPOINT;
import static com.gamesbykevin.tradingbot.calculator.CalculatorHelper.*;
import static com.gamesbykevin.tradingbot.calculator.strategy.NR.PERIODS_NR4;
import static com.gamesbykevin.tradingbot.calculator.strategy.NR.PERIODS_NR7;
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

                case BB:
                    strategy = new BB();
                    break;

                case BBER:
                    strategy = new BBER();
                    break;

                case BBR:
                    strategy = new BBR();
                    break;

                case EMA:
                    strategy = new EMA();
                    break;

                case EMA2:
                    strategy = new EMA(31, 13);
                    break;

                case EMAR:
                    strategy = new EMAR();
                    break;

                case EMAS:
                    strategy = new EMAS();
                    break;

                case EMASV:
                    strategy = new EMASV();
                    break;

                case EMV:
                    strategy = new EMV();
                    break;

                case EMVS:
                    strategy = new EMVS();
                    break;

                case HA:
                    strategy = new HA();
                    break;

                case MACD:
                    strategy = new MACD();
                    break;

                case MACDD:
                    strategy = new MACDD();
                    break;

                case MACS:
                    strategy = new MACS();
                    break;

                case NP:
                    strategy = new NP();
                    break;

                case NR4:
                    strategy = new NR(PERIODS_NR4);
                    break;

                case NR7:
                    strategy = new NR(PERIODS_NR7);
                    break;

                case NVI:
                    strategy = new NVI();
                    break;

                case OA:
                    strategy = new OA();
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

                case RSIA:
                    strategy = new RSIA();
                    break;

                case RSIM:
                    strategy = new RSIM();
                    break;

                case SO:
                    strategy = new SO();
                    break;

                case SOC:
                    strategy = new SOC();
                    break;

                case SOD:
                    strategy = new SOD();
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
            final String json = getJsonResponse(String.format(ENDPOINT_HISTORIC, productId, key.duration));

            //convert json text to multi array
            double[][] data = GSon.getGson().fromJson(json, double[][].class);

            //get the size of our history
            final int size = getHistory().size();

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

                //make sure the history is long enough, and that it has changed before we start doing our calculations
                if (getHistory().size() >= HISTORICAL_PERIODS_MINIMUM && size != getHistory().size()) {

                    //now do all strategy calculations
                    for (int i = 0; i < MY_TRADING_STRATEGIES.length; i++) {
                        getStrategies().get(MY_TRADING_STRATEGIES[i]).calculate(history);
                    }
                }

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

    public List<Period> getHistory() {
        return this.history;
    }

    private HashMap<TradingStrategy, Strategy> getStrategies() {
        return this.strategies;
    }

    public Strategy getStrategy(Agent agent) {
        return getStrategy(agent.getStrategy());
    }

    public Strategy getStrategy(TradingStrategy strategy) {
        return getStrategies().get(strategy);
    }
}