package com.gamesbykevin.tradingbot.agent;

import com.coinbase.exchange.api.entity.NewLimitOrderSingle;
import com.coinbase.exchange.api.entity.Product;
import com.coinbase.exchange.api.orders.Order;
import com.gamesbykevin.tradingbot.Main;
import com.gamesbykevin.tradingbot.calculator.Calculator;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static com.gamesbykevin.tradingbot.agent.AgentManager.displayMessage;
import static com.gamesbykevin.tradingbot.calculator.Calculator.*;

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
     * If the stock price increases let's set a bar so in case the price goes back down we can still sell and make some $
     */
    public static float HARD_STOP_RATIO;

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

    /**
     * The possible status of our limit order
     */
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

    protected static void checkSell(Agent agent, Calculator calculator, Product product, double currentPrice) {

        //do we sell the stock
        agent.setReasonSell(null);

        //check for a sell signal
        calculator.getIndicator(agent.getStrategy()).checkSellSignal(agent, calculator.getHistory(), currentPrice);

        //if the current stock price is less than what we paid, we don't want to sell because we would lose $
        if (currentPrice < agent.getWallet().getPurchasePrice())
            agent.setReasonSell(null);

        //if the price dropped below our hard stop, we will sell to cut our losses
        if (currentPrice <= agent.getHardStop())
            agent.setReasonSell(ReasonSell.Reason_HardStop);

        //what is the increase we check to see if we set a new hard stop amount
        double increase = (agent.getWallet().getPurchasePrice() * HARD_STOP_RATIO);

        //if the price has increased some more, let's set a new hard stop
        if (currentPrice > agent.getHardStop() + increase && currentPrice > agent.getWallet().getPurchasePrice() + increase) {

            //set our new hard stop limit
            agent.setHardStop(agent.getHardStop() + (increase));

            //if the price is higher than the next hard stop, increase the hard stop again to right below the current price
            if (currentPrice > agent.getHardStop() + increase)
                agent.setHardStop(currentPrice - increase);

            //write hard stop amount to our log file
            displayMessage(agent, "New hard stop $" + agent.getHardStop(), true);
        }

        //if there is a reason then we will sell
        if (agent.getReasonSell() != null) {

            //if there is a reason, display message
            displayMessage(agent, agent.getReasonSell().getDescription(), true);

            //create and assign our limit order
            agent.setOrder(createLimitOrder(agent, Action.Sell, product, currentPrice));

        } else {

            //we are still waiting
            displayMessage(agent, "Waiting. Product " + product.getId() + " Current $" + currentPrice + ", Purchase $" + agent.getWallet().getPurchasePrice() + ", Quantity: " + agent.getWallet().getQuantity(), true);
        }
    }

    protected static void checkBuy(Agent agent, Calculator calculator, Product product, double currentPrice) {

        //flag buy false before we check
        agent.setBuy(false);

        //reset our hard stop until we actually buy
        agent.setHardStop(0);

        //check for a buy signal
        calculator.getIndicator(agent.getStrategy()).checkBuySignal(agent, calculator.getHistory(), currentPrice);

        //we will buy if there is a reason
        if (agent.hasBuy()) {

            //let's set our hard stop if it hasn't been set already
            if (agent.getHardStop() == 0)
                agent.setHardStop(currentPrice - (currentPrice * HARD_STOP_RATIO));

            //write hard stop amount to our log file
            displayMessage(agent, "Current Price $" + currentPrice + ", Hard stop $" + agent.getHardStop(), true);

            //create and assign our limit order
            agent.setOrder(createLimitOrder(agent, Action.Buy, product, currentPrice));

        } else {

            //we are still waiting
            displayMessage(agent, "Waiting. Available funds $" + agent.getWallet().getFunds(), false);
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
        displayMessage(agent, "Checking order status: " + order.getStatus() + ", settled: " + order.getSettled() + ", attempt(s): " + agent.getAttempts(), true);

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
            displayMessage(agent, "Canceling order: " + orderId, true);

            //cancel the order
            Main.getOrderService().cancelOrder(orderId);

            //notify we sent the message
            displayMessage(agent, "Cancel order message sent", true);
        }

        //let's say that we are still pending so we continue to wait until we have confirmation of something
        return Status.Pending;
    }

    private static synchronized Order createLimitOrder(Agent agent, Action action, Product product, double currentPrice) {

        //the price we want to buy/sell
        BigDecimal price = new BigDecimal(currentPrice);

        //what is the quantity that we are buying/selling
        final double quantity;

        //create a penny in case we need to alter the current price
        BigDecimal penny = new BigDecimal(.01);

        switch (action) {

            case Buy:

                //add 1 cent
                //price.add(penny);

                //see how much we can buy
                quantity = (agent.getWallet().getFunds() / currentPrice);
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
        if (size.doubleValue() < product.getBase_min_size()) {
            displayMessage(agent, "Not enough quantity: " + size.doubleValue() + ", min: " + product.getBase_min_size(), true);
            return null;
        }

        //create our limit order
        NewLimitOrderSingle limitOrder = new NewLimitOrderSingle();

        //which coin we are trading
        limitOrder.setProduct_id(product.getId());

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
        displayMessage(agent, "Creating limit order (" + product.getId() + "): " + action.getDescription() + " $" + price.doubleValue() + ", Quantity: " + size.doubleValue(), true);

        //our market order
        Order order = null;

        //how many attempts to try
        int attempts = 0;

        if (Main.PAPER_TRADING) {

            //if we are paper trading populate the order object ourselves
            order = new Order();
            order.setPrice(price.toString());
            order.setSize(size.toString());
            order.setProduct_id(product.getId());
            order.setStatus(Status.Done.getDescription());
            order.setSide(action.getDescription());
            order.setType(ORDER_DESC);

        } else {

            //sometimes creating an order doesn't work so we will try more than once if not successful
            while (order == null) {

                //keep track of the number of attempts
                attempts++;

                //notify user we are trying to create the limit order
                displayMessage(agent, "Creating limit order attempt: " + attempts, true);

                try {

                    //create our limit order
                    order = Main.getOrderService().createOrder(limitOrder);

                    //if we got our order, exit loop
                    if (order != null)
                        break;

                } catch (Exception e) {

                    //keep track of any errors
                    displayMessage(e, agent.getWriter());
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
            displayMessage(agent, "Order created status: " + order.getStatus() + ", id: " + order.getId(), true);
        } else {
            displayMessage(agent, "Order NOT created", true);
        }

        //return our order
        return order;
    }

    public static BigDecimal formatValue(final double value) {
        return formatValue(ROUND_DECIMALS_PRICE, value);
    }

    public static BigDecimal formatValue(final int decimals, final double value) {
        try {
            BigDecimal result = BigDecimal.valueOf(value);
            return result.setScale(decimals, RoundingMode.HALF_DOWN);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return BigDecimal.valueOf(0d);
    }

    public static String getStockInvestmentDesc(Agent agent) {
        return "Owned Stock: " + formatValue(agent.getWallet().getQuantity());
    }
}