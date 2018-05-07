package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.EMA;
import com.gamesbykevin.tradingbot.calculator.indicator.momentun.RSI;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;

import java.util.List;

import static com.gamesbykevin.tradingbot.agent.AgentManagerHelper.displayMessage;
import static com.gamesbykevin.tradingbot.calculator.CalculatorHelper.hasCrossover;

/**
 * EMA / RSI
 */
public class EMAR extends Strategy {

    //our ema object reference
    private EMA emaObj;

    //our rsi object reference
    private RSI rsiObj;

    //list of configurable values
    private static final int PERIODS_EMA_LONG = 12;
    private static final int PERIODS_EMA_SHORT = 5;
    private static final int PERIODS_RSI = 21;
    private static final float RSI_LINE = 50.0f;

    private final float rsiLine;

    public EMAR() {
        this(PERIODS_EMA_LONG, PERIODS_EMA_SHORT, PERIODS_RSI, RSI_LINE);
    }

    public EMAR(int emaLong, int emaShort, int periodsRSI, float rsiLine) {

        this.rsiLine = rsiLine;

        //create our objects
        this.emaObj = new EMA(emaLong, emaShort);
        this.rsiObj = new RSI(periodsRSI);
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //if rsi is over the line and we have a bullish crossover
        if (getRecent(rsiObj.getRsiVal()) > rsiLine && hasCrossover(true, emaObj.getEmaShort(), emaObj.getEmaLong()))
            agent.setBuy(true);

        //display our data
        displayData(agent, agent.hasBuy());
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //recent values
        double current = getRecent(rsiObj.getRsiVal());
        double emaShort = getRecent(emaObj.getEmaShort());
        double emaLong = getRecent(emaObj.getEmaLong());

        //if rsi is under the line and the fast line is below the slow long indicating a downward trend
        if (current < rsiLine && emaShort < emaLong)
            agent.setReasonSell(ReasonSell.Reason_Strategy);

        //adjust our hard stop price to protect our investment
        if (emaShort < emaLong || current < rsiLine)
            adjustHardStopPrice(agent, currentPrice);

        //display our data
        displayData(agent, agent.getReasonSell() != null);
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display our information
        rsiObj.displayData(agent, write);
        emaObj.displayData(agent, write);
    }

    @Override
    public void calculate(List<Period> history) {

        //calculate
        rsiObj.calculate(history);
        emaObj.calculate(history);
    }
}