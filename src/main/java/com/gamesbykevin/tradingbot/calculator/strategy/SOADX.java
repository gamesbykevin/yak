package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.Period.Fields;
import com.gamesbykevin.tradingbot.calculator.indicator.momentun.SO;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.ADX;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.SMA;
import com.gamesbykevin.tradingbot.trade.TradeHelper.ReasonSell;

import java.util.List;
import com.gamesbykevin.tradingbot.calculator.Calculator.Candle;
import java.util.HashMap;

/**
 * Stochastic Oscillator / Average Directional Index
 */
public class SOADX extends Strategy {

    //how to access our indicator objects
    private static int INDEX_SO_FULL;
    private static int INDEX_SO_SLOW;
    private static int INDEX_ADX;
    private static int INDEX_SMA;

    //list of configurable values
    private static final int PERIODS_SO_FULL_MARKET = 70;
    private static final int PERIODS_SO_FULL_MARKET_SMA = 3;
    private static final int PERIODS_SO_FULL_OSCILLATOR = 3;
    private static final int PERIODS_SO_SLOW_MARKET = 14;
    private static final int PERIODS_SO_SLOW_MARKET_SMA = 1;
    private static final int PERIODS_SO_SLOW_OSCILLATOR = 3;
    private static final int PERIODS_SMA_VOLUME = 250;

    private static final int PERIODS_ADX = 14;
    private static final double OSCILLATOR_TREND = 50.0d;
    private static final double OSCILLATOR_HIGH = 80.0d;
    private static final double OSCILLATOR_LOW = 20.0d;
    private static final double ADX_TREND = 20.0d;


    public SOADX() {

        //call parent
        super(Key.SOADX);

        //add our indicators
        INDEX_SO_FULL = addIndicator(new SO(PERIODS_SO_FULL_MARKET, PERIODS_SO_FULL_MARKET_SMA, PERIODS_SO_FULL_OSCILLATOR));
        INDEX_SO_SLOW = addIndicator(new SO(PERIODS_SO_SLOW_MARKET, PERIODS_SO_SLOW_MARKET_SMA, PERIODS_SO_SLOW_OSCILLATOR));
        INDEX_ADX = addIndicator(new ADX(PERIODS_ADX));
        INDEX_SMA = addIndicator(new SMA(PERIODS_SMA_VOLUME, Fields.Volume));
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //get our indicator objects
        SO full = (SO)getIndicator(INDEX_SO_FULL);
        SO slow = (SO)getIndicator(INDEX_SO_SLOW);
        ADX adx = (ADX)getIndicator(INDEX_ADX);
        SMA sma = (SMA)getIndicator(INDEX_SMA);

        //make sure past couple values are going above the trend
        double slowOscPrev = getRecent(slow.getStochasticOscillator(), 2);
        double slowOscCurr = getRecent(slow.getStochasticOscillator(), 1);
        double fullOscPrev = getRecent(full.getStochasticOscillator(), 2);
        double fullOscCurr = getRecent(full.getStochasticOscillator(), 1);

        //are things looking bullish?
        if (slowOscPrev > OSCILLATOR_TREND && slowOscCurr > OSCILLATOR_HIGH &&
                fullOscPrev > OSCILLATOR_TREND && fullOscCurr > OSCILLATOR_TREND) {

            //if adx is above the trend
            if (getRecent(adx.getAdx()) > ADX_TREND) {

                //if the volume is above the sma average let's buy
                if (getRecent(history, Fields.Volume) > getRecent(sma.getSma()))
                    agent.setBuy(true);
            }
        }
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //get our indicator objects
        SO full = (SO)getIndicator(INDEX_SO_FULL);
        SO slow = (SO)getIndicator(INDEX_SO_SLOW);
        ADX adx = (ADX)getIndicator(INDEX_ADX);
        SMA sma = (SMA)getIndicator(INDEX_SMA);

        //if we go below 50 protect our profits
        if (getRecent(slow.getStochasticOscillator()) < OSCILLATOR_TREND ||
                getRecent(full.getStochasticOscillator()) < OSCILLATOR_TREND ||
                getRecent(adx.getAdx()) < ADX_TREND) {
                    adjustHardStopPrice(agent, currentPrice);
        }

        //if indicators fall below these levels we will sell
        if (getRecent(slow.getStochasticOscillator()) < OSCILLATOR_LOW &&
                getRecent(full.getStochasticOscillator()) < OSCILLATOR_TREND &&
                getRecent(adx.getAdx()) < ADX_TREND) {
                    agent.setReasonSell(ReasonSell.Reason_Strategy);
                    adjustHardStopPrice(agent, currentPrice);
        }
    }
}