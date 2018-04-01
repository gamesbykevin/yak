package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;

import java.util.ArrayList;
import java.util.List;

import static com.gamesbykevin.tradingbot.calculator.CalculatorHelper.hasCrossover;
import static com.gamesbykevin.tradingbot.calculator.strategy.EMA.calculateEmaList;

/**
 * Positive volume index
 */
public class PVI extends Strategy {

    //our cumulative and ema lists
    private List<Double> pviCumulative, pviEma;

    //our list of variations
    protected static int[] LIST_PERIODS_EMA = {200};

    //list of configurable values
    protected static int PERIODS_EMA = 200;

    public PVI() {

        //call parent
        super();

        //create new lists
        this.pviCumulative = new ArrayList<>();
        this.pviEma = new ArrayList<>();
    }

    public List<Double> getPviCumulative() {
        return this.pviCumulative;
    }

    public List<Double> getPviEma() {
        return this.pviEma;
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //if we have crossover and cumulative value is > than previous period value that is signal to buy
        if (hasCrossover(true, getPviCumulative(), getPviEma()) && getRecent(getPviCumulative()) > getRecent(getPviCumulative(), 2))
            agent.setBuy(true);

        //display our data
        displayData(agent, agent.hasBuy());
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //if we have crossover and cumulative value is < than previous period value that is signal to sell
        if (hasCrossover(false, getPviCumulative(), getPviEma()) && getRecent(getPviCumulative()) < getRecent(getPviCumulative(), 2))
            agent.setReasonSell(ReasonSell.Reason_Strategy);

        //display our data
        displayData(agent, agent.getReasonSell() != null);
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display the information
        display(agent, "PVI Cum: ", getPviCumulative(), write);
        display(agent, "PVI Ema: ", getPviEma(),        write);
    }

    @Override
    public void calculate(List<Period> history) {

        //clear list
        getPviCumulative().clear();

        //start at 1,000
        double pvi = 1000;

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

            //we only update cumulative pvi if the volume increases
            if (changeVolume > 0) {
                pvi += changePrice;
            }

            //add the nvi value to our list
            getPviCumulative().add(pvi);
        }

        //now that we have our standard list, let's calculate ema
        calculateEmaList(getPviEma(), getPviCumulative(), PERIODS_EMA);
    }
}