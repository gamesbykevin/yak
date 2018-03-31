package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.Period.Fields;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;

import java.util.ArrayList;
import java.util.List;

import static com.gamesbykevin.tradingbot.calculator.strategy.SMA.calculateSMA;

public class RSI extends Strategy {

    //keep a historical list of the rsi so we can check for divergence
    private List<Double> rsiVal;

    //keep an average of the price
    private List<Double> smaPrice;

    //our list of variations
    protected static int[] LIST_PERIODS_SMA_PRICE = {50};
    protected static int[] LIST_PERIODS_RSI = {5};
    protected static float[] LIST_SUPPORT_LINE = {30.0f};
    protected static float[] LIST_RESISTANCE_LINE = {70.0f};

    //list of configurable values
    protected static int PERIODS_SMA_PRICE = 50;
    protected static int PERIODS_RSI = 5;
    protected static float SUPPORT_LINE = 30.0f;
    protected static float RESISTANCE_LINE = 70.0f;

    public RSI() {

        //call parent
        super();

        //create new list(s)
        this.rsiVal = new ArrayList<>();
        this.smaPrice = new ArrayList<>();
    }

    public List<Double> getRsiVal() {
        return this.rsiVal;
    }

    public List<Double> getSmaPrice() {
        return this.smaPrice;
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //first we need to make sure we are below the support line
        if (getRecent(getRsiVal()) < SUPPORT_LINE) {

            //now we need to check that price is in an overall uptrend
            if (getRecent(history, Fields.Close) > getRecent(getSmaPrice()))
                agent.setBuy(true);
        }

        //display our data
        displayData(agent, agent.hasBuy());
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //first we need to make sure we are above the resistance line
        if (getRecent(getRsiVal()) > RESISTANCE_LINE) {

            //now we need to check that price is in an overall downtrend
            if (getRecent(history, Fields.Close) < getRecent(getSmaPrice()))
                agent.setReasonSell(ReasonSell.Reason_Strategy);
        }

        //display our data
        displayData(agent, agent.getReasonSell() != null);
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display the volume
        display(agent, "RSI: ", getRsiVal(), write);
        display(agent, "SMA: ", getSmaPrice(), write);
    }

    @Override
    public void calculate(List<Period> history) {

        //calculate rsi values
        calculateRsi(history, getRsiVal(), PERIODS_RSI);

        //calculate sma of price
        calculateSMA(history, getSmaPrice(), PERIODS_SMA_PRICE, Fields.Close);
    }

    protected static void calculateRsi(List<Period> history, List<Double> populate, int periods) {

        //clear our historical rsi list
        populate.clear();

        //calculate as many periods as we can
        for (int i = 0; i < history.size(); i++) {

            //skip if we don't have enough data
            if (i <= periods)
                continue;

            //find the start and end periods
            final int start = i - periods;
            final int end = i;

            //calculate the rsi for the given periods
            final double tmpRsi = calculateRsi(history, start, end);

            //add the rsi value to our list
            populate.add(tmpRsi);
        }
    }

    /**
     * Calcuate the rsi value for the specified range
     * @param history Our historical data
     * @param startIndex Beginning period
     * @param endIndex Ending period
     * @return The rsi value
     */
    private static double calculateRsi(List<Period> history, int startIndex, int endIndex) {

        //track total gains and losses
        float gain = 0, loss = 0;
        float gainCurrent = 0, lossCurrent = 0;

        //count the periods
        final int size = (endIndex - startIndex) - 1;

        //go through the periods to calculate rsi
        for (int i = startIndex; i < endIndex; i++) {

            //get the close prices to compare
            double previous = history.get(i - 1).close;
            double next     = history.get(i).close;

            if (next > previous) {

                //here we have a gain
                gain += (next - previous);

            } else {

                //here we have a loss
                loss += (previous - next);
            }
        }

        //calculate the average gain and loss
        float avgGain = (gain / (float)size);
        float avgLoss = (loss / (float)size);

        //get the latest price in our list so we can compare to the current price
        final double recentPrice = history.get(endIndex - 1).close;

        //the recent period will be the current price
        final double currentPrice = history.get(endIndex).close;

        //check if the current price is a gain or loss
        if (currentPrice > recentPrice) {
            gainCurrent = (float)(currentPrice - recentPrice);
        } else {
            lossCurrent = (float)(recentPrice - currentPrice);
        }

        //smothered rsi including current gain loss
        float smotheredRS = (
            ((avgGain * size) + gainCurrent) / (float)(size + 1)
        ) / (
            ((avgLoss * size) + lossCurrent) / (float)(size + 1)
        );

        //calculate our rsi value
        final float rsi = 100f - (100f / (1f + smotheredRS));

        //return our rsi value
        return rsi;
    }
}