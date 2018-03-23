package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonBuy;

import java.util.List;

import static com.gamesbykevin.tradingbot.agent.AgentManager.displayMessage;

public class NR7 extends NR {

    /**
     * Narrow Range 7 will always be 7 periods
     */
    public static final int PERIODS = 7;

    public NR7() {

        //call parent
        super(PERIODS);
    }
}