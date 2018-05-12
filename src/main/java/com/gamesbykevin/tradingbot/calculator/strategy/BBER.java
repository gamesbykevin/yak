package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.indicator.volatility.BB;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.EMA;
import com.gamesbykevin.tradingbot.calculator.indicator.momentun.RSI;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;

import java.util.ArrayList;
import java.util.List;

import static com.gamesbykevin.tradingbot.agent.AgentManagerHelper.displayMessage;

/**
 * Bollinger Bands, EMA, & RSI
 */
public class BBER extends Strategy {

    //how to access our indicator objects
    private static int INDEX_EMA_SHORT;
    private static int INDEX_EMA_LONG;
    private static int INDEX_RSI;
    private static int INDEX_BB;

    //our list of variations
    private static final float RSI_LINE = 50.0f;
    private static final int PERIODS_EMA_LONG = 75;
    private static final int PERIODS_EMA_SHORT = 5;
    private static final int PERIODS_RSI = 14;
    private static final int PERIODS_BB = 20;
    private static final float MULTIPLIER_BB = 2.0f;

    //our rsi line
    private final float rsiLine;

    public BBER() {
        this(PERIODS_EMA_LONG, PERIODS_EMA_SHORT, PERIODS_BB, MULTIPLIER_BB, PERIODS_RSI, RSI_LINE);
    }

    public BBER(int periodsEmaLong, int periodsEmaShort, int periodsBB, float multiplierBB, int periodsRSI, float rsiLine) {

        //save our value
        this.rsiLine = rsiLine;

        //add our indicators
        INDEX_EMA_SHORT = addIndicator(new EMA(periodsEmaShort));
        INDEX_EMA_LONG = addIndicator(new EMA(periodsEmaLong));
        INDEX_RSI = addIndicator(new RSI(periodsRSI));
        INDEX_BB = addIndicator(new BB(periodsBB, multiplierBB));
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        EMA emaLongObj = (EMA)getIndicator(INDEX_EMA_LONG);
        BB bbObj = (BB)getIndicator(INDEX_BB);
        RSI rsiObj = (RSI)getIndicator(INDEX_RSI);

        //get the current values
        double close = getRecent(history, Period.Fields.Close);
        double ema = getRecent(emaLongObj.getEma());
        double middle = getRecent(bbObj.getMiddle().getSma());
        double rsi = getRecent(rsiObj.getRsiVal());

        //if the candle closed above our long ema and above the bollinger bands middle line, and our rsi is above the line
        if (close > ema && close > middle && rsi >= RSI_LINE)
                agent.setBuy(true);
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        EMA emaLongObj = (EMA)getIndicator(INDEX_EMA_LONG);
        BB bbObj = (BB)getIndicator(INDEX_BB);
        RSI rsiObj = (RSI)getIndicator(INDEX_RSI);

        //get the current values
        double close = getRecent(history, Period.Fields.Close);
        double ema = getRecent(emaLongObj.getEma());
        double middle = getRecent(bbObj.getMiddle().getSma());
        double rsi = getRecent(rsiObj.getRsiVal());

        //if at least one of our values are below trending set the hard stop $
        if (close < ema || close < middle || rsi <= RSI_LINE)
            adjustHardStopPrice(agent, currentPrice);

        //if the close is below the ema long and bb middle and rsi is heading towards oversold
        if (close < ema && close < middle && rsi <= RSI_LINE)
            agent.setReasonSell(ReasonSell.Reason_Strategy);
    }
}