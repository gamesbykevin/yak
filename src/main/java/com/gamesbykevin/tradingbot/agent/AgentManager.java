package com.gamesbykevin.tradingbot.agent;

import com.coinbase.exchange.api.entity.Product;
import com.gamesbykevin.tradingbot.MainHelper;
import com.gamesbykevin.tradingbot.calculator.Calculator;
import com.gamesbykevin.tradingbot.calculator.Calculator.Duration;
import com.gamesbykevin.tradingbot.calculator.strategy.Strategy;
import com.gamesbykevin.tradingbot.calculator.strategy.StrategyHelper;
import com.gamesbykevin.tradingbot.util.LogFile;

import java.io.PrintWriter;

import static com.gamesbykevin.tradingbot.Main.FUNDS;
import static com.gamesbykevin.tradingbot.agent.AgentHelper.getFileName;
import static com.gamesbykevin.tradingbot.agent.AgentManagerHelper.displayMessage;
import static com.gamesbykevin.tradingbot.agent.AgentManagerHelper.runSimulation;
import static com.gamesbykevin.tradingbot.calculator.Calculator.HISTORICAL_PERIODS_MINIMUM;
import static com.gamesbykevin.tradingbot.util.LogFile.getFilenameManager;

public class AgentManager {

    //our primary agent used to trade
    private Agent agentPrimary;

    //agent that we use for simulations
    private Agent agentSimulation;

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

    //has the size of the historical data changed
    private boolean dirty = false;

    /**
     * Different trading strategies we can use
     */
    public enum TradingStrategy {

        ADL, ADX, BB, BBER, BBR, EMA, EMAR, EMAS, EMASV, EMV,
        EMVS, HA, MACD, MACDD, MACS, NP, NR, NVI, OA, OBV,
        PVI, RSI, RSIA, RSIM, SO, SOC, SOD, SOEMA, SR, TWO_RSI

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

        //create our agents last
        createAgents();
    }

    private void createAgents() {

        //create our primary agent
        this.agentPrimary = new Agent(getFunds(), getProductId(), false);

        //create our simulation agent
        this.agentSimulation = new Agent(FUNDS, getProductId(), true);
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
                final int sizeNew = getCalculator().getHistory().size();

                //if we haven't detected a change yet, check now
                if (!dirty)
                    dirty = (size != sizeNew);

                if (success) {

                    //rest call is successful
                    displayMessage("Rest call successful. History size: " + sizeNew, (sizeNew != size) ? getWriter() : null);

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

                //trade with the agent as long as we have a strategy
                if (getAgentPrimary().getTradingStrategy() != null) {

                    //get the strategy related to the agent
                    Strategy strategy = getCalculator().getStrategyObj(getAgentPrimary());

                    //update the agent
                    getAgentPrimary().update(strategy, getCalculator().getHistory(), getProduct(), getCurrentPrice());

                }

                //if the agent does not have a trading strategy or does not have any pending transactions and the size of the history has changed
                if (getAgentPrimary().getTradingStrategy() == null || (!getAgentPrimary().isPending() && dirty)) {

                    //if the agent doesn't have a strategy run the simulations to find and assign one
                    runSimulation(this, getAgentSimulation());

                    //get our winning strategy
                    Strategy strategyObj = getCalculator().getStrategyObj(getAgentPrimary());

                    //write the chosen strategy to the console / log
                    displayMessage("Strategy: " + getAgentPrimary().getTradingStrategy() + ", Stop Ratio: " + getAgentPrimary().getHardStopRatio() + ", " + strategyObj.getStrategyDesc(), getWriter());

                    //make sure the correct variables are set
                    StrategyHelper.setupValues(getAgentPrimary().getTradingStrategy(), strategyObj.getIndexStrategy());

                    //now perform our calculation
                    strategyObj.calculate(getCalculator().getHistory());

                    //turn our dirty flag off now that we have run a simulation
                    dirty = false;
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

        //message with all agent totals
        String result = getProductId() + " : " + getAgentPrimary().getTradingStrategy();
        result += " - $" + AgentHelper.round(getAssets(getAgentPrimary()));

        //add our min value
        result += ",  Min $" + AgentHelper.round(getAgentPrimary().getFundsMin());

        //add our max value
        result += ",  Max $" + AgentHelper.round(getAgentPrimary().getFundsMax());

        //if this agent has stopped trading, include it in the message
        if (getAgentPrimary().hasStopTrading())
            result += ", (Stopped)";

        //make new line
        result += "\n";

        //return our result
        return result;
    }

    /**
     * Get the total assets
     * @return The total funds available + the quantity of stock we currently own @ the current stock price
     */
    public double getTotalAssets() {

        //return the total amount
        return getAssets(getAgentPrimary());
    }

    private double getAssets(Agent agent) {
        return agent.getAssets(getCurrentPrice());
    }

    protected PrintWriter getWriter() {
        return this.writer;
    }

    public double getFunds() {
        return (this.funds);
    }

    private Agent getAgentSimulation() {
        return this.agentSimulation;
    }

    protected Agent getAgentPrimary() {
        return this.agentPrimary;
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
}