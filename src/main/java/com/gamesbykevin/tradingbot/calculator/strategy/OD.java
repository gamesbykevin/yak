package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.indicator.momentun.RSI;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.ADX;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.SMA;

import java.util.List;

import static com.gamesbykevin.tradingbot.calculator.strategy.StrategyHelper.hasTrendDownward;

/**
 * One day candle strategy
 * ADX / RSI / SMA SHORT / SMA LONG / SMA TREND
 */
public class OD extends Strategy {

    //how to access our indicator objects
    private static int INDEX_ADX;
    private static int INDEX_RSI;
    private static int INDEX_SMA_SHORT;
    private static int INDEX_SMA_LONG;
    private static int INDEX_SMA_TREND;

    //list of configurable values
    private static final int PERIODS_ADX = 14;
    private static final int PERIODS_RSI = 14;
    private static final int PERIODS_SMA_SHORT = 10;
    private static final int PERIODS_SMA_LONG = 25;
    private static final int PERIODS_SMA_TREND = 50;
    private static final int PERIODS_CONFIRM_DECREASE = 3;
    private static final float ADX_TREND = 20f;
    private static final float RSI_OVERSOLD = 30f;
    private static final float RSI_OVERBOUGHT = 70f;

    public OD() {
        this(PERIODS_ADX, PERIODS_RSI, PERIODS_SMA_SHORT, PERIODS_SMA_LONG, PERIODS_SMA_TREND);
    }

    public OD(int periodsAdx, int periodsRsi, int periodsSmaShort, int periodsSmaLong, int periodsSmaTrend) {

        //call parent
        super(Key.OD);

        //add our indicators
        INDEX_ADX = addIndicator(new ADX(periodsAdx));
        INDEX_RSI = addIndicator(new RSI(periodsRsi));
        INDEX_SMA_SHORT = addIndicator(new SMA(periodsSmaShort));
        INDEX_SMA_LONG = addIndicator(new SMA(periodsSmaLong));
        INDEX_SMA_TREND = addIndicator(new SMA(periodsSmaTrend));
    }

    @Override
    public boolean hasBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //our indicator objects
        ADX adx = (ADX)getIndicator(INDEX_ADX);
        RSI rsi = (RSI)getIndicator(INDEX_RSI);
        SMA smaShort = (SMA)getIndicator(INDEX_SMA_SHORT);
        SMA smaLong = (SMA)getIndicator(INDEX_SMA_LONG);
        SMA smaTrend = (SMA)getIndicator(INDEX_SMA_TREND);

        //verify strong trend
        if (getRecent(adx.getAdx()) >= ADX_TREND) {

            //get our recent short, long, & trend values
            double smaS = getRecent(smaShort.getSma());
            double smaL = getRecent(smaLong.getSma());
            double smaT = getRecent(smaTrend.getSma());

            //in addition to a strong trend we need to verify the trend is upward
            if (smaS > smaL && smaL > smaT)
                return true;
        }

        //if the indicator is oversold our indicator is showing buy
        if (getRecent(rsi.getValueRSI()) <= RSI_OVERSOLD)
            return true;

        //no signal
        return false;
    }

    @Override
    public boolean hasSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //our indicator objects
        RSI rsi = (RSI)getIndicator(INDEX_RSI);
        SMA smaShort = (SMA)getIndicator(INDEX_SMA_SHORT);
        SMA smaLong = (SMA)getIndicator(INDEX_SMA_LONG);
        SMA smaTrend = (SMA)getIndicator(INDEX_SMA_TREND);

        //get our recent short, long, & trend values
        double smaS = getRecent(smaShort.getSma());
        double smaL = getRecent(smaLong.getSma());
        double smaT = getRecent(smaTrend.getSma());

        //protect our investment if a bear trend is showing
        if (smaS < smaL || smaS < smaT || currentPrice < smaL || currentPrice < smaT)
            adjustHardStopPrice(agent, currentPrice);

        //if our short sma has been declining protect our investment
        if (hasTrendDownward(smaShort.getSma(), PERIODS_CONFIRM_DECREASE))
            adjustHardStopPrice(agent, currentPrice);

        //if the indicator is oversold our indicator is showing buy
        if (getRecent(rsi.getValueRSI()) >= RSI_OVERBOUGHT)
            adjustHardStopPrice(agent, currentPrice);

        //no signal
        return false;
    }
}