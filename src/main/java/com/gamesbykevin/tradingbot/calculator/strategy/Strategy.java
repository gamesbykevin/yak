package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.Period.Fields;

import java.util.List;

import static com.gamesbykevin.tradingbot.agent.AgentManagerHelper.displayMessage;

public abstract class Strategy {

    //which data are we using to do our calculations
    private int indexStrategy = 0;

    /**
     * When displaying data how many periods do we print/write to the console/log
     */
    private static int RECENT_PERIODS = 5;

    protected Strategy() {
        //default constructor
    }

    public abstract void checkBuySignal(Agent agent, List<Period> history, double currentPrice);

    public abstract void checkSellSignal(Agent agent, List<Period> history, double currentPrice);

    public abstract void calculate(List<Period> history);

    protected abstract void displayData(Agent agent, boolean write);

    public void setIndexStrategy(final int indexStrategy) {
        this.indexStrategy = indexStrategy;
    }

    public int getIndexStrategy() {
        return this.indexStrategy;
    }

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

    protected double getRecent(List<Period> periods, Fields field) {
        return getRecent(periods, field, 1);
    }

    /**
     * Get the recent data
     * @param periods The list of historical periods where we will be grabbing our data
     * @param field The desired field (examples: open, close, low, high, volume)
     * @param previous The index location of the desired data
     * @return The value of the desired data for the specified recent index
     */
    protected double getRecent(List<Period> periods, Fields field, int previous) {

        Period period = periods.get(periods.size() - previous);

        switch (field) {

            case Volume:
                return period.volume;

            case Close:
                return period.close;

            case Time:
                return period.time;

            case Low:
                return period.low;

            case Open:
                return period.open;

            case High:
                return period.high;

            default:
                throw new RuntimeException("Field not handled: " + field);
        }
    }

    protected double getRecent(List<Double> list) {
        return list.get(list.size() - 1);
    }

    protected double getRecent(List<Double> list, int index) {
        return list.get(list.size() - index);
    }
}