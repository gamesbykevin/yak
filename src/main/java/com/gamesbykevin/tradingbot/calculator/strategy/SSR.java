package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.indicator.momentun.RSI;
import com.gamesbykevin.tradingbot.calculator.indicator.momentun.SO;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.SMA;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;

import java.util.List;

import static com.gamesbykevin.tradingbot.calculator.utils.CalculatorHelper.hasCrossover;

/**
 * Stochastic Oscillator / Simple Moving Average / Relative Strength Index
 */
public class SSR extends Strategy {

    //our indicator objects
    private SO objSO;
    private SMA objSMA;
    private RSI objRSI;

    //configurable values
    private static final int PERIODS_SMA = 150;
    private static final int PERIODS_RSI = 3;
    private static final float OVERBOUGHT = 70.0f;
    private static final float OVERSOLD = 30.0f;
    private static final int PERIODS_SO_MARKET_RATE = 6;
    private static final int PERIODS_SO_MARKET_RATE_SMA = 3;
    private static final int PERIODS_SO_STOCHASTIC_SMA = 3;

    public SSR() {

        //create our indicators
        this.objSO = new SO(PERIODS_SO_MARKET_RATE, PERIODS_SO_MARKET_RATE_SMA, PERIODS_SO_STOCHASTIC_SMA);
        this.objRSI = new RSI(PERIODS_RSI);
        this.objSMA = new SMA(PERIODS_SMA);
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //get the recent period
        Period period = history.get(history.size() - 1);

        //if the close is above the sma we have a bullish trend
        if (period.close > getRecent(objSMA.getSma())) {

            //we also want the rsi and the stochastic oscillator to be over sold
            if (getRecent(objRSI.getRsiVal()) < OVERSOLD && getRecent(objSO.getStochasticOscillator()) < OVERSOLD) {

                //last thing we check is for the stochastic bullish crossover before we buy
                if (hasCrossover(true, objSO.getMarketRateFull(), objSO.getStochasticOscillator()))
                    agent.setBuy(true);
            }
        }

        //display our data
        displayData(agent, agent.hasBuy());
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //get the recent period
        Period period = history.get(history.size() - 1);

        //if the close is below the sma we have a bearish trend
        if (period.close < getRecent(objSMA.getSma()))
            adjustHardStopPrice(agent, currentPrice);

        //if the stock is overbought, adjust our hard stop price and sell
        if (getRecent(objRSI.getRsiVal()) > OVERBOUGHT && getRecent(objSO.getStochasticOscillator()) > OVERBOUGHT) {
            agent.setReasonSell(ReasonSell.Reason_Strategy);
            adjustHardStopPrice(agent, currentPrice);
        }

        //if bearish crossover we go short
        if (hasCrossover(false, objSO.getMarketRateFull(), objSO.getStochasticOscillator()))
            adjustHardStopPrice(agent, currentPrice);

        //display our data
        displayData(agent, agent.getReasonSell() != null);
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display info
        objSO.displayData(agent, write);
        objSMA.displayData(agent, write);
        objRSI.displayData(agent, write);
    }

    @Override
    public void calculate(List<Period> history, int newPeriods) {

        //perform calculations
        objSO.calculate(history, newPeriods);
        objSMA.calculate(history, newPeriods);
        objRSI.calculate(history, newPeriods);
    }

    @Override
    public void cleanup() {
        objSO.cleanup();
        objSMA.cleanup();
        objRSI.cleanup();
    }
}