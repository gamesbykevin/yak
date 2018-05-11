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

    protected static void updateCalculators(AgentManager manager) {

        //check all our desired durations
        for (int i = 0; i < MY_PERIOD_DURATIONS.length; i++) {

            //get the current calculator
            Calculator calculator = manager.getCalculators().get(MY_PERIOD_DURATIONS[i]);

            //can we make a service call to update our calculator
            if (calculator.canExecute()) {

                //how many periods do we have?
                final int size = calculator.getHistory().size();

                //display message as sometimes the call is not successful
                displayMessage("Making rest call to retrieve history " + manager.getProductId() + " (" + MY_PERIOD_DURATIONS[i].description + ")", null);

                //update our historical data and update the last update
                boolean success = calculator.update(manager.getProductId());

                //get the size again so we can compare and see if it has changed
                final int change = calculator.getHistory().size();

                //if a new candle has been added recalculate our strategies
                if (size != change)
                    calculator.calculate(size > change ? size - change : change - size);

                if (success) {

                    //rest call is successful
                    displayMessage("Rest call successful. History size: " + change, (change != size) ? manager.getWriter() : null);

                } else {

                    //rest call isn't successful
                    displayMessage("Rest call is NOT successful.", manager.getWriter());
                }
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