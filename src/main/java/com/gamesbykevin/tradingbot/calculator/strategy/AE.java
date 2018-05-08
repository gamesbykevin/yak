package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.indicator.trend.EMA;
import com.gamesbykevin.tradingbot.calculator.indicator.volatility.ATR;

import java.util.ArrayList;
import java.util.List;

/**
 * Average True Range / Exponential Moving Average
 */
public class AE extends Strategy {

    //our indicator objects
    private ATR objATR;
    private List<Double> ema;

    //configurable values
    private static final int PERIODS_ATR = 10;
    private static final int PERIODS_EMA = 20;
    private static final int PERIODS_CONFIRM = 10;

    private final int periodsEMA, periodsConfirm;

    public AE() {
        this(PERIODS_ATR, PERIODS_EMA, PERIODS_CONFIRM);
    }

    public AE(int periodsATR, int periodsEMA, int periodsConfirm) {

        //store our periods
        this.periodsEMA = periodsEMA;
        this.periodsConfirm = periodsConfirm;

        //create our new indicator objects
        this.objATR = new ATR(periodsATR);
        this.ema = new ArrayList<>();
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //get recent and previous values
        double currATR = getRecent(objATR.getTrueRangeAverage());
        double prevATR = getRecent(objATR.getTrueRangeAverage(), 2);

        double currEMA = getRecent(ema);
        double prevEMA = getRecent(ema, 2);

        //if the average true range goes above the ema of atr
        if (prevATR < prevEMA && currATR > currEMA) {

            //get the gain of the current periods
            double difference = history.get(history.size() - 1).close - history.get(history.size() - 1).open;

            //also make sure the gain is bigger than the previous few periods
            if (difference > 0) {

                //did we confirm the breakout?
                boolean confirm = true;

                //check previous periods to confirm
                for (int i = 0; i < periodsConfirm; i++) {

                    //get the current period
                    Period period = history.get(history.size() - i);

                    //if the different is greater than the difference we don't have a breakout
                    if (period.close - period.open > difference) {
                        confirm = false;
                        break;
                    }
                }

                //if candle breakout
                if (confirm) {

                    //we will buy
                    agent.setBuy(true);

                    //hard stop will be the low of this breakout candle
                    agent.setHardStopPrice(history.get(history.size() - 1).low);
                }
            }
        }

        //display our data
        displayData(agent, agent.hasBuy());
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //get the most recent period
        Period period = history.get(history.size() - 1);

        //if there was another gain and the low is > our hard stop price
        if (period.close > period.open && period.low > agent.getHardStopPrice())
            agent.setHardStopPrice(period.low);

        //display our data
        displayData(agent, agent.getReasonSell() != null);
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display the information
        objATR.displayData(agent, write);
        display(agent, "EMA: ", ema, write);
    }

    @Override
    public void calculate(List<Period> history) {

        //do our calculations
        objATR.calculate(history);
        EMA.calculateEmaList(ema, objATR.getTrueRangeAverage(), periodsEMA);
    }
}