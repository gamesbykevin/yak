package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.Period.Fields;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;

import java.util.List;

import static com.gamesbykevin.tradingbot.calculator.CalculatorHelper.hasCrossover;

/**
 * EMA / RSI
 */
public class EMAR extends Strategy {

    //our ema object reference
    private EMA emaObj;

    //our rsi object reference
    private RSI rsiObj;

    /**
     * How many periods to calculate long ema
     */
    private static final int PERIODS_EMA_LONG = 12;

    /**
     * How many periods to calculate short ema
     */
    private static final int PERIODS_EMA_SHORT = 5;

    /**
     * How many periods to calculate rsi
     */
    private static final int PERIODS_RSI = 21;

    /**
     * What is our rsi line to detect bullish / bearish trends
     */
    private static final float RSI_LINE = 50.0f;


    public EMAR(int periodsLong, int periodsShort, int periodsRSI) {

        //call parent
        super(0);

        //create our objects
        this.emaObj = new EMA(periodsLong, periodsShort);
        this.rsiObj = new RSI(periodsRSI, 1, RSI_LINE, RSI_LINE);
    }

    public EMAR() {
        this(PERIODS_EMA_LONG, PERIODS_EMA_SHORT, PERIODS_RSI);
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //RSI values
        double current = getRecent(rsiObj.getRsiVal());
        double previous = getRecent(rsiObj.getRsiVal(), 2);

        if (current > RSI_LINE && hasCrossover(true, emaObj.getEmaShort(), emaObj.getEmaLong())) {

            //if rsi is over the line and we have a bullish crossover
            agent.setBuy(true);

        } else if (previous < RSI_LINE && current > RSI_LINE && getRecent(emaObj.getEmaShort()) > getRecent(emaObj.getEmaLong())) {

            //if rsi JUST went over and ema short > ema long without checking for a crossover
            agent.setBuy(true);
        }

        //display our data
        displayData(agent, agent.hasBuy());
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        /*
        //RSI values
        double current = getRecent(rsiObj.getRsiVal());
        double previous = getRecent(rsiObj.getRsiVal(), 2);

        if (current < RSI_LINE && hasCrossover(false, emaObj.getEmaShort(), emaObj.getEmaLong())) {

            //if rsi is under the line and we have a bearish crossover
            agent.setReasonSell(ReasonSell.Reason_Strategy);

        } else if (previous > RSI_LINE && current < RSI_LINE && getRecent(emaObj.getEmaShort()) < getRecent(emaObj.getEmaLong())) {

            //if rsi JUST went under and ema short < ema long without checking for a crossover
            agent.setReasonSell(ReasonSell.Reason_Strategy);
        }
        */

        //if we have bearish crossover
        if (hasCrossover(false, emaObj.getEmaShort(), emaObj.getEmaLong()))
            agent.setReasonSell(ReasonSell.Reason_Strategy);


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