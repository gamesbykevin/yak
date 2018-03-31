package com.gamesbykevin.tradingbot.agent;

import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.strategy.Strategy;
import com.gamesbykevin.tradingbot.calculator.strategy.StrategyHelper;
import com.gamesbykevin.tradingbot.util.PropertyUtil;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class AgentManagerHelper {

    /**
     * The minimum number of periods required in order to run a simulation
     */
    public static final int PERIODS_SIMULATION_MIN = 260;

    /**
     * Are we simulating strategies to see the result?
     */
    public static boolean SIMULATE_STRATEGIES = false;

    protected static void runSimulation(AgentManager manager) {

        //create temporary list for the history
        List<Period> tmpHistory = new ArrayList<>();

        //display message
        displayMessage("Running simulations...", manager.getWriter());

        //check each agent belonging to the manager
        for (int i = 0; i < manager.getTmpAgents().size(); i++) {

            //get the agent
            Agent agent = manager.getTmpAgents().get(i);

            //get the assigned strategy
            Strategy strategy = manager.getCalculator().getStrategy(agent);

            //reset the index of the strategy assigned to the agent
            strategy.setIndexStrategy(0);

            while (true) {

                //setup the correct values before we run our simulation
                boolean result = StrategyHelper.setupValues(agent.getStrategy(), strategy.getIndexStrategy());

                //if there was an issue, we checked all scenarios for this strategy and we can move to the next
                if (!result) {
                    strategy.setIndexStrategy(0);
                    break;
                }

                //reset the agent funds
                agent.reset(manager.getFundsPerAgent());

                //clear the history
                tmpHistory.clear();

                //start out with a minimum # of periods
                for (int index = 0; index < PERIODS_SIMULATION_MIN; index++) {
                    tmpHistory.add(manager.getCalculator().getHistory().get(index));
                }

                //continue to simulate until we check all of our history
                while (tmpHistory.size() < manager.getCalculator().getHistory().size()) {

                    //obtain the latest index
                    int index = tmpHistory.size();

                    //the current price will be the closing price of the current period
                    double currentPrice = manager.getCalculator().getHistory().get(index).close;

                    //recalculate based on the current history
                    strategy.calculate(tmpHistory);

                    //simulate the agent
                    agent.update(strategy, tmpHistory, manager.getProduct(), currentPrice);

                    //add to the tmp history
                    tmpHistory.add(manager.getCalculator().getHistory().get(index));
                }

                //display the results of our simulation
                String message = manager.getProductId() + " - " + agent.getStrategy() + " - (Index " + strategy.getIndexStrategy() + ")";

                //the most recent period close will be the current price
                double currentPrice = manager.getCalculator().getHistory().get(manager.getCalculator().getHistory().size() - 1).close;

                //get the agents assets
                double assets = agent.getAssets(currentPrice);

                //did we pass or fail?
                message += (assets >= manager.getFundsPerAgent()) ? ", Status: PASS" : ", Status: FAIL";

                //add our start and finish
                message += ",  Start $" + manager.getFundsPerAgent() + ",  End $" + assets;

                //display our message
                displayMessage(message, manager.getWriter());

                //increase the index and check another configuration of the same strategy
                strategy.setIndexStrategy(strategy.getIndexStrategy() + 1);
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
        displayMessage(agent.getProductId() + "-" + agent.getStrategy() + " " + message, write ? agent.getWriter() : null);
    }
}