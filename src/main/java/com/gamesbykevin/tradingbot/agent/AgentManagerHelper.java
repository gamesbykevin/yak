package com.gamesbykevin.tradingbot.agent;

import com.gamesbykevin.tradingbot.calculator.Calculator;
import com.gamesbykevin.tradingbot.calculator.strategy.Strategy;
import com.gamesbykevin.tradingbot.util.PropertyUtil;

import java.io.PrintWriter;

import static com.gamesbykevin.tradingbot.calculator.Calculator.HISTORICAL_PERIODS_MINIMUM;
import static com.gamesbykevin.tradingbot.calculator.Calculator.MY_PERIOD_DURATIONS;

public class AgentManagerHelper {

    protected static void updateAgents(AgentManager manager) {

        //update each agent
        for (int i = 0; i < manager.getAgents().size(); i++) {

            try {

                //get our agent
                Agent agent = manager.getAgents().get(i);

                //get the assigned calculator
                Calculator calculator = manager.getCalculators().get(agent.getDuration());

                //make sure we have enough data before we start trading
                if (calculator.getHistory().size() < HISTORICAL_PERIODS_MINIMUM) {

                    //we are not ready to trade yet
                    displayMessage(manager.getProductId() + ": Not enough periods to trade yet (" + agent.getDuration().description + ")- " + calculator.getHistory().size() + " < " + HISTORICAL_PERIODS_MINIMUM, null);

                } else {

                    //get our trading strategy
                    Strategy strategy = calculator.getStrategy(agent.getTradingStrategy());

                    //update the agent accordingly
                    agent.update(strategy, calculator.getHistory(), manager.getProduct(), manager.getCurrentPrice());

                }

            } catch (Exception ex1) {

                //if there is an exception we don't want to impact all agents
                ex1.printStackTrace();
                displayMessage(ex1, manager.getWriter());
            }
        }
    }

    protected static void displayMessage(String message, PrintWriter writer) {
        PropertyUtil.displayMessage(message, writer);
    }

    public static void displayMessage(Exception e, PrintWriter writer) {
        PropertyUtil.displayMessage(e, writer);
    }

    public static void displayMessage(Agent agent, String message, boolean write) {
        displayMessage(agent.getProductId() + "-" + agent.getTradingStrategy() + " " + message, write ? agent.getWriter() : null);
    }
}