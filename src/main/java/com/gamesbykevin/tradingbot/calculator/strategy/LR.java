package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;

import java.util.List;

import static com.gamesbykevin.tradingbot.agent.AgentManagerHelper.displayMessage;

/**
 * Linear Regression
 */
public class LR extends Strategy {

    /**
     * How many periods are we calculating slope?
     */
    private static final int PERIODS = 10;

    //how many periods do we calculate slope
    private final int periods;

    //the slope of our regression line
    private float slope;

    //the y-intercept
    private double yIntercept;

    //the largest distance from the middle regression line
    private double difference;

    public LR() {
        this(PERIODS);
    }

    public LR(int periods) {
        this.periods = periods;
    }

    public int getPeriods() {
        return this.periods;
    }

    public float getSlope() {
        return this.slope;
    }

    public double getYintercept() {
        return this.yIntercept;
    }

    public double getDifference() {
        return this.difference;
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //find our lower regression line
        final double lower = calculateY(getPeriods()) - getDifference();

        //the slope is in an uptrend and the price touched the lower regression line
        if (getSlope() > 0 && history.get(history.size() - 1).close <= lower)
            agent.setBuy(true);

        //display our info
        displayMessage(agent, "Close $ " + history.get(history.size() - 1).close, agent.hasBuy());
        displayData(agent, agent.hasBuy());
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //find our upper regression line
        final double upper = calculateY(getPeriods()) + getDifference();

        //the slope is in a downtrend and the price touched the upper regression line
        if (getSlope() < 0 && history.get(history.size() - 1).close >= upper) {
            agent.setReasonSell(ReasonSell.Reason_Strategy);

            //adjust our hard stop price to protect our investment
            adjustHardStopPrice(agent, currentPrice);
        }

        //display our info
        displayMessage(agent, "Close $ " + history.get(history.size() - 1).close, agent.getReasonSell() != null);
        displayData(agent, agent.getReasonSell() != null);
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display info
        displayMessage(agent, "Slope $ " + calculateY(getPeriods()), write);
        displayMessage(agent, "Upper : " + (calculateY(getPeriods()) + getDifference()), write);
        displayMessage(agent, "Lower : " + (calculateY(getPeriods()) - getDifference()), write);
        displayMessage(agent, "Slope : " + getSlope(), write);
        displayMessage(agent, "yInt  : " + getYintercept(), write);
        displayMessage(agent, "Diff  : " + getDifference(), write);
    }

    @Override
    public void calculate(List<Period> history) {

        //the number of periods
        double n = getPeriods();

        //calculate the sum of x, the sum of y, the sum of x squared, the sum of y squared, the sum of x times y
        double sumX = 0;
        double sumY = 0;
        double sumX2 = 0;
        double sumY2 = 0;
        double sumXY = 0;

        //our start and end
        int start = history.size() - getPeriods();
        int end = history.size();

        //loop through our periods to calculate our data
        for (int index = start; index < end; index++) {

            //get our x,y
            double x = index - start;
            double y = history.get(index).close;

            //calculate our data
            sumX += x;
            sumY += y;
            sumX2 += (x * x);
            sumY2 += (y * y);
            sumXY += (x * y);
        }

        //now lets calculate our slope
        this.slope = (float)(((n * sumXY) - (sumX * sumY)) / ((n * sumX2) - (sumX * sumX)));

        //lets get the yIntercept
        this.yIntercept = ((sumY / n) - (getSlope() * (sumX / n)));

        //what is the largest difference?
        this.difference = 0;

        //now check the periods to identify the most of (high / low)
        for (int index = start; index < end; index++) {

            //what is the x coordinate
            float x = index - start;

            //what is the current y-coordinate
            double y = calculateY(x);

            //always check for the larger difference
            if (history.get(index).high - y > this.difference)
                this.difference = history.get(index).high - y;
            if (y - history.get(index).low > this.difference)
                this.difference = y - history.get(index).low;
        }
    }

    private double calculateY(float x) {
        return (getSlope() * x) + getYintercept();
    }
}