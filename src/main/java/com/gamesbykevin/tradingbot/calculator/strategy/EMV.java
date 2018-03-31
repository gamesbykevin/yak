package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;

import java.util.ArrayList;
import java.util.List;

import static com.gamesbykevin.tradingbot.calculator.strategy.SMA.calculateSMA;

/**
 * Ease of Movement
 */
public class EMV extends Strategy {

    //our list of variations
    protected static int[] LIST_PERIODS_EMV = {14};

    //list of configurable values
    protected static int PERIODS_EMV = 14;

    /**
     * What is the default volume we used to calculate the box ratio
     */
    protected static double VOLUME_DEFAULT = 100000000d;

    //list of emv values
    private List<Double> smaEmv, valEmv;

    public EMV() {

        //call parent with default value
        super();

        //create new lists
        this.smaEmv = new ArrayList<>();
        this.valEmv = new ArrayList<>();
    }

    public List<Double> getSmaEmv() {
        return this.smaEmv;
    }

    public List<Double> getValEmv() {
        return this.valEmv;
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        double current = getRecent(getSmaEmv());
        double previous = getRecent(getSmaEmv(), 2);

        //if the sma crosses above 0, let's buy
        if (previous < 0 && current > 0)
            agent.setBuy(true);

        //display our data
        displayData(agent, agent.hasBuy());
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        double current = getRecent(getSmaEmv());
        double previous = getRecent(getSmaEmv(), 2);

        //if the sma crosses below 0, let's sell
        if (previous > 0 && current < 0)
            agent.setReasonSell(ReasonSell.Reason_Strategy);

        //display our data
        displayData(agent, agent.getReasonSell() != null);
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display the information
        display(agent, "SMA EMV: ", getSmaEmv(), write);
        display(agent, "Val EMV: ", getValEmv(), write);
    }

    @Override
    public void calculate(List<Period> history) {

        //clear our list
        getValEmv().clear();

        for (int i = 0; i < history.size(); i++) {

            //we need to be able to look at the previous value
            if (i < 1)
                continue;

            //get the current and previous periods
            Period current = history.get(i);
            Period previous = history.get(i - 1);

            //calculate distance moved
            double distance = ((current.high + current.low) / 2.0d) - ((previous.high + previous.low) / 2.0d);

            //calculate the box ratio
            double ratio = (getRecent(history, Period.Fields.Volume) / VOLUME_DEFAULT) / (current.high - current.low);

            //calculate our emv value
            double emv = (distance / ratio);

            //add the new value to our list
            getValEmv().add(emv);
        }

        //now that we have our list of emv values, calculate sma
        calculateSMA(getValEmv(), getSmaEmv(), PERIODS_EMV);
    }
}