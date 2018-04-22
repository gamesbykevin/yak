package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.Period.Fields;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;

import java.util.ArrayList;
import java.util.List;

import static com.gamesbykevin.tradingbot.calculator.strategy.SMA.calculateSMA;

/**
 * 2 period RSI
 */
public class TWO_RSI extends Strategy {

    //our rsi object
    private RSI rsiObj;

    //list of configurable values
    private static final int PERIODS_SMA = 50;
    private static final double MIN_RSI = 5.0d;
    private static final double MAX_RSI = 95.0d;
    private static final int PERIODS_RSI = 2;

    private final double minRSI, maxRSI;

    public TWO_RSI() {
        this(PERIODS_SMA, MIN_RSI, MAX_RSI, PERIODS_RSI);
    }

    public TWO_RSI(int periodsSMA, double minRSI, double maxRSI, int periodsRSI) {
        this.minRSI = minRSI;
        this.maxRSI = maxRSI;

        //create new list
        this.rsiObj = new RSI(periodsSMA, periodsRSI, 0, 0);
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //get the current and previous rsi values
        double rsiCurrent = getRecent(rsiObj.getRsiVal());
        double rsiPrevious = getRecent(rsiObj.getRsiVal(), 2);

        //get the current and previous sma values
        double smaCurrent = getRecent(rsiObj.getSmaPrice());
        double smaPrevious = getRecent(rsiObj.getSmaPrice(), 2);

        //get the current and previous stock price
        double priceCurrent = getRecent(history, Fields.Close);
        double pricePrevious = getRecent(history, Fields.Close, 2);

        if (rsiCurrent < minRSI && smaPrevious > pricePrevious && smaCurrent < priceCurrent) {

            //if we are below the rsi support line and the stock price crosses above the sma average
            agent.setBuy(true);

        } else if (smaCurrent < priceCurrent && rsiPrevious > minRSI && rsiCurrent < minRSI) {

            //if the stock price is above the sma average and the rsi crosses below the support line
            agent.setBuy(true);
        }

        //display our data
        displayData(agent, agent.hasBuy());
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //get the current and previous rsi values
        double rsiCurrent = getRecent(rsiObj.getRsiVal());
        double rsiPrevious = getRecent(rsiObj.getRsiVal(), 2);

        //get the current and previous sma values
        double smaCurrent = getRecent(rsiObj.getSmaPrice());
        double smaPrevious = getRecent(rsiObj.getSmaPrice(), 2);

        //get the current and previous stock price
        double priceCurrent = getRecent(history, Fields.Close);
        double pricePrevious = getRecent(history, Fields.Close, 2);

        if (rsiCurrent > maxRSI && smaPrevious < pricePrevious && smaCurrent > priceCurrent) {

            //if we are above the rsi resistance line and the stock price crosses below the sma average
            agent.setReasonSell(ReasonSell.Reason_Strategy);

        } else if (smaCurrent > priceCurrent && rsiPrevious < maxRSI && rsiCurrent > maxRSI) {

            //if the stock price is below the sma average and the rsi crosses above the resistance line
            agent.setReasonSell(ReasonSell.Reason_Strategy);
        }

        //display our data
        displayData(agent, agent.getReasonSell() != null);
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display the information
        rsiObj.displayData(agent, write);
    }

    @Override
    public void calculate(List<Period> history) {

        //perform our calculations
        rsiObj.calculate(history);
    }
}