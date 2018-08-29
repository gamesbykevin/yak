package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.EMA;

import java.util.List;

/**
 * Moving average ribbon strategy
 */
public class MARS extends Strategy {

    //our multiple periods in ascending order
    private static final int[] PERIODS = {10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 110, 120, 130, 140, 150, 200};

    //how we will access our objects, these values will change
    private int[] INDEXES = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};

    //how many periods do we confirm for support resistance
    private static final int PERIODS_CONFIRM = 20;

    //where is the support and resistance
    private int indexSupport = -1, indexResistance = -1;

    public MARS() {

        //call parent
        super(Key.MARS);

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
    public boolean hasBuySignal(Agent agent, List<Period> history, double currentPrice) {

        indexSupport = -1;
        indexResistance = -1;

        //get the short and fast ema
        double fastest = getRecent((EMA)getIndicator(INDEXES[0]));
        double slowest = getRecent((EMA)getIndicator(INDEXES[INDEXES.length - 1]));

        //determine what is the support / resistance line
        if (fastest > slowest) {

            //where is the support line at
            for (int index = INDEXES.length - 1; index >= 0; index --) {

                //were we successful testing the support line
                boolean success = true;

                //let's confirm we reach support
                for (int i = 1; i <= PERIODS_CONFIRM; i++) {

                    //get the current period
                    Period current = history.get(history.size() - i);

                    //get the current ema value
                    double ema = getRecent((EMA)getIndicator(INDEXES[index]), i);

                    //if the current periods low is below the ema, it broke support
                    if (current.low < ema) {
                        success = false;
                        break;
                    }
                }

                if (success) {

                    //if successful, update our index support line
                    indexSupport = index;

                } else {

                    //if not successful, no need to continue
                    break;
                }
            }

        } else {

        }

        /*
        //let's confirm everything is down so we increase our chances buying at the dip (aka support line)
        for (int i = 0; i < PERIODS.length - 1; i++) {

            //get the short and fast ema
            double fast = getRecent((EMA)getIndicator(INDEXES[i]));
            double slow = getRecent((EMA)getIndicator(INDEXES[i + 1]));

            //if the fast is greater than the slow we don't want to buy yet
            if (fast > slow)
                return false;
        }
        */

        //no signal
        return false;
    }

    @Override
    public boolean hasSellSignal(Agent agent, List<Period> history, double currentPrice) {

        /*
        //if half cross above let's assume this is as good as it gets
        for (int i = 0; i < (PERIODS.length / 2); i++) {

            //get the short and fast ema
            double fast = getRecent((EMA)getIndicator(INDEXES[i]));
            double slow = getRecent((EMA)getIndicator(INDEXES[i + 1]));

            //if the fast is less then it hasn't peaked to our liking
            if (fast < slow)
                return false;
        }
        */

        //no signal
        return false;
    }
}