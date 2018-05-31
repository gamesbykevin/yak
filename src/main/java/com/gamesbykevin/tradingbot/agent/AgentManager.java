package com.gamesbykevin.tradingbot.agent;

import com.coinbase.exchange.api.entity.Product;
import com.gamesbykevin.tradingbot.calculator.Calculator;
import com.gamesbykevin.tradingbot.calculator.Calculator.Candle;
import com.gamesbykevin.tradingbot.calculator.strategy.Strategy;
import com.gamesbykevin.tradingbot.util.LogFile;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.gamesbykevin.tradingbot.agent.AgentHelper.HARD_STOP_RATIO;
import static com.gamesbykevin.tradingbot.agent.AgentManagerHelper.*;
import static com.gamesbykevin.tradingbot.calculator.Calculator.MY_TRADING_STRATEGIES;
import static com.gamesbykevin.tradingbot.util.LogFile.FILE_SEPARATOR;
import static com.gamesbykevin.tradingbot.util.LogFile.getFilenameManager;

public class AgentManager {

    //our agent list
    private List<Agent> agents;

    //our reference to the calculator
    private List<Calculator> calculators;

    //are we updating the agent?
    private boolean working = false;

    //the product we are trading
    private final Product product;

    //object used to write to a text file
    private PrintWriter writer;

    //how many funds did we start with
    private final double funds;

    public AgentManager(final Product product, final double funds) {

        //store the product this agent is trading
        this.product = product;

        //how many funds do we start with?
        this.funds = funds;

        //create new calculator for each candle duration and perform our initial calculations
        for (Candle candle : Candle.values()) {
            getCalculators().add(new Calculator(candle, getProductId(), getWriter()));
            getCalculators().get(getCalculators().size() - 1).calculate(this, 0);
        }

        //create our list of agents
        this.agents = new ArrayList<>();

        Candle candle = Candle.FiveMinutes;

        //create an agent for each strategy
        for (int i = 0; i < MY_TRADING_STRATEGIES.length; i++) {

            //create our agent
            Agent agent = new Agent(getFunds(), getProductId(), MY_TRADING_STRATEGIES[i], candle);

            //add agent to the list
            getAgents().add(agent);
        }
    }

    public synchronized void update(final double price) {

        //if all agents have stopped trading don't continue
        if (hasStoppedTrading())
            return;

        //don't continue if we are currently working
        if (working)
            return;

        //flag that this agent is working
        working = true;

        try {

            //update our calculator, etc...
            updateCalculators(this);

            //update our agents
            updateAgents(this, price);

        } catch (Exception ex) {

            //print stack trace and write exception to log file
            ex.printStackTrace();
            displayMessage(ex, getWriter());

        } finally {

            //last step is to make that we are done working
            working = false;
        }
    }

    public double getTotalAssets() {

        //total assets
        double result = 0;

        //add all of our assets up
        for (int i = 0; i < getAgents().size(); i++) {
            result += getTotalAssets(getAgents().get(i));
        }

        //return our result
        return result;
    }

    /**
     * Get the total assets
     * @return The total funds available + the quantity of stock we currently own @ the current stock price
     */
    public double getTotalAssets(Agent agent) {

        //return the total amount
        return getAssets(agent);
    }

    public double getTotalAssets(Strategy.Key key) {

        for (int i = 0; i < getAgents().size(); i++) {

            Agent agent = getAgents().get(i);

            if (agent.getStrategyKey() == key)
                return getAssets(agent);
        }

        return 0;
    }

    private double getAssets(Agent agent) {
        return agent.getAssets();
    }

    public PrintWriter getWriter() {

        //create our object to write to a text file
        if (this.writer == null)
            this.writer = LogFile.getPrintWriter(getFilenameManager(), LogFile.getLogDirectory() + FILE_SEPARATOR + getProductId());

        //return our print writer
        return this.writer;
    }

    public double getFunds() {
        return (this.funds);
    }

    public String getProductId() {
        return getProduct().getId();
    }

    public Product getProduct() {
        return this.product;
    }

    public List<Agent> getAgents() {
        return this.agents;
    }

    /**
     * Have all the agents stopped trading?
     * @return true = yes, false = no
     */
    public boolean hasStoppedTrading() {

        //check every agent
        for (int i = 0; i < getAgents().size(); i++) {

            //if one agent is still trading, return false
            if (!getAgents().get(i).hasStop())
                return false;
        }

        //all agents are done return true
        return true;
    }

    public List<Calculator> getCalculators() {

        if (this.calculators == null)
            this.calculators = new ArrayList<>();

        return this.calculators;
    }

    public Calculator getCalculator(Candle candle) {

        for (int i = 0; i < getCalculators().size(); i++) {

            if (getCalculators().get(i).getCandle() == candle)
                return getCalculators().get(i);
        }

        //if nothing was found return null (shouldn't happen)
        return null;
    }
}