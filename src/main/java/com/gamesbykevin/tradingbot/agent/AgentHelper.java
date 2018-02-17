package com.gamesbykevin.tradingbot.agent;

import com.coinbase.exchange.api.entity.NewLimitOrderSingle;
import com.coinbase.exchange.api.orders.Order;
import com.gamesbykevin.tradingbot.Main;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static com.gamesbykevin.tradingbot.rsi.Calculator.PERIODS;

public class AgentHelper {

    /**
     * How much do we round the decimals when purchasing stock
     */
    public static final int ROUND_DECIMALS_PRICE = 4;

    /**
     * How much do we round the decimals when choosing quantity
     */
    public static final int ROUND_DECIMALS_QUANTITY = 2;


    /**
     * Every order will be a limit order
     */
    private static final String ORDER_DESC = "limit";

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

    public enum Action {

        Buy("buy"),
        Sell("sell");

        private final String description;

        Action(String description) {
            this.description = description;
        }

        public String getDescription() {
            return this.description;
        }
    }

    //how many times do we check to see if the limit order is successful
    private static final int FAILURE_LIMIT = 10;

    //how long do we wait until between creating orders
    private static final long LIMIT_ORDER_STATUS_DELAY = 250L;

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

    /**
     * Do we want to send a notification for every transaction?
     */
    public static boolean NOTIFICATION_EVERY_TRANSACTION = false;

    protected static void checkSell(final Agent agent) {

        final double priceHigh = agent.getWallet().getPurchasePrice();
        final double priceGain = agent.getWallet().getPurchasePrice() + (agent.getWallet().getPurchasePrice() * SELL_GAIN_RATIO);
        final double priceLow = agent.getWallet().getPurchasePrice() - (agent.getWallet().getPurchasePrice() * SELL_LOSS_RATIO);

        //do we sell the stock
        boolean sell = false;

        //if (agent.getWallet().getCurrentPrice() > priceHigh && agent.getRsiCurrent() >= RESISTANCE_LINE) {

        //if the stock is worth more than what we paid, and we are above the resistance and we see a divergence sell quickly
        if (agent.getWallet().getCurrentPrice() > priceHigh && agent.getCalculator().hasDivergence(true, agent.getWallet().getCurrentPrice(), agent.getRsiCurrent())  && agent.getRsiCurrent() >= RESISTANCE_LINE) {

            agent.displayMessage("We see a divergence in the uptrend", true);

            //it grew enough, sell it
            sell = true;

        } else if (agent.getWallet().getCurrentPrice() >= priceGain) {

            agent.displayMessage("The stock price has exceeded our price gain ratio", true);

            //regardless of rsi, if we made enough money to sell it
            sell = true;

        } else if (agent.getWallet().getCurrentPrice() <= priceLow) {

            agent.displayMessage("We have lost too much, sell now", true);

            //it dropped enough, sell it
            sell = true;
        }

        //are we selling stock?
        if (sell) {

            //create and assign our limit order
            agent.setOrder(createLimitOrder(agent, Action.Sell));

        } else {

            //we are still waiting
            agent.displayMessage("Waiting. Product " + agent.getProductId() + " Current $" + agent.getWallet().getCurrentPrice() + ", Purchase $" + agent.getWallet().getPurchasePrice() + ", Quantity: " + agent.getWallet().getQuantity(), true);
        }
    }

