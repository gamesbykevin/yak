package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.ADX;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.EMA;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;

import java.util.List;

import static com.gamesbykevin.tradingbot.calculator.strategy.StrategyHelper.hasCrossover;

/**
 * Average Directional Index / Exponential Moving Average
 */
public class AE extends Strategy {

    //how to access our indicator objects
    private static int INDEX_EMA_SHORT;
    private static int INDEX_EMA_LONG;
    private static int INDEX_ADX;

    //configurable values
    private static final int PERIODS_EMA_LONG = 10;
    private static final int PERIODS_EMA_SHORT = 3;
    private static final int PERIODS_ADX = 14;
    private static final int PERIODS_ADX_CONFIRM = 2;
    private static final double ADX_TREND = 20.0d;

    //how many periods do we confirm our adx is not declining
    private final int periodsAdxConfirm;

    public AE() {
        this(PERIODS_EMA_LONG, PERIODS_EMA_SHORT, PERIODS_ADX, PERIODS_ADX_CONFIRM);
    }

    public AE(int periodsEmaLong, int periodsEmaShort, int periodsAdx, int periodsAdxConfirm) {

        //save the periods to confirm
        this.periodsAdxConfirm = periodsAdxConfirm;

        //add our indicators
        INDEX_EMA_SHORT = addIndicator(new EMA(periodsEmaShort));
        INDEX_EMA_LONG = addIndicator(new EMA(periodsEmaLong));
        INDEX_ADX = addIndicator(new ADX(periodsAdx));
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        ADX objADX = (ADX)getIndicator(INDEX_ADX);
        EMA objShortEMA = (EMA)getIndicator(INDEX_EMA_SHORT);
        EMA objLongEMA = (EMA)getIndicator(INDEX_EMA_LONG);

        //make sure adx is trending
        if (getRecent(objADX.getAdx()) > ADX_TREND) {

            //confirm our adx isn't in a downtrend
            boolean confirm = true;

            //check the previous number of periods to ensure adx is not declining
            for (int i = 1; i <= periodsAdxConfirm; i++) {

                //if the current adx value is less than the previous adx value
                if (getRecent(objADX.getAdx(), i) <= getRecent(objADX.getAdx(), i + 1)) {
                    confirm = false;
                    break;
                }
            }

            //make sure there is no downtrend
            if (confirm) {

                //if the short crosses above the long let's buy and enter the trade
                if (hasCrossover(true, objShortEMA.getEma(), objLongEMA.getEma()))
                    agent.setBuy(true);
            }
        }
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        ADX objADX = (ADX)getIndicator(INDEX_ADX);
        EMA objShortEMA = (EMA)getIndicator(INDEX_EMA_SHORT);
        EMA objLongEMA = (EMA)getIndicator(INDEX_EMA_LONG);

        //if the ema short crosses below the ema long, it is time to sell
        if (getRecent(objShortEMA.getEma()) < getRecent(objLongEMA.getEma())) {
            agent.setReasonSell(ReasonSell.Reason_Strategy);
            adjustHardStopPrice(agent, currentPrice);
        }

        //if the adx value goes below the trend, let's update our hard stop $
        if (getRecent(objADX.getAdx()) < ADX_TREND)
            adjustHardStopPrice(agent, currentPrice);
    }
}