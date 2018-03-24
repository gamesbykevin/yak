package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonBuy;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;
import org.hibernate.validator.internal.constraintvalidators.bv.past.PastValidatorForReadableInstant;

import java.util.ArrayList;
import java.util.List;

import static com.gamesbykevin.tradingbot.calculator.CalculatorHelper.hasCrossover;

public class EMA extends Strategy {

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

    //how many periods
    private final int periodsLong, periodsShort;

    public EMA(int periodsLong, int periodsShort) {

        //call parent with default value
        super(0);

        //if long is less than short throw exception
        if (periodsLong <= periodsShort)
            throw new RuntimeException("The long periods are less than the short. L=" + periodsLong + ", S=" + periodsShort);

        //store our periods
        this.periodsLong = periodsLong;
        this.periodsShort = periodsShort;

        //create our lists
        this.emaLong = new ArrayList<>();
        this.emaShort = new ArrayList<>();
    }

    public EMA() {
        this(PERIODS_EMA_LONG, PERIODS_EMA_SHORT);
    }

    public List<Double> getEmaShort() {
        return this.emaShort;
    }

    public List<Double> getEmaLong() {
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
            agent.setReasonSell(ReasonSell.Reason_1);

        //if no reason to sell yet, check if the price drops below the ema values
        if (agent.getReasonSell() == null) {

            //get the current ema long and short values
            double emaLong = getRecent(getEmaLong());
            double emaShort = getRecent(getEmaShort());

            //get the low of the most recent period
            double recentLow = history.get(history.size() - 1).low;

            //if the recent low price is less than both the long/short ema values, we need to exit our trade
            if (recentLow < emaLong && recentLow < emaShort)
                agent.setReasonSell(ReasonSell.Reason_3);

        }

        //display data
        displayData(agent, agent.getReasonSell() != null);
    }

    @Override
    protected void displayData(Agent agent, boolean write) {

        //display the recent ema values which we use as a signal
        display(agent, "EMA Short: ", getEmaShort(), (periodsShort / 2), agent.getReasonBuy() != null);
        display(agent, "EMA Long: ", getEmaLong(), (periodsShort / 2),agent.getReasonBuy() != null);
    }

    @Override
    public void calculate(List<Period> history) {

        //calculate ema for short and long periods
        calculateEMA(history, getEmaShort(), periodsShort);
        calculateEMA(history, getEmaLong(), periodsLong);
    }

    /**
     * Calculate the SMA (simple moving average)
     * @param currentPeriod The desired period of the SMA we want
     * @param periods The number of periods to check
     * @return The average of the sum of closing prices within the specified period
     */
    protected static double calculateSMA(List<Period> history, int currentPeriod, int periods) {

        //the total sum
        double sum = 0;

        //check every period
        for (int i = currentPeriod - periods; i < currentPeriod; i++) {

            //add to the total sum
            sum += history.get(i).close;
        }

        //return the average of the sum
        return (sum / (double)periods);
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