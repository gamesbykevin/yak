package com.gamesbykevin.tradingbot.wallet;

import com.coinbase.exchange.api.entity.NewLimitOrderSingle;
import com.coinbase.exchange.api.entity.Product;
import com.coinbase.exchange.api.orders.Order;
import com.gamesbykevin.tradingbot.Main;
import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.rsi.Calculator;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static com.gamesbykevin.tradingbot.rsi.Calculator.PERIODS;
import static com.gamesbykevin.tradingbot.util.Email.sendEmail;

public class Wallet {

    //money we have to invest
    private double funds = 0;

    //quantity of stock we bought
    private double quantity = 0;

    //the price we bought the stock
    private double purchasePrice = 0;

    /**
     * The support line meaning the stock is oversold
     */
    public static float SUPPORT_LINE;

    /**
     * The resistance line meaning the stock is overbought
     */
    public static float RESISTANCE_LINE;

    /**
     * The starting ratio point to sell if the stock drops too much to stop the bleeding
     */
    public static float SELL_LOSS_RATIO;

    /**
     * If the stock increases enough we will sell regardless of rsi value
     */
    public static float SELL_GAIN_RATIO;

    /**
     * If we lose an overall % of our funds let's stop the bleeding
     */
    public static float STOP_TRADING_RATIO;

    //how many funds did we start with
    private final double startingFunds;

    //should we stop trading
    private boolean stopTrading = false;

    //how many times we check to see if the limit order is successful
    private static final int FAILURE_LIMIT = 5;

    //how long do we wait until we check the status of our limit order
    private static final long LIMIT_ORDER_STATUS_DELAY = 175L;

    //every order will be a limit order
    private static final String ORDER_DESC = "limit";

    //keep track of our limit order success
    private int sellOrders = 0, sellSuccesses = 0, sellFailures = 0;
    private int buyOrders = 0, buySuccesses = 0, buyFailures = 0;

    public enum Action {

        Buy("buy"),
        Sell("sell");

        private final String description;

        Action(String description) {
            this.description = description;
        }

        private String getDescription() {
            return this.description;
        }
    }

    public enum Status {

        Pending("pending"),
        Open("open"),
        Done("done"),
        Filled("filled"),
        Cancelled("cancelled"),
        Rejected("rejected");

        private final String description;

        Status(String description) {
            this.description = description;
        }

        private String getDescription() {
            return this.description;
        }
    }

    public Wallet(double funds) {
        this.funds = funds;
        this.startingFunds = funds;
    }

    public void setStopTrading(boolean stopTrading) {
        this.stopTrading = stopTrading;
    }

    public boolean hasStopTrading() {
        return this.stopTrading;
    }

    public void setFunds(double funds) {
        this.funds = funds;
    }

