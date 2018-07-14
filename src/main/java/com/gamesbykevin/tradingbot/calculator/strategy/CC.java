package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.indicator.momentun.CCI;

import java.util.List;

import static com.gamesbykevin.tradingbot.trade.TradeHelper.createTrade;

/**
 * Commodity Channel Index
 */
public class CC extends Strategy {

    //how to access our indicator objects
    private static int INDEX_CC;

    //configurable values
    private static final int PERIODS_CCI = 20;
    private static final float SIGNAL_BULLISH = 100;
    private static final float SIGNAL_BULLISH_HIGH = 200;
    private static final float SIGNAL_BEARISH = -100;

    public CC() {

        //call parent
        super(Key.CC);

        //add our indicator objects
        INDEX_CC = addIndicator(new CCI(PERIODS_CCI));
    }

    @Override
    public boolean hasBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //access our indicators
        CCI objCCI = (CCI)getIndicator(INDEX_CC);

        double cciPrevious1 = getRecent(objCCI.getCCI(), 2);
        double cciPrevious2 = getRecent(objCCI.getCCI(), 3);
        double cciCurrent = getRecent(objCCI.getCCI());

        if (cciPrevious2 > 0 && cciPrevious1 > 0 && cciPrevious1 < SIGNAL_BULLISH && cciCurrent >= SIGNAL_BULLISH) {

            //look backwards for our stop loss $
            for (int index = objCCI.getCCI().size() - 1; index >= 0; index --) {

                //if we found a rating below 0 our stop loss will be here
                if (objCCI.getCCI().get(index) < 0) {

                    //the hard stop price will be the low of the previous candle that was below 0
                    createTrade(agent);
                    agent.getTrade().goShort(agent, history.get(index).low);
                    break;
                }
            }

            return true;
        }

        //no signal
        return false;
    }

    @Override
    public boolean hasSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //access our indicators
        CCI objCCI = (CCI)getIndicator(INDEX_CC);

        //sell if we go so high
        if (getRecent(objCCI.getCCI()) >= SIGNAL_BULLISH_HIGH)
            return true;

        //if we touch 0, sell
        if (getRecent(objCCI.getCCI()) < 0)
            return true;

        //if we go below the bearish signal, exit the trade
        if (getRecent(objCCI.getCCI()) < SIGNAL_BEARISH)
            return true;

        //no signal
        return false;
    }
}