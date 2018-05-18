package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.EMA;
import com.gamesbykevin.tradingbot.calculator.indicator.momentun.RSI;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;

import java.util.List;

import static com.gamesbykevin.tradingbot.agent.AgentManagerHelper.displayMessage;
import static com.gamesbykevin.tradingbot.calculator.utils.CalculatorHelper.hasCrossover;

/**
 * Exponential Moving Average / Relative Strength Index
 */
public class EMAR extends Strategy {

    //how to access our indicator objects
    private static int INDEX_EMA_LONG;
    private static int INDEX_EMA_SHORT;
    private static int INDEX_RSI;

    //list of configurable values
    private static final int PERIODS_EMA_LONG = 12;
    private static final int PERIODS_EMA_SHORT = 5;
    private static final int PERIODS_RSI = 21;
    private static final float RSI_LINE = 50.0f;

    //our rsi trend line
    private final float rsiLine;

    public EMAR() {
        this(PERIODS_EMA_LONG, PERIODS_EMA_SHORT, PERIODS_RSI, RSI_LINE);
    }

    public EMAR(int emaLong, int emaShort, int periodsRSI, float rsiLine) {

        //save our rsi line
        this.rsiLine = rsiLine;

        //add our indicators
        INDEX_EMA_SHORT = addIndicator(new EMA(emaShort));
        INDEX_EMA_LONG = addIndicator(new EMA(emaLong));
        INDEX_RSI = addIndicator(new RSI(periodsRSI));
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        RSI rsiObj = (RSI)getIndicator(INDEX_RSI);
        EMA emaShortObj = (EMA)getIndicator(INDEX_EMA_SHORT);
        EMA emaLongObj = (EMA)getIndicator(INDEX_EMA_LONG);

        //if rsi is over the line and we have a bullish crossover
        if (getRecent(rsiObj.getValueRSI()) > rsiLine && hasCrossover(true, emaShortObj.getEma(), emaLongObj.getEma()))
            agent.setBuy(true);
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        RSI rsiObj = (RSI)getIndicator(INDEX_RSI);
        EMA emaShortObj = (EMA)getIndicator(INDEX_EMA_SHORT);
        EMA emaLongObj = (EMA)getIndicator(INDEX_EMA_LONG);

        //recent values
        double current = getRecent(rsiObj.getValueRSI());
        double emaShort = getRecent(emaShortObj.getEma());
        double emaLong = getRecent(emaLongObj.getEma());

        //if rsi is under the line and the fast line is below the slow long indicating a downward trend
        if (current < rsiLine && emaShort < emaLong)
            agent.setReasonSell(ReasonSell.Reason_Strategy);

        //adjust our hard stop price to protect our investment
        if (emaShort < emaLong || current < rsiLine)
            adjustHardStopPrice(agent, currentPrice);
    }
}