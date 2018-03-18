package com.gamesbykevin.tradingbot.calculator;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonBuy;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;

import java.util.ArrayList;
import java.util.List;

import static com.gamesbykevin.tradingbot.calculator.CalculatorHelper.hasCrossover;

public class EMA extends Indicator {

    //list of ema values for our long period
    private List<Double> emaLong;

    //list of ema values for our short period
    private List<Double> emaShort;

    /**
     * How many periods to calculate long ema
     */
    public static int PERIODS_EMA_LONG;

    /**
     * How many periods to calculate short ema
     */
    public static int PERIODS_EMA_SHORT;

    public EMA() {
        super(0);

        this.emaLong = new ArrayList<>();
        this.emaShort = new ArrayList<>();
    }

    private List<Double> getEmaShort() {
        return this.emaShort;
    }

    private List<Double> getEmaLong() {
        return this.emaLong;
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //if we have a bullish crossover, we expect price to go up
        if (hasCrossover(true, getEmaShort(), getEmaLong()))
            agent.setReasonBuy(ReasonBuy.Reason_1);

        //display data
        displayData(agent, agent.getReasonBuy() != null);
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //if we have a bearish crossover, we expect price to go down
        if (hasCrossover(false, getEmaShort(), getEmaLong()))
            agent.setReasonSell(ReasonSell.Reason_3);

        //display data
        displayData(agent, agent.getReasonSell() != null);
    }

    @Override
    protected void displayData(Agent agent, boolean write) {

        //display the recent ema values which we use as a signal
        display(agent, "EMA Short: ", getEmaShort(), PERIODS_EMA_SHORT, agent.getReasonBuy() != null);
        display(agent, "EMA Long: ", getEmaLong(), PERIODS_EMA_SHORT,agent.getReasonBuy() != null);
    }

    @Override
    public void calculate(List<Period> history) {

        //calculate ema for short and long periods
        calculateEMA(history, getEmaShort(), PERIODS_EMA_SHORT);
        calculateEMA(history, getEmaLong(), PERIODS_EMA_LONG);
    }

    /**
     * Calculate the SMA (simple moving average)
     * @param currentPeriod The desired period of the SMA we want
     * @param periods The number of periods to check
     * @return The average of the sum of closing prices within the specified period
     */
    private static double calculateSMA(List<Period> history, int currentPeriod, int periods) {

        //the total sum
        double sum = 0;

        //number of prices we add together
        int count = 0;

        //check every period
        for (int i = currentPeriod - periods; i < currentPeriod; i++) {

            //add to the total sum
            sum += history.get(i).close;

            //keep track of how many we add
            count++;
        }

        //return the average of the sum
        return (sum / (double)count);
    }

    private static double calculateEMA(List<Period> history, int current, int periods, double emaPrevious) {

        //what is our multiplier
        final float multiplier = ((float)2 / ((float)periods + 1));

        //this close price is the current price
        final double currentPrice = history.get(current).close;

        //calculate our ema
        final double ema;

        if (emaPrevious != 0) {

            ema = ((currentPrice - emaPrevious) * multiplier) + emaPrevious;

        } else {

            //calculate simple moving average since there is no previous ema
            final double sma = calculateSMA(history, current - 1, periods);

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
            if (i <= periods)
                continue;

            //either the ema will be 0 or we get the most recent
            final double previousEma = (emaList.isEmpty()) ? 0 : emaList.get(emaList.size() - 1);

            //calculate the ema for the current period
            final double ema = calculateEMA(history, i, periods, previousEma);

            //add it to the list
            emaList.add(ema);
        }
    }
}