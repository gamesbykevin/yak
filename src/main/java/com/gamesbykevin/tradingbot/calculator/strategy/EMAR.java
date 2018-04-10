package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.Period.Fields;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;

import java.util.List;

import static com.gamesbykevin.tradingbot.agent.AgentManagerHelper.displayMessage;
import static com.gamesbykevin.tradingbot.calculator.CalculatorHelper.hasCrossover;

/**
 * EMA / RSI
 */
public class EMAR extends Strategy {

    //our ema object reference
    private EMA emaObj;

    //our rsi object reference
    private RSI rsiObj;

    protected static int[] LIST_PERIODS_EMA_LONG = {12};
    protected static int[] LIST_PERIODS_EMA_SHORT = {5};
    protected static int[] LIST_PERIODS_RSI = {21};
    protected static float[] LIST_RSI_LINE = {50.0f};

    /*
    //our list of variations
    protected static int[] LIST_PERIODS_EMA_LONG = {12, 26, 28};
    protected static int[] LIST_PERIODS_EMA_SHORT = {5, 12, 14};
    protected static int[] LIST_PERIODS_RSI = {21, 14, 12};
    protected static float[] LIST_RSI_LINE = {50.0f, 50.0f, 50.0f};
    */

    //list of configurable values
    protected static float RSI_LINE = 50.0f;

    public EMAR() {

        //call parent
        super();

        //create our objects
        this.emaObj = new EMA();
        this.rsiObj = new RSI();
    }

    @Override
    public String getStrategyDesc() {
        return "PERIODS_EMA_LONG = " + LIST_PERIODS_EMA_LONG[getIndexStrategy()] + ", PERIODS_EMA_SHORT = " + LIST_PERIODS_EMA_SHORT[getIndexStrategy()] + ", PERIODS_RSI = " + LIST_PERIODS_RSI[getIndexStrategy()] + ", RSI_LINE = " + LIST_RSI_LINE[getIndexStrategy()];
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //RSI values
        double current = getRecent(rsiObj.getRsiVal());
        double previous = getRecent(rsiObj.getRsiVal(), 2);

        if (current > RSI_LINE && hasCrossover(true, emaObj.getEmaShort(), emaObj.getEmaLong())) {

            //if rsi is over the line and we have a bullish crossover
            agent.setBuy(true);

            //write to log
            displayMessage(agent, "current > RSI_LINE && hasCrossover(true, emaObj.getEmaShort(), emaObj.getEmaLong())", !agent.isSimulation());

        } else if (previous < RSI_LINE && current > RSI_LINE && getRecent(emaObj.getEmaShort()) > getRecent(emaObj.getEmaLong())) {

            //if rsi JUST went over and ema short > ema long without checking for a crossover
            agent.setBuy(true);

            //write to log
            displayMessage(agent, "previous < RSI_LINE && current > RSI_LINE && getRecent(emaObj.getEmaShort()) > getRecent(emaObj.getEmaLong())", !agent.isSimulation());
        }

        //display our data
        displayData(agent, agent.hasBuy());
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //RSI values
        double current = getRecent(rsiObj.getRsiVal());
        double previous = getRecent(rsiObj.getRsiVal(), 2);

        double emaShort = getRecent(emaObj.getEmaShort());
        double emaLong = getRecent(emaObj.getEmaLong());

        if (current < RSI_LINE && hasCrossover(false, emaObj.getEmaShort(), emaObj.getEmaLong())) {

            //if rsi is under the line and we have a bearish crossover
            agent.setReasonSell(ReasonSell.Reason_Strategy);

            //write to log
            displayMessage(agent, "current < RSI_LINE && hasCrossover(false, emaObj.getEmaShort(), emaObj.getEmaLong())", !agent.isSimulation());

        } else if (previous > RSI_LINE && current < RSI_LINE && currentPrice < emaShort && emaShort < emaLong) {

            //if rsi JUST went under and the current price is less than both ema's
            agent.setReasonSell(ReasonSell.Reason_Strategy);

            //write to log
            displayMessage(agent, "previous > RSI_LINE && current < RSI_LINE && currentPrice < emaShort && emaShort < emaLong", !agent.isSimulation());
        }

        /*
        else if (currentPrice < getRecent(emaObj.getEmaShort()) && currentPrice < getRecent(emaObj.getEmaLong())) {

            //if the current price dropped below both ema's this is a sign of a downward trend
            agent.setReasonSell(ReasonSell.Reason_Strategy);

            //write to log
            displayMessage(agent, "currentPrice < getRecent(emaObj.getEmaShort()) && currentPrice < getRecent(emaObj.getEmaLong())", !agent.isSimulation());
        }
        */

        //if we have bearish crossover
        //if (hasCrossover(false, emaObj.getEmaShort(), emaObj.getEmaLong()))
        //    agent.setReasonSell(ReasonSell.Reason_Strategy);

        //display our data
        displayData(agent, agent.getReasonSell() != null);
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display our information
        rsiObj.displayData(agent, write);
        emaObj.displayData(agent, write);
    }

    @Override
    public void calculate(List<Period> history) {

        //calculate
        rsiObj.calculate(history);
        emaObj.calculate(history);
    }
}