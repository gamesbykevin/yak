package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.ADX;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.EMA;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;

import java.util.List;

import static com.gamesbykevin.tradingbot.calculator.CalculatorHelper.hasCrossover;

/**
 * Average Directional Index / Exponential Moving Average
 */
public class AE extends Strategy {

    //our indicator objects
    private ADX objADX;
    private EMA objEMA;

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
        this.periodsAdxConfirm = periodsAdxConfirm;
        this.objADX = new ADX(periodsAdx);
        this.objEMA = new EMA(periodsEmaLong, periodsEmaShort);
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

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
                if (hasCrossover(true, objEMA.getEmaShort(), objEMA.getEmaLong()))
                    agent.setBuy(true);
            }
        }

        //display our data
        displayData(agent, agent.hasBuy());
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //if the ema short crosses below the ema long, it is time to sell
        if (getRecent(objEMA.getEmaShort()) < getRecent(objEMA.getEmaLong())) {
            agent.setReasonSell(ReasonSell.Reason_Strategy);
            adjustHardStopPrice(agent, currentPrice);
        }

        //if the adx value goes below the trend, let's update our hard stop $
        if (getRecent(objADX.getAdx()) < ADX_TREND)
            adjustHardStopPrice(agent, currentPrice);

        //display our data
        displayData(agent, agent.getReasonSell() != null);
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display info
        objADX.displayData(agent, write);
        objEMA.displayData(agent, write);
    }

    @Override
    public void calculate(List<Period> history, int newPeriods) {

        //perform calculations
        objADX.calculate(history, newPeriods);
        objEMA.calculate(history, newPeriods);
    }
}