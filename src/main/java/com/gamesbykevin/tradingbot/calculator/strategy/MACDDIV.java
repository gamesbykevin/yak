package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonBuy;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;

import java.util.ArrayList;
import java.util.List;

import static com.gamesbykevin.tradingbot.calculator.CalculatorHelper.hasDivergence;
import static com.gamesbykevin.tradingbot.calculator.strategy.MACD.*;

public class MACDDIV extends Strategy {

    //our macd object
    private MACD macdObj;

    public MACDDIV() {

        //call parent
        super(PERIODS_MACD);

        this.macdObj = new MACD();
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //if bullish divergence, buy
        if (hasDivergence(history, getPeriods(), true, macdObj.getHistogram()))
            agent.setReasonBuy(ReasonBuy.Reason_9);

        //display our data
        displayData(agent, agent.getReasonBuy() != null);
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //if we have a bearish divergence, we expect price to go down
        if (hasDivergence(history, getPeriods(), false, macdObj.getHistogram()))
            agent.setReasonSell(ReasonSell.Reason_10);

        //display our data
        displayData(agent, agent.getReasonSell() != null);
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display the histogram values which we use as a signal
        display(agent, "Histogram: ", macdObj.getHistogram(), getPeriods(), write);

        //display values
        this.macdObj.displayData(agent, write);
    }

    @Override
    public void calculate(List<Period> history) {

        //make macd calculations
        this.macdObj.calculate(history);
    }
}