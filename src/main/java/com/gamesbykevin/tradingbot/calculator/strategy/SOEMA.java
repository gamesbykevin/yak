package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;

import java.util.List;

import static com.gamesbykevin.tradingbot.calculator.CalculatorHelper.hasCrossover;

/**
 * stochastic oscillator and ema
 */
public class SOEMA extends Strategy {

    //our reference object(s)
    private EMA emaObj;
    private SO soObj;

    /**
     * Number of periods used to calculate ema short
     */
    private static final int PERIODS_EMA_SHORT = 2;

    /**
     * Number of periods used to calculate ema long
     */
    private static final int PERIODS_EMA_LONG = 4;

    /**
     * Additional indicator value we use for trading
     */
    private static final int SO_INDICATOR = 50;

    /**
     * Number of periods for stochastic oscillator
     */
    private static final int PERIODS_SO = 5;

    /**
     * Number of periods we calculate sma so to get our indicator
     */
    private static final int PERIODS_SMA_SO = 3;

    /**
     * Number of long periods
     */
    private static final int PERIODS_LONG = 100;


    public SOEMA() {

        //use default value
        super(0);

        //create new object
        this.emaObj = new EMA(PERIODS_EMA_LONG, PERIODS_EMA_SHORT);
        this.soObj = new SO(PERIODS_SMA_SO, PERIODS_SO, PERIODS_LONG, PERIODS_LONG);
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        double previousSO = getRecent(soObj.getStochasticOscillator(), 2);
        double currentSO = getRecent(soObj.getStochasticOscillator());

        if (currentSO < SO_INDICATOR && hasCrossover(true, emaObj.getEmaShort(), emaObj.getEmaLong())) {

            //make sure we are below the indicator and we have a bullish crossover
            agent.setBuy(true);

        } else if (previousSO > SO_INDICATOR && currentSO < SO_INDICATOR && getRecent(emaObj.getEmaShort()) > getRecent(emaObj.getEmaLong())) {

            //confirm we just crossed below the indicator and the ema short is > ema long
            agent.setBuy(true);
        }

        //display our data
        displayData(agent, agent.hasBuy());
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        double previousSO = getRecent(soObj.getStochasticOscillator(), 2);
        double currentSO = getRecent(soObj.getStochasticOscillator());

        if (currentSO > SO_INDICATOR && hasCrossover(false, emaObj.getEmaShort(), emaObj.getEmaLong())) {

            //make sure we are above the indicator and we have a bearish crossover
            agent.setReasonSell(ReasonSell.Reason_Strategy);

        } else if (previousSO < SO_INDICATOR && currentSO > SO_INDICATOR && getRecent(emaObj.getEmaShort()) < getRecent(emaObj.getEmaLong())) {

            //confirm we just crossed above the indicator and the ema short is < ema long
            agent.setReasonSell(ReasonSell.Reason_Strategy);
        }

        //display our data
        displayData(agent, agent.getReasonSell() != null);
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display the information
        this.soObj.displayData(agent, write);
        this.emaObj.displayData(agent, write);
    }

    @Override
    public void calculate(List<Period> history) {

        //calculate our value(s)
        this.soObj.calculate(history);
        this.emaObj.calculate(history);
    }
}