    protected static void checkBuy(final Agent agent) {

        boolean buy = false;

        //if we are at or below the support line, let's check if we are in a good place to buy
        if (agent.getRsiCurrent() < SUPPORT_LINE) {

            //if we have a divergence in our downtrend, let's buy
            if (agent.getCalculator().hasDivergence(false, agent.getWallet().getCurrentPrice(), agent.getRsiCurrent())) {
                agent.displayMessage("We see a divergence in the downtrend", true);
                buy = true;
            }
        }

        switch (agent.getCalculator().getTrend()) {

            //if there is a constant uptrend we will buy regardless of rsi or divergence
            case Upward:

                //if there are no breaks it is constant
                if (agent.getCalculator().getBreaks() < 1) {
                    buy = true;
                    agent.displayMessage("There is a constant upward trend so we will buy", true);
                }
                break;

            //if there is a constant downward trend, let's wait before buying
            case Downward:

                //if there are no breaks it is constant
                if (agent.getCalculator().getBreaks() < 1) {
                    buy = false;
                    agent.displayMessage("There is a constant downward trend with no breaks so we will wait a little longer to buy", true);
                }
        }

        /*
        //if the stock is oversold we are on the right track
        if (agent.getRsiCurrent() < SUPPORT_LINE) {

            switch (agent.getCalculator().getTrend()) {

                case Upward:

                    //if the rsi is low and we see a constant upward trend, we will buy
                    if (agent.getCalculator().getBreaks() < 1) {
                        buy = true;
                        agent.displayMessage("There is a constant upward trend", true);
                    } else {
                        agent.displayMessage("There is an upward trend, but there are " + agent.getCalculator().getBreaks() + " break(s)", true);
                    }
                    break;

                //we like downward trends
                case Downward:

                    //there is a downward trend but some breaks so we think it will go back upwards
                    if (agent.getCalculator().getBreaks() >= (PERIODS / 2)) {
                        buy = true;
                        agent.displayMessage("There is a downward trend, but we see at least half of the periods with breaks so we will buy", true);
                    } else if (agent.getCalculator().getBreaks() < 1) {
                        agent.displayMessage("There is a constant downward trend with no breaks so we will wait a little longer to buy", true);
                    } else {
                        agent.displayMessage("There is a downward trend, but not enough breaks to buy (" + agent.getCalculator().getBreaks() + ")", true);
                    }
                    break;
            }

        } else {

            //if there is a constant upward trend lets buy anyway regardless of rsi
            switch (agent.getCalculator().getTrend()) {
                case Upward:
                    if (agent.getCalculator().getBreaks() < 1) {
                        buy = true;
                        agent.displayMessage("There is a constant upward trend, so we will buy", true);
                    }
                    break;
            }
        }
        */

        //are we buying stock?
        if (buy) {

            //create and assign our limit order
            agent.setOrder(createLimitOrder(agent, Action.Buy));

        } else {

            //we are still waiting
            agent.displayMessage("Waiting. Product " + agent.getProductId() + ", Available funds $" + agent.getWallet().getFunds(), true);
        }
    }

    protected static synchronized Status updateLimitOrder(final Agent agent, final String orderId) {

        //check the current order and let's see if we can tell when it is done
        Order order = Main.getOrderService().getOrder(orderId);

        //if the order was not found it must have been cancelled
        if (order == null)
            return Status.Cancelled;

        //keep track of the number of attempts
        agent.setAttempts(agent.getAttempts() + 1);

        //write order status to log
        agent.displayMessage("Checking order status: " + order.getStatus() + ", settled: " + order.getSettled() + ", attempt(s): " + agent.getAttempts(), true);

        if (order.getStatus().equalsIgnoreCase(Status.Filled.getDescription())) {
            return Status.Filled;
        } else if (order.getStatus().equalsIgnoreCase(Status.Done.getDescription())) {

            //if an order is done an settled we assume success
            if (order.getSettled())
                return Status.Filled;

            return Status.Done;
        } else if (order.getStatus().equalsIgnoreCase(Status.Open.getDescription())) {
            //do nothing
        } else if (order.getStatus().equalsIgnoreCase(Status.Pending.getDescription())) {
            //do nothing
        } else if (order.getStatus().equalsIgnoreCase(Status.Rejected.getDescription())) {
            return Status.Rejected;
        } else if (order.getStatus().equalsIgnoreCase(Status.Cancelled.getDescription())) {
            return Status.Cancelled;
        }

        //if we have exceeded our waiting limit and the order has not settled
        if (agent.getAttempts() >= FAILURE_LIMIT && !order.getSettled()) {

            //we are now going to cancel the order
            agent.displayMessage("Canceling order: " + orderId, true);

            //cancel the order
            Main.getOrderService().cancelOrder(orderId);

            //notify we sent the message
            agent.displayMessage("Cancel order message sent", true);
        }

        //let's say that we are still pending so we continue to wait until we have confirmation of something
        return Status.Pending;
    }

