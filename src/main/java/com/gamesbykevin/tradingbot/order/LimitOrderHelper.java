package com.gamesbykevin.tradingbot.order;

import com.coinbase.exchange.api.entity.NewLimitOrderSingle;
import com.coinbase.exchange.api.entity.Product;
import com.coinbase.exchange.api.orders.Order;
import com.gamesbykevin.tradingbot.Main;
import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.util.Email;

import java.math.BigDecimal;

import static com.gamesbykevin.tradingbot.Main.PAPER_TRADING_FEES;
import static com.gamesbykevin.tradingbot.agent.AgentHelper.ROUND_DECIMALS_PRICE;
import static com.gamesbykevin.tradingbot.agent.AgentHelper.ROUND_DECIMALS_QUANTITY;
import static com.gamesbykevin.tradingbot.agent.AgentHelper.round;
import static com.gamesbykevin.tradingbot.agent.AgentManagerHelper.displayMessage;

public class LimitOrderHelper extends BasicOrderHelper {

    /**
     * Our orders are limit orders in order to not have to pay a fee
     */
    private static final String LIMIT_ORDER_DESC = "limit";

    /**
     * Assume each trade has a 0.3% transaction fee
     */
    private static final float FEE_RATE = .003f;

    /**
     * How many times do we check to see if the order was created before failing
     */
    private static final int FAILURE_LIMIT = 5;

    //after creating a limit order, how long do we wait before we check if created (in milliseconds)
    private static final long LIMIT_ORDER_STATUS_DELAY = 250L;

    public static synchronized Order createLimitOrder(Agent agent, Action action, Product product, double currentPrice, boolean aboveSMA) {

        //the price we want to buy/sell
        BigDecimal price = new BigDecimal(currentPrice);

        //what is the quantity that we are buying/selling
        final float size;

        //create a penny in case we need to alter the current price
        BigDecimal penny;

        //if we are treating this as a market order we don't need to adjust the $
        if (PAPER_TRADING_FEES) {
            penny = new BigDecimal(0);
        } else {
            penny = new BigDecimal(.01);
        }

        switch (action) {

            case Buy:

                //subtract a penny so when the price meets or goes below it executes
                price = price.subtract(penny);

                //see how much we can buy based on our risk ratio
                if (aboveSMA) {
                    size = (float)((agent.getWallet().getFunds() * TRADE_RISK_RATIO_ABOVE_SMA) / currentPrice);
                } else {
                    size = (float)((agent.getWallet().getFunds() * TRADE_RISK_RATIO_BELOW_SMA) / currentPrice);
                }
                break;

            case Sell:

                //add a penny so when the price meets or exceeds above it executes
                price = price.add(penny);

                //sell all the quantity we have
                size = agent.getWallet().getQuantity();
                break;

            default:
                throw new RuntimeException("Action not defined: " + action.toString());
        }

        //round few decimals so our numbers aren't
        price = round(ROUND_DECIMALS_PRICE, price);

        //the quantity we want to purchase
        BigDecimal quantity = round(ROUND_DECIMALS_QUANTITY, size);

        //make sure we have enough quantity to buy or else we can't continue
        if (quantity.floatValue() < product.getBase_min_size()) {

            //create our message
            final String message = "Not enough quantity: " + quantity.floatValue() + ", min: " + product.getBase_min_size();

            //write message to log file
            displayMessage(agent, message, true);

            //stop trading
            agent.setStop(true);

            //send notification message
            Email.sendEmail("We stopped trading because we are unable to " + action.getDescription() + " " + product.getId(), message);

            //no order is created so we return null
            return null;
        }

        //create our limit order
        NewLimitOrderSingle newOrder = new NewLimitOrderSingle();

        //which coin we are trading
        newOrder.setProduct_id(product.getId());

        //are we buying or selling
        newOrder.setSide(action.getDescription());

        //type of order "limit", "stop", etc...
        newOrder.setType(LIMIT_ORDER_DESC);

        //our price
        newOrder.setPrice(price);

        //our quantity
        newOrder.setSize(quantity);

        //set to post only to avoid fees
        newOrder.setPost_only(true);

        //write order details to log
        displayMessage(agent, "Creating order (" + product.getId() + "): " + action.getDescription() + " $" + price.doubleValue() + ", Quantity: " + quantity.floatValue(), true);

        //our order object
        Order order = null;

        //how many attempts to try
        int attempts = 0;

        if (Main.PAPER_TRADING) {

            //if we are paper trading populate the order object ourselves
            order = new Order();
            order.setPrice(price.toString());
            order.setSize(quantity.toString());
            order.setFilled_size(quantity.toString());

            //are we applying fees to this order
            if (PAPER_TRADING_FEES) {

                //fees are a % of the total dollar amount you are investing
                double fee = (price.doubleValue() * quantity.floatValue()) * FEE_RATE;
                order.setFill_fees(fee + "");

            } else {
                order.setFill_fees("0");
            }

            order.setProduct_id(product.getId());
            order.setStatus(Status.Done.getDescription());
            order.setSide(action.getDescription());
            order.setType(LIMIT_ORDER_DESC);

        } else {

            //sometimes creating an order doesn't work so we will try more than once if not successful
            while (order == null) {

                //keep track of the number of attempts
                attempts++;

                //notify user we are trying to create the order
                displayMessage(agent, "Creating order attempt: " + attempts, true);

                try {

                    //create our limit order
                    order = Main.getOrderService().createOrder(newOrder);

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

    public static synchronized Status updateLimitOrder(final Agent agent, final String orderId) {

        //check the current order and let's see if we can tell when it is done
        Order order = Main.getOrderService().getOrder(orderId);

        //if the order was not found it must have been cancelled
        if (order == null)
            return Status.Cancelled;

        //write order status to log
        displayMessage(agent, "Checking order status: " + order.getStatus() + ", settled: " + order.getSettled(), true);

        //if the order was successful, update our local order instance
        if (order.getStatus().equalsIgnoreCase(Status.Filled.getDescription()) ||
                order.getStatus().equalsIgnoreCase(Status.Done.getDescription()) && order.getSettled()) {
            agent.getOrder().setFilled_size(order.getFilled_size());
            agent.getOrder().setFill_fees(order.getFill_fees());
            agent.getOrder().setPrice(order.getPrice());
            agent.getOrder().setSize(order.getSize());
        }

        //is this order settled
        agent.getOrder().setSettled(order.getSettled());

        if (order.getStatus().equalsIgnoreCase(Status.Filled.getDescription())) {

            //return that the order has been filled
            return Status.Filled;

        } else if (order.getStatus().equalsIgnoreCase(Status.Done.getDescription())) {

            //if an order is done and settled we assume success
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

        //let's say that we are still pending so we continue to wait until we have confirmation of something
        return Status.Pending;
    }

    public static synchronized void cancelOrder(Agent agent, String orderId) {

        //don't cancel if we are simulating or paper trading
        if (Main.PAPER_TRADING)
            return;

        //we are now going to cancel the order
        displayMessage(agent, "Canceling order: " + orderId, true);

        //cancel the order
        final String result = Main.getOrderService().cancelOrder(orderId);

        //notify we sent the message
        displayMessage(agent, "Cancel order message sent", true);

        //if we receive a result back, display it
        if (result != null)
            displayMessage(agent, result, true);
    }
}