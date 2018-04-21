package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;

import java.util.ArrayList;
import java.util.List;

import static com.gamesbykevin.tradingbot.calculator.CalculatorHelper.hasCrossover;

/**
 * Moving average ribbon strategy
 */
public class MARS extends Strategy {

    //our list of ema values
    private List<List<Double>> emas;

    //our multiple periods in ascending order
    private static final int[] PERIODS = {3, 5, 7, 10, 12, 14, 16};

    public MARS() {

        //create new list(s)
        this.emas = new ArrayList<>();

        //add a new array list for each period
        for (int i = 0; i < PERIODS.length; i++) {
            this.emas.add(new ArrayList<>());
        }
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //is there an upward trend
        boolean trend = true;

        //make sure each ema has a crossover
        for (int i = 0; i < PERIODS.length - 1; i++) {

            //get the short and fast ema
            double fast = getRecent(emas.get(i));
            double slow = getRecent(emas.get(i + 1));

            //if the fast is less this isn't an upward trend
            if (fast < slow) {
                trend = false;
                break;
            }
        }

        //if there is a trend, check when the longest emas cross over since that would happen last
        if (trend && hasCrossover(true, emas.get(PERIODS.length - 2), emas.get(PERIODS.length - 1)))
            agent.setBuy(true);

        //display our data for what it is worth
        displayData(agent, agent.hasBuy());
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //is there a downward trend
        boolean trend = true;

        //check half the periods when selling
        for (int i = 0; i < (PERIODS.length / 2); i++) {

            //get the short and fast ema
            double fast = getRecent(emas.get(i));
            double slow = getRecent(emas.get(i + 1));

            //if the fast is more, the trend hasn't gone downward (yet)
            if (fast > slow) {
                trend = false;
                break;
            }
        }

        //if we confirm the data is heading downward, sell
        if (trend)
            agent.setReasonSell(ReasonSell.Reason_Strategy);

        //display our data for what it is worth
        displayData(agent, agent.getReasonSell() != null);
    }

    @Override
    protected void displayData(Agent agent, boolean write) {

        //display values
        for (int i = 0; i < emas.size(); i++) {
            display(agent, "EMA (" + PERIODS[i] + ") : ", emas.get(i), write);
        }
    }

    @Override
    public void calculate(List<Period> history) {

        //calculate the different ema values
        for (int i = 0; i < emas.size(); i++) {
            EMA.calculateEMA(history, emas.get(i), PERIODS[i]);
        }
    }
}