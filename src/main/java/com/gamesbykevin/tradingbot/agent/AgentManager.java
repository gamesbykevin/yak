package com.gamesbykevin.tradingbot.agent;

import com.coinbase.exchange.api.entity.Product;
import com.gamesbykevin.tradingbot.calculator.Calculator;
import com.gamesbykevin.tradingbot.calculator.Calculator.Duration;
import com.gamesbykevin.tradingbot.util.LogFile;
import com.gamesbykevin.tradingbot.util.PropertyUtil;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import static com.gamesbykevin.tradingbot.Main.PAPER_TRADING;
import static com.gamesbykevin.tradingbot.Main.TRADING_STRATEGIES;
import static com.gamesbykevin.tradingbot.calculator.Calculator.HISTORICAL_PERIODS_MINIMUM;
import static com.gamesbykevin.tradingbot.calculator.Calculator.MY_TRADING_STRATEGIES;
import static com.gamesbykevin.tradingbot.util.Email.getFileDateDesc;

public class AgentManager {

    //list of agents we are trading with different strategies
    private final List<Agent> agents;

    //our reference to calculate calculator
    private Calculator calculator;

    //when is the last time we loaded historical data
    private long previous;

    //are we updating the agent?
    private boolean working = false;

    //the duration of data we are checking
    private final Duration myDuration;

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

        ADL, ADX, BB, BBER, BBR, EMA, EMA2, EMAR, EMAS, EMASV,
        EMV, EMVS, HA, MACD, MACDD, MACS, NP, NR4, NR7, NVI,
        OA, OBV, PVI, RSI, RSIA, RSIM, SO, SOC, SOD, SOEMA,
        SR, TWO_RSI,
    }

    public AgentManager(final Product product, final double funds, final Calculator.Duration myDuration) {

        //store the product this agent is trading
        this.product = product;

        //create our object to write to a text file
        this.writer = LogFile.getPrintWriter(getFileName(null));

        //store our period duration
        this.myDuration = myDuration;

        //update the previous run time, so it runs immediately since we don't have data yet
        this.previous = System.currentTimeMillis() - (getMyDuration().duration * 1000);

        //create new list of agents
        this.agents = new ArrayList<>();

        //populate our strategies
        loadStrategies();

        //create our agents
        createAgents(funds);
    }

    private void createAgents(double funds) {

        //how many funds does each agent get?
        double fundsPerAgent = funds / (double)MY_TRADING_STRATEGIES.length;

        //create an agent for every trading strategy that we have specified
        for (int i = 0; i < MY_TRADING_STRATEGIES.length; i++) {
            this.agents.add(new Agent(MY_TRADING_STRATEGIES[i], fundsPerAgent, product.getId(), getFileName(MY_TRADING_STRATEGIES[i])));
        }
    }

    private void loadStrategies() {

        //make sure we aren't using duplicate strategies
        for (int i = 0; i < TRADING_STRATEGIES.length; i++) {
            for (int j = 0; j < TRADING_STRATEGIES.length; j++) {

                //don't check the same element
                if (i == j)
                    continue;

                //if the value already exists we have duplicate strategies
                if (TRADING_STRATEGIES[i].trim().equalsIgnoreCase(TRADING_STRATEGIES[j].trim()))
                    throw new RuntimeException("Duplicate trading strategy in your property file \"" + TRADING_STRATEGIES[i] + "\"");
            }
        }

        //create a new array which will contain our trading strategies
        MY_TRADING_STRATEGIES = new TradingStrategy[TRADING_STRATEGIES.length];

        //temp list of all values so we can check for a match
        TradingStrategy[] tmp = TradingStrategy.values();

        //make sure the specified strategies exist
        for (int i = 0; i < TRADING_STRATEGIES.length; i++) {

            //check each strategy for a match
            for (int j = 0; j < tmp.length; j++) {

                //if the spelling matches we have found our strategy
                if (tmp[j].toString().trim().equalsIgnoreCase(TRADING_STRATEGIES[i].trim())) {

                    //assign our strategy
                    MY_TRADING_STRATEGIES[i] = tmp[j];

                    //exit the loop
                    break;
                }
            }

            //no matching strategy was found throw exception
            if (MY_TRADING_STRATEGIES[i] == null)
                throw new RuntimeException("Strategy not found \"" + TRADING_STRATEGIES[i] + "\"");
        }

        //create our calculator now that we have the strategies
        this.calculator = new Calculator();
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
                for (int i = 0; i < getAgents().size(); i++) {
                    getAgents().get(i).update(getCalculator(), getProduct(), getCurrentPrice());
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

        for (int i = 0; i < getAgents().size(); i++) {

            result += getProductId() + " : " + getAgents().get(i).getStrategy() + " - $" + AgentHelper.formatValue(getAssets(getAgents().get(i)));

            //add our min value
            result += ",  Min $" + AgentHelper.formatValue(getAgents().get(i).getFundsMin());

            //add our max value
            result += ",  Max $" + AgentHelper.formatValue(getAgents().get(i).getFundsMax());

            //make new line
            result += "\n";
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

        for (int i = 0; i < getAgents().size(); i++) {

            //add the total amount
            amount += getAssets(getAgents().get(i));
        }

        //return the total amount
        return amount;
    }

    public double getAssets(TradingStrategy strategy) {

        double amount = 0;

        for (int i = 0; i < getAgents().size(); i++) {

            //add the total for every agent with the matching strategy
            if (getAgents().get(i).getStrategy() == strategy)
                amount += getAssets(getAgents().get(i));
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