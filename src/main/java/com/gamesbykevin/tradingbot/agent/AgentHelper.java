package com.gamesbykevin.tradingbot.agent;

import com.coinbase.exchange.api.entity.NewLimitOrderSingle;
import com.coinbase.exchange.api.entity.Product;
import com.coinbase.exchange.api.orders.Order;
import com.gamesbykevin.tradingbot.Main;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.strategy.Strategy;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;
import com.gamesbykevin.tradingbot.util.Email;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static com.gamesbykevin.tradingbot.agent.AgentManagerHelper.displayMessage;
import static com.gamesbykevin.tradingbot.util.Email.getFileDateDesc;
import static com.gamesbykevin.tradingbot.wallet.Wallet.STOP_TRADING_RATIO;

public class AgentHelper {

    /**
     * How much do we round the decimals when purchasing stock
     */
    public static final int ROUND_DECIMALS_PRICE = 2;

    /**
     * How much do we round the decimals when choosing quantity
     */
    public static final int ROUND_DECIMALS_QUANTITY = 2;

    /**
     * Our orders are limit orders in order to not have to pay a fee
     */
    private static final String LIMIT_ORDER_DESC = "limit";

    /**
     * This is when we need to specify a hard stop
     */
    private static final String STOP_LOSS_DESC = "loss";

    /**
     * If the stock price increases let's set a bar so in case the price goes back down we can still sell and make some $
     */
    public static float HARD_STOP_RATIO;

    /**
     * Do we want to send a notification for every transaction?
     */
    public static boolean NOTIFICATION_EVERY_TRANSACTION = false;

    //how many times do we check to see if the limit order is successful
    private static final int FAILURE_LIMIT = 20;

    //how long do we wait until between creating orders
    private static final long LIMIT_ORDER_STATUS_DELAY = 250L;

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

    protected static void checkSell(Agent agent, Strategy strategy, List<Period> history, Product product, double currentPrice) {

        //check for a sell signal, if we don't have a reason yet
        if (agent.getReasonSell() == null)
            strategy.checkSellSignal(agent, history, currentPrice);

        //if the current stock price is less than what we paid, we don't want to sell because we would lose $
        if (currentPrice < agent.getWallet().getPurchasePrice())
            agent.setReasonSell(null);

        //if the price dropped below our hard stop, we will sell to cut our losses
        if (currentPrice <= agent.getHardStop()) {

            //reason for selling is that we hit our hard stop
            agent.setReasonSell(ReasonSell.Reason_HardStop);

        } else {

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
        }

        //if there is a reason then we will sell
        if (agent.getReasonSell() != null) {

            //if there is a reason, display message
            displayMessage(agent, agent.getReasonSell().getDescription(), true);

            if (agent.getReasonSell() == ReasonSell.Reason_HardStop) {

                //if the reason for selling is we reached our hard stop price, sell for that hard stop price
                //agent.setOrder(createLimitOrder(agent, Action.Sell, product, agent.getHardStop()));

            } else {

                //any other reason our hard stop will be the current price
                agent.setHardStop(currentPrice);

                //create and assign our limit order
                //agent.setOrder(createLimitOrder(agent, Action.Sell, product, currentPrice));aa
            }

        } else {

        }
    }

    protected static void checkBuy(Agent agent, Strategy strategy, List<Period> history, Product product, double currentPrice) {

        //flag buy false before we check
        agent.setBuy(false);

        //reset our hard stop until we actually buy
        agent.setHardStop(0);

        //check for a buy signal
        strategy.checkBuySignal(agent, history, currentPrice);

        //we will buy if there is a reason
        if (agent.hasBuy()) {

            //let's set our hard stop if it hasn't been set already
            if (agent.getHardStop() == 0)
                agent.setHardStop(currentPrice - (currentPrice * HARD_STOP_RATIO));

            //display which strategy we are using
            displayMessage(agent, " Details: " + strategy.getStrategyDesc(), true);

            //write hard stop amount to our log file
            displayMessage(agent, "Current Price $" + currentPrice + ", Hard stop $" + agent.getHardStop(), true);

            //create and assign our limit order
            agent.setOrder(createLimitOrder(agent, Action.Buy, product, currentPrice));

            //we don't have a reason to sell just yet
            agent.setReasonSell(null);

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

            //we will cancel the buy order if the order has not been resolved
            if (order.getSide().equalsIgnoreCase(Action.Buy.getDescription())) {
                cancelOrder(agent, orderId);
            } else {
                //we don't want to cancel the sell limit order, let's wait until it is created
            }
        }

        //let's say that we are still pending so we continue to wait until we have confirmation of something
        return Status.Pending;
    }

    protected static synchronized void cancelOrder(Agent agent, String orderId) {

        //don't cancel if we are simulating or paper trading
        if (Main.PAPER_TRADING || agent.isSimulation())
            return;

        //we are now going to cancel the order
        displayMessage(agent, "Canceling order: " + orderId, true);

        //cancel the order
        Main.getOrderService().cancelOrder(orderId);

        //notify we sent the message
        displayMessage(agent, "Cancel order message sent", true);
    }

