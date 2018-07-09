package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.indicator.williams.AccelerationDecelerationOscillator;
import com.gamesbykevin.tradingbot.calculator.indicator.williams.Alligator;
import com.gamesbykevin.tradingbot.calculator.indicator.williams.Fractal;
import com.gamesbykevin.tradingbot.calculator.indicator.williams.Fractal.Status;
import com.gamesbykevin.tradingbot.trade.TradeHelper.ReasonSell;

import java.util.List;

import static com.gamesbykevin.tradingbot.calculator.indicator.williams.Alligator.PERIODS_JAW_OFFSET;
import static com.gamesbykevin.tradingbot.calculator.indicator.williams.Alligator.PERIODS_LIPS_OFFSET;
import com.gamesbykevin.tradingbot.calculator.Calculator.Candle;
import java.util.HashMap;

/**
 * Fractals / Acceleration Deceleration Oscillator / Alligator
 */
public class FADOA extends Strategy {

    //configurable values
    private static final int PERIODS_FRACTAL = 5;
    private static final int PERIODS_JAW = 13;
    private static final int PERIODS_TEETH = 8;
    private static final int PERIODS_LIPS = 5;
    private static final int PERIODS_OSCILLATOR_SMA = 5;

    //how to access our indicator objects
    private static int INDEX_FRACTAL;
    private static int INDEX_ALLIGATOR;
    private static int INDEX_OSCILLATOR;

    public FADOA() {

        //call parent
        super(Key.FADOA);

        //add our indicators
        INDEX_FRACTAL = addIndicator(new Fractal(PERIODS_FRACTAL));
        INDEX_ALLIGATOR = addIndicator(new Alligator(PERIODS_JAW, PERIODS_TEETH, PERIODS_LIPS));
        INDEX_OSCILLATOR = addIndicator(new AccelerationDecelerationOscillator(PERIODS_OSCILLATOR_SMA));
    }

    @Override
    public boolean hasBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //get our indicators
        Alligator gator = (Alligator)getIndicator(INDEX_ALLIGATOR);
        Fractal fractal = (Fractal)getIndicator(INDEX_FRACTAL);
        AccelerationDecelerationOscillator oscillator = (AccelerationDecelerationOscillator)getIndicator(INDEX_OSCILLATOR);

        //which fractal should we check
        int indexOffset = (PERIODS_FRACTAL / 2) + 1;

        //get the status of our fractal
        Status status = fractal.getStatusList().get(fractal.getStatusList().size() - indexOffset);

        //make sure we have a buy fractal first
        if (status == Status.Both || status == Status.Buy) {

            //get the alligator teeth value offsetting the index
            double value = gator.getLips().getSmma().get(gator.getLips().getSmma().size() - PERIODS_LIPS_OFFSET);

            //get the recent period
            Period period = history.get(history.size() - 1);

            //if the $ is above the gator's lips
            if (period.close > value) {

                double val1 = oscillator.getOscillator().get(oscillator.getOscillator().size() - 1);
                double val2 = oscillator.getOscillator().get(oscillator.getOscillator().size() - 2);
                double val3 = oscillator.getOscillator().get(oscillator.getOscillator().size() - 3);
                double val4 = oscillator.getOscillator().get(oscillator.getOscillator().size() - 4);

                if (val1 > val2 && val2 > val3 && val1 > 0 ) {
                    //if latest 2 are increasing and the latest is above 0
                    return true;
                } else if (val1 > val2 && val2 > val3 && val3 > val4 && val1 < 0) {
                    //if all are increasing and the latest is below 0
                    return true;
                }
            }
        }

        //no signal
        return false;
    }

    @Override
    public boolean hasSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //get our indicators
        Alligator gator = (Alligator)getIndicator(INDEX_ALLIGATOR);
        Fractal fractal = (Fractal)getIndicator(INDEX_FRACTAL);
        AccelerationDecelerationOscillator oscillator = (AccelerationDecelerationOscillator)getIndicator(INDEX_OSCILLATOR);

        //which fractal should we check
        int indexOffset = (PERIODS_FRACTAL / 2) + 1;

        //get the status of our fractal
        Status status = fractal.getStatusList().get(fractal.getStatusList().size() - indexOffset);

        //make sure we have a sell fractal first
        if (status == Status.Both || status == Status.Sell) {

            //get the alligator jaw value offsetting the index
            double value = gator.getJaw().getSmma().get(gator.getJaw().getSmma().size() - PERIODS_JAW_OFFSET);

            //get the recent period
            Period period = history.get(history.size() - 1);

            //if the $ is below the gator's jaw
            if (period.close < value) {

                double val1 = oscillator.getOscillator().get(oscillator.getOscillator().size() - 1);
                double val2 = oscillator.getOscillator().get(oscillator.getOscillator().size() - 2);
                double val3 = oscillator.getOscillator().get(oscillator.getOscillator().size() - 3);
                double val4 = oscillator.getOscillator().get(oscillator.getOscillator().size() - 4);

                if (val1 < val2 && val2 < val3 && val1 < 0) {

                    //if latest 2 are decreasing and the latest is below 0
                    return true;

                } else if (val1 < val2 && val2 < val3 && val3 < val4 && val1 > 0) {

                    //if all are decreasing and the latest is above 0
                    return true;

                }
            }
        }

        //no signal
        return false;
    }
}