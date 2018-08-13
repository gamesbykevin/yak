package com.gamesbykevin.tradingbot.calculator.indicator.trend;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.indicator.Indicator;

import java.util.ArrayList;
import java.util.List;

/**
 * Support & Resistance
 */
public class SR extends Indicator {

    //our lines of support
    private List<Double> supportLevel1;
    private List<Double> supportLevel2;
    private List<Double> supportLevel3;

    //our lines of resistance
    private List<Double> resistanceLevel1;
    private List<Double> resistanceLevel2;
    private List<Double> resistanceLevel3;

    public SR() {

        //call parent
        super(Indicator.Key.SR, 0);

        //create our support and resistance objects
        this.supportLevel1 = new ArrayList<>();
        this.supportLevel2 = new ArrayList<>();
        this.supportLevel3 = new ArrayList<>();
        this.resistanceLevel1 = new ArrayList<>();
        this.resistanceLevel2 = new ArrayList<>();
        this.resistanceLevel3 = new ArrayList<>();
    }

    public List<Double> getSupportLevel1() {
        return this.supportLevel1;
    }

    public List<Double> getSupportLevel2() {
        return this.supportLevel2;
    }

    public List<Double> getSupportLevel3() {
        return this.supportLevel3;
    }

    public List<Double> getResistanceLevel1() {
        return this.resistanceLevel1;
    }

    public List<Double> getResistanceLevel2() {
        return this.resistanceLevel2;
    }

    public List<Double> getResistanceLevel3() {
        return this.resistanceLevel3;
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display the information
        display(agent, "Resistance Level 1: ", getResistanceLevel1(), write);
        display(agent, "Support    Level 1: ", getSupportLevel1(), write);
        display(agent, "Resistance Level 2: ", getResistanceLevel2(), write);
        display(agent, "Support    Level 2: ", getSupportLevel2(), write);
        display(agent, "Resistance Level 3: ", getResistanceLevel3(), write);
        display(agent, "Support    Level 3: ", getSupportLevel3(), write);
    }

    @Override
    public void calculate(List<Period> history, int newPeriods) {

        //where do we start?
        int start = getSupportLevel1().isEmpty() ? 0 : history.size() - newPeriods;

        //check every period
        for (int i = start; i < history.size(); i++) {

            //skip until we have enough data
            if (i < getPeriods())
                continue;

            //get the current period
            Period period = history.get(i);

            //calculate the pivot point
            double pivotPoint = (period.high + period.low + period.close) / 3;

            //calculate level 1
            double resistance1 = (2 * pivotPoint) - period.low;
            double support1 = (2 * pivotPoint) - period.high;

            //calculate level 2
            double resistance2 = (pivotPoint - support1) + resistance1;
            double support2 = pivotPoint - (resistance1 - support1);

            //calculate level 3
            double resistance3 = (pivotPoint - support2) + resistance2;
            double support3 = pivotPoint - (resistance2 - support2);

            //add values to the list
            getSupportLevel1().add(support1);
            getSupportLevel2().add(support2);
            getSupportLevel3().add(support3);
            getResistanceLevel1().add(resistance1);
            getResistanceLevel2().add(resistance2);
            getResistanceLevel3().add(resistance3);
        }
    }

    @Override
    public void cleanup() {
        cleanup(getSupportLevel1());
        cleanup(getSupportLevel2());
        cleanup(getSupportLevel3());
        cleanup(getResistanceLevel1());
        cleanup(getResistanceLevel2());
        cleanup(getResistanceLevel3());
    }
}
