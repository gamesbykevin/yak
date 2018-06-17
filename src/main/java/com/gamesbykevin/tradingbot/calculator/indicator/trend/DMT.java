package com.gamesbykevin.tradingbot.calculator.indicator.trend;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.indicator.Indicator;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import static com.gamesbykevin.tradingbot.util.PropertyUtil.displayMessage;

/**
 * Demark Trend Indicator
 */
public class DMT extends Indicator {

    //2 bullish candle indicators
    private Period bullPrev, bullCurr;

    //2 bearish candle indicators
    private Period bearPrev, bearCurr;

    //the points on our line(s)
    private List<Double> bullSlopeData, bearSlopeData;

    //the slope line for the bullish and bearish lines
    private float slopeBull, slopeBear;

    public DMT() {

        //call parent
        super(Key.DMT, 0);

        //create new lists
        this.bullSlopeData = new ArrayList<>();
        this.bearSlopeData = new ArrayList<>();
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        PrintWriter writer = (write) ? agent.getWriter() : null;

        //display our data
        displayMessage("DM: Bullish previous " + getPeriodDesc(getBullPrev()), writer);
        displayMessage("DM: Bullish current  " + getPeriodDesc(getBullCurr()), writer);
        displayMessage("DM: Bearish previous " + getPeriodDesc(getBearPrev()), writer);
        displayMessage("DM: Bearish current  " + getPeriodDesc(getBearCurr()), writer);
        displayMessage("DM: Bullish Slope: " + getSlopeBull(), writer);
        displayMessage("DM: Bearish Slope: " + getSlopeBear(), writer);
        display(agent, "Bullish $: ", getBullSlopeData(), write);
        display(agent, "Bearish $: ", getBearSlopeData(), write);
    }

    @Override
    public void calculate(List<Period> history, int newPeriods) {

        //set to null so we can identify once again
        setBullPrev(null);
        setBullCurr(null);
        setBearPrev(null);
        setBearCurr(null);

        //reset slope values
        setSlopeBull(0);
        setSlopeBear(0);

        //track index so we know the period difference between the candles
        int bullIndexCurr = 0;
        int bullIndexPrev = 0;
        int bearIndexCurr = 0;
        int bearIndexPrev = 0;

        //clear our lists
        getBearSlopeData().clear();
        getBullSlopeData().clear();

        //start with the most recent period and go backwards
        for (int index = history.size() - 2; index > 0; index--) {

            //get the current period
            Period center = history.get(index);

            //we need to check the periods on both sides
            Period right = history.get(index + 1);
            Period left = history.get(index - 1);

            //check candles for uptrend
            if (center.low < right.low && center.low < left.low) {

                if (getBullCurr() == null) {

                    //we found our first bullish candle
                    setBullCurr(center);

                    //remember the index
                    bullIndexCurr = index;

                } else if (getBullPrev() == null) {

                    //we need to make sure the candle is lower than the other bull candle we identified
                    if (center.low < getBullCurr().low) {

                        //save the candle
                        setBullPrev(center);

                        //remember the index
                        bullIndexPrev = index;
                    }
                }
            }

            //check candles for downtrend
            if (center.high > right.high && center.high > left.high) {

                if (getBearCurr() == null) {

                    //we found our first bearish candle
                    setBearCurr(center);

                    //remember the index
                    bearIndexCurr = index;

                } else if (getBearPrev() == null) {

                    //we need to make sure the candle is higher than the other bear candle we identified
                    if (center.high > getBearCurr().high) {

                        //save the candle
                        setBearPrev(center);

                        //remember the index
                        bearIndexPrev = index;
                    }
                }
            }
        }

        //we can't do anything if the periods don't exist
        if (getBearPrev() == null || getBullPrev() == null)
            return;
        if (getBearCurr() == null || getBullCurr() == null)
            return;

        //calculate the slope
        setSlopeBear(calculateSlope(bearIndexPrev, bearIndexCurr, (float)getBearPrev().high, (float)getBearCurr().high));
        setSlopeBull(calculateSlope(bullIndexPrev, bullIndexCurr, (float)getBullPrev().low, (float)getBullCurr().low));

        //create our line(s)
        calculateSlopePoints(bullIndexCurr, bullIndexPrev, history.size(), getSlopeBull(), (float)getBullPrev().low, true);
        calculateSlopePoints(bearIndexCurr, bearIndexPrev, history.size(), getSlopeBear(), (float)getBearPrev().high, false);
    }

    private float calculateSlope(float x1, float x2, float y1, float y2) {
        return ((y2 - y1) / (x2 - x1));
    }

    private void calculateSlopePoints(int indexCurr, int indexPrev, int indexEnd, float slope, float yIntercept, boolean bull) {

        //now let's plot out our y-coordinates
        for (int index = indexCurr + 1; index < indexEnd; index++) {

            float m = slope;
            float x = (index - indexPrev);
            float b = yIntercept;

            //calculate the y-coordinate
            double y = (m * x) + b;

            //add the point to our list
            if (bull) {
                getBullSlopeData().add(y);
            } else {
                getBearSlopeData().add(y);
            }
        }

    }

    public float getSlopeBull() {
        return this.slopeBull;
    }

    public void setSlopeBull(float slopeBull) {
        this.slopeBull = slopeBull;
    }

    public float getSlopeBear() {
        return this.slopeBear;
    }

    public void setSlopeBear(float slopeBear) {
        this.slopeBear = slopeBear;
    }

    public List<Double> getBullSlopeData() {
        return this.bullSlopeData;
    }

    public List<Double> getBearSlopeData() {
        return this.bearSlopeData;
    }

    public Period getBullPrev() {
        return this.bullPrev;
    }

    public void setBullPrev(Period bullPrev) {
        this.bullPrev = bullPrev;
    }

    public Period getBullCurr() {
        return this.bullCurr;
    }

    public void setBullCurr(Period bullCurr) {
        this.bullCurr = bullCurr;
    }

    public Period getBearPrev() {
        return this.bearPrev;
    }

    public void setBearPrev(Period bearPrev) {
        this.bearPrev = bearPrev;
    }

    public Period getBearCurr() {
        return this.bearCurr;
    }

    public void setBearCurr(Period bearCurr) {
        this.bearCurr = bearCurr;
    }

    @Override
    public void cleanup() {
        cleanup(getBullSlopeData());
        cleanup(getBearSlopeData());
    }
}