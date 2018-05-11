package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.Slope;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.MACD;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;

import java.util.List;

import static com.gamesbykevin.tradingbot.agent.AgentManagerHelper.displayMessage;

/**
 * Moving Average Crossover Divergence / Slope
 */
public class MSL extends Strategy {

    //what is our slope line
    private float slope = 0f;

    //the index where our crossover happens
    private int x1, x2;

    //our macd indicator object
    private MACD objMacd;

    //our list of variations
    private static final int PERIODS_MACD_SIGNAL = 9;
    private static final int PERIODS_EMA_LONG = 26;
    private static final int PERIODS_EMA_SHORT = 12;

    public MSL() {
        this(PERIODS_EMA_LONG, PERIODS_EMA_SHORT, PERIODS_MACD_SIGNAL);
    }

    public MSL(int emaLong, int emaShort, int macdSignal) {

        //create object
        this.objMacd = new MACD(emaLong, emaShort, macdSignal);
    }

    private MACD getObjMacd() {
        return this.objMacd;
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //macd line crosses above signal line and both values are below 0
        boolean crossBelow = getRecent(getObjMacd().getMacdLine()) > getRecent(getObjMacd().getSignalLine()) && (getRecent(getObjMacd().getMacdLine()) < 0 && getRecent(getObjMacd().getSignalLine()) < 0);

        //macd line crosses above signal line and both values are above 0
        boolean crossAbove = getRecent(getObjMacd().getMacdLine()) > getRecent(getObjMacd().getSignalLine()) && (getRecent(getObjMacd().getMacdLine()) > 0 && getRecent(getObjMacd().getSignalLine()) > 0);

        //ensure previous 2 histogram values are increasing
        boolean increase = getRecent(getObjMacd().getHistogram(), 1) > getRecent(getObjMacd().getHistogram(), 2) && getRecent(getObjMacd().getHistogram(), 2) >  getRecent(getObjMacd().getHistogram(), 3);

        //based on our slope that is the support line for closing price
        double slopePrice = getSlopePrice(history);

        //get the latest period
        Period period = history.get(history.size() - 1);

        //here are our buy signals
        if (getSlope() > 0) {

            if (increase && crossBelow && period.close > period.open) {
                agent.setBuy(true);
                displayMessage(agent, "increase && crossBelow && period.close > period.open", true);
            } else if (increase && crossAbove && period.close > period.open && period.close >= slopePrice) {
                agent.setBuy(true);
                displayMessage(agent, "increase && crossAbove && period.close > period.open && period.close >= slopePrice && getSlope() > 0", true);
            }
        }

        //display our data
        displayMessage(agent, "x1 = " + getX1() + ", x2 = " + getX2() + ", y1 $" + history.get(getX1()).close + ", y2 $" + history.get(getX2()).close, agent.hasBuy());
        displayMessage(agent,"Close $" + period.close, agent.hasBuy());
        displayMessage(agent,"Slope $" + getSlopePrice(history), agent.hasBuy());
        displayData(agent, agent.hasBuy());
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //get the latest period
        Period periodCurrent = history.get(history.size() - 1);
        Period periodPrevious = history.get(history.size() - 2);

        //get the current slope price
        double slopePrice = getSlopePrice(history);

        //ensure previous 2 histogram values are decreasing
        final boolean decrease = (getRecent(getObjMacd().getHistogram(), 1) < getRecent(getObjMacd().getHistogram(), 2)) &&
                                (getRecent(getObjMacd().getHistogram(), 2) < getRecent(getObjMacd().getHistogram(), 3)) &&
                                (getRecent(getObjMacd().getHistogram(), 2) <= 0);

        //when we confirm our histogram is decreasing and our current close is less than the previous
        if (decrease && periodCurrent.close <= slopePrice)
            agent.setReasonSell(ReasonSell.Reason_Strategy);

        //adjust our hard stop price to protect our investment
        if (periodCurrent.close <= slopePrice)
            adjustHardStopPrice(agent, currentPrice);

        /*
        //if the histogram is less than 0 and the current close price is below the slope price
        if (getRecent(getHistogram()) < 0 && periodCurrent.close <= slopePrice)
            agent.setReasonSell(ReasonSell.Reason_Strategy);
        */

        //display our data
        displayMessage(agent,"Close $" + periodCurrent.close, agent.hasBuy());
        displayMessage(agent,"Slope $" + getSlopePrice(history), agent.hasBuy());
        displayData(agent, agent.getReasonSell() != null);
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display the recent MSL values which we use as a signal
        displayMessage(agent, "Slope: " + getSlope(), write);
        getObjMacd().displayData(agent, write);
    }

    @Override
    public void calculate(List<Period> history, int newPeriods) {

        //calculate
        getObjMacd().calculate(history, newPeriods);
        calculateSlope(history);
    }

    private void calculateSlope(List<Period> history) {

        //did we find the 2 latest crossovers
        boolean foundLatest = false, foundPrevious = false;

        //we need the difference so we are checking the correct historical periods
        int difference = history.size() - getObjMacd().getHistogram().size();

        //find the latest crossover
        for (int index = getObjMacd().getHistogram().size() - 2; index >= 0; index--) {

            //if the current is greater than 0 and the previous is below we have found the crossover
            if (getObjMacd().getHistogram().get(index) > 0 && getObjMacd().getHistogram().get(index - 1) < 0) {

                if (!foundLatest) {
                    setX2(difference + index);
                    foundLatest = true;
                } else {
                    setX1(difference + index);
                    foundPrevious = true;
                    break;
                }
            }
        }

        //this should never happen
        if (!foundLatest || !foundPrevious)
            throw new RuntimeException("Unable to locate latest crossover. foundLatest=" + foundLatest + ", foundPrevious=" + foundPrevious);

        //we are checking the close price for slope
        final double y1 = history.get(getX1()).close;
        final double y2 = history.get(getX2()).close;

        //the slope between the 2 recent crossovers will be our support line
        setSlope(Slope.getSlope(getX1(), getX2(), y1, y2));
    }

    public float getSlope() {
        return this.slope;
    }

    public void setSlope(float slope) {
        this.slope = slope;
    }

    public int getX1() {
        return this.x1;
    }

    public void setX1(int x1) {
        this.x1 = x1;
    }

    public int getX2() {
        return this.x2;
    }

    public void setX2(int x2) {
        this.x2 = x2;
    }

    private double getSlopePrice(List<Period> history) {

        //we assume x1 is where x = 0
        final double yIntercept = history.get(getX1()).close;

        //the slope price given how many periods have elapsed since the x1 crossover
        return (getSlope() * (history.size() - getX1())) + yIntercept;
    }
}