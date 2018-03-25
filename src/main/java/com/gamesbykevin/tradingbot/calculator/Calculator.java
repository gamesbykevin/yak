package com.gamesbykevin.tradingbot.calculator;

import com.gamesbykevin.tradingbot.agent.AgentManager.TradingStrategy;
import com.gamesbykevin.tradingbot.calculator.strategy.*;
import com.gamesbykevin.tradingbot.util.GSon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.gamesbykevin.tradingbot.Main.ENDPOINT;
import static com.gamesbykevin.tradingbot.calculator.strategy.ADX.PERIODS_ADX;
import static com.gamesbykevin.tradingbot.calculator.CalculatorHelper.*;
import static com.gamesbykevin.tradingbot.calculator.strategy.EMA.PERIODS_EMA_LONG;
import static com.gamesbykevin.tradingbot.calculator.strategy.EMA.PERIODS_EMA_SHORT;
import static com.gamesbykevin.tradingbot.calculator.strategy.NR.PERIODS_NR4;
import static com.gamesbykevin.tradingbot.calculator.strategy.NR.PERIODS_NR7;
import static com.gamesbykevin.tradingbot.calculator.strategy.OBV.PERIODS_OBV;
import static com.gamesbykevin.tradingbot.calculator.strategy.RSI.PERIODS_RSI;
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

        //create an object for each strategy
        for (TradingStrategy tradingStrategy : TradingStrategy.values()) {

            Strategy strategy = null;

            switch (tradingStrategy) {

                case ADX:
                    strategy = new ADX();
                    break;

                case MACD:
                    strategy = new MACD();
                    break;

                case RSI:
                    strategy = new RSI();
                    break;

                case OBV:
                    strategy = new OBV();
                    break;

                case EMA:
                    strategy = new EMA();
                    break;

                case MACS:
                    strategy = new MACS();
                    break;

                case RSI_2:
                    strategy = new TWO_RSI();
                    break;

                case NR7:
                    strategy = new NR(PERIODS_NR7);
                    break;

                case MACDD:
                    strategy = new MACDDIV();
                    break;

                case HA:
                    strategy = new HA();
                    break;

                case NR4:
                    strategy = new NR(PERIODS_NR4);
                    break;

                case RSIA:
                    strategy = new RSIA();
                    break;

                case RSIM:
                    strategy = new RSIM();
                    break;

                case BB:
                    strategy = new BB();
                    break;

                case BBER:
                    strategy = new BBER();
                    break;

                case SOD:
                    strategy = new SOD();
                    break;

                case SOC:
                    strategy = new SOC();
                    break;

                case SOEMA:
                    strategy = new SOEMA();
                    break;

                case ADL:
                    strategy = new ADL();
                    break;

                case BBR:
                    strategy = new BBR();
                    break;

                case EMAS:
                    strategy = new EMASV();
                    break;

                case OA:
                    strategy = new OA();
                    break;

                default:
                    throw new RuntimeException("Strategy not found: " + tradingStrategy);
            }

            //add to hash map
            getStrategies().put(tradingStrategy, strategy);
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

                //make sure the history is long enough
                if (getHistory().size() < PERIODS_OBV)
                    throw new RuntimeException("History not long enough to calculate OBV");
                if (getHistory().size() < PERIODS_RSI)
                    throw new RuntimeException("History not long enough to calculate RSI");
                if (getHistory().size() < PERIODS_EMA_SHORT)
                    throw new RuntimeException("History not long enough to calculate EMA (short)");
                if (getHistory().size() < PERIODS_EMA_LONG)
                    throw new RuntimeException("History not long enough to calculate EMA (long)");
                if (getHistory().size() < PERIODS_ADX)
                    throw new RuntimeException("History not long enough to calculate ADX");

                //now do all indicator calculations
                for (Strategy indicator : getStrategies().values()) {
                    indicator.calculate(getHistory());
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

    public Strategy getIndicator(TradingStrategy strategy) {
        return getStrategies().get(strategy);
    }
}