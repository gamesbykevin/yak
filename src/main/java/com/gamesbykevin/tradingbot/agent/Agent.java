package com.gamesbykevin.tradingbot.agent;

import com.coinbase.exchange.api.entity.Product;
import com.coinbase.exchange.api.orders.Order;
import com.gamesbykevin.tradingbot.Main;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.strategy.Strategy;
import com.gamesbykevin.tradingbot.transaction.Transaction;
import com.gamesbykevin.tradingbot.transaction.Transaction.Result;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;
import com.gamesbykevin.tradingbot.util.Email;
import com.gamesbykevin.tradingbot.util.LogFile;
import com.gamesbykevin.tradingbot.wallet.Wallet;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import static com.gamesbykevin.tradingbot.agent.AgentManager.TradingStrategy;
import static com.gamesbykevin.tradingbot.agent.AgentHelper.*;
import static com.gamesbykevin.tradingbot.agent.AgentManagerHelper.displayMessage;
import static com.gamesbykevin.tradingbot.wallet.Wallet.STOP_TRADING_RATIO;

public class Agent implements IAgent {

    //list of wallet for each product we are investing
    private Wallet wallet;

    //object used to write to a text file
    private PrintWriter writer;

    //do we have an order?
    private Order order = null;

    //list of transactions
    private List<Transaction> transactions;

    //do we stop trading
    private boolean stopTrading = false;

    //number of attempts we try to verify the order
    private int attempts = 0;

    //the reason why we are selling
    private ReasonSell reason;

    //what is our assigned trading strategy
    private TradingStrategy tradingStrategy = null;

    //the product we are trading
    private final String productId;

    //what is our hard stop amount
    private double hardStop = 0;

    //let's keep track of how low and high our money goes
    private double fundsMin, fundsMax;

    //do we buy stock?
    private boolean buy = false;

    //is this agent used to run simulations
    private final boolean simulation;

    protected Agent(double funds, String productId, boolean simulation) {

        //create new list of transactions
        this.transactions = new ArrayList<>();

        //store the product reference
        this.productId = productId;

        //is this agent running simulations?
        this.simulation = simulation;

        //reset our information
        reset(funds);
    }

    @Override
    public void reset(double funds) {

        //where to put our log file
        String directory = LogFile.LOG_DIRECTORY + "\\" + getProductId() + "\\";

        //our simulation won't have a log file
        if (!isSimulation())
            this.writer = LogFile.getPrintWriter(getFileName(), directory);

        //we don't want to buy when reset
        setBuy(false);

        //we don't want to sell either
        setReasonSell(null);

        //set order null
        this.order = null;

        //we don't want to stop trading
        setStopTrading(false);

        //reset our hard stop
        setHardStop(0);

        //clear our list
        this.transactions.clear();

        //create a wallet so we can track our investments
        this.wallet = new Wallet(funds);

        //assign our min/max when we start
        this.setFundsMin(funds);
        this.setFundsMax(funds);

        //display message and write to file
        //displayMessage(this, "Starting $" + funds, true);
    }

    @Override
    public synchronized void update(Strategy strategy, List<Period> history, Product product, double currentPrice) {

        //skip if we lost too much $
        if (hasStopTrading())
            return;

        //get our current assets
        double assets = getAssets(currentPrice);

        //track our min/max value
        if (assets < getFundsMin()) {

            //set the new minimum if lower than previous
            setFundsMin(assets);

        } else if (assets > getFundsMax()) {

            //set new maximum if greater than previous
            setFundsMax(assets);

        }

        //if we don't have an active order look at the market data for a chance to buy
        if (getOrder() == null) {

            if (getWallet().getQuantity() > 0) {

                //we have quantity let's see if we can sell it
                checkSell(this, strategy, history, product, currentPrice);

            } else {

                //we don't have any quantity so let's see if we can buy
                checkBuy(this, strategy, history, product, currentPrice);

            }

            //reset our attempts counter, which is used when we create a limit order
            setAttempts(0);

        } else {

            //what is the status of our order
            AgentHelper.Status status = null;

            if (Main.PAPER_TRADING || isSimulation()) {

                //if we are paper trading assume the order has been completed
                status = AgentHelper.Status.Filled;

            } else {

                //let's check if our order is complete
                status = updateLimitOrder(this, getOrder().getId());
            }

            //so what do we do now
            switch (status) {

                case Filled:

                    //update our wallet with the order info
                    fillOrder(getOrder(), product);

                    //now that the order has been filled, remove it
                    setOrder(null);
                    break;

                case Rejected:
                case Cancelled:

                    //if the order has been rejected or cancelled we will remove it
                    setOrder(null);
                    break;

                case Open:
                case Pending:
                case Done:

                    //do nothing
                    break;
            }

            //if we lost too much money and have no quantity pending, we will stop trading
            if (getWallet().getFunds() < (STOP_TRADING_RATIO * getWallet().getStartingFunds()) && getWallet().getQuantity() <= 0)
                setStopTrading(true);

            //if our money has gone up, increase the stop trading limit
            if (getWallet().getFunds() > getWallet().getStartingFunds()) {

                final double oldRatio = (STOP_TRADING_RATIO * getWallet().getStartingFunds());
                getWallet().setStartingFunds(getWallet().getFunds());
                final double newRatio = (STOP_TRADING_RATIO * getWallet().getStartingFunds());
                displayMessage(this, "Good news, stop trading limit has increased", true);
                displayMessage(this, "    Funds $" + AgentHelper.formatValue(getWallet().getFunds()), true);
                displayMessage(this, "Old limit $" + AgentHelper.formatValue(oldRatio), true);
                displayMessage(this, "New limit $" + AgentHelper.formatValue(newRatio), true);
                displayMessage(this, "If your funds fall below the new limit we will stop trading", true);
            }

            //notify if this agent has stopped trading
            if (hasStopTrading()) {

                String subject = "We stopped trading";
                String text1 = "Funds $" + AgentHelper.formatValue(getWallet().getFunds());
                String text2 = "Limit $" + AgentHelper.formatValue(STOP_TRADING_RATIO * getWallet().getStartingFunds());
                String text3 = "Min $" + AgentHelper.formatValue(getFundsMin());
                String text4 = "Max $" + AgentHelper.formatValue(getFundsMax());
                displayMessage(this, subject, true);
                displayMessage(this, text1, true);
                displayMessage(this, text2, true);
                displayMessage(this, text3, true);
                displayMessage(this, text4, true);

                //include the funds in our message
                String message = text1 + "\n" + text2 + "\n" + text3 + "\n" + text4 + "\n";

                //also include the summary of wins/losses
                message = message + TransactionHelper.getDescWins(this) + "\n";
                message = message + TransactionHelper.getDescLost(this) + "\n";

                //send email notification
                if (!isSimulation())
                    Email.sendEmail(subject + " (" + getProductId() + "-" + getTradingStrategy() + ")", message);
            }
        }
    }