    public double getFunds() {
        return this.funds;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public double getQuantity() {
        return this.quantity;
    }

    public synchronized void update(final Agent agent, final Calculator calculator, final Product product, final double currentPrice) {

        String subject = null, text = null;

        //if we have quantity check current stock price
        if (quantity > 0) {

            final double priceHigh = purchasePrice;
            final double priceGain = purchasePrice + (purchasePrice * SELL_GAIN_RATIO);
            final double priceLow = purchasePrice - (purchasePrice * SELL_LOSS_RATIO);

            //do we sell the stock
            boolean sell = false;

            if (currentPrice > priceHigh && calculator.getRsi() >= RESISTANCE_LINE) {

                //it grew enough, sell it
                sell = true;

            } else if (currentPrice >= priceGain) {

                //regardless of rsi, if we made enough money to sell it
                sell = true;

            } else if (currentPrice <= priceLow) {

                //it dropped enough, sell it
                sell = true;

            } else {

                agent.displayMessage("Waiting. Product " + product.getId() + " Current $" + currentPrice + ", Purchase $" + purchasePrice + ", Quantity: " + getQuantity(), true);
            }

            //if we are selling our stock
            if (sell) {

                //are we successful selling
                final boolean success;

                //if we aren't paper trading sell the stock for real
                if (!Main.PAPER_TRADING) {
                    success = createLimitOrder(agent, product, currentPrice, Action.Sell);
                    //agent.displayMessage("Total sell orders : " + this.sellOrders, true);
                    //agent.displayMessage("Total sell success: " + this.sellSuccesses, true);
                    //agent.displayMessage("Total sell failure: " + this.sellFailures, true);
                } else {

                    //when paper trading we are always successful
                    success = true;
                }

                //update bot and funds if we are successful
                if (success) {

                    final double result = (currentPrice * getQuantity());

                    //add the money back to our total funds
                    setFunds(getFunds() + result);

                    final double priceBought = (this.purchasePrice * getQuantity());
                    final double priceSold = (currentPrice * getQuantity());

                    //display money changed
                    if (priceBought > priceSold) {
                        subject = "We lost $" + (priceBought - priceSold);
                    } else {
                        subject = "We made $" + (priceSold - priceBought);
                    }

                    //display the transaction
                    text = "Sell " + product.getId() + " quantity: " + getQuantity() + " @ $" + currentPrice + " remaining funds $" + getFunds();

                    //display message(s)
                    agent.displayMessage(subject, true);
                    agent.displayMessage(text, true);

                    //reset quantity back to 0
                    setQuantity(0);
                }
            }

            //if we lost too much money and have no quantity we will stop trading
            if (getFunds() < (STOP_TRADING_RATIO * startingFunds) && getQuantity() <= 0)
                setStopTrading(true);

            if (hasStopTrading()) {
                subject = "We stopped trading " + product.getId();
                text = "Funds dropped below our comfort level ($" + getFunds() + "). Stopped Trading for " + product.getId();
                agent.displayMessage(text,true);
            }

        } else {

            boolean buy = false;

            //if the stock is oversold we are on the right track
            if (calculator.getRsi() < SUPPORT_LINE) {

                switch (calculator.getTrend()) {

                    case Upward:

                        //if the rsi is low and we see a constant upward trend, we will buy
                        if (calculator.getBreaks() < 1) {
                            buy = true;
                            agent.displayMessage("There is a constant upward trend", true);
                        }
                        break;

                    //we like downward trends
                    case Downward:

                        //there is a downward trend but some breaks so we think it will go back upwards
                        if (calculator.getBreaks() > (int)(PERIODS / 2)) {
                            buy = true;
                            agent.displayMessage("There is a downward trend, but we see at least half of the periods with breaks so we will buy", true);
                        } else if (calculator.getBreaks() < 1) {
                            agent.displayMessage("There is a constant downward trend, and we will wait a little longer to buy", true);
                        } else {
                            agent.displayMessage("There is a downward trend, but not enough breaks to buy", true);
                        }
                        break;
                }

            } else {

                //if there is a constant upward trend lets buy anyway regardless of rsi
                switch (calculator.getTrend()) {
                    case Upward:
                        if (calculator.getBreaks() < 1) {
                            buy = true;
                            agent.displayMessage("There is a constant upward trend, so we will buy", true);
                        }
                        break;
                }
            }

            //are we buying stock
            if (buy) {

                //are we successful buying
                final boolean success;

                //if we aren't paper trading buy the stock for real
                if (!Main.PAPER_TRADING) {
                    success = createLimitOrder(agent, product, currentPrice, Action.Buy);
                    //agent.displayMessage("Total buy orders : " + this.buyOrders, true);
                    //agent.displayMessage("Total buy success: " + this.buySuccesses, true);
                    //agent.displayMessage("Total buy failure: " + this.buyFailures, true);
                } else {

                    //when paper trading we are always successful
                    success = true;

                    //calculate quantity if paper trading
                    setQuantity(getFunds() / currentPrice);
                }

                //update bot and funds if we are successful
                if (success) {

                    //store the purchase price
                    this.purchasePrice = currentPrice;

                    //our funds are now gone since we bought as much stock as possible
                    setFunds(0);

                    //display the transaction
                    agent.displayMessage("Buy " + product.getId() + " quantity: " + getQuantity() + " @ $" + this.purchasePrice, true);
                }

            } else {
                agent.displayMessage("Waiting. Product " + product.getId() + ", Available funds $" + getFunds(), true);
            }
        }

        //send message only if we stopped trading for this coin
        if (subject != null && text != null && hasStopTrading())
            sendEmail(subject, text);
    }

    private synchronized boolean createLimitOrder(final Agent agent, Product product, double currentPrice, Action action) {

        //were we successful
        boolean success;

        //do we cancel the order
        boolean cancel = false;

        //the price we want to buy/sell
        BigDecimal price;

        //what is the quantity that we are buying/selling
        final double tmpQuantity;

        switch (action) {

            case Buy:

                //keep track of our buy orders
                this.buyOrders++;

                //adjust price
                price = new BigDecimal(currentPrice - .01);

                //see how much we can buy
                tmpQuantity = (getFunds() / currentPrice);
                break;

            case Sell:

                //keep track of our sell orders
                this.sellOrders++;

                //adjust price
                price = new BigDecimal(currentPrice + .01);

                //sell all the quantity we have
                tmpQuantity = getQuantity();
                break;

            default:
                throw new RuntimeException("Action not defined: " + action.toString());
        }

        //the quantity we want to purchase
        BigDecimal size = new BigDecimal(tmpQuantity).setScale(4, RoundingMode.HALF_DOWN);

        //make sure we have enough quantity to buy or else we can't continue
        if (size.doubleValue() < product.getBase_min_size()) {
            agent.displayMessage("Not enough quantity: " + size.doubleValue() + ", min: " + product.getBase_min_size(), true);
            return false;
        }

        //create our limit order
        NewLimitOrderSingle limitOrder = new NewLimitOrderSingle();

        //which coin we are trading
        limitOrder.setProduct_id(product.getId());

        //are we buying or selling
        limitOrder.setSide(action.getDescription());

        //this is a limit order
        limitOrder.setType(ORDER_DESC);

        //our price
        limitOrder.setPrice(price);

        //our quantity
        limitOrder.setSize(size);

        //write limit order to log
        agent.displayMessage("Creating limit order (" + product.getId() + "): " + action.getDescription() + " $" + price.toString() + ", Quantity: " + size.doubleValue(), true);

        //our market order
        Order order = null;

        //how many attempts to try
        int attempts = 0;

        //there is a chance the order is null, so we will continue to create until not null
        while (order == null) {

            //create limit order
            order = Main.getOrderService().createOrder(limitOrder);

            //keep track of the number of attempts
            attempts++;

            //notify user we are trying to create the limit order
            agent.displayMessage("Creating limit order attempt: " + attempts, true);

            //if we reach our limit, just stop
            if (attempts >= FAILURE_LIMIT)
                break;

            try {
                //sleep for a short time
                Thread.sleep(LIMIT_ORDER_STATUS_DELAY);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (order != null) {
            //write order status to log
            agent.displayMessage("Order created", true);
            agent.displayMessage("Order status: " + order.getStatus(), true);
        } else {
            agent.displayMessage("Order NOT created", true);
            return false;
        }

        //how many times have we checked the status
        attempts = 0;

        //keep checking the status of our order until we get the result we want
        while(true) {

            //get the order from gdax so we can check the updated status
            order = Main.getOrderService().getOrder(order.getId());

            //write order status to log
            agent.displayMessage("Checking order status: " + order.getStatus() + ", attempts: " + attempts, true);

            try {
                //wait for a brief moment
                Thread.sleep(LIMIT_ORDER_STATUS_DELAY);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (order.getStatus().equalsIgnoreCase(Status.Filled.getDescription())) {

                //we are successful
                success = true;

                //exit loop
                break;

            } else if (order.getStatus().equalsIgnoreCase(Status.Done.getDescription())) {

                //we are successful
                success = true;

                //exit loop
                break;

            } else if (order.getStatus().equalsIgnoreCase(Status.Open.getDescription())) {

                //do nothing

            } else if (order.getStatus().equalsIgnoreCase(Status.Pending.getDescription())) {

                //do nothing

            } else if (order.getStatus().equalsIgnoreCase(Status.Rejected.getDescription())) {

                //we lose
                success = false;

                //cancel order
                cancel = true;

                //exit loop
                break;

            } else if (order.getStatus().equalsIgnoreCase(Status.Cancelled.getDescription())) {

                //we lose
                success = false;

                //exit loop
                break;
            }

            //keep track of our attempts
            attempts++;

            if (attempts >= FAILURE_LIMIT) {

                //we lose
                success = false;

                //cancel the order
                cancel = true;

                //exit loop
                break;
            }
        }

        //are we cancelling the order?
        if (cancel) {

            //cancel the order
            final String result = Main.getOrderService().cancelOrder(order.getId());

            //write to log file
            agent.displayMessage("Order cancelled: " + result, true);
        }

        //keep track of our successes
        switch (action) {

            case Buy:

                //keep track of our buy orders and quantity
                if (success) {
                    setQuantity(size.doubleValue());
                    this.buySuccesses++;
                } else {
                    this.buyFailures++;
                }
                break;

            case Sell:

                //keep track of our sell orders and quantity
                if (success) {
                    this.sellSuccesses++;
                } else {
                    this.sellFailures++;
                }
                break;

            default:
                throw new RuntimeException("Action not defined: " + action.toString());
        }

        return success;
    }

    public int getBuySuccesses() {
        return this.buySuccesses;
    }

    public int getBuyFailures() {
        return this.buyFailures;
    }

    public int getBuyOrders() {
        return this.buyOrders;
    }

    public int getSellSuccesses() {
        return this.sellSuccesses;
    }

    public int getSellFailures() {
        return this.sellFailures;
    }

    public int getSellOrders() {
        return this.sellOrders;
    }
}