package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.EMA;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;

import java.util.List;

import static com.gamesbykevin.tradingbot.calculator.strategy.StrategyHelper.hasCrossover;

/**
 * Moving average ribbon strategy
 */
public class MARS extends Strategy {

    //our multiple periods in ascending order
    private static final int[] PERIODS = {10, 20, 30, 40, 50, 60, 70, 80};

    //how we will access our objects
    private int[] INDEXES = {1, 2, 3, 4, 5, 6, 7, 8};

    public MARS() {

        //first we sort the periods in ascending order
        sortPeriods();

        //then we add our indicators
        for (int i = 0; i < PERIODS.length; i++) {
            INDEXES[i] = addIndicator(new EMA(PERIODS[i]));
        }
    }

    /**
     * Sort the periods array to ensure they are in ascending order
     */
    private void sortPeriods() {

        //make sure periods are in ascending order by sorting
        for (int i = 0; i < PERIODS.length; i++) {

            for (int j = i + 1; j < PERIODS.length; j++) {

                //don't check same element (this shouldn't happen)
                if (i == j)
                    continue;

                final int tmp1 = PERIODS[i];
                final int tmp2 = PERIODS[j];

                //the first number should be less
                if (tmp2 < tmp1) {

                    //switch values
                    PERIODS[j] = tmp1;
                    PERIODS[i] = tmp2;
                }
            }
        }
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //is there an upward trend
        boolean trend = true;

        //make sure each ema has a crossover
        for (int i = 0; i < PERIODS.length - 1; i++) {

            //get the short and fast ema
            double fast = getRecent((EMA)getIndicator(INDEXES[i]));
            double slow = getRecent((EMA)getIndicator(INDEXES[i + 1]));

            //if the fast is less this isn't an upward trend
            if (fast < slow) {
                trend = false;
                break;
            }
        }

        //if there is a trend, check when the longest emas cross over since that would happen last
        if (trend && hasCrossover(true, ((EMA)getIndicator(INDEXES[INDEXES.length - 2])).getEma(), ((EMA)getIndicator(INDEXES[INDEXES.length - 1])).getEma()))
            agent.setBuy(true);
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //is there a downward trend
        boolean trend = true;

        //check only some of the periods when selling
        for (int i = 0; i < (PERIODS.length / 2); i++) {

            //get the short and fast ema
            double fast = getRecent((EMA)getIndicator(INDEXES[i]));
            double slow = getRecent((EMA)getIndicator(INDEXES[i + 1]));

            //if the fast is more, the trend hasn't gone downward (yet)
            if (fast > slow) {
                trend = false;
                break;
            }
        }

        //if we confirm the data is heading downward, sell
        if (trend)
            agent.setReasonSell(ReasonSell.Reason_Strategy);

        //adjust our hard stop price to protect our investment checking the first 2 lists
        if (INDEXES.length >= 2 && getRecent((EMA)getIndicator(INDEXES[0])) < getRecent((EMA)getIndicator(INDEXES[1])))
            adjustHardStopPrice(agent, currentPrice);
    }
}