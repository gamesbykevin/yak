package com.gamesbykevin.tradingbot.calculator;

import com.gamesbykevin.tradingbot.agent.Agent;

import java.util.List;

import static com.gamesbykevin.tradingbot.agent.AgentManagerHelper.displayMessage;
import static com.gamesbykevin.tradingbot.calculator.CalculatorHelper.getValue;

public abstract class Calculation {

    /**
     * When displaying data how many periods do we print/write to the console/log
     */
    private static final int RECENT_PERIODS = 10;

    public abstract void calculate(List<Period> history, int newPeriods);

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

}