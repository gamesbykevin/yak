package com.gamesbykevin.tradingbot.calculator;

import com.gamesbykevin.tradingbot.calculator.Calculator.Candle;
import com.gamesbykevin.tradingbot.calculator.strategy.*;

import java.util.ArrayList;
import java.util.List;

import static com.gamesbykevin.tradingbot.Main.getTradingStrategies;
import static com.gamesbykevin.tradingbot.calculator.Calculator.MY_TRADING_STRATEGIES;
import static com.gamesbykevin.tradingbot.calculator.Period.*;
import static com.gamesbykevin.tradingbot.util.History.NOTIFY_LIMIT;
import static com.gamesbykevin.tradingbot.util.PropertyUtil.DEBUG;
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

            case AR:
                strategy = new AR();
                break;

            case AS:
                strategy = new AS();
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

            case CC:
                strategy = new CC();
                break;

            case DM:
                strategy = new DM();
                break;

            case EC:
                strategy = new EC();
                break;

            case EMA3:
                strategy = new EMA3();
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

            case ERS:
                strategy = new ERS();
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

            case HASO:
                strategy = new HASO();
                break;

            case LDB:
                strategy = new LDB();
                break;

            case MA:
                strategy = new MA();
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

            case MH:
                strategy = new MH();
                break;

            case MP:
                strategy = new MP();
                break;

            case MS:
                strategy = new MS();
                break;

            case NR4:
                strategy = new NR4();
                break;

            case NR7:
                strategy = new NR7();
                break;

            case NVE:
                strategy = new NVE();
                break;

            case PSE:
                strategy = new PSE();
                break;

            case PVE:
                strategy = new PVE();
                break;

            case RA:
                strategy = new RA();
                break;

            case SEMAS:
                strategy = new SEMAS();
                break;

            case SMASR:
                strategy = new SMASR();
                break;

            case SOADX:
                strategy = new SOADX();
                break;

            case SOEMA:
                strategy = new SOEMA();
                break;

            case SSR:
                strategy = new SSR();
                break;

            case TWOA:
                strategy = new TWOA();
                break;

            case TWOR:
                strategy = new TWOR();
                break;

            case VS:
                strategy = new VS();
                break;

            case VWM:
                strategy = new VWM();
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
            for (int i = 0; i < getTradingStrategies().length; i++) {
                for (int j = 0; j < getTradingStrategies().length; j++) {

                    //don't check the same element
                    if (i == j)
                        continue;

                    //if the value already exists we have duplicate strategies
                    if (getTradingStrategies()[i].trim().equalsIgnoreCase(getTradingStrategies()[j].trim()))
                        throw new RuntimeException("Duplicate trading strategy in your property file \"" + getTradingStrategies()[i] + "\"");
                }
            }

            //create our trading array
            MY_TRADING_STRATEGIES = new Strategy.Key[getTradingStrategies().length];

            //temp list of all values so we can check for a match
            Strategy.Key[] tmp = Strategy.Key.values();

            //make sure the specified strategies exist
            for (int i = 0; i < getTradingStrategies().length; i++) {

                //check each strategy for a match
                for (int j = 0; j < tmp.length; j++) {

                    //if the spelling matches we have found our strategy
                    if (tmp[j].toString().trim().equalsIgnoreCase(getTradingStrategies()[i].trim())) {

                        //assign our strategy
                        MY_TRADING_STRATEGIES[i] = tmp[j];

                        //exit the loop
                        break;
                    }
                }

                //no matching strategy was found throw exception
                if (MY_TRADING_STRATEGIES[i] == null)
                    throw new RuntimeException("Strategy not found \"" + getTradingStrategies()[i] + "\"");
            }
        }
    }

    public static long getTimeMax(List<Period> history) {

        long timeMax = 0;

        //search for the highest value
        for (int i = 0; i < history.size(); i++) {

            if (history.get(i).time > timeMax)
                timeMax = history.get(i).time;
        }

        return timeMax;
    }

    /**
     * Update the history according to our custom candle
     * @param history Final list of candles
     * @param historyTmp List of temp candles so we can create our custom period
     * @param candle The custom period we are trying to create
     * @param data JSon response of period data
     */
    public static void updateHistory(List<Period> history, List<Period> historyTmp, Candle candle, double[][] data) {

        //check each row of data to filter out the valid periods
        for (int row = 0; row < data.length; row++) {

            //get the time from the period
            long time = (long)data[row][PERIOD_INDEX_TIME];

            //we are only interested in new candles
            if (time <= history.get(history.size() - 1).time)
                continue;

            //make sure we don't already have this
            boolean duplicate = false;

            //make sure they aren't already part of the tmp list
            for (int i = 0; i < historyTmp.size(); i++) {

                //update the data if already existing
                if (historyTmp.get(i).time == time) {
                    historyTmp.get(i).low = data[row][PERIOD_INDEX_LOW];
                    historyTmp.get(i).high = data[row][PERIOD_INDEX_HIGH];
                    historyTmp.get(i).open = data[row][PERIOD_INDEX_OPEN];
                    historyTmp.get(i).close = data[row][PERIOD_INDEX_CLOSE];
                    historyTmp.get(i).volume = data[row][PERIOD_INDEX_VOLUME];
                    duplicate = true;
                    break;
                }
            }

            //if we already have this, skip to the next
            if (duplicate)
                continue;

            //now we can create a period
            Period period = new Period();
            period.time = time;
            period.low = data[row][PERIOD_INDEX_LOW];
            period.high = data[row][PERIOD_INDEX_HIGH];
            period.open = data[row][PERIOD_INDEX_OPEN];
            period.close = data[row][PERIOD_INDEX_CLOSE];
            period.volume = data[row][PERIOD_INDEX_VOLUME];

            //and add to our tmp list
            historyTmp.add(period);
        }

        //create new candles while we have enough data
        while (historyTmp.size() >= (candle.duration / candle.dependency.duration)) {

            long timePrevious = history.get(history.size() - 1).time;

            //create our new candle
            Period candleNew = new Period();

            //assign values for our candle
            candleNew.time = timePrevious + candle.duration;
            candleNew.open = 0;
            candleNew.close = 0;
            candleNew.low = 0;
            candleNew.high = 0;
            candleNew.volume = 0;

            //check all the candles so we can create our new one
            for (int index = 0; index < historyTmp.size(); index++) {

                //periods have to be in range
                if (historyTmp.get(index).time <= timePrevious)
                    continue;
                if (historyTmp.get(index).time > candleNew.time)
                    continue;

                //if this is the very next candle we have our open
                if (candleNew.open == 0 || historyTmp.get(index).time == timePrevious + candle.dependency.duration)
                    candleNew.open = historyTmp.get(index).open;

                //if this is the end, we have our close
                if (candleNew.close == 0 || historyTmp.get(index).time == candleNew.time)
                    candleNew.close = historyTmp.get(index).close;

                //we have a new low
                if (candleNew.low == 0 || historyTmp.get(index).low < candleNew.low)
                    candleNew.low = historyTmp.get(index).low;

                //we have a new high
                if (candleNew.high == 0 || historyTmp.get(index).high > candleNew.high)
                    candleNew.high = historyTmp.get(index).high;

                //add to the total volume
                candleNew.volume += historyTmp.get(index).volume;

                //remove the candle that we used
                historyTmp.remove(index);

                //subtract 1
                index--;
            }

            //add our new custom candle to the list
            history.add(candleNew);
        }

        if (DEBUG) {

            displayMessage("Custom Candles");

            //print all our periods
            for (int index = 0; index < history.size(); index++) {
                Period p = history.get(index);
                displayMessage("time: " + p.time + ", open $" + p.open + ", close $" + p.close + ", low $" + p.low + ", high $" + p.high + ", volume: " + p.volume);
            }

            displayMessage("Queued Candles");

            for (int index = 0; index < historyTmp.size(); index++) {
                Period p = historyTmp.get(index);
                displayMessage("time: " + p.time + ", open $" + p.open + ", close $" + p.close + ", low $" + p.low + ", high $" + p.high + ", volume: " + p.volume);
            }
        }
    }

    public static void updateHistory(List<Period> history, double[][] data) {

        //get the greatest time
        long timeMax = getTimeMax(history);

        //parse each period from the data
        for (int row = data.length - 1; row >= 0; row--) {

            //if this is not new history we want to verify the existing history
            if (timeMax > data[row][PERIOD_INDEX_TIME]) {
                verifyHistory(history, data[row]);
            } else {
                addHistory(history, data[row]);
            }
        }
    }

    private static void verifyHistory(List<Period> history, double[] data) {

        long time = (long)data[PERIOD_INDEX_TIME];

        for (int index = 0; index < history.size(); index++) {

            Period period = history.get(index);

            //if the time exists update the existing data
            if (period.time == time) {

                period.low = data[PERIOD_INDEX_LOW];
                period.high = data[PERIOD_INDEX_HIGH];
                period.open = data[PERIOD_INDEX_OPEN];
                period.close = data[PERIOD_INDEX_CLOSE];
                period.volume = data[PERIOD_INDEX_VOLUME];
            }
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

    /**
     * Merge the array list of periods together based on the provided candle
     * @param history List of candles that we want to merge
     * @param candle The custom candle we want to create
     */
    public static void merge(List<Period> history, Candle candle) {

        //our custom candle
        Period tmp = null;

        //create tmp list for custom candles
        List<Period> custom = new ArrayList<>();

        //merge every period available
        for (int index = history.size() - 1; index >= 0; index--) {

            //get the current period
            Period period = history.get(index);

            //if the period has not been created
            if (tmp == null) {

                //create a new candle
                tmp = new Period();

                //store our initial values
                tmp.time = period.time;
                tmp.open = period.open;
                tmp.close = period.close;
                tmp.low = period.low;
                tmp.high = period.high;
                tmp.volume = period.volume;

            } else {

                //let's see how we can update our existing candle
                if (period.low < tmp.low)
                    tmp.low = period.low;
                if (period.high > tmp.high)
                    tmp.high = period.high;

                //add to the volume
                tmp.volume += period.volume;

                //how many times does the dependency candle fit inside the candle
                int factor = (int)(candle.duration / candle.dependency.duration);

                //check if the candle ended
                if (period.time <= tmp.time - (candle.dependency.duration * (factor - 1))) {

                    //if the candle ended this is our open $ since we are going backwards
                    tmp.open = period.open;

                    //add this period to the first item
                    custom.add(0, tmp);

                    //display message if we are missing a candle
                    if (period.time < tmp.time - candle.duration)
                        displayMessage("Candle missing: " + period.time + " < " + tmp.time + " - " +  candle.duration);

                    //set the period back to null
                    tmp = null;

                } else if (period.time < tmp.time - candle.duration) {

                    //throw exception if we are missing candle(s)
                    //throw new RuntimeException("Candle missing: " + period.time + " < " + tmp.time + " - " +  candle.duration);
                }
            }
        }

        //remove obsolete periods
        history.clear();

        //add our custom periods to the history list
        for (int index = 0; index < custom.size(); index++) {
            history.add(custom.get(index));
        }

        //just make sure the periods are in order
        sortHistory(history);
    }
}