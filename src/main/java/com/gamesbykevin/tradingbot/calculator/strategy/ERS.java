package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.indicator.momentun.RSI;
import com.gamesbykevin.tradingbot.calculator.indicator.momentun.SO;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.EMA;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;

import java.util.List;

/**
 * Exponential Moving Average / Relative Strength Index / Stochastic Oscillator
 */
public class ERS extends Strategy {

    //our indicator objects
    private SO objSO;
    private EMA objShortEMA;
    private EMA objLongEMA;
    private RSI objRSI;

    //configurable values
    private static final int PERIODS_EMA_SHORT = 5;
    private static final int PERIODS_EMA_LONG = 10;
    private static final int PERIODS_RSI = 14;
    private static final float OVERBOUGHT = 75.0f;
    private static final float OVERSOLD = 25.0f;
    private static final float RSI_LINE = 50.0f;
    private static final int PERIODS_SO_MARKET_RATE = 14;
    private static final int PERIODS_SO_MARKET_RATE_SMA = 3;
    private static final int PERIODS_SO_STOCHASTIC_SMA = 3;

    public ERS() {

        //create our indicator objects
        this.objSO = new SO(PERIODS_SO_MARKET_RATE, PERIODS_SO_MARKET_RATE_SMA, PERIODS_SO_STOCHASTIC_SMA);
        this.objShortEMA = new EMA(PERIODS_EMA_SHORT);
        this.objLongEMA = new EMA(PERIODS_EMA_LONG);
        this.objRSI = new RSI(PERIODS_RSI);
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //the short period has crossed the long period
        if (getRecent(objShortEMA.getEma()) > getRecent(objLongEMA.getEma())) {

            //rsi is above 50
            if (getRecent(objRSI.getRsiVal()) > RSI_LINE) {

                //get the current market rate and stochastic values
                double marketRate = getRecent(objSO.getMarketRateFull());
                double stochastic = getRecent(objSO.getStochasticOscillator());

                //make sure stochastic lines are below overbought and above oversold
                if (marketRate < OVERBOUGHT && marketRate > OVERSOLD &&
                    stochastic < OVERBOUGHT && stochastic > OVERSOLD) {

                    //make sure both stochastic and market rate values are increasing
                    if (marketRate > getRecent(objSO.getMarketRateFull(), 2) &&
                        stochastic > getRecent(objSO.getStochasticOscillator(), 2)) {
                        agent.setBuy(true);
                    }
                }
            }
        }

        //display our data
        displayData(agent, agent.hasBuy());
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //if the fast went below the long
        if (getRecent(objShortEMA.getEma()) < getRecent(objLongEMA.getEma())) {

            //protect our investment
            adjustHardStopPrice(agent, currentPrice);

            //if rsi is lower then we have confirmation to sell
            if (getRecent(objRSI.getRsiVal()) < RSI_LINE)
                agent.setReasonSell(ReasonSell.Reason_Strategy);
        }

        //display our data
        displayData(agent, agent.getReasonSell() != null);
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display info
        objSO.displayData(agent, write);
        objShortEMA.displayData(agent, write);
        objLongEMA.displayData(agent, write);
        objRSI.displayData(agent, write);
    }

    @Override
    public void calculate(List<Period> history, int newPeriods) {

        //perform calculations
        objSO.calculate(history, newPeriods);
        objShortEMA.calculate(history, newPeriods);
        objLongEMA.calculate(history, newPeriods);
        objRSI.calculate(history, newPeriods);
    }

    @Override
    public void cleanup() {
        objSO.cleanup();
        objShortEMA.cleanup();
        objLongEMA.cleanup();
        objRSI.cleanup();
    }
}