    private void fillOrder(final Order order, final Product product) {

        if (order.getSide().equalsIgnoreCase(AgentHelper.Action.Buy.getDescription())) {

            //create a new transaction to track
            Transaction transaction = new Transaction();

            //update our transaction
            transaction.update(this, product, order);

            //add to our list
            this.transactions.add(transaction);

        } else if (order.getSide().equalsIgnoreCase(AgentHelper.Action.Sell.getDescription())) {

            //get the most recent transaction so we can complete it
            Transaction transaction = transactions.get(transactions.size() - 1);

            //update our transaction
            transaction.update(this, product, order);

            //display wins and losses
            displayMessage(this, TransactionHelper.getDescWins(this), true);
            displayMessage(this, TransactionHelper.getDescLost(this), true);

            //display average transaction time
            displayMessage(this, TransactionHelper.getAverageDurationDesc(this), true);

            //display the total $ amount invested in stocks
            displayMessage(this, AgentHelper.getStockInvestmentDesc(this), true);

            //display the count and reasons why we sold our stock
            TransactionHelper.displaySellReasonCount(this, Result.Win);
            TransactionHelper.displaySellReasonCount(this, Result.Lose);

        }
    }

    protected double getAssets(double currentPrice) {
        return (getWallet().getQuantity() * currentPrice) + getWallet().getFunds();
    }

    /**
     * Is this agent pending?
     * @return true if it has an outstanding order, or if it has stock quantity
     */
    protected boolean isPending() {
        return (getOrder() != null || getWallet().getQuantity() > 0.0d);
    }

    public void setOrder(final Order order) {
        this.order = order;
    }

    public Order getOrder() {
        return this.order;
    }

    public Wallet getWallet() {
        return this.wallet;
    }

    public void setStopTrading(boolean stopTrading) {
        this.stopTrading = stopTrading;
    }

    public boolean hasStopTrading() {
        return this.stopTrading;
    }

    protected void setAttempts(final int attempts) {
        this.attempts = attempts;
    }

    protected int getAttempts() {
        return this.attempts;
    }

    public void setReasonSell(final ReasonSell reason) {
        this.reason = reason;
    }

    public ReasonSell getReasonSell() {
        return this.reason;
    }

    public List<Transaction> getTransactions() {
        return this.transactions;
    }

    public double getHardStop() {
        return this.hardStop;
    }

    public void setHardStop(double hardStop) {
        this.hardStop = hardStop;
    }

    public double getFundsMax() {
        return this.fundsMax;
    }

    public double getFundsMin() {
        return this.fundsMin;
    }

    public void setFundsMax(double fundsMax) {
        this.fundsMax = fundsMax;
    }

    public void setFundsMin(double fundsMin) {
        this.fundsMin = fundsMin;
    }

    public void setBuy(boolean buy) {
        this.buy = buy;
    }

    public boolean hasBuy() {
        return this.buy;
    }

    public boolean isSimulation() {
        return this.simulation;
    }

    public void setTradingStrategy(TradingStrategy tradingStrategy) {
        this.tradingStrategy = tradingStrategy;
    }

    public TradingStrategy getTradingStrategy() {
        return this.tradingStrategy;
    }

    public String getProductId() {
        return this.productId;
    }

    public PrintWriter getWriter() {
        return this.writer;
    }
}