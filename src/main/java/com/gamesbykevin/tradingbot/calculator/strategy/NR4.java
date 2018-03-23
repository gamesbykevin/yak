package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonBuy;

import java.util.List;

import static com.gamesbykevin.tradingbot.agent.AgentManager.displayMessage;

public class NR4 extends NR {

    /**
     * Narrow Range 4 will always be 4 periods
     */
    public static final int PERIODS = 4;

    public NR4() {

        //call parent
        super(PERIODS);
    }
}