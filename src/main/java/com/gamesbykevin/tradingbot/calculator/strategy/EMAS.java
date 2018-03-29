package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.Period.Fields;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;

import java.util.ArrayList;
import java.util.List;

import static com.gamesbykevin.tradingbot.calculator.CalculatorHelper.hasCrossover;
import static com.gamesbykevin.tradingbot.calculator.strategy.SMA.calculateSMA;

public class EMAS extends Strategy {

    //our ema object reference
    private EMA emaObj;

    /**
     * How many periods to calculate long ema
     */
    private static final int PERIODS_EMA_LONG = 26;

    /**
     * How many periods to calculate short ema
     */
    private static final int PERIODS_EMA_SHORT = 12;

    /**
     * How many periods to calculate price SMA
     */
    private static final int PERIODS_PRICE_SMA = 50;

    //list of sma prices
    private List<Double> priceSMA;

    public EMAS() {
        this(PERIODS_EMA_LONG, PERIODS_EMA_SHORT, PERIODS_PRICE_SMA);
    }

    public EMAS(int periodsLong, int periodsShort, int periodsSma) {

        //call parent
        super(periodsSma);

        //create new objects
        this.emaObj = new EMA(periodsLong, periodsShort);
        this.priceSMA = new ArrayList<>();
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //if the fast ema is greater than the simple moving average
        if (getRecent(emaObj.getEmaShort()) > getRecent(priceSMA)) {

            //then if we have crossover let's buy
            if (hasCrossover(true, emaObj.getEmaShort(), emaObj.getEmaLong()))
                agent.setBuy(true);
        }
        //display our data
        displayData(agent, agent.hasBuy());
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //if the fast ema is less than the simple moving average
        if (getRecent(emaObj.getEmaShort()) < getRecent(priceSMA)) {

            //then if we have crossover let's sell
            if (hasCrossover(false, emaObj.getEmaShort(), emaObj.getEmaLong()))
                agent.setReasonSell(ReasonSell.Reason_Strategy);
        }

        //display our data
        displayData(agent, agent.getReasonSell() != null);
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display the information
        this.emaObj.displayData(agent, write);
        display(agent, "SMA Price: ", priceSMA, getPeriods() / 10, write);
    }

    @Override
    public void calculate(List<Period> history) {

        //do our calculations
        this.emaObj.calculate(history);

        //calculate our sma price
        calculateSMA(history, priceSMA, getPeriods(), Fields.Close);
    }
}