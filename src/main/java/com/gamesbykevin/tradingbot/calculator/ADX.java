package com.gamesbykevin.tradingbot.calculator;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper;

import java.util.ArrayList;
import java.util.List;

import static com.gamesbykevin.tradingbot.agent.AgentManager.displayMessage;
import static com.gamesbykevin.tradingbot.calculator.CalculatorHelper.hasCrossover;

public class ADX extends Indicator {

    //the average directional index
    private List<Double> adx;

    //our +- indicators to calculate adx and signal trades
    private List<Double> dmPlusIndicator;
    private List<Double> dmMinusIndicator;

    /**
     * How many periods do we calculate the average directional index
     */
    public static int PERIODS_ADX;

    /**
     * The minimum value to determine there is a price trend
     */
    public static double TREND_ADX;

    public ADX() {

        //call parent
        super(PERIODS_ADX);

        this.adx = new ArrayList<>();
        this.dmPlusIndicator = new ArrayList<>();
        this.dmMinusIndicator = new ArrayList<>();
    }

    private List<Double> getAdx() {
        return this.adx;
    }

    private List<Double> getDmPlusIndicator() {
        return this.dmPlusIndicator;
    }

    private List<Double> getDmMinusIndicator() {
        return this.dmMinusIndicator;
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //get the most recent adx value
        double adx = getAdx().get(getAdx().size() - 1);

        //check the most recent adx to see if there is a trend in price
        if (adx >= TREND_ADX) {

            //if dm plus crosses above dm minus, that is our signal to buy
            if (hasCrossover(true, getDmPlusIndicator(), getDmMinusIndicator()))
                agent.setReasonBuy(TransactionHelper.ReasonBuy.Reason_6);
        }

        //display data
        displayData(agent, agent.getReasonBuy() != null);
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //get the most recent adx value
        double adx = getAdx().get(getAdx().size() - 1);

        if (adx >= TREND_ADX) {

            //if the minus has crossed below the plus that is our signal to sell
            if (hasCrossover(false, getDmPlusIndicator(), getDmMinusIndicator()))
                agent.setReasonSell(TransactionHelper.ReasonSell.Reason_9);
        }

        //display data
        displayData(agent, agent.getReasonSell() != null);
    }

    @Override
    protected void displayData(Agent agent, boolean write) {

        //display the recent values which we use as a signal
        display(agent, "+DI: ", getDmPlusIndicator(), getPeriods(), write);
        display(agent, "-DI: ", getDmMinusIndicator(), getPeriods(), write);
        displayMessage(agent, "ADX: " + getAdx().get(getAdx().size() - 1), write);
    }

    @Override
    public void calculate(List<Period> history) {

        //clear the list(s) before we calculate
        adx.clear();
        dmPlusIndicator.clear();
        dmMinusIndicator.clear();

        //our temp lists
        List<Double> tmpDmPlus = new ArrayList<>();
        List<Double> tmpDmMinus = new ArrayList<>();
        List<Double> tmpTrueRange = new ArrayList<>();

        //calculate for the entire history that we have
        for (int i = 0; i < history.size(); i++) {

            //we can't check the previous period here
            if (i <= 0)
                continue;

            //get the current and previous periods
            Period previous = history.get(i - 1);
            Period current = history.get(i);

            //calculate the high difference
            double highDiff = current.high - previous.high;

            //calculate the low difference
            double lowDiff = current.low - previous.low;

            //which values do we set
            if (highDiff > lowDiff) {

                //if less than 0, zero will be assigned
                if (highDiff < 0)
                    highDiff = 0d;

                tmpDmPlus.add(highDiff);
                tmpDmMinus.add(0d);

            } else {

                //if less than 0, zero will be assigned
                if (lowDiff < 0)
                    lowDiff = 0d;

                tmpDmPlus.add(0d);
                tmpDmMinus.add(lowDiff);
            }

            //current high minus current low
            double method1 = current.high - current.low;

            //if we have the previous period, current high minus previous period close (absolute value)
            double method2 = (previous == null) ? 0d : Math.abs(current.high - previous.close);

            //if we have the previous period, current low minus previous period close (absolute value)
            double method3 = (previous == null) ? 0d : Math.abs(current.low - previous.close);

            //the true range will be the greatest value of the 3 methods
            if (method1 >= method2 && method1 >= method3) {
                tmpTrueRange.add(method1);
            } else if (method2 >= method1 && method2 >= method3) {
                tmpTrueRange.add(method2);
            } else if (method3 >= method1 && method3 >= method2) {
                tmpTrueRange.add(method3);
            } else {
                tmpTrueRange.add(method1);
            }
        }

        List<Double> dmPlus = new ArrayList<>();
        List<Double> dmMinus = new ArrayList<>();
        List<Double> trueRange = new ArrayList<>();

        //smooth the values
        smooth(tmpDmMinus, dmMinus);
        smooth(tmpDmPlus, dmPlus);
        smooth(tmpTrueRange, trueRange);

        for (int i = 0; i < dmPlus.size(); i++) {

            //calculate the +- indicators
            double newPlus = (dmPlus.get(i) / trueRange.get(i)) * 100.0d;
            double newMinus = (dmMinus.get(i) / trueRange.get(i)) * 100.0d;

            //add it to the list
            dmPlusIndicator.add(newPlus);
            dmMinusIndicator.add(newMinus);
        }

        //directional movement index
        List<Double> dmIndex = new ArrayList<>();

        //calculate each dm index
        for (int i = 0; i < dmPlus.size(); i++) {
            double result1 = Math.abs(dmPlusIndicator.get(i) - dmMinusIndicator.get(i));
            double result2 = dmPlusIndicator.get(i) + dmMinusIndicator.get(i);
            dmIndex.add((result1 / result2) * 100.0d);
        }

        double sum = 0;

        //get the average for the first value
        for (int i = 0; i < getPeriods(); i++) {
            sum += dmIndex.get(i);
        }

        //our first value is the average
        adx.add(sum / (double)getPeriods());

        //calculate the remaining average directional index values
        for (int i = getPeriods(); i < dmIndex.size(); i++) {

            //get the most recent adx
            double previousAdx = adx.get(adx.size() - 1);

            //calculate the new adx value
            double newAdx = ((previousAdx * (double)(getPeriods() - 1)) + dmIndex.get(i)) / (double)getPeriods();

            //add new value to our list
            adx.add(newAdx);
        }
    }

    /**
     * Smooth out the values
     * @param tmp Our temp list of values
     * @param result Our final result of smoothed values
     */
    private void smooth(List<Double> tmp, List<Double> result) {

        double sum = 0;

        //add the sum of the first x periods to get the first value
        for (int i = 0; i < getPeriods(); i++) {
            sum += tmp.get(i);
        }

        //add first result to our list as a sum
        result.add(sum);

        //now lets smooth the values for the remaining
        for (int i = getPeriods(); i < tmp.size(); i++) {

            //calculate our current
            double currentSum = 0;

            //calculate the sum of the current period
            for (int x = i - getPeriods() + 1; x <= i; x++) {
                currentSum += tmp.get(x);
            }

            //get the previous value
            double previousValue = result.get(result.size() - 1);

            //calculate the new smoothed value
            double newValue = previousValue - (previousValue / (double)getPeriods()) + currentSum;

            //add the smoothed value to our list
            result.add(newValue);
        }
    }
}