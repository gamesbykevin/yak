package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.Period.PeriodField;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonBuy;
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

    /**
     * What is the size of our periods
     */
    public static final int PERIODS_EMA_LONG = 35;
    public static final int PERIODS_EMA_SHORT = 5;
    public static final int PERIODS_SMA_TREND = 150;
    public static final int PERIODS_SMA_VOLUME = 200;

    //what are our periods
    private final int periodsEmaLong, periodsEmaShort, periodsSmaTrend, periodsSmaVolume;

    public EMASV(int periodsEmaLong, int periodsEmaShort, int periodsSmaTrend, int periodsSmaVolume) {

        //call parent with default
        super(0);

        //store our settings
        this.periodsEmaLong = periodsEmaLong;
        this.periodsEmaShort = periodsEmaShort;
        this.periodsSmaTrend = periodsSmaTrend;
        this.periodsSmaVolume = periodsSmaVolume;

        //create new object(s)
        this.emaObj = new EMA(this.periodsEmaLong, this.periodsEmaShort);
        this.smaPrice = new ArrayList<>();
        this.smaVolume = new ArrayList<>();
    }

    public EMASV() {
        this(PERIODS_EMA_LONG, PERIODS_EMA_SHORT, PERIODS_SMA_TREND, PERIODS_SMA_VOLUME);
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
        double previous = getRecent(getSmaPrice(), periodsEmaShort);

        //get the current sma value
        double current = getRecent(getSmaPrice());

        //make sure the sma is trading higher than previous
        if (previous < current) {

            //we also want the current volume to be larger than our sma volume
            if (history.get(history.size() - 1).volume > getRecent(getSmaVolume())) {

                //then if we have crossover let's buy
                if (hasCrossover(true, emaObj.getEmaShort(), emaObj.getEmaLong()))
                    agent.setReasonBuy(ReasonBuy.Reason_20);
            }
        }

        //display our data
        displayData(agent, agent.getReasonBuy() != null);
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //get the previous sma value
        double previous = getRecent(getSmaPrice(), periodsEmaShort);

        //get the current sma value
        double current = getRecent(getSmaPrice());

        //make sure the sma is trading lower than previous
        if (current < previous) {

            //we also want the current volume to be larger than our sma volume
            if (history.get(history.size() - 1).volume > getRecent(getSmaVolume())) {

                //then if we have crossover let's sell
                if (hasCrossover(false, emaObj.getEmaShort(), emaObj.getEmaLong()))
                    agent.setReasonSell(ReasonSell.Reason_21);
            }
        }

        //display our data
        displayData(agent, agent.getReasonSell() != null);
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display the information
        this.emaObj.displayData(agent, write);
        display(agent, "SMA Price: ", getSmaPrice(), periodsEmaShort, write);
        display(agent, "SMA Volume: ", getSmaVolume(), periodsEmaShort, write);
    }

    @Override
    public void calculate(List<Period> history) {

        //do our calculations
        this.emaObj.calculate(history);

        //calculate our sma price
        calculateSMA(history, getSmaPrice(), periodsSmaTrend, PeriodField.Close);

        //calculate our sma volume
        calculateSMA(history, getSmaVolume(), periodsSmaVolume, PeriodField.Volume);
    }
}