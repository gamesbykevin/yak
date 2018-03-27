package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.Period.Fields;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;

import java.util.List;

import static com.gamesbykevin.tradingbot.calculator.CalculatorHelper.hasCrossover;

public class EMAR extends Strategy {

    //our ema object reference
    private EMA emaObj;

    //our rsi object reference
    private RSI rsiObj;

    /**
     * How many periods to calculate long ema
     */
    private static final int PERIODS_EMA_LONG = 26;

    /**
     * How many periods to calculate short ema
     */
    private static final int PERIODS_EMA_SHORT = 12;

    /**
     * How many periods to calculate rsi
     */
    private static final int PERIODS_RSI = 14;

    /**
     * The support line meaning the stock is oversold
     */
    private static final float SUPPORT_LINE = 30.0f;

    /**
     * The resistance line meaning the stock is overbought
     */
    private static final float RESISTANCE_LINE = 70.0f;


    public EMAR(int periodsLong, int periodsShort, int periodsRSI) {

        //call parent
        super(0);

        //create our objects
        this.emaObj = new EMA(periodsLong, periodsShort);
        this.rsiObj = new RSI(periodsRSI);
    }

    public EMAR() {
        this(PERIODS_EMA_LONG, PERIODS_EMA_SHORT, PERIODS_RSI);
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //we want the rsi to be below the support line
        if (getRecent(rsiObj.getRsiVal()) < SUPPORT_LINE) {

            //the current price needs to be above the short ema to show uptrend
            if (getRecent(emaObj.getEmaShort()) < getRecent(history, Fields.Close)) {

                //last we also want a crossover, then we are in a good position to buy
                if (hasCrossover(true, emaObj.getEmaShort(), emaObj.getEmaLong()))
                    agent.setBuy(true);
            }
        }

        //display our data
        displayData(agent, agent.hasBuy());
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //we want the rsi to be above the resistance line
        if (getRecent(rsiObj.getRsiVal()) > RESISTANCE_LINE) {

            //the current price needs to be below the short ema to show downtrend
            if (getRecent(emaObj.getEmaShort()) > getRecent(history, Fields.Close)) {

                //last we also want a crossover, then we are in a good position to sell
                if (hasCrossover(false, emaObj.getEmaShort(), emaObj.getEmaLong()))
                    agent.setReasonSell(ReasonSell.Reason_Strategy);
            }
        }

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