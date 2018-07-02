package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.Period.Fields;
import com.gamesbykevin.tradingbot.calculator.indicator.volume.ADL;
import com.gamesbykevin.tradingbot.calculator.indicator.volatility.BB;
import com.gamesbykevin.tradingbot.calculator.indicator.momentun.RSI;

import java.util.List;

import static com.gamesbykevin.tradingbot.calculator.strategy.StrategyHelper.hasTrendDownward;
import static com.gamesbykevin.tradingbot.calculator.strategy.StrategyHelper.hasTrendUpward;

/**
 * Bollinger Bands / Accumulation Distribution Line / Relative Strength Index
 */
public class BBAR extends Strategy {

    //how to access our indicator objects
    private static int INDEX_BB;
    private static int INDEX_RSI;
    private static int INDEX_ADL;

    //list of configurable values
    protected static int PERIODS_BB = 10;
    protected static int PERIODS_RSI = 14;

    //how many periods do we check to confirm trend
    private static final int PERIOD_TREND = 5;

    //multiplier for standard deviation
    private static final float MULTIPLIER = 2.0f;

    //what is the bollinger band squeeze ratio
    private static final float SQUEEZE_RATIO = .040f;

    public BBAR() {
        this(PERIODS_BB, MULTIPLIER, PERIODS_RSI);
    }

    public BBAR(int periodsBB, float multiplier, int periodsRSI) {

        //call parent
        super(Key.BBAR);

        //add our indicator objects
        INDEX_ADL = addIndicator(new ADL());
        INDEX_RSI = addIndicator(new RSI(periodsRSI));
        INDEX_BB = addIndicator(new BB(periodsBB, multiplier));
    }

    @Override
    public boolean hasBuySignal(Agent agent, List<Period> history, double currentPrice) {

        BB objBB = (BB)getIndicator(INDEX_BB);
        ADL objADL = (ADL)getIndicator(INDEX_ADL);
        RSI objRSI = (RSI)getIndicator(INDEX_RSI);

        //what is the price percentage
        float percentage = (float)(getRecent(objBB.getWidth()) / getRecent(history, Fields.Close));

        //if the squeeze is on, then let's try to figure out bullish divergence
        if (percentage <= SQUEEZE_RATIO) {

            //make sure both indicators are going up
            if (hasTrendUpward(objADL.getVolume(), PERIOD_TREND) &&
                    hasTrendUpward(objRSI.getValueRSI(), PERIOD_TREND)) {

                //check that the price is heading down, then we have a bullish divergence
                if (hasTrendDownward(history, Fields.Close, PERIOD_TREND))
                    return true;
            }
        }

        //display our data
        //displayMessage(agent, "Ratio %" + SQUEEZE_RATIO, false);
        //displayMessage(agent, "Price %" + percentage, false);
        return false;
    }

    @Override
    public boolean hasSellSignal(Agent agent, List<Period> history, double currentPrice) {

        BB objBB = (BB)getIndicator(INDEX_BB);

        //get the current and previous values
        double closeCurr = getRecent(history, Fields.Close);
        double closePrev = getRecent(history, Fields.Close, 2);
        double lowPrev = getRecent(objBB.getLower(), 2);
        double lowCurr = getRecent(objBB.getLower());
        double midPrev = getRecent(objBB.getMiddle().getSma(), 2);
        double midCurr = getRecent(objBB.getMiddle().getSma());
        double upPrev = getRecent(objBB.getUpper(), 2);
        double upCurr = getRecent(objBB.getUpper());

        //if we fall below the lower, we need to sell or if we were above the middle and just fell below it or if the current close goes below the upper line, let's sell
        if (closePrev > lowPrev && closeCurr < lowCurr ||
            closePrev > midPrev && closeCurr < midCurr ||
            closePrev > upPrev && closeCurr < upCurr)
            return true;

        //adjust our hard stop price to protect our investment
        if (closeCurr < midCurr || closeCurr < lowCurr)
            goShort(agent, getShortLow(history));

        //no signal yet
        return false;
    }
}