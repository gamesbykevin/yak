package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.Period.Fields;

import java.util.List;

import static com.gamesbykevin.tradingbot.agent.AgentManager.displayMessage;

public abstract class Strategy {

    //the number of periods for the indicator
    private final int periods;

    protected Strategy(final int periods) {

        //assign the periods
        this.periods = periods;
    }

    public abstract void checkBuySignal(Agent agent, List<Period> history, double currentPrice);

    public abstract void checkSellSignal(Agent agent, List<Period> history, double currentPrice);

    public abstract void calculate(List<Period> history);

    protected abstract void displayData(Agent agent, boolean write);

    protected int getPeriods() {
        return this.periods;
    }

    public static void display(Agent agent, String desc, List<Double> list, int periods, boolean write) {

        String info = "";
        for (int i = list.size() - periods; i < list.size(); i++) {

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

        Period period = periods.get(periods.size() - 1);

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