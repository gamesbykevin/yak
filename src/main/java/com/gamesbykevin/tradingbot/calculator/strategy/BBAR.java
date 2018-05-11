package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.Period.Fields;
import com.gamesbykevin.tradingbot.calculator.indicator.volume.ADL;
import com.gamesbykevin.tradingbot.calculator.indicator.volatility.BB;
import com.gamesbykevin.tradingbot.calculator.indicator.momentun.RSI;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;

import java.util.List;

import static com.gamesbykevin.tradingbot.agent.AgentManagerHelper.displayMessage;
import static com.gamesbykevin.tradingbot.calculator.CalculatorHelper.hasTrendDownward;
import static com.gamesbykevin.tradingbot.calculator.CalculatorHelper.hasTrendUpward;

/**
 * Bollinger Bands / Accumulation Distribution Line / Relative Strength Index
 */
public class BBAR extends Strategy {

    //list of configurable values
    protected static int PERIODS_BB = 10;
    protected static int PERIODS_RSI = 14;

    //how many periods do we check to confirm trend
    private static final int PERIOD_TREND = 5;

    //multiplier for standard deviation
    private static final float MULTIPLIER = 2.0f;

    //what is the bollinger band squeeze ratio
    private static final float SQUEEZE_RATIO = .040f;

    //our bollinger bands object
    private BB objBB;

    //our relative strength index
    private RSI objRSI;

    //our accumulation distribution line
    private ADL objADL;

    public BBAR() {
        this(PERIODS_BB, MULTIPLIER, PERIODS_RSI);
    }

    public BBAR(int periodsBB, float multiplier, int periodsRSI) {

        //create our indicator objects
        this.objBB = new BB(periodsBB, multiplier);
        this.objRSI = new RSI(periodsRSI);
        this.objADL = new ADL();
    }

    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //what is the price percentage
        float percentage = (float)(getRecent(this.objBB.getWidth()) / getRecent(history, Fields.Close));

        //if the squeeze is on, then let's try to figure out bullish divergence
        if (percentage <= SQUEEZE_RATIO) {

            //make sure both indicators are going up
            if (hasTrendUpward(this.objADL.getVolume(), PERIOD_TREND) &&
                    hasTrendUpward(this.objRSI.getRsiVal(), PERIOD_TREND)) {

                //check that the price is heading down, then we have a bullish divergence
                if (hasTrendDownward(history, Fields.Close, PERIOD_TREND))
                    agent.setBuy(true);
            }
        }

        //display our data
        displayMessage(agent, "Ratio %" + SQUEEZE_RATIO, agent.hasBuy());
        displayMessage(agent, "Price %" + percentage, agent.hasBuy());
        displayData(agent, agent.hasBuy());
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //get the current and previous values
        double closeCurr = getRecent(history, Fields.Close);
        double closePrev = getRecent(history, Fields.Close, 2);
        double lowPrev = getRecent(objBB.getLower(), 2);
        double lowCurr = getRecent(objBB.getLower());
        double midPrev = getRecent(objBB.getMiddle().getSma(), 2);
        double midCurr = getRecent(objBB.getMiddle().getSma());
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

        //adjust our hard stop price to protect our investment
        if (closeCurr < midCurr || closeCurr < lowCurr)
            adjustHardStopPrice(agent, currentPrice);

        //display our data
        displayData(agent, agent.getReasonSell() != null);
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display our data
        this.objBB.displayData(agent, write);
        this.objADL.displayData(agent, write);
        this.objRSI.displayData(agent, write);
    }

    @Override
    public void calculate(List<Period> history, int newPeriods) {

        //do our calculations
        this.objBB.calculate(history, newPeriods);
        this.objADL.calculate(history, newPeriods);
        this.objRSI.calculate(history, newPeriods);
    }
}