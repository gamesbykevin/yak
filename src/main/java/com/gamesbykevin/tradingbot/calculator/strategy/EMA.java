package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.Period.Fields;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;

import java.util.ArrayList;
import java.util.List;

import static com.gamesbykevin.tradingbot.calculator.CalculatorHelper.hasCrossover;
import static com.gamesbykevin.tradingbot.calculator.strategy.SMA.calculateSMA;

/**
 * Exponential moving average
 */
public class EMA extends Strategy {

    //list of ema values for our long period
    private List<Double> emaLong;

    //list of ema values for our short period
    private List<Double> emaShort;

    //list of configurable values
    private static final int PERIODS_EMA_LONG = 26;
    private static final int PERIODS_EMA_SHORT = 12;

    private final int periodsLong, periodsShort;

    public EMA() {
        this(PERIODS_EMA_LONG, PERIODS_EMA_SHORT);
    }

    public EMA(int periodsLong, int periodsShort) {

        //call parent
        super();

        //create our lists
        this.emaLong = new ArrayList<>();
        this.emaShort = new ArrayList<>();

        this.periodsLong = periodsLong;
        this.periodsShort = periodsShort;
    }

    public List<Double> getEmaShort() {
        return this.emaShort;
    }

    public List<Double> getEmaLong() {
        return this.emaLong;
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        double prevEmaS = getRecent(getEmaShort(), 2);
        double prevEmaL = getRecent(getEmaLong(), 2);
        double currEmaS = getRecent(getEmaShort());
        double currEmaL = getRecent(getEmaLong());

        //if the short ema crossed above the long ema we will buy
        if (prevEmaS < prevEmaL && currEmaS > currEmaL)
            agent.setBuy(true);

        //display data
        displayData(agent, agent.hasBuy());
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        double emaShort = getRecent(getEmaShort());
        double emaLong = getRecent(getEmaLong());
        double close =  getRecent(history, Fields.Close);

        //if the current candle is below both short and long, let's sell
        if (close < emaShort && close < emaLong)
            agent.setReasonSell(ReasonSell.Reason_Strategy);

        //display data
        displayData(agent, agent.getReasonSell() != null);
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display the recent ema values which we use as a signal
        display(agent, "EMA Short :", getEmaShort(), write);
        display(agent, "EMA Long  :", getEmaLong(), write);
    }

    @Override
    public void calculate(List<Period> history) {

        //calculate ema for short and long periods
        calculateEMA(history, getEmaShort(), periodsShort);
        calculateEMA(history, getEmaLong(), periodsLong);
    }

    private static final double calculateEMA(List<Period> history, int current, int periods, double emaPrevious) {

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
            final double sma = calculateSMA(history, current + 1, periods, Fields.Close);

            //use sma to help calculate the first ema value
            ema = ((currentPrice - sma) * multiplier) + sma;
        }

        //return our result
        return ema;
    }

    protected static void calculateEMA(List<Period> history, List<Double> emaList, int periods) {

        //clear our list
        emaList.clear();

        //for an accurate ema we want to calculate as many data points as possible
        for (int i = 0; i < history.size(); i++) {

            //skip if we can't go back far enough
            if (i < periods)
                continue;

            //either the ema will be 0 or we get the most recent
            final double previousEma = (emaList.isEmpty()) ? 0 : emaList.get(emaList.size() - 1);

            //calculate the ema for the current period
            final double ema = calculateEMA(history, i, periods, previousEma);

            //add it to the list
            emaList.add(ema);
        }
    }

    /**
     * Calculate ema and populate the provided emaList
     * @param populate Our result ema list that needs calculations
     * @param data The list of values we will use to do the calculations
     * @param periods The range of periods to make each calculation
     */
    protected static void calculateEmaList(List<Double> populate, List<Double> data, int periods) {

        //clear list
        populate.clear();

        //we add the sum to get the sma (simple moving average)
        double sum = 0;

        //calculate sma first
        for (int i = 0; i < periods; i++) {
            sum += data.get(i);
        }

        //we now have the sma as a start
        final double sma = sum / (float)periods;

        //here is our multiplier
        final double multiplier = ((float)2 / ((float)periods + 1.0f));

        //calculate our first ema
        final double ema = ((data.get(periods - 1) - sma) * multiplier) + sma;

        //add the ema value to our list
        populate.add(ema);

        //now let's calculate the remaining periods for ema
        for (int i = periods; i < data.size(); i++) {

            //get our previous ema
            final double previousEma = populate.get(populate.size() - 1);

            //get our close value
            final double close = data.get(i);

            //calculate our new ema
            final double newEma = ((close - previousEma) * multiplier) + previousEma;

            //add our new ema value to the list
            populate.add(newEma);
        }
    }
}