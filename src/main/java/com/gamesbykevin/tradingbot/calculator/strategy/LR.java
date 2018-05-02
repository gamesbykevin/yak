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

    //the slope
    private float slope;

    //the slope over a longer period
    private float slopeLong;

    //the price on the slope line from the most recent period
    private double slopePrice;

    //the distance from the regression line
    private double difference;

    public LR() {
        this(PERIODS);
    }

    public LR(int periods) {
        this.periods = periods;
    }

    public float getSlope() {
        return this.slope;
    }

    public float getSlopeLong() {
        return this.slopeLong;
    }

    public double getSlopePrice() {
        return this.slopePrice;
    }

    public double getDifference() {
        return this.difference;
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //find our lower regression line
        final double lower = getSlopePrice() - getDifference();

        //the slope is in an uptrend and the price touched the lower regression line
        if (getSlopeLong() > 0 && history.get(history.size() - 1).close <= lower)
            agent.setBuy(true);

        //display our info
        displayMessage(agent, "Close  $" + history.get(history.size() - 1).close, agent.hasBuy());
        displayData(agent, agent.hasBuy());
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //find our upper regression line
        final double upper = getSlopePrice() + getDifference();

        //the slope is in a downtrend and the price touched the upper regression line
        if (getSlopeLong() < 0 && history.get(history.size() - 1).close >= upper)
            agent.setReasonSell(ReasonSell.Reason_Strategy);

        //display our info
        displayMessage(agent, "Close  $" + history.get(history.size() - 1).close, agent.getReasonSell() != null);
        displayData(agent, agent.getReasonSell() != null);
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display info
        displayMessage(agent, "Slope  $" + getSlopePrice(), write);
        displayMessage(agent, "Slope  :" + getSlope(), write);
        displayMessage(agent, "Long SL:" + getSlopeLong(), write);
        displayMessage(agent, "Diff   : " + getDifference(), write);
        displayMessage(agent, "Upper  : " + (getSlopePrice() + getDifference()), write);
        displayMessage(agent, "Lower  : " + (getSlopePrice() - getDifference()), write);
    }

    @Override
    public void calculate(List<Period> history) {

        //check back a little ways
        int index1 = history.size() - (periods * 2) - 2;
        int index2 = history.size() - (periods * 1) - 2;

        //we need this data to get our slope
        double x1 = index1;
        double x2 = index2;
        double y1 = history.get(index1).close;
        double y2 = history.get(index2).close;

        //calculate slope
        this.slope = SL.getSlope(x1, x2, y1, y2);

        //calculate the slope for a longer period of time
        this.slopeLong = SL.getSlope(x1, history.size() - 1, y1, history.get(history.size() - 1).close);

        //what is the largest difference
        this.difference = 0;

        //now check the periods to identify the most of (high / low)
        for (int i = index1; i <= index2; i++) {

            //what is the x coordinate
            float x = index2 - i;
            float m = getSlope();
            double b = y1;

            //what is the current y-coordinate (aka $)
            double y = (m * x) + b;

            //always check for the larger difference
            if (history.get(i).high - y > this.difference)
                this.difference = history.get(i).high - y;
            if (y - history.get(i).low > this.difference)
                this.difference = y - history.get(i).low;
        }

        //how many periods since the slope started
        float x = (history.size() - 1 - index1);

        //calculate the slope price of the most recent period
        this.slopePrice = (getSlope() * x) + y1;
    }
}