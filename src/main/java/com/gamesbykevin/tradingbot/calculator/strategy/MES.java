package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.Period.Fields;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;

import java.util.ArrayList;
import java.util.List;

/**
 * MACD / EMA / SMA
 */
public class MES extends Strategy {

    //configurable values
    private static final int PERIODS_SMA_SHORT = 15;
    private static final int PERIODS_EMA_SHORT = 5;
    private static final int PERIODS_MACD_SHORT = 12;
    private static final int PERIODS_MACD_LONG = 26;
    private static final int PERIODS_MACD_SIGNAL = 9;

    //check recent periods to confirm recent crossover
    private static final int PERIOD_DISTANCE_REFERENCE = 5;

    private final int periodsSmaShort, periodsEmaShort;

    //objects with indicator values
    private MACD objMACD;
    private List<Double> emaShort;
    private List<Double> smaShort;

    public MES() {
        this(PERIODS_SMA_SHORT, PERIODS_EMA_SHORT, PERIODS_MACD_SHORT, PERIODS_MACD_LONG, PERIODS_MACD_SIGNAL);
    }

    public MES(int periodsSmaShort, int periodsEmaShort, int periodsMacdShort, int periodsMacdLong, int periodsMacdSignal) {

        //store our config settings
        this.periodsSmaShort = periodsSmaShort;
        this.periodsEmaShort = periodsEmaShort;

        //create our indicator objects
        this.objMACD = new MACD(periodsMacdLong, periodsMacdShort, periodsMacdSignal);
        this.emaShort = new ArrayList<>();
        this.smaShort = new ArrayList<>();
    }

    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        boolean confirmA = false, confirmB = false;

        //make sure we recently crossed
        for (int index = 0; index < PERIOD_DISTANCE_REFERENCE; index++) {

            if (!confirmA && getRecent(objMACD.getMacdLine(), index + 2) < getRecent(objMACD.getSignalLine(), index + 2))
                confirmA = true;
            if (!confirmB && getRecent(emaShort, index + 2) < getRecent(smaShort, index + 2))
                confirmB = true;
        }

        //if we confirm macd and ema were previously down
        if (confirmA && confirmB) {

            //first we make sure the macd line crossed above the signal line
            if (getRecent(objMACD.getMacdLine()) > getRecent(objMACD.getSignalLine())) {

                //make sure the fast ema crosses above the sma short, let's buy
                if (getRecent(emaShort) > getRecent(smaShort))
                    agent.setBuy(true);
            }
        }

        //display our data
        displayData(agent, agent.hasBuy());
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //first we make sure the macd line crossed below the signal line
        if (getRecent(objMACD.getMacdLine()) < getRecent(objMACD.getSignalLine())) {

            //next make sure the fast ema crosses below the sma short
            if (getRecent(emaShort) < getRecent(smaShort))
                agent.setReasonSell(ReasonSell.Reason_Strategy);
        }

        //display our data
        displayData(agent, agent.getReasonSell() != null);
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display our data
        this.objMACD.displayData(agent, write);
        display(agent, "SMA Short :", smaShort, write);
        display(agent, "EMA Short :", emaShort, write);
    }

    @Override
    public void calculate(List<Period> history) {

        //do our calculations
        this.objMACD.calculate(history);
        SMA.calculateSMA(history, smaShort, periodsSmaShort, Fields.Close);
        EMA.calculateEMA(history, emaShort, periodsEmaShort);
    }
}