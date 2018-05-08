package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.Period.Fields;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.EMA;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.SMA;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.MACD;
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
    private static final int PERIOD_DISTANCE_REFERENCE = 3;

    private final int periodsEmaShort;

    //objects with indicator values
    private MACD objMACD;
    private List<Double> emaShort;
    private SMA smaShort;

    public MES() {
        this(PERIODS_SMA_SHORT, PERIODS_EMA_SHORT, PERIODS_MACD_SHORT, PERIODS_MACD_LONG, PERIODS_MACD_SIGNAL);
    }

    public MES(int periodsSmaShort, int periodsEmaShort, int periodsMacdShort, int periodsMacdLong, int periodsMacdSignal) {

        //store our config settings
        this.periodsEmaShort = periodsEmaShort;

        //create our indicator objects
        this.objMACD = new MACD(periodsMacdLong, periodsMacdShort, periodsMacdSignal);
        this.emaShort = new ArrayList<>();
        this.smaShort = new SMA(periodsSmaShort);
    }

    private MACD getObjMacd() {
        return this.objMACD;
    }

    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        boolean confirmA = false, confirmB = false;

        //make sure we recently crossed
        for (int index = 0; index < PERIOD_DISTANCE_REFERENCE; index++) {

            if (!confirmA && getRecent(getObjMacd().getMacdLine(), index + 2) < getRecent(getObjMacd().getSignalLine(), index + 2))
                confirmA = true;
            if (!confirmB && getRecent(emaShort, index + 2) < getRecent(smaShort.getSma(), index + 2))
                confirmB = true;
        }

        //if we confirm macd and ema were previously down
        if (confirmA && confirmB) {

            //first we make sure the macd line crossed above the signal line
            if (getRecent(getObjMacd().getMacdLine()) > getRecent(getObjMacd().getSignalLine())) {

                //make sure the fast ema crosses above the sma short, let's buy
                if (getRecent(emaShort) > getRecent(smaShort.getSma()))
                    agent.setBuy(true);
            }
        }

        //display our data
        displayData(agent, agent.hasBuy());
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //first we make sure the macd line crossed below the signal line
        if (getRecent(getObjMacd().getMacdLine()) < getRecent(getObjMacd().getSignalLine())) {

            //next make sure the fast ema crosses below the sma short
            if (getRecent(emaShort) < getRecent(smaShort.getSma()))
                agent.setReasonSell(ReasonSell.Reason_Strategy);

        } else {

            double close = getRecent(history, Fields.Close);

            //if the close is less than the emas and the macd line is negative
            if (close < getRecent(emaShort) && close < getRecent(smaShort.getSma()) && getRecent(getObjMacd().getMacdLine()) < 0)
                agent.setReasonSell(ReasonSell.Reason_Strategy);
        }

        //adjust our hard stop price to protect our investment
        if (periodsEmaShort < smaShort.getPeriods() && getRecent(emaShort) < getRecent(smaShort.getSma()))
            adjustHardStopPrice(agent, currentPrice);

        //display our data
        displayData(agent, agent.getReasonSell() != null);
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display our data
        getObjMacd().displayData(agent, write);
        display(agent, "SMA Short :", smaShort.getSma(), write);
        display(agent, "EMA Short :", emaShort, write);
    }

    @Override
    public void calculate(List<Period> history) {

        //do our calculations
        getObjMacd().calculate(history);
        smaShort.calculate(history);
        EMA.calculateEMA(history, emaShort, periodsEmaShort);
    }
}