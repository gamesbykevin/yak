package com.gamesbykevin.tradingbot.calculator.indicator.trend;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.Period.Fields;
import com.gamesbykevin.tradingbot.calculator.indicator.Indicator;

import java.util.ArrayList;
import java.util.List;

import static com.gamesbykevin.tradingbot.calculator.indicator.trend.SMA.calculateSMA;

/**
 * Exponential moving average
 */
public class EMA extends Indicator {

    //list of ema values
    private List<Double> emaList;

    //list of configurable values
    public static final int PERIODS = 12;

    //what is the default fields we will use to calculate
    private static final List<Fields> DEFAULT_FIELDS = new ArrayList<>();

    public EMA() {
        this(PERIODS);
    }

    public EMA(int periods) {

        //call parent
        super(Indicator.Key.EMA, periods);

        //create our lists
        this.emaList = new ArrayList<>();
    }

    public List<Double> getEma() {
        return this.emaList;
    }

    private static List<Fields> getDefaultFields() {

        //we only need 1 element if the list is empty
        if (DEFAULT_FIELDS.isEmpty())
            DEFAULT_FIELDS.add(Fields.Close);

        return DEFAULT_FIELDS;
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display the recent ema values which we use as a signal
        display(agent, "EMA (" + getPeriods() + ") :", getEma(), write);
    }

    @Override
    public void calculate(List<Period> history, int newPeriods) {

        //calculate ema for short and long periods
        calculateEMA(history, getEma(), newPeriods, getPeriods());
    }

    private static double calculateEMA(List<Period> history, int current, int periods, double emaPrevious) {

        //what is our multiplier
        final float multiplier = ((float)2 / ((float)periods + 1.0f));

        //this close price is the current price
        final double currentPrice = history.get(current).close;

        //calculate our ema
        final double ema;

        if (emaPrevious != 0) {

            ema = ((currentPrice - emaPrevious) * multiplier) + emaPrevious;

        } else {

            //calculate simple moving average since there is no previous ema
            final double sma = calculateSMA(history, current + 1, periods, getDefaultFields());

            //use sma to help calculate the first ema value
            ema = ((currentPrice - sma) * multiplier) + sma;
        }

        //return our result
        return ema;
    }

    public static void calculateEMA(List<Period> history, List<Double> populate, int newPeriods, int periods) {

        //where do we start
        int start = populate.isEmpty() ? 0 : history.size() - newPeriods;

        //for an accurate ema we want to calculate as many data points as possible
        for (int i = start; i < history.size(); i++) {

            //skip if we can't go back far enough
            if (i < periods)
                continue;

            //either the ema will be 0 or we get the most recent
            final double previousEma = (populate.isEmpty()) ? 0 : populate.get(populate.size() - 1);

            //calculate the ema for the current period
            final double ema = calculateEMA(history, i, periods, previousEma);

            //add it to the list
            populate.add(ema);
        }
    }

    /**
     * Calculate ema and populate the provided emaList
     * @param data The list of values we will use to do the calculations
     * @param newPeriods The new number of periods we need to calculate
     */
    public void calculateEma(List<Double> data, int newPeriods) {

        //here is our multiplier
        final double multiplier = ((float) 2 / ((float)getPeriods() + 1.0f));

        //where do we start?
        int start = getEma().isEmpty() ? getPeriods() : data.size() - newPeriods;

        //if the list is empty calculate our first ema value
        if (getEma().isEmpty()) {

            //we add the sum to get the sma (simple moving average)
            double sum = 0;

            //calculate sma first
            for (int i = 0; i < getPeriods(); i++) {
                sum += data.get(i);
            }

            //we now have the sma as a start
            final double sma = sum / (float)getPeriods();

            //calculate our first ema
            final double ema = ((data.get(getPeriods() - 1) - sma) * multiplier) + sma;

            //add the ema value to our list
            getEma().add(ema);
        }

        //now let's calculate the remaining periods for ema
        for (int i = start; i < data.size(); i++) {

            //get our previous ema
            final double previousEma = getEma().get(getEma().size() - 1);

            //get our close value
            final double close = data.get(i);

            //calculate our new ema
            final double newEma = ((close - previousEma) * multiplier) + previousEma;

            //add our new ema value to the list
            getEma().add(newEma);
        }
    }

    @Override
    public void cleanup() {
        cleanup(getEma());
    }
}