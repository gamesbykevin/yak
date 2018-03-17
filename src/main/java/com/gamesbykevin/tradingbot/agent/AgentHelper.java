package com.gamesbykevin.tradingbot.agent;

import com.coinbase.exchange.api.entity.NewLimitOrderSingle;
import com.coinbase.exchange.api.entity.Product;
import com.coinbase.exchange.api.orders.Order;
import com.gamesbykevin.tradingbot.Main;
import com.gamesbykevin.tradingbot.calculator.Calculator;
import com.gamesbykevin.tradingbot.calculator.EMA;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonBuy;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static com.gamesbykevin.tradingbot.agent.AgentManager.displayMessage;
import static com.gamesbykevin.tradingbot.calculator.Calculator.PERIODS_MACD;
import static com.gamesbykevin.tradingbot.calculator.Calculator.PERIODS_RSI;

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
     * If the stock increases enough we will sell regardless of calculator value
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

        final double priceHigh = agent.getWallet().getPurchasePrice();
        final double priceGain = agent.getWallet().getPurchasePrice() + (agent.getWallet().getPurchasePrice() * SELL_GAIN_RATIO);
        final double priceLow = agent.getWallet().getPurchasePrice() - (agent.getWallet().getPurchasePrice() * SELL_LOSS_RATIO);

        //do we sell the stock
        agent.setReasonSell(null);

        //our strategy will determine how we trade
        switch (agent.getStrategy()) {

            case RSI_MACD:

                //let's see if we are above resistance line before selling
                if (calculator.getCurrentRsi() >= RESISTANCE_LINE) {

                    //if the price is greater than what we paid and we are showing a downward divergence
                    if (currentPrice > priceHigh && calculator.hasMacdConvergenceDivergence(false, PERIODS_MACD, currentPrice))
                        agent.setReasonSell(ReasonSell.Reason_8);
                }
                break;

            case EMA:

                //if the price is greater than what we paid and  we have a bearish crossover, we expect price to go down
                if (currentPrice > priceHigh && calculator.hasEmaCrossover(false)) {

                    agent.setReasonSell(ReasonSell.Reason_3);

                    //display the recent ema values which we use as a signal
                    EMA.displayEma(agent, "EMA Short: ", calculator.getEmaShort(), true);
                    EMA.displayEma(agent, "EMA Long: ", calculator.getEmaLong(), true);

                } else {

                    //display the recent ema values which we use as a signal
                    EMA.displayEma(agent, "EMA Short: ", calculator.getEmaShort(), false);
                    EMA.displayEma(agent, "EMA Long: ", calculator.getEmaLong(), false);
                }
                break;

            case OBV:

                //if the price is greater than what we paid and the volume has a divergence let's sell
                if (currentPrice > priceHigh && calculator.hasDivergenceObv(true))
                    agent.setReasonSell(ReasonSell.Reason_7);

                break;

            case RSI:

                //let's see if we are above resistance line before selling
                if (calculator.getCurrentRsi() >= RESISTANCE_LINE) {

                    //if the price is higher than purchase and there is a rsi divergence
                    if (currentPrice > priceHigh && calculator.hasDivergenceRsi(true))
                        agent.setReasonSell(ReasonSell.Reason_6);
                }

                //display rsi value
                displayMessage(agent, "RSI: " + calculator.getCurrentRsi(), true);
                break;

            case MACD:

                //if the price is higher than purchase and we have a bearish crossover, we expect price to go down
                if (currentPrice > priceHigh && calculator.hasMacdCrossover(false)) {

                    agent.setReasonSell(ReasonSell.Reason_4);

                    //display the recent ema values which we use as a signal
                    EMA.displayEma(agent, "MACD Line: ", calculator.getMacdLine(), true);
                    EMA.displayEma(agent, "Signal Line: ", calculator.getSignalLine(), true);

                } else {

                    //display the recent ema values which we use as a signal
                    EMA.displayEma(agent, "MACD Line: ", calculator.getMacdLine(), false);
                    EMA.displayEma(agent, "Signal Line: ", calculator.getSignalLine(), false);
                }

                //if no reason to sell yet, check if the price drops below the ema values
                if (agent.getReasonSell() == null) {

                    //get the current ema long and short values
                    double emaLong = calculator.getEmaLong().get(calculator.getEmaLong().size() - 1);
                    double emaShort = calculator.getEmaShort().get(calculator.getEmaShort().size() - 1);

                    //if the current price went below the ema long and short values, we need to exit
                    if (currentPrice < emaLong && currentPrice < emaShort) {

                        //display values
                        EMA.displayEma(agent, "EMA Short", calculator.getEmaShort(), true);
                        EMA.displayEma(agent, "EMA Long", calculator.getEmaLong(), true);
                        displayMessage(agent, "Current price: $" + currentPrice, true);

                        //assign our reason to sell
                        agent.setReasonSell(ReasonSell.Reason_5);
                    }
                }
                break;

            default:
                throw new RuntimeException("Strategy not found: " + agent.getStrategy());
        }

        //if no reason to sell yet, check these to see if we made enough money or lost enough
        if (agent.getReasonSell() == null) {
            if (currentPrice >= priceGain) {
                agent.setReasonSell(ReasonSell.Reason_1);
            } else if (currentPrice <= priceLow) {
                agent.setReasonSell(ReasonSell.Reason_2);
            }
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

        //check for reasons first
        agent.setReasonBuy(null);

        //our strategy will determine how we trade
        switch (agent.getStrategy()) {

            case RSI_MACD:

                //let's see if we are below support line before buying
                if (calculator.getCurrentRsi() <= SUPPORT_LINE) {

                    //if we are showing an upward divergence
                    if (calculator.hasMacdConvergenceDivergence(true, PERIODS_MACD, currentPrice))
                        agent.setReasonBuy(ReasonBuy.Reason_5);
                }
                break;

            case EMA:

                //if we have a bullish crossover, we expect price to go up
                if (calculator.hasEmaCrossover(true)) {

                    //display the recent ema values which we use as a signal
                    EMA.displayEma(agent, "EMA Short: ", calculator.getEmaShort(), true);
                    EMA.displayEma(agent, "EMA Long: ", calculator.getEmaLong(), true);

                    //assign our reason for buying
                    agent.setReasonBuy(ReasonBuy.Reason_1);

                } else {

                    //display the recent ema values which we use as a signal
                    EMA.displayEma(agent, "EMA Short: ", calculator.getEmaShort(), false);
                    EMA.displayEma(agent, "EMA Long: ", calculator.getEmaLong(), false);
                }
                break;

            case OBV:

                //if volume has a divergence let's buy
                if (calculator.hasDivergenceObv(false))
                    agent.setReasonBuy(ReasonBuy.Reason_4);

                break;

            case RSI:

                //if we are at or below the support line, let's check if we are in a good place to buy
                if (calculator.getCurrentRsi() <= SUPPORT_LINE) {

                    //if we have a divergence in our downtrend, let's buy
                    if (calculator.hasDivergenceRsi(false))
                        agent.setReasonBuy(ReasonBuy.Reason_3);
                }

                //display rsi value
                displayMessage(agent, "RSI: " + calculator.getCurrentRsi(), (agent.getReasonBuy() != null));
                break;

            case MACD:

                if (calculator.hasMacdCrossover(true)) {

                    //display the recent ema values which we use as a signal
                    EMA.displayEma(agent, "MACD Line: ", calculator.getMacdLine(), true);
                    EMA.displayEma(agent, "Signal Line: ", calculator.getSignalLine(), true);

                    agent.setReasonBuy(ReasonBuy.Reason_2);

                } else {

                    //display the recent ema values which we use as a signal
                    EMA.displayEma(agent, "MACD Line: ", calculator.getMacdLine(), false);
                    EMA.displayEma(agent, "Signal Line: ", calculator.getSignalLine(), false);
                }
                break;

            default:
                throw new RuntimeException("Strategy not found: " + agent.getStrategy());
        }

        //we will buy if there is a reason
        if (agent.getReasonBuy() != null) {

            //if there is a reason display it
            displayMessage(agent, agent.getReasonBuy().getDescription(), true);

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
                    displayMessage(e, true, agent.getWriter());
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
        BigDecimal result = BigDecimal.valueOf(value);
        return result.setScale(decimals, RoundingMode.HALF_DOWN);
    }

    public static String getStockInvestmentDesc(Agent agent) {
        return "Owned Stock: " + formatValue(agent.getWallet().getQuantity());
    }
}