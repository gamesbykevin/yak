package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.indicator.momentun.RSI;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.EMA;

import java.util.List;

/**
 * EMA of 7 period RSI / EMA of 14 period RSI
 */
public class REMA extends Strategy {

    //how to access our indicator objects
    private static int INDEX_RSI_SHORT;
    private static int INDEX_RSI_LONG;

    //configurable values
    private static final int PERIODS_RSI_SHORT = 7;
    private static final int PERIODS_RSI_LONG = 14;
    private static final int PERIODS_EMA = 10;

    //the ema for the short and long rsi
    private EMA emaRsiShort, emaRsiLong;

    public REMA() {

        //call parent
        super(Key.REMA);

        //create indicators
        INDEX_RSI_SHORT = addIndicator(new RSI(PERIODS_RSI_SHORT));
        INDEX_RSI_LONG = addIndicator(new RSI(PERIODS_RSI_LONG));

        //create our ema objects
        this.emaRsiShort = new EMA(PERIODS_EMA);
        this.emaRsiLong = new EMA(PERIODS_EMA);
    }

    @Override
    public boolean hasBuySignal(Agent agent, List<Period> history, double currentPrice) {

        if (getRecent(emaRsiShort) > getRecent(emaRsiLong))
            return true;

        return false;
    }

    @Override
    public boolean hasSellSignal(Agent agent, List<Period> history, double currentPrice) {

        if (getRecent(emaRsiShort) < getRecent(emaRsiLong))
            return true;

        return false;
    }

    @Override
    public void calculate(List<Period> history, int newPeriods) {

        //calculate per usual
        super.calculate(history, newPeriods);

        RSI rsiShort = (RSI)getIndicator(INDEX_RSI_SHORT);
        RSI rsiLong = (RSI)getIndicator(INDEX_RSI_LONG);

        //calculate our custom objects
        this.emaRsiShort.calculateEma(rsiShort.getValueRSI(), newPeriods);
        this.emaRsiLong.calculateEma(rsiLong.getValueRSI(), newPeriods);
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display per usual
        super.displayData(agent, write);

        //display our custom objects
        this.emaRsiShort.displayData(agent, write);
        this.emaRsiLong.displayData(agent, write);
    }

    @Override
    public void cleanup() {

        //cleanup per usual
        super.cleanup();

        //clean up our custom ema objects
        this.emaRsiShort.cleanup();
        this.emaRsiLong.cleanup();
    }
}
