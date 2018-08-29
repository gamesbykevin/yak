package com.gamesbykevin.tradingbot.agent;

import com.gamesbykevin.tradingbot.calculator.Calculator;
import com.gamesbykevin.tradingbot.calculator.strategy.Strategy;
import com.gamesbykevin.tradingbot.util.PropertyUtil;

import java.io.PrintWriter;

import static com.gamesbykevin.tradingbot.calculator.Calculation.getRecent;
import static com.gamesbykevin.tradingbot.calculator.Calculator.HISTORICAL_PERIODS_MINIMUM;
import static com.gamesbykevin.tradingbot.calculator.Calculator.PERIODS_SMA;
import static com.gamesbykevin.tradingbot.trade.TradeHelper.NEW_LINE;

public class AgentManagerHelper {

    protected static void updateCalculators(AgentManager manager) {

        //update each calculator
        manager.getCalculator().update(manager);
    }

    protected static void updateAgents(AgentManager manager) {

        //update each agent
        for (int i = 0; i < manager.getAgents().size(); i++) {

            try {

                //get our agent
                Agent agent = manager.getAgents().get(i);

                //get the calculator for the candle the agent is assigned
                Calculator calculator = manager.getCalculator();

                //make sure we have enough data before we start trading
                if (calculator.getHistory().size() < HISTORICAL_PERIODS_MINIMUM) {

                    //we are not ready to trade yet
                    displayMessage(manager.getProductId() + ": Not enough periods to trade yet (" + calculator.getCandle().description + ")- " + calculator.getHistory().size() + " < " + HISTORICAL_PERIODS_MINIMUM, null);

                } else {

                    //are we above our sma?
                    boolean aboveSMA = true;

                    //only check if we have data to compare
                    if (PERIODS_SMA > 0) {

                        final int confirm = 3;

                        //if we have more than x periods, let's confirm we are above the sma
                        if (PERIODS_SMA >= confirm) {

                            for (int index = 1; index <= confirm; index++) {

                                double close = calculator.getHistory().get(calculator.getHistory().size() - index).close;
                                double sma = getRecent(calculator.getObjSMA(), index);

                                if (close < sma) {
                                    aboveSMA = false;
                                    break;
                                }
                            }

                        } else {

                            //get the $ for comparison
                            final double close = calculator.getHistory().get(calculator.getHistory().size() - 1).close;
                            final double sma = getRecent(calculator.getObjSMA().getSma());

                            //are we above?
                            aboveSMA = (close > sma);
                        }
                    }

                    //update the agent
                    agent.update(manager.getCalculator(), manager.getProduct(), manager.getPrice(), aboveSMA);
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

        String result = NEW_LINE;

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

            //start with product, strategy, candle description
            result += manager.getProductId() + " : " + agent.getStrategyKey() + " (" + agent.getCandle().description + ")";

            //how much $ does the agent currently have
            result += " - $" + AgentHelper.round(agent.getAssets());

            if (agent.hasStop()) {
                //if this agent has stopped trading, include it in the message
                result += ", (Stopped)";
            } else if (agent.getWallet().getQuantity() > 0) {
                //if there is pending stock we include that as well
                result += ", (Pending)";
            }

            //make new line
            result += NEW_LINE;
        }

        //return our result
        return result;
    }
}