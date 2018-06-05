package com.gamesbykevin.tradingbot.agent;

import com.gamesbykevin.tradingbot.calculator.Calculator;
import com.gamesbykevin.tradingbot.calculator.strategy.Strategy;
import com.gamesbykevin.tradingbot.util.PropertyUtil;

import java.io.PrintWriter;

import static com.gamesbykevin.tradingbot.calculator.Calculator.HISTORICAL_PERIODS_MINIMUM;

public class AgentManagerHelper {

    protected static void updateCalculators(AgentManager manager) {

        //update each calculator
        for (int i = 0; i < manager.getCalculators().size(); i++) {
            manager.getCalculators().get(i).update(manager);
        }
    }

    protected static void updateAgents(AgentManager manager) {

        //update each agent
        for (int i = 0; i < manager.getAgents().size(); i++) {

            try {

                //get our agent
                Agent agent = manager.getAgents().get(i);

                //get the calculator for the candle the agent is assigned
                Calculator calculator = manager.getCalculator(agent.getCandle());

                //make sure we have enough data before we start trading
                if (calculator.getHistory().size() < HISTORICAL_PERIODS_MINIMUM) {

                    //we are not ready to trade yet
                    displayMessage(manager.getProductId() + ": Not enough periods to trade yet (" + calculator.getCandle().description + ")- " + calculator.getHistory().size() + " < " + HISTORICAL_PERIODS_MINIMUM, null);

                } else {

                    //update the agent accordingly
                    agent.update(manager.getCalculators(), manager.getProduct(), manager.getPrice());

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
        displayMessage(agent.getProductId() + "-" + agent.getStrategyKey() + " " + message, write ? agent.getWriter() : null);
    }

    public static String getAgentDetails(AgentManager manager) {

        String result = "\n";

        //sort the agents to show which are most profitable
        for (int i = 0; i < manager.getAgents().size(); i++) {
            for (int j = i; j < manager.getAgents().size(); j++) {

                //don't check the same agent
                if (i == j)
                    continue;

                Agent agent1 = manager.getAgents().get(i);
                Agent agent2 = manager.getAgents().get(j);

                double assets1 = agent1.getAssets();
                double assets2 = agent2.getAssets();

                //if the next agent has more funds, switch
                if (assets2 > assets1) {

                    //switch positions of our agents
                    manager.getAgents().set(i, agent2);
                    manager.getAgents().set(j, agent1);
                }
            }
        }

        //message with all agent totals
        for (int i = 0; i < manager.getAgents().size(); i++) {

            Agent agent = manager.getAgents().get(i);

            //start with product, strategy, hard stop ratio, and candle duration
            result += manager.getProductId() + " : " + agent.getStrategyKey();

            //how much $ does the agent currently have
            result += " - $" + AgentHelper.round(agent.getAssets());

            //if this agent has stopped trading, include it in the message
            if (agent.hasStop())
                result += ", (Stopped)";

            //make new line
            result += "\n";
        }

        //return our result
        return result;
    }
}