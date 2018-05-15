package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.indicator.williams.Fractal;
import com.gamesbykevin.tradingbot.calculator.indicator.williams.Fractal.Status;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;

import java.util.List;

/**
 * Fractal
 */
public class F extends Strategy {

    //configurable value
    private static final int PERIODS = 5;

    //how we will access our indicator
    private static int INDEX_FRACTAL;

    public F() {

        //add indicator(s)
        INDEX_FRACTAL = addIndicator(new Fractal(PERIODS));
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //get our indicator
        Fractal fractal = (Fractal)getIndicator(INDEX_FRACTAL);

        //what is our status? we can't check the latest because the fractal indicator is in the middle
        Status status = fractal.getStatusList().get(fractal.getStatusList().size() - (PERIODS / 2) - 1);

        //is it time to buy?
        if (status == Status.Buy || status == Status.Both)
            agent.setBuy(true);
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //get our indicator
        Fractal fractal = (Fractal)getIndicator(INDEX_FRACTAL);

        //what is our status? we can't check the latest because the fractal indicator is in the middle
        Status status = fractal.getStatusList().get(fractal.getStatusList().size() - (PERIODS / 2) - 1);

        //is it time to sell?
        if (status == Status.Sell || status == Status.Both)
            agent.setReasonSell(ReasonSell.Reason_Strategy);
    }
}