package com.gamesbykevin.tradingbot.calculator;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonBuy;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;

import java.util.ArrayList;
import java.util.List;

import static com.gamesbykevin.tradingbot.calculator.RSI.calculateRsi;

public class TWO_RSI extends Indicator {

    private List<Double> rsi;

    /**
     * Minimum required rsi value
     */
    public static final double MIN_RSI = 10.0d;

    /**
     * Maximum required rsi value
     */
    public static final double MAX_RSI = 90.0d;

    /**
     * The two rsi will always be 2 periods
     */
    public static final int TWO_RSI = 2;

    public TWO_RSI() {

        //call parent
        super(TWO_RSI);

        //create new list
        this.rsi = new ArrayList<>();
    }

    private List<Double> getRsi() {
        return this.rsi;
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //get the most recent rsi value
        double rsi = getRsi().get(getRsi().size() - 1);

        //if above the max we have a buy signal
        if (rsi > MAX_RSI)
            agent.setReasonBuy(ReasonBuy.Reason_7);

        //display our data
        displayData(agent, agent.getReasonBuy() != null);
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //get the most recent rsi value
        double rsi = getRsi().get(getRsi().size() - 1);

        //if below the min we have a sell signal
        if (rsi < MIN_RSI)
            agent.setReasonSell(ReasonSell.Reason_10);

        //display our data
        displayData(agent, agent.getReasonSell() != null);
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display the information
        display(agent, "RSI: ", getRsi(), getPeriods(), write);
    }

    @Override
    public void calculate(List<Period> history) {
        calculateRsi(history, getRsi(), getPeriods());
    }
}