package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.Period.Fields;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;

import java.util.ArrayList;
import java.util.List;

import static com.gamesbykevin.tradingbot.calculator.strategy.SMA.calculateSMA;

/**
 * Ease of Movement / Simple moving average
 */
public class EMVS extends Strategy {

    //our emv object
    private EMV emvObj;

    //list of prices
    private List<Double> smaPrice;

    //our list of variations
    protected static int[] LIST_PERIODS_SMA = {30};
    protected static int[] LIST_PERIODS_EMV = {14};

    //list of configurable values
    protected static int PERIODS_SMA = 30;

    public EMVS() {

        //call parent
        super();

        //create new object
        this.emvObj = new EMV();
        this.smaPrice = new ArrayList<>();
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //if emv is > 0 and the close is > than sma
        if (getRecent(emvObj.getSmaEmv()) > 0 && getRecent(history, Fields.Close) > getRecent(smaPrice))
            agent.setBuy(true);

        //display our data
        displayData(agent, agent.hasBuy());
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //if emv is < 0 and the close is < than sma
        if (getRecent(emvObj.getSmaEmv()) < 0 && getRecent(history, Fields.Close) < getRecent(smaPrice))
            agent.setReasonSell(ReasonSell.Reason_Strategy);

        //display our data
        displayData(agent, agent.getReasonSell() != null);
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display the information
        emvObj.displayData(agent, write);
        display(agent, "SMA: ", smaPrice, write);
    }

    @Override
    public void calculate(List<Period> history) {

        //do our calculations
        emvObj.calculate(history);
        calculateSMA(history, smaPrice, PERIODS_SMA, Fields.Close);
    }
}