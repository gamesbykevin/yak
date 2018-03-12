package com.gamesbykevin.tradingbot.calculator;

import java.util.List;

public class CalculatorHelper {

    /**
     * Check the history <br>
     * We will do 3 things<br>
     * 1) Add the period if it doesn't exist in the history
     * 2) Sort the list so the most recent period is at the end of the array list
     * 3) Verify the list to make sure the gap between each period matches our duration
     * @param history Our current list of history periods
     * @param period The period we want to check
     * @return true if no issues were found, false if there is a gap between periods
     */
    protected static void checkHistory(List<Period> history, Period period) {

        //did we find an existing period
        boolean match = false;

        //check every period
        for (int i = 0; i < history.size(); i++) {

            //if the time matches it already exists
            if (history.get(i).time == period.time) {

                //flag match
                match = true;

                //exit loop
                break;
            }
        }

        //if there was a match we don't need to add
        if (match)
            return;

        //since it wasn't found, add it to the list
        history.add(period);
    }

    /**
     * Sort the list so the most recent period is at the end of the array list
     * @param history Our current list of history periods
     */
    protected static void sortHistory(List<Period> history) {

        //sort so the periods are in order from oldest to newest
        for (int x = 0; x < history.size(); x++) {
            for (int y = x; y < history.size() - 1; y++) {

                //get the current and next period
                Period tmp1 = history.get(x);
                Period tmp2 = history.get(y + 1);

                //if the next object does not have a greater time, we need to swap
                if (tmp1.time > tmp2.time) {

                    //swap the values
                    history.set(x,     tmp2);
                    history.set(y + 1, tmp1);
                }
            }
        }
    }
}