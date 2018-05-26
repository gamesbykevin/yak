package com.gamesbykevin.tradingbot.agent;

import com.coinbase.exchange.api.entity.NewLimitOrderSingle;
import com.coinbase.exchange.api.entity.Product;
import com.coinbase.exchange.api.orders.Order;
import com.gamesbykevin.tradingbot.Main;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;
import com.gamesbykevin.tradingbot.util.Email;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static com.gamesbykevin.tradingbot.Main.PAPER_TRADING_FEES;
import static com.gamesbykevin.tradingbot.agent.AgentManagerHelper.displayMessage;
import static com.gamesbykevin.tradingbot.wallet.Wallet.STOP_TRADING_RATIO;

public class AgentHelper {

    /**
     * How much do we round the decimals when purchasing stock
     */
    public static final int ROUND_DECIMALS_PRICE = 2;

    /**
     * How much do we round the decimals when choosing quantity
     */
    public static final int ROUND_DECIMALS_QUANTITY = 3;

    /**
     * Our orders are limit orders in order to not have to pay a fee
     */
    private static final String LIMIT_ORDER_DESC = "limit";

    /**
     * If the stock price increases let's set a bar so in case the price goes back down we can still sell and make some $
     */
    public static float HARD_STOP_RATIO;

    /**
     * Do we want to send a notification for every transaction?
     */
    public static boolean NOTIFICATION_EVERY_TRANSACTION = false;

    /**
     * How many times do we check to see if the order was created before failing
     */
    private static final int FAILURE_LIMIT = 5;

    //after creating a limit order, how long do we wait before we check if created (in milliseconds)
    private static final long LIMIT_ORDER_STATUS_DELAY = 250L;

    /**
     * Assume each trade has a 0.3% transaction fee
     */
    private static final float FEE_RATE = .003f;

    /**
     * How many times do we wait for the sell order to fill before we cancel
     */
    public static int SELL_ATTEMPT_LIMIT = 10;

    /**
     * How many current prices do we track looking for a decline when selling?
     */
    public static int CURRENT_PRICE_HISTORY;

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

    protected static synchronized Status updateLimitOrder(final Agent agent, final String orderId) {

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

    protected static synchronized void cancelOrder(Agent agent, String orderId) {

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

    protected static synchronized Order createLimitOrder(Agent agent, Action action, Product product, double currentPrice) {

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

                //see how much we can buy
                size = (float)(agent.getWallet().getFunds() / currentPrice);
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
            agent.setStopTrading(true);

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

                //fees are 0.25% of the total dollar amount you are investing
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

    protected static void checkStanding(Agent agent) {

        //if we lost too much money and have no quantity pending, we will stop trading
        if (agent.getWallet().getFunds() < (STOP_TRADING_RATIO * agent.getWallet().getFundsBeforeTrade()) && agent.getWallet().getQuantity() <= 0)
            agent.setStopTrading(true);

        //if our money has gone up, increase the stop trading limit
        if (agent.getWallet().getFunds() > agent.getWallet().getFundsBeforeTrade()) {

            final double oldRatio = (STOP_TRADING_RATIO * agent.getWallet().getFundsBeforeTrade());
            agent.getWallet().setFundsBeforeTrade(agent.getWallet().getFunds());
            final double newRatio = (STOP_TRADING_RATIO * agent.getWallet().getFundsBeforeTrade());
            displayMessage(agent, "Good news, stop trading limit has increased", true);
            displayMessage(agent, "    Funds $" + AgentHelper.round(agent.getWallet().getFunds()), true);
            displayMessage(agent, "Old limit $" + AgentHelper.round(oldRatio), true);
            displayMessage(agent, "New limit $" + AgentHelper.round(newRatio), true);
            displayMessage(agent, "If your funds fall below the new limit we will stop trading", true);
        }

        //notify if this agent has stopped trading
        if (agent.hasStopTrading()) {

            String subject = "We stopped trading";
            String text6 = "Started $" + AgentHelper.round(agent.getWallet().getInitialFunds());
            String text1 = "Funds   $" + AgentHelper.round(agent.getWallet().getFunds());
            String text2 = "Limit   $" + AgentHelper.round(STOP_TRADING_RATIO * agent.getWallet().getFundsBeforeTrade());
            String text3 = "Min     $" + AgentHelper.round(agent.getFundsMin());
            String text4 = "Max     $" + AgentHelper.round(agent.getFundsMax());
            String text5 = "Fees    $" + AgentHelper.round(TransactionHelper.getTotalFees(agent));
            displayMessage(agent, subject, true);
            displayMessage(agent, text6, true);
            displayMessage(agent, text1, true);
            displayMessage(agent, text2, true);
            displayMessage(agent, text3, true);
            displayMessage(agent, text4, true);
            displayMessage(agent, text5, true);

            //include the funds in our message
            String message = text6 + "\n" + text1 + "\n" + text2 + "\n" + text3 + "\n" + text4 + "\n" + text5 + "\n";

            //also include the summary of wins/losses
            message += TransactionHelper.getDescWins(agent) + "\n";
            message += TransactionHelper.getDescLost(agent) + "\n";

            //send email notification
            Email.sendEmail(subject + " (" + agent.getProductId() + "-" + agent.getTradingStrategy() + "-" + agent.getDuration().description + "-" + agent.getHardStopRatio() + ")", message);
        }
    }

    public static BigDecimal round(double number) {
        return round(ROUND_DECIMALS_QUANTITY, number);
    }

    public static BigDecimal round(int decimals, float number) {
        return round(decimals, BigDecimal.valueOf(number));
    }

    public static BigDecimal round(int decimals, double number) {
        return round(decimals, BigDecimal.valueOf(number));
    }

    public static BigDecimal round(int decimals, BigDecimal number) {

        try {
            return number.setScale(decimals, RoundingMode.HALF_DOWN);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return BigDecimal.valueOf(0d);
    }

    public static String getStockInvestmentDesc(Agent agent) {
        return "Owned Stock: " + round(agent.getWallet().getQuantity());
    }

    protected static boolean hasDecline(double[] price) {

        for (int i = 0; i < price.length - 1; i++) {

            //if the next price is more than the current or 0, we can't confirm decline yet
            if (price[i] < price[i + 1] || price[i] <= 0 || price[i + 1] <= 0)
                return false;
        }

        //every value continued to go down, so we have a decline
        return true;
    }
}