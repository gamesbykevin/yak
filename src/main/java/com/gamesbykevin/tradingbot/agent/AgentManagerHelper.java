package com.gamesbykevin.tradingbot.agent;

import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.util.PropertyUtil;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class AgentManagerHelper {

    /**
     * The minimum number of periods required in order to run a simulation
     */
    public static final int PERIODS_SIMULATION_MIN = 61;

    /**
     * Are we simulating strategies to see the result?
     */
    public static boolean SIMULATE_STRATEGIES = false;

    protected static void runSimulation(AgentManager manager) {

        //create temporary list for the history
        List<Period> tmpHistory = new ArrayList<>();

        //display message
        displayMessage("Running simulations...", manager.getWriter());

        //reset our simulation agents
        for (int i = 0; i < manager.getTmpAgents().size(); i++) {
            manager.getTmpAgents().get(i).reset(manager.getFundsPerAgent());
        }

        //start out with a minimum # of periods
        for (int index = 0; index < PERIODS_SIMULATION_MIN; index++) {
            tmpHistory.add(manager.getCalculator().getHistory().get(index));
        }

        //continue to simulate until we check all of our history
        while (tmpHistory.size() < manager.getCalculator().getHistory().size()) {

            //obtain the latest index
            int index = tmpHistory.size();

            //update each agent
            for (int i = 0; i < manager.getTmpAgents().size(); i++) {

                //get the current agent to run our simulation
                Agent tmpAgent = manager.getTmpAgents().get(i);

                //the current price will be the closing price of the current period
                double currentPrice = manager.getCalculator().getHistory().get(index).close;

                //simulate the agent
                tmpAgent.update(manager.getCalculator().getStrategy(tmpAgent), tmpHistory, manager.getProduct(), currentPrice);
            }

            //add to the tmp history
            tmpHistory.add(manager.getCalculator().getHistory().get(index));

        }

        //display message
        displayMessage("Simulation results...", manager.getWriter());

        //check the status of each agent
        for (int i = 0; i < manager.getTmpAgents().size(); i++) {

            //get the current agent to run our simulation
            Agent tmpAgent = manager.getTmpAgents().get(i);

            //construct our message
            String message = manager.getProductId() + " - " + tmpAgent.getStrategy();

            //get the agents assets
            double assets = tmpAgent.getAssets(manager.getCalculator().getHistory().get(manager.getCalculator().getHistory().size() - 1).close);

            //did we pass or fail?
            message += (assets >= manager.getFundsPerAgent()) ? ", Status: PASS" : ", Status: FAIL";

            //add our start and finish
            message += ",  Start $" + manager.getFundsPerAgent();
            message += ",  End $" + assets;

            //display our message
            displayMessage(message, manager.getWriter());
        }
    }

    protected static void displayMessage(String message, PrintWriter writer) {
        PropertyUtil.displayMessage(message, writer);
    }

    public static void displayMessage(Exception e, PrintWriter writer) {
        PropertyUtil.displayMessage(e, writer);
    }

    public static void displayMessage(Agent agent, String message, boolean write) {
        displayMessage(agent.getProductId() + "-" + agent.getStrategy() + " " + message, write ? agent.getWriter() : null);
    }
}