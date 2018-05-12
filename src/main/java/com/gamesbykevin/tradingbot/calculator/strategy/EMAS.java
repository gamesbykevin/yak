package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.Period.Fields;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.EMA;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.SMA;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;

import java.util.List;

import static com.gamesbykevin.tradingbot.agent.AgentManagerHelper.displayMessage;
import static com.gamesbykevin.tradingbot.calculator.utils.CalculatorHelper.hasCrossover;
import static com.gamesbykevin.tradingbot.calculator.indicator.trend.SMA.calculateSMA;

/**
 * EMA / SMA
 */
public class EMAS extends Strategy {

    //our ema object reference
    private EMA emaShortObj, emaLongObj;

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
        this.emaShortObj = new EMA(emaShort);
        this.emaLongObj = new EMA(emaLong);
        this.smaObjShort = new SMA(smaShort);
        this.smaObjLong = new SMA(smaLong);
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //recent closing $
        final double close = getRecent(history, Fields.Close);

        //our previous values
        double prevEmaShort = getRecent(emaShortObj.getEma(), 2);
        double prevEmaLong = getRecent(emaLongObj.getEma(), 2);

        //our current values
        double currEmaShort = getRecent(emaShortObj.getEma());
        double currEmaLong = getRecent(emaLongObj.getEma());
        double currSmaShort = getRecent(smaObjShort.getSma());

        //the short ema needs to cross above the long ema and the close needs to be above the sma
        if (prevEmaShort < prevEmaLong && currEmaShort > currEmaLong && close > currSmaShort)
            agent.setBuy(true);

        //display our data
        displayMessage(agent, "Close $" + close, agent.hasBuy());
        displayData(agent, agent.hasBuy());
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //recent closing $
        final double close = getRecent(history, Fields.Close);

        //our current values
        double currEmaShort = getRecent(emaShortObj.getEma());
        double currEmaLong = getRecent(emaLongObj.getEma());
        double currSmaShort = getRecent(smaObjShort.getSma());
        double currSmaLong = getRecent(smaObjLong.getSma());

        //if the close $ is below the long sma we will sell
        if (close < currSmaLong)
            agent.setReasonSell(ReasonSell.Reason_Strategy);

        //adjust our hard stop price to protect our investment
        if (close < currSmaShort || currEmaShort < currEmaLong)
            adjustHardStopPrice(agent, currentPrice);

        //display our data
        displayMessage(agent, "Close $" + close, agent.hasBuy());
        displayData(agent, agent.getReasonSell() != null);
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display the information
        emaShortObj.displayData(agent, write);
        emaLongObj.displayData(agent, write);
        smaObjShort.displayData(agent, write);
        smaObjLong.displayData(agent, write);
    }

    @Override
    public void calculate(List<Period> history, int newPeriods) {

        //do our calculations
        emaShortObj.calculate(history, newPeriods);
        emaLongObj.calculate(history, newPeriods);
        smaObjShort.calculate(history, newPeriods);
        smaObjLong.calculate(history, newPeriods);
    }

    @Override
    public void cleanup() {
        emaShortObj.cleanup();
        emaLongObj.cleanup();
        smaObjShort.cleanup();
        smaObjLong.cleanup();
    }
}