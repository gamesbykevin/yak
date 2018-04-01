package com.gamesbykevin.tradingbot.agent;

import com.gamesbykevin.tradingbot.agent.AgentManager.TradingStrategy;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.strategy.Strategy;
import com.gamesbykevin.tradingbot.calculator.strategy.StrategyHelper;
import com.gamesbykevin.tradingbot.util.PropertyUtil;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import static com.gamesbykevin.tradingbot.calculator.Calculator.MY_TRADING_STRATEGIES;

public class AgentManagerHelper {

    /**
     * How many periods do we need to start the simulations
     */
    public static final int SIMULATION_PERIODS_MIN = 201;

    protected static void runSimulation(AgentManager manager, Agent agentSimulation) {

        //obtain our reference to the history
        List<Period> history = manager.getCalculator().getHistory();

        //create temporary list for the history
        List<Period> tmpHistory = new ArrayList<>();

        //display message
        displayMessage("Running simulations...", manager.getWriter());

        //track the most money made
        double winningFunds = 0;

        //what is our winning strategy
        TradingStrategy winningStrategy = null;
        Strategy winningStrategyObj = null;

        //which settings works best for the strategy
        int winningStrategyIndex = 0;

        //simulate each of our specified trading strategies
        for (int i = 0; i < MY_TRADING_STRATEGIES.length; i++) {

            TradingStrategy current = MY_TRADING_STRATEGIES[i];

            //get the assigned strategy
            Strategy strategyObj = manager.getCalculator().getStrategyObj(current);

            //reset the index of the strategy assigned to the agent to 0
            strategyObj.setIndexStrategy(0);

            while (true) {

                //setup the correct values before we run our simulation
                boolean result = StrategyHelper.setupValues(current, strategyObj.getIndexStrategy());

                //if false we checked all scenarios for this strategy and we can move to the next
                if (!result)
                    break;

                //reset the agent funds
                agentSimulation.reset(manager.getFunds());

                //assign the trading strategy
                agentSimulation.setTradingStrategy(current);

                //clear the history
                tmpHistory.clear();

                //start out with a minimum # of periods
                for (int index = 0; index <= SIMULATION_PERIODS_MIN; index++) {
                    tmpHistory.add(history.get(index));
                }

                //continue to simulate until we check all of our history
                while (tmpHistory.size() < history.size()) {

                    //obtain the latest index
                    int index = tmpHistory.size();

                    //keep the index in bounds when simulating the last period
                    if (index >= history.size())
                        index = history.size() - 1;

                    //the current price will be the closing price of the current period
                    double currentPrice = history.get(index).close;

                    //recalculate based on the current history
                    strategyObj.calculate(tmpHistory);

                    //simulate the agent
                    agentSimulation.update(strategyObj, tmpHistory, manager.getProduct(), currentPrice);

                    //add to the tmp history
                    if (tmpHistory.size() < history.size())
                        tmpHistory.add(history.get(index));
                }

                //display the results of our simulation
                String message = manager.getProductId() + " - " + agentSimulation.getTradingStrategy() + " - (Index " + strategyObj.getIndexStrategy() + ")";

                //the most recent period close will be the current price
                double currentPrice = history.get(history.size() - 1).close;

                //get the agents assets
                double assets = agentSimulation.getAssets(currentPrice);

                //did we pass or fail?
                message += (assets >= manager.getFunds()) ? ", Status: PASS" : ", Status: FAIL";

                //add our start and finish
                message += ",  Start $" + manager.getFunds() + ",  End $" + assets;

                //if the assets are greater we have a new winning strategy
                if (assets > winningFunds) {
                    winningFunds = assets;
                    winningStrategy = current;
                    winningStrategyIndex = strategyObj.getIndexStrategy();
                    winningStrategyObj = strategyObj;
                }

                //display our message
                displayMessage(message, manager.getWriter());

                //increase the index and check another configuration of the same strategy
                strategyObj.setIndexStrategy(strategyObj.getIndexStrategy() + 1);
            }
        }

        tmpHistory.clear();
        tmpHistory = null;

        //make sure we found a winning strategy before updating our agent
        if (winningStrategy != null) {
            winningStrategyObj.setIndexStrategy(winningStrategyIndex);
            manager.getAgentPrimary().setTradingStrategy(winningStrategy);
        }

        //display message
        displayMessage("Simulations complete...", manager.getWriter());
    }

    protected static void displayMessage(String message, PrintWriter writer) {
        PropertyUtil.displayMessage(message, writer);
    }

    public static void displayMessage(Exception e, PrintWriter writer) {
        PropertyUtil.displayMessage(e, writer);
    }

    public static void displayMessage(Agent agent, String message, boolean write) {

        //skip if this is a simulation
        if (agent.isSimulation())
            return;

        displayMessage(agent.getProductId() + "-" + agent.getTradingStrategy() + " " + message, write ? agent.getWriter() : null);
    }
}