package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.indicator.williams.AwesomeOscillator;
import com.gamesbykevin.tradingbot.calculator.indicator.williams.Fractal;
import com.gamesbykevin.tradingbot.calculator.indicator.williams.Fractal.Status;
import com.gamesbykevin.tradingbot.trade.TradeHelper.ReasonSell;

import java.util.List;
import com.gamesbykevin.tradingbot.calculator.Calculator.Candle;
import java.util.HashMap;

/**
 * Fractals / Awesome Oscillator
 */
public class FAO extends Strategy {

    //configurable values
    private static final int PERIODS_FRACTAL = 5;
    private static final int PERIODS_AWESOME_LONG = 34;
    private static final int PERIODS_AWESOME_SHORT = 5;

    //how to access our indicator objects
    private static int INDEX_FRACTAL;
    private static int INDEX_AWESOME;

    public FAO() {

        //call parent
        super(Key.FAO);

        //add our indicators
        INDEX_FRACTAL = addIndicator(new Fractal(PERIODS_FRACTAL));
        INDEX_AWESOME = addIndicator(new AwesomeOscillator(PERIODS_AWESOME_SHORT, PERIODS_AWESOME_LONG));
    }

    @Override
    public boolean hasBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //get our indicators
        AwesomeOscillator awesome = (AwesomeOscillator) getIndicator(INDEX_AWESOME);
        Fractal fractal = (Fractal)getIndicator(INDEX_FRACTAL);

        //which fractal should we check
        int indexOffset = (PERIODS_FRACTAL / 2) + 1;

        //get the status of our fractal
        Status status = fractal.getStatusList().get(fractal.getStatusList().size() - indexOffset);

        //make sure we have a buy fractal first
        if (status == Status.Both || status == Fractal.Status.Buy) {

            //check our recent histogram values
            double val1 = awesome.getHistogram().get(awesome.getHistogram().size() - 1);
            double val2 = awesome.getHistogram().get(awesome.getHistogram().size() - 2);
            double val3 = awesome.getHistogram().get(awesome.getHistogram().size() - 3);

            //make sure all histogram values are above 0
            if (val1 > 0 && val2 > 0 && val3 > 0) {

                //val3 > val2 shows falling value, then val1 > val2 shows rising again
                if (val3 > val2 && val1 > val2)
                    return true;
            }
        }

        //no signal
        return false;
    }

    @Override
    public boolean hasSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //get our indicators
        AwesomeOscillator awesome = (AwesomeOscillator) getIndicator(INDEX_AWESOME);
        Fractal fractal = (Fractal)getIndicator(INDEX_FRACTAL);

        //which fractal should we check
        int indexOffset = (PERIODS_FRACTAL / 2) + 1;

        //get the status of our fractal
        Status status = fractal.getStatusList().get(fractal.getStatusList().size() - indexOffset);

        //make sure we have a sell fractal first
        if (status == Status.Both || status == Fractal.Status.Sell) {

            //check our recent histogram values
            double val1 = awesome.getHistogram().get(awesome.getHistogram().size() - 1);
            double val2 = awesome.getHistogram().get(awesome.getHistogram().size() - 2);
            double val3 = awesome.getHistogram().get(awesome.getHistogram().size() - 3);

            //make sure all histogram values are below 0
            if (val1 < 0 && val2 < 0 && val3 < 0) {

                //val3 < val2 which shows rising, then val1 < val2 shows falling again
                if (val3 < val2 && val1 < val2) {
                    return true;
                }
            }
        }

        //no signal
        return false;
    }
}