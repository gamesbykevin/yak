package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.indicator.williams.Alligator;
import com.gamesbykevin.tradingbot.calculator.indicator.williams.Fractal;
import com.gamesbykevin.tradingbot.calculator.indicator.williams.Fractal.Status;
import com.gamesbykevin.tradingbot.trade.TradeHelper.ReasonSell;

import java.util.List;

import static com.gamesbykevin.tradingbot.calculator.indicator.williams.Alligator.PERIODS_TEETH_OFFSET;

/**
 * Fractals / Alligator
 */
public class FA extends Strategy {

    //configurable values
    private static final int PERIODS_FRACTAL = 5;
    private static final int PERIODS_JAW = 13;
    private static final int PERIODS_TEETH = 8;
    private static final int PERIODS_LIPS = 5;

    //how to access our indicator objects
    private static int INDEX_FRACTAL;
    private static int INDEX_ALLIGATOR;

    public FA() {

        //call parent
        super(Key.FA);

        //add our indicators
        INDEX_FRACTAL = addIndicator(new Fractal(PERIODS_FRACTAL));
        INDEX_ALLIGATOR = addIndicator(new Alligator(PERIODS_JAW, PERIODS_TEETH, PERIODS_LIPS));
    }

    @Override
    public boolean hasBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //get our indicators
        Alligator gator = (Alligator)getIndicator(INDEX_ALLIGATOR);
        Fractal fractal = (Fractal)getIndicator(INDEX_FRACTAL);

        //check our recent periods
        for (int index = 0; index < PERIODS_FRACTAL * 2; index++) {

            //which fractal should we check
            int indexOffset = (PERIODS_FRACTAL / 2) + 1 + index;

            //where is our fractal at?
            int fractalIndex = fractal.getStatusList().size() - indexOffset;

            //get the status of our fractal
            Status status = fractal.getStatusList().get(fractalIndex);

            //make sure we have a buy fractal first
            if (status != Status.Both && status != Status.Buy)
                continue;

            //our indexes
            final int alligatorIndex = gator.getTeeth().getSmma().size() - PERIODS_TEETH_OFFSET - indexOffset;
            final int historyIndex = history.size() - indexOffset;

            //get the alligator teeth value at the same periods as the fractal
            double teeth = gator.getTeeth().getSmma().get(alligatorIndex);

            //look at the same period as the fractal
            Period period = history.get(historyIndex);

            //if the $ is below the gator's teeth we don't have a buy opportunity
            if (period.close < teeth)
                continue;

            //we need a candle to break above this high $
            double highLine = period.high;

            //check the next few periods to confirm we are above the alligators teeth
            for (int i = 1; i <= PERIODS_FRACTAL; i++) {

                //if our index is out of bounds we don't have enough information to confirm
                if (alligatorIndex + i >= gator.getTeeth().getSmma().size() || historyIndex + i >= history.size())
                    return false;

                //make sure the close $ stays above the teeth
                double tmpTeeth = gator.getTeeth().getSmma().get(alligatorIndex + i);
                double tmpClose = history.get(historyIndex + i).close;

                //if the close $ goes below the teeth we need to exit our buy trade
                if (tmpTeeth >= tmpClose)
                    return false;
            }

            //check the periods afterwards for a price breakout
            for (int i = historyIndex + PERIODS_FRACTAL; i < history.size(); i++) {

                //if the high exceeds our previous high line it is time to buy
                if (history.get(i).high > highLine)
                    return true;
            }
        }

        //no signal
        return false;
    }

    @Override
    public boolean hasSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //get our indicators
        Alligator gator = (Alligator)getIndicator(INDEX_ALLIGATOR);

        //get our teeth values
        double prevTeeth = gator.getTeeth().getSmma().get(gator.getTeeth().getSmma().size() - 2);;
        double currTeeth = gator.getTeeth().getSmma().get(gator.getTeeth().getSmma().size() - 1);

        //get our jaw values
        double prevJaw = gator.getJaw().getSmma().get(gator.getJaw().getSmma().size() - 2);
        double currJaw = gator.getJaw().getSmma().get(gator.getJaw().getSmma().size() - 1);

        //get our lip values
        double prevLips = gator.getLips().getSmma().get(gator.getLips().getSmma().size() - 2);
        double currLips = gator.getLips().getSmma().get(gator.getLips().getSmma().size() - 1);

        //if any cross happens let's sell
        if (prevTeeth < prevJaw && currTeeth > currJaw) {
            return true;
        } else if (prevJaw < prevTeeth && currJaw > currTeeth) {
            return true;
        } else if (prevTeeth < prevLips && currTeeth > currLips) {
            return true;
        } else if (prevLips < prevTeeth && currLips > currTeeth) {
            return true;
        } else if (prevJaw < prevLips && currJaw > currLips) {
            return true;
        } else if (prevLips < prevJaw && currLips > currJaw) {
            return true;
        }

        //no signal
        return false;
    }
}