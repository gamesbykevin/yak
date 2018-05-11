package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.Period.Fields;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.HA;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.SMA;
import com.gamesbykevin.tradingbot.calculator.indicator.momentun.SO;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;

import java.util.ArrayList;
import java.util.List;

/**
 * Heiken Ashi / Stochastic Oscillator
 */
public class HASO extends Strategy {

    //our indicators
    private HA objHA;
    private SO objSO;

    //configurable values
    private static final int PERIODS_MARKET_RATE = 14;
    private static final int PERIODS_MARKET_RATE_SMA = 7;
    private static final int PERIODS_STOCHASTIC_SMA = 3;
    private static final int HA_PERIODS = 10;
    private static final float STOCHASTIC_MAX = 70.0f;
    private static final float STOCHASTIC_MIN = 30.0f;

    public HASO() {
        this(PERIODS_MARKET_RATE, PERIODS_MARKET_RATE_SMA, PERIODS_STOCHASTIC_SMA, HA_PERIODS);
    }

    public HASO(int periodsMarketRate, int periodsMarketRateSma, int periodsStochasticSma, int periodsHa) {

        //create our indicators
        this.objHA = new HA(periodsHa);
        this.objSO = new SO(periodsMarketRate, periodsMarketRateSma, periodsStochasticSma);
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //get the most recent ha candles
        Period curr = objHA.getHaPeriods().get(objHA.getHaPeriods().size() - 1);
        Period prev1 = objHA.getHaPeriods().get(objHA.getHaPeriods().size() - 2);
        Period prev2 = objHA.getHaPeriods().get(objHA.getHaPeriods().size() - 3);

        //if 3 candles ago things were bearish, but the next 2 candles are bullish
        if (objHA.isBearish(prev2) && objHA.isBullish(prev1) && objHA.isBullish(curr)) {

            //if our stochastic indicator is low, let's buy
            if (getRecent(objSO.getStochasticOscillator()) <= STOCHASTIC_MIN)
                agent.setBuy(true);
        }

        //display our data
        displayData(agent, agent.hasBuy());
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //get the most recent ha candles
        Period curr = objHA.getHaPeriods().get(objHA.getHaPeriods().size() - 1);
        Period prev1 = objHA.getHaPeriods().get(objHA.getHaPeriods().size() - 2);

        //if the last 2 candles are bearish
        if (objHA.isBearish(prev1) && objHA.isBearish(curr)) {

            //if our stochastic indicator is high, let's sell
            if (getRecent(objSO.getStochasticOscillator()) >= STOCHASTIC_MAX)
                agent.setReasonSell(ReasonSell.Reason_Strategy);

        }

        //adjust our hard stop price to protect our investment
        if (objHA.isBearish(prev1) || objHA.isBearish(curr))
            adjustHardStopPrice(agent, currentPrice);

        //display our data
        displayData(agent, agent.getReasonSell() != null);
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display our information
        objHA.displayData(agent, write);
        objSO.displayData(agent, write);
    }

    @Override
    public void calculate(List<Period> history, int newPeriods) {

        //calculate
        objHA.calculate(history, newPeriods);
        objSO.calculate(history, newPeriods);
    }
}