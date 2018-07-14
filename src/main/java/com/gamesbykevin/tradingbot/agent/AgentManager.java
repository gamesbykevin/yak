package com.gamesbykevin.tradingbot.agent;

import com.coinbase.exchange.api.entity.Product;
import com.gamesbykevin.tradingbot.calculator.Calculator;
import com.gamesbykevin.tradingbot.calculator.Calculator.Candle;
import com.gamesbykevin.tradingbot.calculator.Period.Fields;
import com.gamesbykevin.tradingbot.calculator.strategy.Strategy;
import com.gamesbykevin.tradingbot.util.Email;
import com.gamesbykevin.tradingbot.util.LogFile;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import static com.gamesbykevin.tradingbot.agent.AgentHelper.round;
import static com.gamesbykevin.tradingbot.agent.AgentManagerHelper.*;
import static com.gamesbykevin.tradingbot.calculator.Calculation.getRecent;
import static com.gamesbykevin.tradingbot.calculator.Calculator.MY_TRADING_STRATEGIES;
import static com.gamesbykevin.tradingbot.calculator.Calculator.PERIODS_SMA;
import static com.gamesbykevin.tradingbot.trade.TradeHelper.NEW_LINE;
import static com.gamesbykevin.tradingbot.util.LogFile.FILE_SEPARATOR;
import static com.gamesbykevin.tradingbot.util.LogFile.getFilenameManager;

public class AgentManager {

    //our agent list
    private List<Agent> agents;

    //our reference to the calculator
    //private List<Calculator> calculators;
    private Calculator calculator;

    //are we updating the agent?
    private boolean working = false;

    //the product we are trading
    private final Product product;

    //object used to write to a text file
    private PrintWriter writer;

    //how many funds did we start with
    private final double funds;

    //current product price
    private double price;

    //which candle we want to start trading
    public static Candle TRADING_CANDLE;

    //is this the first time checking the x period SMA line
    private boolean initialize = false;

    //is the recent candle close below the x period SMA?
    private boolean belowSMA = false;

    public AgentManager(Product product, double funds) {

        //store the product this agent is trading
        this.product = product;

        //how many funds do we start with?
        this.funds = funds;

        //add 1 calculator (for now)
        this.calculator = new Calculator(TRADING_CANDLE, getProductId(), getWriter());

        //create our list of agents
        this.agents = new ArrayList<>();

        //create an agent for each strategy
        for (int i = 0; i < MY_TRADING_STRATEGIES.length; i++) {

            //create our agent
            Agent agent = new Agent(getFunds(), getProductId(), MY_TRADING_STRATEGIES[i], TRADING_CANDLE);

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

        //store our object references
        this.price = price;

        //flag that this agent is working
        working = true;

        try {

            //update our calculator, etc...
            updateCalculators(this);

            //update our agents
            updateAgents(this);

            //track if we go below / above sma
            checkSMA();

        } catch (Exception ex) {

            //print stack trace and write exception to log file
            ex.printStackTrace();
            displayMessage(ex, getWriter());

        } finally {

            //last step is to make that we are done working
            working = false;
        }
    }

    /**
     * Check if we are above / below our trading sma
     */
    private void checkSMA() {

        if (PERIODS_SMA < 1)
            return;

        //info for our message
        String subject = null, text = null;

        //get the recent values
        final double close = getRecent(getCalculator().getHistory(), Fields.Close);
        final double sma = getRecent(getCalculator().getObjSMA().getSma());

        //if there is a significant change in  SMA notify the user
        if (!initialize || (belowSMA && close > sma) || (!belowSMA && close < sma)) {

            if (close > sma) {

                subject = getProductId() + " is above the " + PERIODS_SMA + " period SMA";
                //text = "We can now resume trading" + NEW_LINE;

                //we are no longer below the sma
                belowSMA = false;

            } else {

                subject = getProductId() + " is below the " + PERIODS_SMA + " period SMA";
                //text = "We will stop trading until it improves" + NEW_LINE;

                //we are below the sma
                belowSMA = true;

            }

            //we are now tracking for a change in $
            initialize = true;

            //show the user our current data
            if (text == null) {
                text = "Close $" + close + ", SMA $" + round(sma);
            } else {
                text += "Close $" + close + ", SMA $" + round(sma);
            }

            //notify
            displayMessage(subject, getWriter());
            displayMessage(text, getWriter());
            Email.sendEmail(subject, text);
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

    public Calculator getCalculator() {
        return this.calculator;
    }

    public double getPrice() {
        return price;
    }
}