package com.gamesbykevin.tradingbot.calculator;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonBuy;

import java.util.ArrayList;
import java.util.List;

import static com.gamesbykevin.tradingbot.calculator.CalculatorHelper.hasCrossover;
import static com.gamesbykevin.tradingbot.calculator.EMA.PERIODS_EMA_LONG;
import static com.gamesbykevin.tradingbot.calculator.EMA.PERIODS_EMA_SHORT;

public class MACD extends Indicator {

    //macdLine values
    private List<Double> macdLine;

    //list of ema values from the macd line
    private List<Double> signalLine;

    //list of ema values for our long period
    private List<Double> emaLong;

    //list of ema values for our short period
    private List<Double> emaShort;

    /**
     * How many periods do we calculate ema from macd line
     */
    public static int PERIODS_MACD;

    public MACD() {

        //call parent
        super(PERIODS_MACD);

        this.macdLine = new ArrayList<>();
        this.signalLine = new ArrayList<>();
        this.emaLong = new ArrayList<>();
        this.emaShort = new ArrayList<>();
    }

    private List<Double> getEmaShort() {
        return this.emaShort;
    }

    private List<Double> getEmaLong() {
        return this.emaLong;
    }

    private List<Double> getMacdLine() {
        return this.macdLine;
    }

    private List<Double> getSignalLine() {
        return this.signalLine;
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        if (hasCrossover(true, getMacdLine(), getSignalLine()))
            agent.setReasonBuy(ReasonBuy.Reason_2);

        //display our data
        displayData(agent, agent.getReasonBuy() != null);
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //if we have a bearish crossover, we expect price to go down
        if (hasCrossover(false, getMacdLine(), getSignalLine()))
            agent.setReasonSell(ReasonSell.Reason_4);

        //if no reason to sell yet, check if the price drops below the ema values
        if (agent.getReasonSell() == null) {

            //get the current ema long and short values
            double emaLong = getEmaLong().get(getEmaLong().size() - 1);
            double emaShort = getEmaShort().get(getEmaShort().size() - 1);

            //get the low of the most recent period
            double recentLow = history.get(history.size() - 1).low;

            //if the recent low price is less than both the long/short ema values, we need to exit our trade
            if (recentLow < emaLong && recentLow < emaShort)
                agent.setReasonSell(ReasonSell.Reason_5);

        }

        //display our data
        displayData(agent, agent.getReasonSell() != null);
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display the recent MACD values which we use as a signal
        display(agent, "MACD Line: ", getMacdLine(), getPeriods(), write);
        display(agent, "Signal Line: ", getSignalLine(), getPeriods(), write);

        //display values
        display(agent, "EMA Short", getEmaShort(), PERIODS_EMA_SHORT, agent.getReasonSell() != null);
        display(agent, "EMA Long", getEmaLong(), PERIODS_EMA_SHORT, agent.getReasonSell() != null);
    }

    @Override
    public void calculate(List<Period> history) {

        //calculate our short and long ema values first
        EMA.calculateEMA(history, getEmaShort(), PERIODS_EMA_SHORT);
        EMA.calculateEMA(history, getEmaLong(), PERIODS_EMA_LONG);

        //now we can calculate our macd line
        calculateMacdLine();

        //then we can calculate our signal line
        calculateSignalLine();
    }

    private void calculateMacdLine() {

        //clear the list
        getMacdLine().clear();

        //we need to start at the right index
        int difference = getEmaShort().size() - getEmaLong().size();

        //calculate for every value possible
        for (int i = 0; i < getEmaLong().size(); i++) {

            //the macd line is the 12 day ema - 26 day ema
            getMacdLine().add(getEmaShort().get(difference + i) - getEmaLong().get(i));
        }
    }

    private void calculateSignalLine() {

        //clear list
        getSignalLine().clear();

        //we add the sum to get the sma (simple moving average)
        double sum = 0;

        //calculate sma first
        for (int i = 0; i < getPeriods(); i++) {
            sum += getMacdLine().get(i);
        }

        //we now have the sma as a start
        final double sma = sum / (float)getPeriods();

        //here is our multiplier
        final double multiplier = ((float)2 / ((float)getPeriods() + 1));

        //calculate our first ema
        final double ema = ((getMacdLine().get(getPeriods() - 1) - sma) * multiplier) + sma;

        //add the 9 day ema to our list
        getSignalLine().add(ema);

        //now let's calculate the remaining periods for ema
        for (int i = getPeriods(); i < getMacdLine().size(); i++) {

            //get our previous ema
            final double previousEma = getSignalLine().get(getSignalLine().size() - 1);

            //get our close value
            final double close = getMacdLine().get(i);

            //calculate our new ema
            final double newEma = ((close - previousEma) * multiplier) + previousEma;

            //add our new ema value to the list
            getSignalLine().add(newEma);
        }
    }
}