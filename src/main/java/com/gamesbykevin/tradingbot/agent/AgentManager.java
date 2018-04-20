package com.gamesbykevin.tradingbot.agent;

import com.coinbase.exchange.api.entity.Product;
import com.gamesbykevin.tradingbot.calculator.Calculator;
import com.gamesbykevin.tradingbot.calculator.Calculator.Duration;
import com.gamesbykevin.tradingbot.calculator.strategy.Strategy;
import com.gamesbykevin.tradingbot.util.History;
import com.gamesbykevin.tradingbot.util.LogFile;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import static com.gamesbykevin.tradingbot.agent.AgentHelper.HARD_STOP_RATIO;
import static com.gamesbykevin.tradingbot.agent.AgentHelper.getPrediction;
import static com.gamesbykevin.tradingbot.agent.AgentHelper.getProbability;
import static com.gamesbykevin.tradingbot.agent.AgentManagerHelper.displayMessage;
import static com.gamesbykevin.tradingbot.calculator.Calculator.HISTORICAL_PERIODS_MINIMUM;
import static com.gamesbykevin.tradingbot.calculator.Calculator.MY_TRADING_STRATEGIES;
import static com.gamesbykevin.tradingbot.util.LogFile.getFilenameManager;

public class AgentManager {

    //our agent list
    private List<Agent> agents;

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

    //how many funds did we start with
    private final double funds;

    /**
     * Different trading strategies we can use
     */
    public enum TradingStrategy {

        ADL, ADX, BB, BBER, EMA,
        EMAR, EMAS, EMV, HA, MACD,
        MACS, NR, NVI, OBV, PVI,
        RSI, SO, SOEMA, SR, TWO_RSI
    }

    public AgentManager(final Product product, final double funds, final Calculator.Duration myDuration) {

        //store the product this agent is trading
        this.product = product;

        //how many funds do we start with?
        this.funds = funds;

        //create our object to write to a text file
        this.writer = LogFile.getPrintWriter(getFilenameManager(), LogFile.getLogDirectory() + "\\" + getProductId());

        //store our period duration
        this.myDuration = myDuration;

        //update the previous run time, so it runs immediately since we don't have data yet
        this.previous = System.currentTimeMillis() - (getMyDuration().duration * 1000);

        //create our calculator
        this.calculator = new Calculator();

        //load any historic candles stored locally
        History.load(this);

        //create our agents last
        createAgents();
    }

    private void createAgents() {

        //create our list of agents
        this.agents = new ArrayList<>();

        //create an agent for each strategy
        for (int i = 0; i < MY_TRADING_STRATEGIES.length; i++) {

            //create an agent for each hard stop ratio
            for (int j = 0; j < HARD_STOP_RATIO.length; j++) {

                //create our agent
                Agent agent = new Agent(getFunds(), getProductId(), MY_TRADING_STRATEGIES[i]);

                //assign the hard stop ratio
                agent.setHardStopRatio(HARD_STOP_RATIO[j]);

                //add agent to the list
                getAgents().add(agent);
            }
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

            //get the size of the history
            final int size = getCalculator().getHistory().size();

            //we don't need to update every second
            if (System.currentTimeMillis() - previous >= (getMyDuration().duration / 6) * 1000) {

                //display message as sometimes the call is not successful
                displayMessage("Making rest call to retrieve history " + getProductId(), null);

                //update our historical data and update the last update
                boolean success = getCalculator().update(getMyDuration(), getProductId());

                //get the size again so we can compare and see if it has changed
                final int change = getCalculator().getHistory().size();

                //if a new candle has been added update
                if (size != change) {

                    //recalculate our strategies
                    getCalculator().calculate();

                    //get probability of price change
                    double probabilityI = getProbability(getCalculator().getHistory(), true);
                    double probabilityD = getProbability(getCalculator().getHistory(), false);

                    //if we have new data calculate the probability of a price increase, and the forecast price
                    displayMessage("Probability $ increase: " + probabilityI, getWriter());
                    displayMessage("Probability $ decrease: " + probabilityD, getWriter());
                    displayMessage("Price prediction of next period $" + getPrediction(getCalculator().getHistory()), getWriter());
                    displayMessage(getAgentDetails(), getWriter());
                }

                if (success) {

                    //rest call is successful
                    displayMessage("Rest call successful. History size: " + change, (change != size) ? getWriter() : null);

                } else {

                    //rest call isn't successful
                    displayMessage("Rest call is NOT successful.", getWriter());
                }

                //store the last run time for our next update
                this.previous = System.currentTimeMillis();
            }

            //make sure we have enough data before we start trading
            if (getCalculator().getHistory().size() < HISTORICAL_PERIODS_MINIMUM) {

                //we are not ready to trade yet
                displayMessage(getProductId() + ": Not enough periods to trade yet - " + getCalculator().getHistory().size() + " < " + HISTORICAL_PERIODS_MINIMUM, null);

            } else {

                //update our agents
                for (int i = 0; i < getAgents().size(); i++) {

                    //get our agent
                    Agent agent = getAgents().get(i);

                    //get our trading strategy
                    Strategy strategy = getCalculator().getStrategy(agent.getTradingStrategy());

                    //update the agent accordingly
                    agent.update(strategy, getCalculator().getHistory(), getProduct(), getCurrentPrice());
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            displayMessage(ex, getWriter());
        }

        //flag that we are no longer working
        working = false;
    }

    public String getAgentDetails() {

        String result = "\n";

        //message with all agent totals
        for (int i = 0; i < getAgents().size(); i++) {

            Agent agent = getAgents().get(i);

            //start with product, strategy, and hard stop ratio
            result += getProductId() + " : " + agent.getTradingStrategy() + ", " + agent.getHardStopRatio();

            //how much $ does the agent currently have
            result += " - $" + AgentHelper.round(getAssets(agent));

            //add our min value
            result += ",  Min $" + AgentHelper.round(agent.getFundsMin());

            //add our max value
            result += ",  Max $" + AgentHelper.round(agent.getFundsMax());

            //if this agent has stopped trading, include it in the message
            if (agent.hasStopTrading())
                result += ", (Stopped)";

            //make new line
            result += "\n";
        }

        //return our result
        return result;
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

    private double getAssets(Agent agent) {
        return agent.getAssets(getCurrentPrice());
    }

    public PrintWriter getWriter() {
        return this.writer;
    }

    public double getFunds() {
        return (this.funds);
    }

    public void setCurrentPrice(final double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public double getCurrentPrice() {
        return this.currentPrice;
    }

    public Duration getMyDuration() {
        return this.myDuration;
    }

    public String getProductId() {
        return getProduct().getId();
    }

    public Calculator getCalculator() {
        return this.calculator;
    }

    public Product getProduct() {
        return this.product;
    }

    public List<Agent> getAgents() {
        return this.agents;
    }
}