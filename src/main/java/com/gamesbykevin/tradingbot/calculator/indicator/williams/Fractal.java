package com.gamesbykevin.tradingbot.calculator.indicator.williams;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Calculator;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.indicator.Indicator;

import java.util.ArrayList;
import java.util.List;

import static com.gamesbykevin.tradingbot.agent.AgentManagerHelper.displayMessage;

public class Fractal extends Indicator {

    //list of fractal status for each period
    private List<Status> statusList;

    /**
     * The number of periods we need to confirm a fractal trend in the middle period
     */
    private static final int PERIODS_CONFIRM = 5;

    /**
     * Different types of fractals we can have
     */
    public enum Status {
        Buy, Sell, Both, None
    }

    public Fractal() {
        this(PERIODS_CONFIRM);
    }

    public Fractal(int periods) {

        //call parent
        super(Indicator.Key.F, periods);

        //make sure the confirm periods is an odd number
        if (getPeriods() % 2 == 0)
            throw new RuntimeException("The periods to confirm should be an odd number");

        //create our list
        this.statusList = new ArrayList<>();
    }

    public List<Status> getStatusList() {
        return this.statusList;
    }

    @Override
    public void calculate(List<Period> history, int newPeriods) {

        //where do we start?
        int start = (getStatusList().isEmpty()) ? 0 : history.size() - newPeriods;

        //check all periods to identify our fractals
        for (int index = start; index < history.size(); index++) {

            if (index < (getPeriods() / 2)) {

                //if we don't have enough periods to make a decision, mark as None
                getStatusList().add(Status.None);
                continue;

            } else if (index >= history.size() - (getPeriods() / 2)) {

                //if we don't have enough periods to make a decision, mark as None
                getStatusList().add(Status.None);
                continue;

            } else {

                //get the status and add to the list
                getStatusList().add(getStatus(history, index));
            }
        }

        //throw an exception if the sizes don't match
        if (history.size() != getStatusList().size())
            throw new RuntimeException("The status list does not match the history size " + history.size() + ", " + getStatusList().size());

        //look at the recent values just in case the status changed
        for (int index = history.size() - getPeriods(); index < history.size() - (getPeriods() / 2); index++) {

            //update the list with the current status
            getStatusList().set(index, getStatus(history, index));

        }
    }

    private Status getStatus(List<Period> history, int index) {

        //did we confirm a fractal trend?
        boolean confirmUp = true;
        boolean confirmDown = true;

        //get the current period
        Period current = history.get(index);

        //check our neighboring periods
        for (int i = 1; i <= (getPeriods() / 2); i++) {

            Period  compare1 = null, compare2 = null;

            //get our neighbors
            if (index + i < history.size())
                compare1 = history.get(index + i);

            if (index - i >= 0)
                compare2 = history.get(index - i);

            //compare only if the objects are not null
            if (compare1 != null && compare2 != null) {

                //if either is above our current, we don't have an up fractal
                if (compare1.high >= current.high || compare2.high >= current.high)
                    confirmUp = false;

                //if either is below our current, we don't have a down fractal
                if (compare1.low <= current.low || compare2.low <= current.low)
                    confirmDown = false;
            } else {

                //if either object is null we can't confirm either
                confirmUp = false;
                confirmDown = false;
            }
        }

        //now let's add our status to the list
        if (confirmUp && confirmDown) {
            return Status.Both;
        } else if (confirmUp && !confirmDown) {
            return Status.Buy;
        } else if (confirmDown && !confirmUp) {
            return Status.Sell;
        } else {
            return Status.None;
        }
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //status description
        String statusDesc = "";

        //construct our message
        for (int i = getPeriods(); i > 0; i--) {

            //add to our status
            statusDesc += getStatusList().get(getStatusList().size() - i);

            //separate each value with a ,
            if (i > 1)
                statusDesc += ", ";
        }

        //display our information
        displayMessage(agent, statusDesc, write);
    }

    @Override
    public void cleanup() {

        while (getStatusList().size() > Calculator.HISTORICAL_PERIODS_MINIMUM) {
            getStatusList().remove(0);
        }
    }
}