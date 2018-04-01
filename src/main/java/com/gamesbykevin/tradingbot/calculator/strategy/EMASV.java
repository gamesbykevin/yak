package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.Period.Fields;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;

import java.util.ArrayList;
import java.util.List;

import static com.gamesbykevin.tradingbot.calculator.CalculatorHelper.hasCrossover;
import static com.gamesbykevin.tradingbot.calculator.strategy.SMA.calculateSMA;

/**
 * Exponential Moving Average / Simple Moving Average / Volume
 */
public class EMASV extends Strategy {

    //our ema object
    private EMA emaObj;

    //our list of sma price values
    private List<Double> smaPrice;

    //our list of sma volume values
    private List<Double> smaVolume;

    //our list of variations
    protected static int[] LIST_PERIODS_EMA_LONG = {26, 35};
    protected static int[] LIST_PERIODS_EMA_SHORT = {12, 5};
    protected static int[] LIST_PERIODS_SMA_PRICE = {50, 150};
    protected static int[] LIST_PERIODS_SMA_VOLUME = {50, 200};

    //list of configurable values
    protected static int PERIODS_EMA_SHORT = 12;
    protected static int PERIODS_SMA_PRICE = 50;
    protected static int PERIODS_SMA_VOLUME = 60;

    public EMASV() {

        //call parent
        super();

        //create new object(s)
        this.emaObj = new EMA();
        this.smaPrice = new ArrayList<>();
        this.smaVolume = new ArrayList<>();
    }

    public List<Double> getSmaPrice() {
        return this.smaPrice;
    }

    public List<Double> getSmaVolume() {
        return this.smaVolume;
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //get the previous sma value
        double previous = getRecent(getSmaPrice(), PERIODS_EMA_SHORT);

        //get the current sma value
        double current = getRecent(getSmaPrice());

        //make sure the sma is trading higher than previous
        if (previous < current) {

            //we also want the current volume to be larger than our sma volume
            if (getRecent(history, Fields.Volume) > getRecent(getSmaVolume())) {

                //then if we have crossover let's buy
                if (hasCrossover(true, emaObj.getEmaShort(), emaObj.getEmaLong()))
                    agent.setBuy(true);
            }
        }

        //display our data
        displayData(agent, agent.hasBuy());
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //get the previous sma value
        double previous = getRecent(getSmaPrice(), PERIODS_EMA_SHORT);

        //get the current sma value
        double current = getRecent(getSmaPrice());

        //make sure the sma is trading lower than previous
        if (current < previous) {

            //we also want the current volume to be larger than our sma volume
            if (getRecent(history, Fields.Volume) > getRecent(getSmaVolume())) {

                //then if we have crossover let's sell
                if (hasCrossover(false, emaObj.getEmaShort(), emaObj.getEmaLong()))
                    agent.setReasonSell(ReasonSell.Reason_Strategy);
            }
        }

        //display our data
        displayData(agent, agent.getReasonSell() != null);
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display the information
        this.emaObj.displayData(agent, write);
        display(agent, "SMA Price: ", getSmaPrice(), write);
        display(agent, "SMA Volume: ", getSmaVolume(), write);
    }

    @Override
    public void calculate(List<Period> history) {

        //do our calculations
        this.emaObj.calculate(history);

        //calculate our sma price
        calculateSMA(history, getSmaPrice(), PERIODS_SMA_PRICE, Fields.Close);

        //calculate our sma volume
        calculateSMA(history, getSmaVolume(), PERIODS_SMA_VOLUME, Fields.Volume);
    }
}