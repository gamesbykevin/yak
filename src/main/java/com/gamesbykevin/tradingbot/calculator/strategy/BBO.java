package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.Period.Fields;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;

import java.util.List;

import static com.gamesbykevin.tradingbot.agent.AgentManagerHelper.displayMessage;

public class BBO extends Strategy {

    //list of configurable values
    protected static int PERIODS = 10;

    //multiplier for standard deviation
    private static final float MULTIPLIER = 2.0f;

    //what is the bollinger band squeeze ratio
    private static final float SQUEEZE_RATIO = .04f;

    //our bollinger bands object
    private BB objBB;

    //our on balance volume
    private OBV objOBV;

    public BBO() {
        this(PERIODS, MULTIPLIER);
    }

    public BBO(int periods, float multiplier) {

        //create our indicator objects
        this.objBB = new BB(periods, multiplier);
        this.objOBV = new OBV();
    }

    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //what is the price percentage
        float percentageCurrent = (float)(getRecent(this.objBB.getWidth()) / getRecent(history, Fields.Close));
        float percentagePrevious = (float)(getRecent(this.objBB.getWidth(), 2) / getRecent(history, Fields.Close, 2));

        //the previous and current volume
        double volumeCurrent = getRecent(this.objOBV.getVolume());
        double volumePrevious = getRecent(this.objOBV.getVolume(), 2);

        //catch when the bb width is narrow and then starts to expand and our volume is increasing
        if (percentagePrevious <= SQUEEZE_RATIO && percentageCurrent > SQUEEZE_RATIO && volumeCurrent > volumePrevious)
            agent.setBuy(true);

        //display our data
        displayMessage(agent, "Curr Volume:" + volumeCurrent, agent.hasBuy());
        displayMessage(agent, "Prev Volume:" + volumePrevious, agent.hasBuy());
        displayMessage(agent, "Prev Price %" + percentagePrevious, agent.hasBuy());
        displayMessage(agent, "Curr Price %" + percentageCurrent, agent.hasBuy());
        displayData(agent, agent.hasBuy());
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //get the current and previous values
        double closeCurr = getRecent(history, Fields.Close);
        double closePrev = getRecent(history, Fields.Close, 2);
        double lowPrev = getRecent(objBB.getLower(), 2);
        double lowCurr = getRecent(objBB.getLower());
        double midPrev = getRecent(objBB.getMiddle(), 2);
        double midCurr = getRecent(objBB.getMiddle());
        double upPrev = getRecent(objBB.getUpper(), 2);
        double upCurr = getRecent(objBB.getUpper());

        //if we fall below the lower, we need to sell
        if (closePrev > lowPrev && closeCurr < lowCurr)
            agent.setReasonSell(ReasonSell.Reason_Strategy);

        //if we were above the middle and just fell below it
        if (closePrev > midPrev && closeCurr < midCurr)
            agent.setReasonSell(ReasonSell.Reason_Strategy);

        //if the current close goes below the upper line, let's sell
        if (closePrev > upPrev && closeCurr < upCurr)
            agent.setReasonSell(ReasonSell.Reason_Strategy);

        //display our data
        displayData(agent, agent.getReasonSell() != null);
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display our data
        this.objBB.displayData(agent, write);
        this.objOBV.displayData(agent, write);
    }

    @Override
    public void calculate(List<Period> history) {

        //do our calculations
        this.objBB.calculate(history);
        this.objOBV.calculate(history);
    }
}