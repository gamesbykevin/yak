package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.Period.Fields;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.SMA;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.SR;

import java.util.List;

import static com.gamesbykevin.tradingbot.agent.AgentManagerHelper.displayMessage;

/**
 * 2 Simple Moving Averages / Support & Resistance
 */
public class SMASR extends Strategy {

    //how to access our indicator objects
    private static int INDEX_SR;
    private static int INDEX_SMA_SHORT;
    private static int INDEX_SMA_LONG;

    //list of configurable values
    private static final int PERIODS_SR = 50;
    private static final int PERIODS_SMA_SHORT = 50;
    private static final int PERIODS_SMA_LONG = 200;

    //save decision
    private boolean boughtLevel1 = false;
    private boolean boughtLevel2 = false;
    private boolean boughtLevel3 = false;

    public SMASR() {

        //call parent
        super(Key.SMASR);

        //add our indicators
        INDEX_SR = addIndicator(new SR());
        INDEX_SMA_SHORT = addIndicator(new SMA(PERIODS_SMA_SHORT));
        INDEX_SMA_LONG = addIndicator(new SMA(PERIODS_SMA_LONG));
    }

    @Override
    public boolean hasBuySignal(Agent agent, List<Period> history, double price) {

        SR sr = (SR)getIndicator(INDEX_SR);
        SMA smaShort = (SMA)getIndicator(INDEX_SMA_SHORT);
        SMA smaLong = (SMA)getIndicator(INDEX_SMA_LONG);

        //let's make sure we are in a bullish trend
        if (getRecent(smaShort.getSma()) > getRecent(smaLong.getSma())) {

            //how many times the support line was not broken
            int level1 = 0;
            int level2 = 0;
            int level3 = 0;

            //let's test the support / resistance to see what level works best for us
            for (int i = PERIODS_SR; i > 0; i--) {

                //get the current period
                Period period = history.get(history.size() - i);

                //get our support lines
                double support1 = getRecent(sr.getSupportLevel1(), i);
                double support2 = getRecent(sr.getSupportLevel2(), i);
                double support3 = getRecent(sr.getSupportLevel3(), i);

                //get our resistance lines
                double resistance1 = getRecent(sr.getResistanceLevel1(), i);
                double resistance2 = getRecent(sr.getResistanceLevel2(), i);
                double resistance3 = getRecent(sr.getResistanceLevel3(), i);

                //test the lines
                if (period.low > support1 && period.high < resistance1)
                    level1++;
                if (period.low > support2 && period.high < resistance2)
                    level2++;
                if (period.low > support3 && period.high < resistance3)
                    level3++;
            }

            //get the most recent period
            Period period = history.get(history.size() - 1);

            //reset to false
            boughtLevel1 = false;
            boughtLevel2 = false;
            boughtLevel3 = false;

            //check the current period low against the most tested line
            if (level1 > level2 && level1 > level3) {

                //if level 1 is the most tested and the low is below support and closes above it, let's buy
                if (period.low < getRecent(sr.getSupportLevel1()) && period.close >= getRecent(sr.getSupportLevel1())) {
                    displayMessage(agent, "Level 1: " + level1 + ", Level 2: " + level2 + ", Level 3: " + level3, true);
                    boughtLevel1 = true;
                    return true;
                }

            } else if (level2 > level1 && level2 > level3) {

                //if level 2 is the most tested and the low is below support and closes above it, let's buy
                if (period.low < getRecent(sr.getSupportLevel2()) && period.close >= getRecent(sr.getSupportLevel2())) {
                    displayMessage(agent, "Level 1: " + level1 + ", Level 2: " + level2 + ", Level 3: " + level3, true);
                    boughtLevel2 = true;
                    return true;
                }

            } else if (level3 > level1 && level3 > level2) {

                //if level 3 is the most tested and the low is below support and closes above it, let's buy
                if (period.low < getRecent(sr.getSupportLevel3()) && period.close >= getRecent(sr.getSupportLevel3())) {
                    displayMessage(agent, "Level 1: " + level1 + ", Level 2: " + level2 + ", Level 3: " + level3, true);
                    boughtLevel3 = true;
                    return true;
                }

            }

        }

        //no signal found yet
        return false;
    }

    @Override
    public boolean hasSellSignal(Agent agent, List<Period> history, double price) {

        SR sr = (SR)getIndicator(INDEX_SR);
        SMA smaShort = (SMA)getIndicator(INDEX_SMA_SHORT);
        SMA smaLong = (SMA)getIndicator(INDEX_SMA_LONG);

        //if a bearish trend, lets sell
        if (getRecent(smaShort.getSma()) < getRecent(smaLong.getSma()))
            return true;

        //get the most recent period
        Period period = history.get(history.size() - 1);

        //check the support and resistance levels
        if (boughtLevel1) {

            //if the candle closed above the resistance line, let's sell
            if (period.close > getRecent(sr.getResistanceLevel1()))
                return true;

            //if the candle high was above, let's adjust our stop
            if (period.high > getRecent(sr.getResistanceLevel1()))
                goShort(agent, getRecent(history, Fields.Low));

            //if the candle closed below the support, we need to sell quickly
            if (period.close < getRecent(sr.getSupportLevel1()))
                return true;

        } else if (boughtLevel2) {

            //if the candle closed above the resistance line, let's sell
            if (period.close > getRecent(sr.getResistanceLevel2()))
                return true;

            //if the candle high was above, let's adjust our stop
            if (period.high > getRecent(sr.getResistanceLevel2()))
                goShort(agent, getRecent(history, Fields.Low));

            //if the candle closed below the support, we need to sell quickly
            if (period.close < getRecent(sr.getSupportLevel2()))
                return true;

        } else if (boughtLevel3) {

            //if the candle closed above the resistance line, let's sell
            if (period.close > getRecent(sr.getResistanceLevel3()))
                return true;

            //if the candle high was above, let's adjust our stop
            if (period.high > getRecent(sr.getResistanceLevel3()))
                goShort(agent, getRecent(history, Fields.Low));

            //if the candle closed below the support, we need to sell quickly
            if (period.close < getRecent(sr.getSupportLevel3()))
                return true;

        }

        //no signal found yet
        return false;
    }
}
