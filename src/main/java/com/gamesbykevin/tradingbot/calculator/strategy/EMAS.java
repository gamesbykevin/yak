package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.Period.Fields;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.EMA;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.SMA;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;

import java.util.ArrayList;
import java.util.List;

import static com.gamesbykevin.tradingbot.agent.AgentManagerHelper.displayMessage;
import static com.gamesbykevin.tradingbot.calculator.CalculatorHelper.hasCrossover;
import static com.gamesbykevin.tradingbot.calculator.indicator.trend.SMA.calculateSMA;

/**
 * EMA / SMA
 */
public class EMAS extends Strategy {

    //our ema object reference
    private EMA emaObj;

    //our sma object reference(s)
    private SMA smaObjShort, smaObjLong;

    //our list of variations
    private static final int PERIODS_EMA_LONG = 169;
    private static final int PERIODS_EMA_SHORT = 144;
    private static final int PERIODS_SMA_LONG = 14;
    private static final int PERIODS_SMA_SHORT = 5;

    public EMAS() {
        this(PERIODS_EMA_LONG, PERIODS_EMA_SHORT, PERIODS_SMA_LONG, PERIODS_SMA_SHORT);
    }

    public EMAS(int emaLong, int emaShort, int smaLong, int smaShort) {

        //create new objects
        this.emaObj = new EMA(emaLong, emaShort);
        this.smaObjShort = new SMA(smaShort);
        this.smaObjLong = new SMA(smaLong);
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //recent closing $
        final double close = getRecent(history, Fields.Close);

        //make sure we have an uptrend short value > long value
        if (getRecent(emaObj.getEmaShort()) > getRecent(emaObj.getEmaLong())) {

            //if the close $ is above the sma we have a buy signal
            if (close > getRecent(smaObjShort.getSma()))
                agent.setBuy(true);
        }

        //display our data
        displayMessage(agent, "Close $" + close, agent.hasBuy());
        displayData(agent, agent.hasBuy());
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //recent closing $
        final double close = getRecent(history, Fields.Close);

        //if the close $ is below the long sma we will sell
        if (close < getRecent(smaObjLong.getSma()))
            agent.setReasonSell(ReasonSell.Reason_Strategy);

        //adjust our hard stop price to protect our investment
        if (close < getRecent(smaObjShort.getSma()))
            adjustHardStopPrice(agent, currentPrice);
        if (getRecent(emaObj.getEmaShort()) < getRecent(emaObj.getEmaLong()))
            adjustHardStopPrice(agent, currentPrice);

        //display our data
        displayMessage(agent, "Close $" + close, agent.hasBuy());
        displayData(agent, agent.getReasonSell() != null);
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display the information
        emaObj.displayData(agent, write);
        smaObjShort.displayData(agent, write);
        smaObjLong.displayData(agent, write);
    }

    @Override
    public void calculate(List<Period> history) {

        //do our calculations
        emaObj.calculate(history);
        smaObjShort.calculate(history);
        smaObjLong.calculate(history);
    }
}