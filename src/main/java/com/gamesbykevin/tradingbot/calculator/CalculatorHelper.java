package com.gamesbykevin.tradingbot.calculator;

import com.gamesbykevin.tradingbot.calculator.Calculator.Candle;
import com.gamesbykevin.tradingbot.calculator.strategy.*;

import java.util.List;

import static com.gamesbykevin.tradingbot.Main.TRADING_STRATEGIES;
import static com.gamesbykevin.tradingbot.calculator.Calculator.MY_TRADING_STRATEGIES;
import static com.gamesbykevin.tradingbot.calculator.Period.*;
import static com.gamesbykevin.tradingbot.util.History.NOTIFY_LIMIT;
import static com.gamesbykevin.tradingbot.util.PropertyUtil.displayMessage;

public class CalculatorHelper {

    protected static Strategy createStrategy(Strategy.Key key) {

        //what is our strategy?
        Strategy strategy;

        //create the correct strategy
        switch (key) {

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

            case CA:
                strategy = new CA();
                break;

            case EMAC:
                strategy = new EMAC();
                break;

            case EMAR:
                strategy = new EMAR();
                break;

            case EMAS:
                strategy = new EMAS();
                break;

            case FA:
                strategy = new FA();
                break;

            case FADOA:
                strategy = new FADOA();
                break;

            case FAO:
                strategy = new FAO();
                break;

            case FMFI:
                strategy = new FMFI();
                break;

            case MACS:
                strategy = new MACS();
                break;

            case MARS:
                strategy = new MARS();
                break;

            case MER:
                strategy = new MER();
                break;

            case MES:
                strategy = new MES();
                break;

            case RA:
                strategy = new RA();
                break;

            case SEMAS:
                strategy = new SEMAS();
                break;

            case SOEMA:
                strategy = new SOEMA();
                break;

            case SSR:
                strategy = new SSR();
                break;

            default:
                throw new RuntimeException("Strategy not found: " + key);
        }

        //return our object
        return strategy;
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
            MY_TRADING_STRATEGIES = new Strategy.Key[TRADING_STRATEGIES.length];

            //temp list of all values so we can check for a match
            Strategy.Key[] tmp = Strategy.Key.values();

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

    public static void updateHistory(List<Period> history, double[][] data) {

        //parse each period from the data
        for (int row = data.length - 1; row >= 0; row--) {
            addHistory(history, data[row]);
        }
    }

    private static boolean hasHistory(List<Period> history, long time) {

        //check every period for a match
        for (int i = 0; i < history.size(); i++) {

            //if the time matches we have the history
            if (history.get(i).time == time)
                return true;
        }

        //we did not find the history
        return false;
    }

    public static void addHistory(List<Period> history, double[] data) {
        addHistory(history,
            (long)data[PERIOD_INDEX_TIME],
            data[PERIOD_INDEX_LOW],
            data[PERIOD_INDEX_HIGH],
            data[PERIOD_INDEX_OPEN],
            data[PERIOD_INDEX_CLOSE],
            data[PERIOD_INDEX_VOLUME]
        );
    }

    public static void addHistory(List<Period> history,
           long time, double low, double high, double open, double close, double volume) {

        //if we don't have it we can add it
        if (!hasHistory(history, time)) {
            Period period = new Period();
            period.time = time;
            period.open = open;
            period.close = close;
            period.high = high;
            period.low = low;
            period.volume = volume;

            //add to our list
            history.add(period);
        }
    }

    /**
     * Sort the list so the most recent period is at the end of the array list
     * @param history Our current list of history periods
     */
    public static void sortHistory(List<Period> history) {

        //sort so the periods are in order from oldest to newest
        for (int x = 0; x < history.size(); x++) {

            if (x != 0 && x % NOTIFY_LIMIT == 0)
                displayMessage(x + " records sorted");

            for (int y = x; y < history.size() - 1; y++) {

                //get the current and next period
                Period tmp1 = history.get(x);
                Period tmp2 = history.get(y + 1);

                //if the next object does not have a greater time, we need to swap
                if (tmp1.time > tmp2.time) {

                    //swap the values
                    history.set(x,     tmp2);
                    history.set(y + 1, tmp1);
                }
            }
        }
    }

    public static Candle getChild(Candle parent) {

        //the child will be determined by the parent
        switch (parent) {

            case TwentyFourHours:
                return Candle.OneHour;

            case SixHours:
                return Candle.OneHour;

            case OneHour:
                return Candle.FifteenMinutes;

            case FifteenMinutes:
                return Candle.FiveMinutes;

            case FiveMinutes:
                return Candle.OneMinute;

            case OneMinute:
                return null;

            default:
                throw new RuntimeException("Child not found: " + parent);
        }
    }
}