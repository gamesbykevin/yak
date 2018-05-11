package com.gamesbykevin.tradingbot.calculator.indicator.momentun;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.indicator.Indicator;

import java.util.ArrayList;
import java.util.List;

import static com.gamesbykevin.tradingbot.calculator.indicator.trend.SMA.calculateSMA;

public class RSI extends Indicator {

    //keep a historical list of the rsi so we can check for divergence
    private List<Double> rsiVal;

    //list of configurable values
    private static final int PERIODS = 14;

    //the number of periods when calculating rsi
    private final int periods;

    public RSI() {
        this(PERIODS);
    }

    public RSI(int periods) {

        //create new list(s)
        this.rsiVal = new ArrayList<>();
        this.periods = periods;
    }

    public List<Double> getRsiVal() {
        return this.rsiVal;
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display the volume
        display(agent, "RSI: ", getRsiVal(), write);
    }

    @Override
    public void calculate(List<Period> history, int newPeriods) {

        //where do we start
        int startIndex = (getRsiVal().isEmpty() ? 0 : history.size() - newPeriods);

        //calculate as many periods as we need
        for (int i = startIndex; i < history.size(); i++) {

            //skip if we don't have enough data
            if (i <= periods)
                continue;

            //find the start and end periods
            final int start = i - periods;
            final int end = i;

            //calculate the rsi for the given periods
            final double tmpRsi = calculateRsi(history, start, end);

            //add the rsi value to our list
            getRsiVal().add(tmpRsi);
        }
    }

    /**
     * Calcuate the rsi value for the specified range
     * @param history Our historical data
     * @param startIndex Beginning period
     * @param endIndex Ending period
     * @return The rsi value
     */
    private double calculateRsi(List<Period> history, int startIndex, int endIndex) {

        //track total gains and losses
        float gain = 0, loss = 0;
        float gainCurrent = 0, lossCurrent = 0;

        //count the periods
        final int size = (endIndex - startIndex) - 1;

        //go through the periods to calculate rsi
        for (int i = startIndex; i < endIndex; i++) {

            //get the close prices to compare
            double prev = history.get(i - 1).close;
            double next = history.get(i).close;

            if (next > prev) {

                //here we have a gain
                gain += (next - prev);

            } else {

                //here we have a loss
                loss += (prev - next);
            }
        }

        //calculate the average gain and loss
        float avgGain = (gain / (float)size);
        float avgLoss = (loss / (float)size);

        //get the previous price in our list so we can compare to the current price
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