    private static synchronized Order createLimitOrder(final Agent agent, final Action action) {

        //the price we want to buy/sell
        BigDecimal price = new BigDecimal(agent.getWallet().getCurrentPrice());

        //what is the quantity that we are buying/selling
        final double quantity;

        //create a penny in case we need to alter the current price
        BigDecimal penny = new BigDecimal(.01);

        switch (action) {

            case Buy:

                //add 1 cent
                //price.add(penny);

                //see how much we can buy
                quantity = (agent.getWallet().getFunds() / agent.getWallet().getCurrentPrice());
                break;

            case Sell:

                //subtract 1 cent
                //price.subtract(penny);

                //sell all the quantity we have
                quantity = agent.getWallet().getQuantity();
                break;

            default:
                throw new RuntimeException("Action not defined: " + action.toString());
        }

        //round few decimals
        price.setScale(ROUND_DECIMALS_PRICE, RoundingMode.HALF_DOWN);

        //the quantity we want to purchase
        BigDecimal size = new BigDecimal(quantity).setScale(ROUND_DECIMALS_QUANTITY, RoundingMode.HALF_DOWN);

        //make sure we have enough quantity to buy or else we can't continue
        if (size.doubleValue() < agent.getProduct().getBase_min_size()) {
            agent.displayMessage("Not enough quantity: " + size.doubleValue() + ", min: " + agent.getProduct().getBase_min_size(), true);
            return null;
        }

        //create our limit order
        NewLimitOrderSingle limitOrder = new NewLimitOrderSingle();

        //which coin we are trading
        limitOrder.setProduct_id(agent.getProduct().getId());

        //set to post only to avoid fees
        limitOrder.setPost_only(true);

        //are we buying or selling
        limitOrder.setSide(action.getDescription());

        //this is a limit order
        limitOrder.setType(ORDER_DESC);

        //our price
        limitOrder.setPrice(price);

        //our quantity
        limitOrder.setSize(size);

        //write limit order to log
        agent.displayMessage("Creating limit order (" + agent.getProductId() + "): " + action.getDescription() + " $" + price.doubleValue() + ", Quantity: " + size.doubleValue(), true);

        //our market order
        Order order = null;

        //how many attempts to try
        int attempts = 0;

        if (Main.PAPER_TRADING) {

            //if we are paper trading populate the order object ourselves
            order = new Order();
            order.setPrice(price.toString());
            order.setSize(size.toString());
            order.setProduct_id(agent.getProductId());
            order.setStatus(Status.Done.getDescription());
            order.setSide(action.getDescription());
            order.setType(ORDER_DESC);

        } else {

            //sometimes creating an order doesn't work so we will try more than once if not successful
            while (order == null) {

                //keep track of the number of attempts
                attempts++;

                //notify user we are trying to create the limit order
                agent.displayMessage("Creating limit order attempt: " + attempts, true);

                try {

                    //create our limit order
                    order = Main.getOrderService().createOrder(limitOrder);

                    //if we got our order, exit loop
                    if (order != null)
                        break;

                } catch (Exception e) {

                    //keep track of any errors
                    agent.displayMessage(e, true);
                }

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
        }

        //write order result to log
        if (order != null) {
            agent.displayMessage("Order created status: " + order.getStatus() + ", id: " + order.getId(), true);
        } else {
            agent.displayMessage("Order NOT created", true);
        }

        //return our order
        return order;
    }
}