package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.indicator.williams.Alligator;
import com.gamesbykevin.tradingbot.calculator.indicator.williams.Fractal;
import com.gamesbykevin.tradingbot.calculator.indicator.williams.Fractal.Status;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;

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

        //add our indicators
        INDEX_FRACTAL = addIndicator(new Fractal(PERIODS_FRACTAL));
        INDEX_ALLIGATOR = addIndicator(new Alligator(PERIODS_JAW, PERIODS_TEETH, PERIODS_LIPS));
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //get our indicators
        Alligator gator = (Alligator)getIndicator(INDEX_ALLIGATOR);
        Fractal fractal = (Fractal)getIndicator(INDEX_FRACTAL);

        //which fractal should we check
        int indexOffset = (PERIODS_FRACTAL / 2) + 1;

        //get the status of our fractal
        Status status = fractal.getStatusList().get(fractal.getStatusList().size() - indexOffset);

        //make sure we have a buy fractal first
        if (status == Status.Both || status == Status.Buy) {

            //get the alligator teeth value offsetting the index
            double value = gator.getTeeth().getSmma().get(gator.getTeeth().getSmma().size() - PERIODS_TEETH_OFFSET);

            //get the recent period
            Period period = history.get(history.size() - 1);

            //if the $ is above the gator's teeth
            if (period.high > value)
                agent.setBuy(true);
        }
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //get our indicators
        Alligator gator = (Alligator)getIndicator(INDEX_ALLIGATOR);
        Fractal fractal = (Fractal)getIndicator(INDEX_FRACTAL);

        //which fractal should we check
        int indexOffset = (PERIODS_FRACTAL / 2) + 1;

        //get the status of our fractal
        Status status = fractal.getStatusList().get(fractal.getStatusList().size() - indexOffset);

        //make sure we have a sell fractal first
        if (status == Status.Both || status == Status.Sell) {

            //get the alligator teeth value offsetting the index
            double value = gator.getTeeth().getSmma().get(gator.getTeeth().getSmma().size() - PERIODS_TEETH_OFFSET - indexOffset);

            //get the recent period
            Period period = history.get(history.size() - indexOffset);

            //if the $ is below the gator's teeth
            if (period.low < value) {
                agent.setReasonSell(ReasonSell.Reason_Strategy);
                adjustHardStopPrice(agent, currentPrice);
            }
        }
    }
}