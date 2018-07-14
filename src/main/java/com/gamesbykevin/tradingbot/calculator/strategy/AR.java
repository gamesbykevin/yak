package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.Period.Fields;
import com.gamesbykevin.tradingbot.calculator.indicator.momentun.RSI;
import com.gamesbykevin.tradingbot.calculator.indicator.volatility.ATR;

import java.util.List;

import static com.gamesbykevin.tradingbot.calculator.strategy.StrategyHelper.hasTrendDownward;
import static com.gamesbykevin.tradingbot.calculator.strategy.StrategyHelper.hasTrendUpward;

/**
 * Average True Range / Relative Strength Index
 */
public class AR extends Strategy {

    //how to access our indicator objects
    private static int INDEX_ATR;
    private static int INDEX_RSI;

    //configurable values
    private static final int PERIODS_ATR = 14;
    private static final int PERIODS_RSI = 14;

    public AR() {
        this(PERIODS_ATR, PERIODS_RSI);
    }

    public AR(int periodsATR, int periodsRSI) {

        //call parent
        super(Key.AR);

        //add our indicator objects
        INDEX_ATR = addIndicator(new ATR(periodsATR));
        INDEX_RSI = addIndicator(new RSI(periodsRSI));
    }

    @Override
    public boolean hasBuySignal(Agent agent, List<Period> history, double currentPrice) {

        ATR objATR = (ATR)getIndicator(INDEX_ATR);
        RSI objRSI = (RSI)getIndicator(INDEX_RSI);

        //if the atr is not going up, but the rsi is
        if (!hasTrendUpward(objATR.getAverageTrueRange(), DEFAULT_PERIODS_CONFIRM_INCREASE) &&
                hasTrendUpward(objRSI.getValueRSI(), DEFAULT_PERIODS_CONFIRM_INCREASE)) {
            return true;
        }

        //no signal
        return false;
    }

    @Override
    public boolean hasSellSignal(Agent agent, List<Period> history, double currentPrice) {

        ATR objATR = (ATR)getIndicator(INDEX_ATR);
        RSI objRSI = (RSI)getIndicator(INDEX_RSI);

        //if atr is not going up, and rsi is going down
        if (!hasTrendUpward(objATR.getAverageTrueRange(), DEFAULT_PERIODS_CONFIRM_INCREASE) && hasTrendDownward(objRSI.getValueRSI(), DEFAULT_PERIODS_CONFIRM_INCREASE))
            return true;

        //if rsi is consistently going down, let's short
        if (hasTrendDownward(objRSI.getValueRSI(), DEFAULT_PERIODS_CONFIRM_INCREASE))
            goShort(agent, getShortLow(history));

        //no signal
        return false;
    }
}