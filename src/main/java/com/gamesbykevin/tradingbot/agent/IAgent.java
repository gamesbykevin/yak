package com.gamesbykevin.tradingbot.agent;

import com.coinbase.exchange.api.entity.Product;
import com.gamesbykevin.tradingbot.calculator.Calculator;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.strategy.Strategy;

import java.util.List;

public interface IAgent {

    /**
     * Logic to update the agent (buy / sell / notification / etc...)
     * @param strategy Which strategy are we using to trade
     * @param history List of historical periods for our product
     * @param product The stock we are trading
     * @param currentPrice The current stock price
     */
    void update(Strategy strategy, List<Period> history, Product product, double currentPrice);
}
