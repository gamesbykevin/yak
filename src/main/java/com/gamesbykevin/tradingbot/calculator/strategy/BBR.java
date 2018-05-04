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
    private static final float SQUEEZE_RATIO = .04f;

    //our rsi signal values
    private static final float RSI_TREND = 50.0f;
    private static final float RSI_OVERBOUGHT = 70.0f;
    private static final float RSI_OVERSOLD = 30.0f;

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
        float percentageCurrent = (float)(getRecent(this.objBB.getWidth()) / getRecent(history, Fields.Close));
        float percentagePrevious = (float)(getRecent(this.objBB.getWidth(), 2) / getRecent(history, Fields.Close, 2));

        //current closing price
        double close = getRecent(history, Fields.Close);

        //first make sure the rsi value is above the trend
        if (getRecent(this.objRSI.getRsiVal()) >= RSI_TREND) {

            /**
             * Catch when the bb width is narrow and then starts to expand and
             * our close price went above the upper band
             */
            if (percentagePrevious <= SQUEEZE_RATIO && percentageCurrent > SQUEEZE_RATIO && close > getRecent(this.objBB.getUpper())
                    ||
                percentageCurrent <= SQUEEZE_RATIO && close > getRecent(this.objBB.getUpper())) {

                //we can buy now
                agent.setBuy(true);

            }
        }

        //display our data
        displayMessage(agent, "RSI Value  :" + getRecent(this.objRSI.getRsiVal()), agent.hasBuy());
        displayMessage(agent, "Curr Close $" + close, agent.hasBuy());
        displayMessage(agent, "Curr Upper :" + getRecent(this.objBB.getUpper()), agent.hasBuy());
        displayMessage(agent, "Prev Price %" + percentagePrevious, agent.hasBuy());
        displayMessage(agent, "Curr Price %" + percentageCurrent, agent.hasBuy());
        displayData(agent, agent.hasBuy());
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //if the rsi is overbought ....
        if (getRecent(this.objRSI.getRsiVal()) >= RSI_OVERBOUGHT) {

            //if the middle band is not up-trending compared to previous we can exit our trade now
            if (getRecent(this.objBB.getMiddle(), 2) > getRecent(this.objBB.getMiddle()))
                agent.setReasonSell(ReasonSell.Reason_Strategy);
        }

        //display our data
        displayMessage(agent, "Curr Mid Val :" + getRecent(this.objBB.getMiddle()), agent.hasBuy());
        displayMessage(agent, "Prev Mid Val :" + getRecent(this.objBB.getMiddle(), 2), agent.hasBuy());
        displayMessage(agent, "RSI Value    :" + getRecent(this.objRSI.getRsiVal()), agent.hasBuy());
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