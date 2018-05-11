package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.indicator.momentun.RSI;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.EMA;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;

import java.util.ArrayList;
import java.util.List;

public class MER extends Strategy {

    //list of ema values
    private List<Double> ema1, ema2, ema3, ema4, ema5;

    //our rsi object
    private RSI objRSI;

    //configurable values
    private static final int PERIODS_EMA_1 = 3;
    private static final int PERIODS_EMA_2 = 5;
    private static final int PERIODS_EMA_3 = 13;
    private static final int PERIODS_EMA_4 = 21;
    private static final int PERIODS_EMA_5 = 80;
    private static final int PERIODS_RSI = 14;
    private static final float RSI_LINE = 50.0f;

    public MER() {

        //create new lists
        this.ema1 = new ArrayList<>();
        this.ema2 = new ArrayList<>();
        this.ema3 = new ArrayList<>();
        this.ema4 = new ArrayList<>();
        this.ema5 = new ArrayList<>();

        //create our new rsi object
        this.objRSI = new RSI(PERIODS_RSI);
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //get the recent period
        Period period = history.get(history.size() - 1);

        //is the close > our 80 period ema then there is bullish trend
        if (period.close > getRecent(ema5)) {

            //if 13 period ema is > 21 period ema (minor bullish trend)
            if (getRecent(ema3,2) < getRecent(ema4,2) && getRecent(ema3) > getRecent(ema4)) {

                //if 3 period ema is > 5 period ema (minor bullish trend)
                if (getRecent(ema1) > getRecent(ema2)) {

                    //if the rsi line is above trend, we will buy
                    if (getRecent(objRSI.getRsiVal()) >= RSI_LINE)
                        agent.setBuy(true);
                }
            }
        }

        //display our data
        displayData(agent, agent.hasBuy());
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //get the recent period
        Period period = history.get(history.size() - 1);

        //if below trend sell immediately
        if (period.close < getRecent(ema5)) {
            agent.setReasonSell(ReasonSell.Reason_Strategy);
            adjustHardStopPrice(agent, currentPrice);
        }

        //if 13 period ema is < 21 period ema (minor bearish trend)
        if (getRecent(ema3) < getRecent(ema4))
            adjustHardStopPrice(agent, currentPrice);

        //if 3 period ema is < 5 period ema (minor bearish trend)
        if (getRecent(ema1) < getRecent(ema2))
            adjustHardStopPrice(agent, currentPrice);

        //if rsi drops below the line we will sell
        if (getRecent(objRSI.getRsiVal()) < RSI_LINE) {
            agent.setReasonSell(ReasonSell.Reason_Strategy);
            adjustHardStopPrice(agent, currentPrice);
        }

        //display our data
        displayData(agent, agent.getReasonSell() != null);
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display the information
        display(agent, "EMA (" + PERIODS_EMA_1 + ") ", ema1, write);
        display(agent, "EMA (" + PERIODS_EMA_2 + ") ", ema2, write);
        display(agent, "EMA (" + PERIODS_EMA_3 + ") ", ema3, write);
        display(agent, "EMA (" + PERIODS_EMA_4 + ") ", ema4, write);
        display(agent, "EMA (" + PERIODS_EMA_5 + ") ", ema5, write);
        objRSI.displayData(agent, write);
    }

    @Override
    public void calculate(List<Period> history, int newPeriods) {

        //do our calculations
        EMA.calculateEMA(history, ema1, newPeriods, PERIODS_EMA_1);
        EMA.calculateEMA(history, ema2, newPeriods, PERIODS_EMA_2);
        EMA.calculateEMA(history, ema3, newPeriods, PERIODS_EMA_3);
        EMA.calculateEMA(history, ema4, newPeriods, PERIODS_EMA_4);
        EMA.calculateEMA(history, ema5, newPeriods, PERIODS_EMA_5);
        objRSI.calculate(history, newPeriods);
    }
}