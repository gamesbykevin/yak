package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static com.gamesbykevin.tradingbot.calculator.CalculatorHelper.hasCrossover;
import static com.gamesbykevin.tradingbot.calculator.strategy.EMA.calculateEmaList;

/**
 * Negative volume index
 */
public class NVI extends Strategy {

    //our cumulative and ema lists
    private List<Double> nviCumulative, nviEma;

    /**
     * How many periods do we calculate for our ema
     */
    private static final int PERIODS_EMA = 255;

    public NVI() {
        this(PERIODS_EMA);
    }

    public NVI(int periods) {

        //call default value
        super(periods);

        //create new lists
        this.nviCumulative = new ArrayList<>();
        this.nviEma = new ArrayList<>();
    }

    public List<Double> getNviCumulative() {
        return this.nviCumulative;
    }

    public List<Double> getNviEma() {
        return this.nviEma;
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //if we have crossover that is signal to buy
        if (hasCrossover(true, getNviCumulative(), getNviEma()))
            agent.setBuy(true);

        //display our data
        displayData(agent, agent.hasBuy());
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //if we have crossover that is signal to sell
        if (hasCrossover(false, getNviCumulative(), getNviEma()))
            agent.setReasonSell(ReasonSell.Reason_Strategy);

        //display our data
        displayData(agent, agent.getReasonSell() != null);
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display the information
        display(agent, "NVI Cum: ", getNviCumulative(), 5, write);
        display(agent, "NVI Ema: ", getNviEma(), 5, write);
    }

    @Override
    public void calculate(List<Period> history) {

        //clear list
        getNviCumulative().clear();

        //start at 1,000
        double nvi = 1000;

        //check all of our periods
        for (int i = 0; i < history.size(); i++) {

            //we have to check the previous period
            if (i < 1)
                continue;

            //get the current and previous periods
            Period current = history.get(i);
            Period previous = history.get(i - 1);

            //calculate the percent volume change between the periods
            double changeVolume = ((current.volume - previous.volume) / previous.volume) * 100d;

            //calculate the percent price change between the periods
            double changePrice = ((current.close - previous.close) / previous.close) * 100.0d;

            //we only update cumulative nvi if the volume decreases
            if (changeVolume < 0) {
                nvi += changePrice;
            }

            //add the nvi value to our list
            getNviCumulative().add(nvi);
        }

        //now that we have our standard list, let's calculate ema
        calculateEmaList(getNviEma(), getNviCumulative(), PERIODS_EMA);
    }
}