    protected static synchronized Order createLimitOrder(Agent agent, Action action, Product product, double currentPrice) {

        //the price we want to buy/sell
        BigDecimal price = new BigDecimal(currentPrice);

        //what is the quantity that we are buying/selling
        final double quantity;

        //create a penny in case we need to alter the current price
        BigDecimal penny = new BigDecimal(.01);

        switch (action) {

            case Buy:

                //subtract 1 cent
                price = price.subtract(penny);

                //see how much we can buy
                quantity = (agent.getWallet().getFunds() / currentPrice);
                break;

            case Sell:

                //add 1 cent
                price = price.add(penny);

                //sell all the quantity we have
                quantity = agent.getWallet().getQuantity();
                break;

            default:
                throw new RuntimeException("Action not defined: " + action.toString());
        }

        //round few decimals so our numbers aren't
        price = round(ROUND_DECIMALS_PRICE, price);

        //the quantity we want to purchase
        BigDecimal size = round(ROUND_DECIMALS_QUANTITY, quantity);

        //make sure we have enough quantity to buy or else we can't continue
        if (size.doubleValue() < product.getBase_min_size()) {
            displayMessage(agent, "Not enough quantity: " + size.doubleValue() + ", min: " + product.getBase_min_size(), true);
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
        newOrder.setSize(size);

        //if we are selling set a hard stop
        if (action == Action.Sell) {

            //set as stop "loss"
            newOrder.setStop(STOP_LOSS_DESC);

            //our stop price will be slightly above the current sell price, and will be less than the current stock price
            BigDecimal stopPrice = round(ROUND_DECIMALS_PRICE, price.add(new BigDecimal(currentPrice * (HARD_STOP_RATIO / 2))));

            //set the stop price in addition to the order price above, it will be higher than the order price
            newOrder.setStop_price(stopPrice);

        } else {

            //set to post only to avoid fees (buy only)
            newOrder.setPost_only(true);
        }

        //write order details to log
        displayMessage(agent, "Creating order (" + product.getId() + "): " + action.getDescription() + " $" + price.doubleValue() + ", Quantity: " + size.doubleValue(), true);

        //our order object
        Order order = null;

        //how many attempts to try
        int attempts = 0;

        if (Main.PAPER_TRADING || agent.isSimulation()) {

            //if we are paper trading populate the order object ourselves
            order = new Order();
            order.setPrice(price.toString());
            order.setSize(size.toString());
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
        if (agent.getWallet().getFunds() < (STOP_TRADING_RATIO * agent.getWallet().getStartingFunds()) && agent.getWallet().getQuantity() <= 0)
            agent.setStopTrading(true);

        //if our money has gone up, increase the stop trading limit
        if (agent.getWallet().getFunds() > agent.getWallet().getStartingFunds()) {

            final double oldRatio = (STOP_TRADING_RATIO * agent.getWallet().getStartingFunds());
            agent.getWallet().setStartingFunds(agent.getWallet().getFunds());
            final double newRatio = (STOP_TRADING_RATIO * agent.getWallet().getStartingFunds());
            displayMessage(agent, "Good news, stop trading limit has increased", true);
            displayMessage(agent, "    Funds $" + AgentHelper.round(agent.getWallet().getFunds()), true);
            displayMessage(agent, "Old limit $" + AgentHelper.round(oldRatio), true);
            displayMessage(agent, "New limit $" + AgentHelper.round(newRatio), true);
            displayMessage(agent, "If your funds fall below the new limit we will stop trading", true);
        }

        //notify if this agent has stopped trading
        if (agent.hasStopTrading()) {

            String subject = "We stopped trading";
            String text1 = "Funds $" + AgentHelper.round(agent.getWallet().getFunds());
            String text2 = "Limit $" + AgentHelper.round(STOP_TRADING_RATIO * agent.getWallet().getStartingFunds());
            String text3 = "Min $" + AgentHelper.round(agent.getFundsMin());
            String text4 = "Max $" + AgentHelper.round(agent.getFundsMax());
            displayMessage(agent, subject, true);
            displayMessage(agent, text1, true);
            displayMessage(agent, text2, true);
            displayMessage(agent, text3, true);
            displayMessage(agent, text4, true);

            //include the funds in our message
            String message = text1 + "\n" + text2 + "\n" + text3 + "\n" + text4 + "\n";

            //also include the summary of wins/losses
            message += TransactionHelper.getDescWins(agent) + "\n";
            message += TransactionHelper.getDescLost(agent) + "\n";

            //send email notification
            if (!agent.isSimulation())
                Email.sendEmail(subject + " (" + agent.getProductId() + "-" + agent.getTradingStrategy() + ")", message);
        }
    }

    public static BigDecimal round(double number) {
        return round(ROUND_DECIMALS_QUANTITY, number);
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

    protected static String getFileName() {
        return getFileDateDesc() + ".log";
    }
}