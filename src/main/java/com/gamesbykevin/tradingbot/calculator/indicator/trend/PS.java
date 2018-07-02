package com.gamesbykevin.tradingbot.calculator.indicator.trend;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Calculator;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.indicator.Indicator;

import java.util.ArrayList;
import java.util.List;

import static com.gamesbykevin.tradingbot.agent.AgentManagerHelper.displayMessage;

/**
 * Parabolic SAR
 */
public class PS extends Indicator {

    private List<Double> sar;
    private List<Double> sarT;
    private List<Double> ep;
    private List<Float> af;
    private List<Double> product;
    private List<Boolean> trend;

    //what is our acceleration factor start?
    private static final float ACCELERATION_FACTOR_MIN = 0.02f;

    //what is our acceleration factor max?
    private static final float ACCELERATION_FACTOR_MAX = 0.2f;

    public PS() {

        //call parent
        super(Key.PS, 0);
    }

    @Override
    public void calculate(List<Period> history, int newPeriods) {

        //where do we start
        int start = (getSar().isEmpty()) ? 0 : history.size() - newPeriods;

        //go through our periods to perform our calculation
        for (int index = start; index < history.size(); index++) {

            Period period = history.get(index);

            //calculate our initial values
            if (getSar().isEmpty()) {

                //psar
                getSar().add(period.low);

                //extreme point
                getExtremePoint().add(period.high);

                //ep - psar
                double difference = getRecent(getExtremePoint()) - getRecent(getSar());

                //acceleration factor
                getAccelerationFactor().add(ACCELERATION_FACTOR_MIN);

                //(ep - psar) * acceleration factor
                getProduct().add(difference * getAccelerationFactor().get(0));

                //start at uptrend
                getTrend().add(true);

                //tomorrow's sar is the current sar + the product
                getSarTomorrow().add(getRecent(getSar()) + getRecent(getProduct()));

            } else {

                double previousSar = getRecent(getSar());
                double previousEp = getRecent(getExtremePoint());
                float previousAf = getAccelerationFactor().get(getAccelerationFactor().size() - 1);
                double previousProduct = getRecent(getProduct());
                boolean previousTrend = getTrend().get(getTrend().size() - 1);

                //calculate the new psar
                if (previousTrend && previousSar + previousProduct > period.low) {
                    getSar().add(previousEp);
                } else if (!previousTrend && previousSar + previousProduct < period.high) {
                    getSar().add(previousEp);
                } else {
                    getSar().add(previousSar + previousProduct);
                }

                //figure out the current trend
                if (getRecent(getSar()) < period.high) {
                    getTrend().add(true);
                } else if (getRecent(getSar()) > period.low) {
                    getTrend().add(false);
                }

                //get the current trend for our next calculation
                boolean currentTrend = getTrend().get(getTrend().size() - 1);

                //calculate the new extreme point
                if (currentTrend && period.high > previousEp) {
                    getExtremePoint().add(period.high);
                } else if (currentTrend && period.high < previousEp) {
                    getExtremePoint().add(previousEp);
                } else if (!currentTrend && period.low < previousEp) {
                    getExtremePoint().add(period.low);
                } else if (!currentTrend && period.low > previousEp) {
                    getExtremePoint().add(previousEp);
                }

                //now we can calculate the new difference
                double difference = getRecent(getExtremePoint()) - getRecent(getSar());

                //now we can calculate our acceleration factor
                if (!currentTrend && previousTrend || currentTrend && !previousTrend) {

                    //if we go from (uptrend to downtrend) or (downtrend to uptrend) reset the acceleration factor
                    getAccelerationFactor().add(ACCELERATION_FACTOR_MIN);

                } else {

                    //we will calculate a new acceleration factor
                    float newAf = previousAf;

                    //increase the acceleration factor if there is a new extreme point
                    if (previousEp != getRecent(getExtremePoint()))
                        newAf += ACCELERATION_FACTOR_MIN;

                    //don't exceed the max value
                    if (newAf >= ACCELERATION_FACTOR_MAX)
                        newAf = ACCELERATION_FACTOR_MAX;

                    //add the new acceleration factor to our list
                    getAccelerationFactor().add(newAf);
                }

                //last but not least we can calculate our product
                getProduct().add(difference * getAccelerationFactor().get(getAccelerationFactor().size() - 1));

                //tomorrow's sar is the current sar + the product
                getSarTomorrow().add(getRecent(getSar()) + getRecent(getProduct()));
            }
        }
    }

    public List<Double> getSarTomorrow() {

        //instantiate if null
        if (this.sarT == null)
            this.sarT = new ArrayList<>();

        return this.sarT;
    }

    public List<Double> getSar() {

        //instantiate if null
        if (this.sar == null)
            this.sar = new ArrayList<>();

        return this.sar;
    }

    public List<Double> getExtremePoint() {

        //instantiate if null
        if (this.ep == null)
            this.ep = new ArrayList<>();

        return this.ep;
    }

    public List<Float> getAccelerationFactor() {

        //instantiate if null
        if (this.af == null)
            this.af = new ArrayList<>();

        return this.af;
    }

    public List<Double> getProduct() {

        //instantiate if null
        if (this.product == null)
            this.product = new ArrayList<>();

        return this.product;
    }

    public List<Boolean> getTrend() {

        //instantiate if null
        if (this.trend == null)
            this.trend = new ArrayList<>();

        return this.trend;
    }

    @Override
    public void cleanup() {

        //cleanup lists
        cleanup(getSar());
        cleanup(getExtremePoint());
        cleanup(getProduct());

        //remove the first values until we are at the desired size
        while (getAccelerationFactor().size() > Calculator.HISTORICAL_PERIODS_MINIMUM) {
            getAccelerationFactor().remove(0);
        }

        //remove the first values until we are at the desired size
        while (getTrend().size() > Calculator.HISTORICAL_PERIODS_MINIMUM) {
            getTrend().remove(0);
        }
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        display(agent, "SAR   : ", getSar(), write);
        display(agent, "EP    : ", getExtremePoint(), write);
        display(agent, "Prod  : ", getProduct(), write);

        String desc = "";
        for (int i = getAccelerationFactor().size() - RECENT_PERIODS; i < getAccelerationFactor().size(); i++) {
            desc += "" + getAccelerationFactor().get(i);
            if (i < getAccelerationFactor().size() - 1)
                desc += ", ";
        }
        displayMessage(agent, "AF    : " + desc, write);

        desc = "";
        for (int i = getTrend().size() - RECENT_PERIODS; i < getTrend().size(); i++) {
            desc += "" + (getTrend().get(i) ? "Uptrend" : "Downtrend");
            if (i < getTrend().size() - 1)
                desc += ", ";
        }
        displayMessage(agent, "Trend : " + desc, write);
    }
}