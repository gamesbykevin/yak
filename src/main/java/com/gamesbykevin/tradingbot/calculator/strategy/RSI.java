package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonBuy;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;

import java.util.ArrayList;
import java.util.List;

import static com.gamesbykevin.tradingbot.agent.AgentManager.displayMessage;
import static com.gamesbykevin.tradingbot.calculator.CalculatorHelper.hasDivergence;

public class RSI extends Strategy {

    //keep a historical list of the rsi so we can check for divergence
    private List<Double> rsi;

    /**
     * How many periods to calculate rsi
     */
    public static int PERIODS_RSI;

    /**
     * The support line meaning the stock is oversold
     */
    public static float SUPPORT_LINE;

    /**
     * The resistance line meaning the stock is overbought
     */
    public static float RESISTANCE_LINE;

    public RSI() {
        this(PERIODS_RSI);
    }

    public RSI(int periods) {

        //call parent
        super(periods);

        //create new list(s)
        this.rsi = new ArrayList<>();
    }

    public List<Double> getRsi() {
        return this.rsi;
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //get the most recent rsi value
        double rsi = getRsi().get(getRsi().size() - 1);

        //if we are at or below the support line, let's check if we are in a good place to buy
        if (rsi <= SUPPORT_LINE) {

            //if there is a bullish divergence let's buy
            if (hasDivergence(history, getPeriods(), true, getRsi()))
                agent.setReasonBuy(ReasonBuy.Reason_3);
        }

        //display our data
        displayData(agent, agent.getReasonBuy() != null);
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //get the most recent rsi value
        double rsi = getRsi().get(getRsi().size() - 1);

        //let's see if we are above resistance line before selling
        if (rsi >= RESISTANCE_LINE) {

            //if there is a bearish divergence let's sell
            if (hasDivergence(history, getPeriods(), false, getRsi()))
                agent.setReasonSell(ReasonSell.Reason_4);
        }

        //display our data
        displayData(agent, agent.getReasonSell() != null);
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display the volume
        display(agent, "RSI: ", getRsi(), getPeriods(), write);
    }

    @Override
    public void calculate(List<Period> history) {
        calculateRsi(history, getRsi(), getPeriods());
    }

    protected static void calculateRsi(List<Period> history, List<Double> rsi, int periods) {

        //clear our historical rsi list
        rsi.clear();

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
            rsi.add(tmpRsi);
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
        float avgGain = (gain / size);
        float avgLoss = (loss / size);

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
            ((avgGain * size) + gainCurrent) / (size + 1)
        ) / (
            ((avgLoss * size) + lossCurrent) / (size + 1)
        );

        //calculate our rsi value
        final float rsi = 100 - (100 / (1 + smotheredRS));

        //return our rsi value
        return rsi;
    }
}