package com.gamesbykevin.tradingbot.calculator.indicator.williams;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.Period.Fields;
import com.gamesbykevin.tradingbot.calculator.indicator.Indicator;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.SMMA;

import java.util.ArrayList;
import java.util.List;

public class Alligator extends Indicator {

    //our configurable values
    public static final int PERIODS_JAW = 13;
    public static final int PERIODS_TEETH = 8;
    public static final int PERIODS_LIPS = 5;

    public static final int PERIODS_JAW_OFFSET = 8;
    public static final int PERIODS_TEETH_OFFSET = 5;
    public static final int PERIODS_LIPS_OFFSET = 3;

    //lists that make up the alligator
    private SMMA jaw, teeth, lips;

    public Alligator() {
        this(PERIODS_JAW, PERIODS_TEETH, PERIODS_LIPS);
    }

    public Alligator(int periodsJaw, int periodsTeeth, int periodsLips) {

        //call parent
        super(Indicator.Key.A,0);

        //add the fields we want to use for the smma
        List<Period.Fields> fields = new ArrayList<>();
        fields.add(Fields.High);
        fields.add(Fields.Low);

        //create our smoothed moving average objects
        this.jaw = new SMMA(periodsJaw, fields);
        this.teeth = new SMMA(periodsTeeth, fields);
        this.lips = new SMMA(periodsLips, fields);
    }

    public SMMA getJaw() {
        return this.jaw;
    }

    public SMMA getLips() {
        return this.lips;
    }

    public SMMA getTeeth() {
        return this.teeth;
    }

    @Override
    public void calculate(List<Period> history, int newPeriods) {

        //perform our calculations
        getJaw().calculate(history, newPeriods);
        getLips().calculate(history, newPeriods);
        getTeeth().calculate(history, newPeriods);
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display our info
        display(agent, "Jaw (" + getJaw().getPeriods() + "): ", getJaw().getSmma(), write);
        display(agent, "Lips (" + getLips().getPeriods() + "): ", getLips().getSmma(), write);
        display(agent, "Teeth (" + getTeeth().getPeriods() + "): ", getTeeth().getSmma(), write);
    }

    @Override
    public void cleanup() {

        //prevent our lists from growing too large
        getJaw().cleanup();
        getLips().cleanup();
        getTeeth().cleanup();
    }
}