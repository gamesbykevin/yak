package com.gamesbykevin.tradingbot.calculator;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.EMA;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.SMA;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.SMMA;

import java.util.List;

import static com.gamesbykevin.tradingbot.agent.AgentManagerHelper.displayMessage;
import static com.gamesbykevin.tradingbot.calculator.utils.CalculatorHelper.getValue;

public abstract class Calculation {

    /**
     * When displaying data how many periods do we print/write to the console/log
     */
    public static final int RECENT_PERIODS = 5;

    /**
     * After we perform all our calculations we don't need to retain all values
     */
    public static final int PERIODS_RETAIN = 500;

    public abstract void calculate(List<Period> history, int newPeriods);

    public abstract void cleanup();

    protected void cleanup(List<Double> list) {

        //remove the first values until we are at the desired size
        while (list.size() > PERIODS_RETAIN) {
            list.remove(0);
        }
    }

    public abstract void displayData(Agent agent, boolean write);

    public static void display(Agent agent, String desc, List<Double> list, boolean write) {

        int size = RECENT_PERIODS;

        if (size >= list.size())
            size = list.size();

        String info = "";
        for (int i = list.size() - size; i < list.size(); i++) {

            if (info != null && info.length() > 0)
                info += ", ";

            if (list.get(i) == 0) {
                info += "0";
            } else {
                info += list.get(i);
            }
        }

        displayMessage(agent, desc + info, write);
    }

    protected double getRecent(List<Period> periods, Period.Fields field) {
        return getRecent(periods, field, 1);
    }

    /**
     * Get the recent data
     * @param periods The list of historical periods where we will be grabbing our data
     * @param field The desired field (examples: open, close, low, high, volume)
     * @param previous How many periods do we go back from the most recent
     * @return The value of the desired data for the specified recent index
     */
    protected double getRecent(List<Period> periods, Period.Fields field, int previous) {
        return getValue(periods, field, periods.size() - previous);
    }

    protected double getRecent(List<Double> list) {
        return getRecent(list, 1);
    }

    protected double getRecent(List<Double> list, int index) {
        return list.get(list.size() - index);
    }

    protected double getRecent(EMA objEMA, int index) {
        return getRecent(objEMA.getEma(), index);
    }

    protected double getRecent(EMA objEMA) {
        return getRecent(objEMA.getEma(), 1);
    }

    protected double getRecent(SMA objSMA, int index) {
        return getRecent(objSMA.getSma(), index);
    }

    protected double getRecent(SMA objSMA) {
        return getRecent(objSMA.getSma(), 1);
    }

    protected double getRecent(SMMA objSMMA) {
        return getRecent(objSMMA.getSmma(), 1);
    }

    protected double getRecent(SMMA objSMMA, int index) {
        return getRecent(objSMMA.getSmma(), index);
    }
}