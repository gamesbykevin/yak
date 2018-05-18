package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.Period.Fields;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.EMA;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.SMA;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;

import java.util.List;

import static com.gamesbykevin.tradingbot.agent.AgentManagerHelper.displayMessage;

/**
 * Exponential Moving Average / Simple Moving Average
 */
public class EMAS extends Strategy {

    //how to access our indicator objects
    private static int INDEX_EMA_SHORT;
    private static int INDEX_EMA_LONG;
    private static int INDEX_SMA_SHORT;
    private static int INDEX_SMA_LONG;

    //our list of variations
    private static final int PERIODS_EMA_LONG = 169;
    private static final int PERIODS_EMA_SHORT = 144;
    private static final int PERIODS_SMA_LONG = 14;
    private static final int PERIODS_SMA_SHORT = 5;

    public EMAS() {
        this(PERIODS_EMA_LONG, PERIODS_EMA_SHORT, PERIODS_SMA_LONG, PERIODS_SMA_SHORT);
    }

    public EMAS(int emaLong, int emaShort, int smaLong, int smaShort) {

        //add our indicators
        INDEX_EMA_SHORT = addIndicator(new EMA(emaShort));
        INDEX_EMA_LONG = addIndicator(new EMA(emaLong));
        INDEX_SMA_SHORT = addIndicator(new SMA(smaShort));
        INDEX_SMA_LONG = addIndicator(new SMA(smaLong));
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        EMA emaShortObj = (EMA)getIndicator(INDEX_EMA_SHORT);
        EMA emaLongObj = (EMA)getIndicator(INDEX_EMA_LONG);
        SMA smaObjShort = (SMA)getIndicator(INDEX_SMA_SHORT);

        //recent closing $
        final double close = getRecent(history, Fields.Close);

        //our previous values
        double prevEmaShort = getRecent(emaShortObj.getEma(), 2);
        double prevEmaLong = getRecent(emaLongObj.getEma(), 2);

        //our current values
        double currEmaShort = getRecent(emaShortObj.getEma());
        double currEmaLong = getRecent(emaLongObj.getEma());
        double currSmaShort = getRecent(smaObjShort.getSma());

        //the short ema needs to cross above the long ema and the close needs to be above the sma
        if (prevEmaShort < prevEmaLong && currEmaShort > currEmaLong && close > currSmaShort)
            agent.setBuy(true);
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        EMA emaShortObj = (EMA)getIndicator(INDEX_EMA_SHORT);
        EMA emaLongObj = (EMA)getIndicator(INDEX_EMA_LONG);
        SMA smaObjShort = (SMA)getIndicator(INDEX_SMA_SHORT);
        SMA smaObjLong = (SMA)getIndicator(INDEX_SMA_LONG);

        //recent closing $
        final double close = getRecent(history, Fields.Close);

        //our current values
        double currEmaShort = getRecent(emaShortObj.getEma());
        double currEmaLong = getRecent(emaLongObj.getEma());
        double currSmaShort = getRecent(smaObjShort.getSma());
        double currSmaLong = getRecent(smaObjLong.getSma());

        //if the close $ is below the long sma we will sell
        if (close < currSmaLong)
            agent.setReasonSell(ReasonSell.Reason_Strategy);

        //adjust our hard stop price to protect our investment
        if (close < currSmaShort || currEmaShort < currEmaLong)
            adjustHardStopPrice(agent, currentPrice);
    }
}