package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.Period.Fields;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.EMA;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;

import java.util.ArrayList;
import java.util.List;

import static com.gamesbykevin.tradingbot.calculator.CalculatorHelper.hasCrossover;
import static com.gamesbykevin.tradingbot.calculator.indicator.trend.SMA.calculateSMA;

/**
 * EMA / SMA
 */
public class EMAS extends Strategy {

    //our ema object reference
    private EMA emaObj;

    //our list of variations
    private static final int PERIODS_EMA_LONG = 26;
    private static final int PERIODS_EMA_SHORT = 12;
    private static final int PERIODS_SMA_PRICE = 50;

    //list of sma prices
    private List<Double> priceSMA;

    private final int periodsSMA;

    public EMAS() {
        this(PERIODS_EMA_LONG, PERIODS_EMA_SHORT, PERIODS_SMA_PRICE);
    }

    public EMAS(int emaLong, int emaShort, int periodsSMA) {

        this.periodsSMA = periodsSMA;

        //create new objects
        this.emaObj = new EMA(emaLong, emaShort);
        this.priceSMA = new ArrayList<>();
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //if the fast ema is greater than the simple moving average
        if (getRecent(emaObj.getEmaShort()) > getRecent(priceSMA)) {

            //then if we have crossover let's buy
            if (hasCrossover(true, emaObj.getEmaShort(), emaObj.getEmaLong()))
                agent.setBuy(true);
        }
        //display our data
        displayData(agent, agent.hasBuy());
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //if the fast ema is less than the simple moving average
        if (getRecent(emaObj.getEmaShort()) < getRecent(priceSMA)) {

            //if the fast ema is less than the long ema we will sell
            if (getRecent(emaObj.getEmaShort()) < getRecent(emaObj.getEmaLong()))
                agent.setReasonSell(ReasonSell.Reason_Strategy);

        }

        //adjust our hard stop price to protect our investment
        if (getRecent(emaObj.getEmaShort()) < getRecent(emaObj.getEmaLong()) || getRecent(emaObj.getEmaShort()) < getRecent(priceSMA))
            adjustHardStopPrice(agent, currentPrice);

        //display our data
        displayData(agent, agent.getReasonSell() != null);
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display the information
        this.emaObj.displayData(agent, write);
        display(agent, "SMA Price: ", priceSMA, write);
    }

    @Override
    public void calculate(List<Period> history) {

        //do our calculations
        this.emaObj.calculate(history);

        //calculate our sma price
        calculateSMA(history, priceSMA, periodsSMA, Fields.Close);
    }
}