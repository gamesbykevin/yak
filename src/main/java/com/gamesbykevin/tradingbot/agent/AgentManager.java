package com.gamesbykevin.tradingbot.agent;

import com.coinbase.exchange.api.entity.Product;
import com.gamesbykevin.tradingbot.calculator.Calculator;
import com.gamesbykevin.tradingbot.util.LogFile;
import com.gamesbykevin.tradingbot.util.PropertyUtil;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import static com.gamesbykevin.tradingbot.Main.PAPER_TRADING;
import static com.gamesbykevin.tradingbot.calculator.Calculator.HISTORICAL_PERIODS_MINIMUM;
import static com.gamesbykevin.tradingbot.util.Email.getFileDateDesc;

public class AgentManager {

    //list of agents we are trading with different strategies
    private final List<Agent> agents;

    //our reference to calculate calculator
    private final Calculator calculator;

    //when is the last time we loaded historical data
    private long previous;

    //are we updating the agent?
    private boolean working = false;

    //the duration of data we are checking
    private final Calculator.Duration myDuration;

    //the product we are trading
    private final Product product;

    //current price of stock
    private double currentPrice = 0;

    //object used to write to a text file
    private final PrintWriter writer;

    /**
     * Different trading strategies we can use
     */
    public enum TradingStrategy {
        RSI, MACD, OBV, EMA, ADX, MACS, RSI_2, NR7, MACDD, HA,
        NR4, RSIM, RSIA, BB, BBER, SOD, SOC, SOEMA, ADL, BBR,
        EMASV, OA, EMV, NVI, PVI, NP, SR, BBER2, BBR2, EMVS,
        SO, EMAR, MACS2, MACD2, RSIM2, MACDD2,
    }

    public AgentManager(final Product product, final double funds, final Calculator.Duration myDuration) {

        //store the product this agent is trading
        this.product = product;

        //create our object to write to a text file
        this.writer = LogFile.getPrintWriter(getFileName(null));

        //store our period duration
        this.myDuration = myDuration;

        //create our calculator
        this.calculator = new Calculator();

        //update the previous run time, so it runs immediately since we don't have data yet
        this.previous = System.currentTimeMillis() - (getMyDuration().duration * 1000);

        //create new list of agents
        this.agents = new ArrayList<>();

        //how many funds does each agent get?
        double fundsPerAgent = funds / (double)TradingStrategy.values().length;

        //create an agent for every trading strategy
        for (TradingStrategy strategy : TradingStrategy.values()) {
            this.agents.add(new Agent(strategy, fundsPerAgent, product.getId(), getFileName(strategy)));
        }
    }

    public synchronized void update(final double price) {

        //don't continue if we are currently working
        if (working)
            return;

        //flag that this agent is working
        working = true;

        //keep track of the current price
        setCurrentPrice(price);

        try {

            //we don't need to update every second
            if (System.currentTimeMillis() - previous >= (getMyDuration().duration / 6) * 1000) {

                //display message as sometimes the call is not successful
                displayMessage("Making rest call to retrieve history " + getProductId(), null);

                //get the size of the history
                final int size = getCalculator().getHistory().size();

                //update our historical data and update the last update
                boolean success = getCalculator().update(getMyDuration(), getProductId());

                //get the new size for comparison
                final int sizeNew = getCalculator().getHistory().size();

                if (success) {

                    //rest call is successful
                    displayMessage("Rest call successful. History size: " + sizeNew, (size != sizeNew) ? getWriter() : null);

                } else {

                    //rest call isn't successful
                    displayMessage("Rest call is NOT successful.", getWriter());
                }

                this.previous = System.currentTimeMillis();
            }

            //make sure we have enough data before we start trading
            if (getCalculator().getHistory().size() < HISTORICAL_PERIODS_MINIMUM) {

                //we are not ready to trade yet
                displayMessage(getProductId() + ": Not enough periods to trade yet - " + getCalculator().getHistory().size() + " < " + HISTORICAL_PERIODS_MINIMUM, null);

            } else {

                //update each agent trading this specific product with their own trading strategy
                for (Agent agent : getAgents()) {
                    agent.update(getCalculator(), getProduct(), getCurrentPrice());
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            displayMessage(ex, getWriter());
        }

        //flag that we are no longer working
        working = false;
    }

    private List<Agent> getAgents() {
        return this.agents;
    }

    public void setCurrentPrice(final double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public double getCurrentPrice() {
        return this.currentPrice;
    }

    public Calculator.Duration getMyDuration() {
        return this.myDuration;
    }

    public String getProductId() {
        return getProduct().getId();
    }

    protected Calculator getCalculator() {
        return this.calculator;
    }

    public Product getProduct() {
        return this.product;
    }

    public String getAgentDetails() {

        //message with all agent totals
        String result = "";

        for (Agent agent : agents) {
            result = result + getProductId() + " : " + agent.getStrategy() + " - $" + AgentHelper.formatValue(getAssets(agent));

            //add our min value
            result = result + ",  Min $" + AgentHelper.formatValue(agent.getFundsMin());

            //add our max value
            result = result + ",  Max $" + AgentHelper.formatValue(agent.getFundsMax());

            //make new line
            result = result + "\n";
        }

        //return our result
        return result;
    }

    /**
     * Get the total assets
     * @return The total funds available + the quantity of stock we currently own @ the current stock price
     */
    public double getTotalAssets() {

        double amount = 0;

        for (Agent agent : agents) {

            //add the total amount
            amount += getAssets(agent);
        }

        //return the total amount
        return amount;
    }

    public double getAssets(TradingStrategy strategy) {

        double amount = 0;

        //add the total for every agent with the matching strategy
        for (Agent agent : getAgents()) {
            if (agent.getStrategy() == strategy)
                amount += getAssets(agent);
        }

        //return the result
        return amount;
    }

    private double getAssets(Agent agent) {
        return agent.getAssets(getCurrentPrice());
    }

    private static void displayMessage(String message, PrintWriter writer) {
        PropertyUtil.displayMessage(message, writer);
    }

    public static void displayMessage(Exception e, PrintWriter writer) {
        PropertyUtil.displayMessage(e, writer);
    }

    public static void displayMessage(Agent agent, String message, boolean write) {
        displayMessage(agent.getProductId() + "-" + agent.getStrategy() + " " + message, write ? agent.getWriter() : null);
    }

    protected String getFileName(TradingStrategy strategy) {

        if (strategy == null) {
            return (getProductId() + "-" + getFileDateDesc() + ".log");
        } else {
            return (getProductId() + "-" + strategy.toString() + "-" + getFileDateDesc() + ".log");
        }
    }

    private PrintWriter getWriter() {
        return this.writer;
    }
}