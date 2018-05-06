package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.Period.Fields;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;

import java.util.List;

import static com.gamesbykevin.tradingbot.agent.AgentManagerHelper.displayMessage;

public class BBR extends Strategy {

    //list of configurable values
    protected static int PERIODS_BB = 10;
    protected static int PERIODS_RSI = 21;

    //multiplier for standard deviation
    private static final float MULTIPLIER = 2.0f;

    //what is the bollinger band squeeze ratio
    private static final float SQUEEZE_RATIO = .040f;

    //our rsi signal values
    private static final float RSI_TREND = 50.0f;
    private static final float RSI_OVERBOUGHT = 70.0f;

    //our bollinger bands object
    private BB objBB;

    //our rsi
    private RSI objRSI;

    public BBR() {
        this(PERIODS_BB, MULTIPLIER, PERIODS_RSI);
    }

    public BBR(int periodsBB, float multiplier, int periodsRSI) {

        //create our indicator objects
        this.objBB = new BB(periodsBB, multiplier);
        this.objRSI = new RSI(periodsRSI);
    }

    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //what is the price percentage
        float percentage = (float)(getRecent(objBB.getWidth()) / getRecent(history, Fields.Close));

        //current closing price
        final double close = getRecent(history, Fields.Close);

        //current rsi value
        final double rsi = getRecent(objRSI.getRsiVal());

        //current upper band
        final double upper = getRecent(objBB.getUpper());

        //first make sure the rsi value is above the trend
        if (rsi >= RSI_TREND) {

            //if the price is narrow and the close is above our upper band
            if (percentage <= SQUEEZE_RATIO && close > upper)
                agent.setBuy(true);
        }

        //display our data
        displayMessage(agent, "RSI   :" + rsi, agent.hasBuy());
        displayMessage(agent, "Close $" + close, agent.hasBuy());
        displayMessage(agent, "Upper :" + upper, agent.hasBuy());
        displayMessage(agent, "Price %" + percentage, agent.hasBuy());
        displayData(agent, agent.hasBuy());
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //indicator values
        double rsi = getRecent(objRSI.getRsiVal());
        double middleCurr = getRecent(objBB.getMiddle());
        double middlePrev = getRecent(objBB.getMiddle(), 2);
        double close = getRecent(history, Fields.Close);

        //if the rsi is overbought ....
        if (rsi >= RSI_OVERBOUGHT) {

            //if the middle band is not up-trending compared to previous we can exit our trade now
            if (middlePrev > middleCurr)
                agent.setReasonSell(ReasonSell.Reason_Strategy);

            //add the increase
            double increase = (agent.getWallet().getPurchasePrice() * agent.getHardStopRatio()) * .5f;

            //if we are in overbought territory set our hard stop now to protect our profit
            agent.adjustHardStopPrice(currentPrice + increase);
        } else if (rsi < RSI_TREND) {

            //if the rsi is going towards oversold territory, let's see if the close drops below our middle band
            if (close < middleCurr)
                agent.setReasonSell(ReasonSell.Reason_Strategy);
        }

        //display our data
        displayMessage(agent, "Close    $" + close, agent.getReasonSell() != null);
        displayMessage(agent, "Curr Mid :" + middleCurr, agent.getReasonSell() != null);
        displayMessage(agent, "Prev Mid :" + middlePrev, agent.getReasonSell() != null);
        displayMessage(agent, "RSI Val  :" + rsi, agent.getReasonSell() != null);
        displayData(agent, agent.getReasonSell() != null);
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display our data
        this.objBB.displayData(agent, write);
        this.objRSI.displayData(agent, write);
    }

    @Override
    public void calculate(List<Period> history) {

        //do our calculations
        this.objBB.calculate(history);
        this.objRSI.calculate(history);
    }
}