package com.gamesbykevin.tradingbot.trade;

import com.coinbase.exchange.api.orders.Order;
import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.agent.AgentHelper;
import com.gamesbykevin.tradingbot.agent.AgentHelper.Action;
import com.gamesbykevin.tradingbot.calculator.Calculator.Candle;
import com.gamesbykevin.tradingbot.trade.TradeHelper.ReasonSell;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static com.gamesbykevin.tradingbot.agent.AgentHelper.CURRENT_PRICE_HISTORY;
import static com.gamesbykevin.tradingbot.agent.AgentHelper.HARD_STOP_RATIO;
import static com.gamesbykevin.tradingbot.agent.AgentHelper.ROUND_DECIMALS_PRICE;
import static com.gamesbykevin.tradingbot.agent.AgentManagerHelper.displayMessage;

public class Trade {

    //what is the lowest and highest $ of the stock during this trade
    private double priceMin;
    private double priceMax;

    //which product is this trade for
    private final String productId;

    //which candle are we trading
    private final Candle candle;

    //when did this trade start
    private final long start;

    //when did this trade finish?
    private long finish;

    //keep track of how many times our trades were not successful
    private int countRejectedBuy = 0;
    private int countRejectedSell = 0;
    private int countCancelBuy = 0;
    private int countCancelSell = 0;

    //track our successful buy and sell orders
    private Order orderBuy;
    private Order orderSell;

    //how much did we win / lose
    private double amount = 0;

    //how many times have we checked an order for it to fill
    private int attempts = 0;

    /**
     * The result of the trade
     */
    public enum Result {
        Win, Lose
    }

    //the result of the transaction
    private Result result = null;

    //why did we sell
    private ReasonSell reason;

    //track the recent price history
    private double[] priceHistory;

    //what is our hard stop amount
    private double hardStopPrice = 0;

    public Trade(String productId, Candle candle) {

        //track our product and candle
        this.productId = productId;
        this.candle = candle;

        //set default values
        setPriceMin(0);
        setPriceMax(0);

        //track when this trade first started
        this.start = System.currentTimeMillis();

        //create new array to track recent periods
        this.priceHistory = new double[CURRENT_PRICE_HISTORY + 1];
    }

    public void update(final Agent agent) {

        //is this transaction a buy or sell?
        boolean buying  = agent.getOrder().getSide().equalsIgnoreCase(Action.Buy.getDescription());

        //we are buying or selling
        if (buying) {

            //this is our buy order
            setOrderBuy(agent.getOrder());

        } else {

            //this is our sell order
            setOrderSell(agent.getOrder());
            setFinish(System.currentTimeMillis());
        }

        //get the price, quantity, and fee from the order
        final double price = getPrice(agent.getOrder());
        final double fee = getFee(agent.getOrder());
        final float quantity = getQuantity(agent.getOrder());

        if (buying) {

            //subtract the purchase price from our available funds
            agent.getWallet().subtractFunds(price * quantity);

            //subtract the fee from our funds as well
            agent.getWallet().subtractFunds(fee);

            //add the quantity purchased
            agent.getWallet().addQuantity(quantity);

        } else {

            //save the reason for selling
            setReasonSell(agent.getReasonSell());

            //add the sold amount to our available funds
            agent.getWallet().addFunds(price * quantity);

            //subtract the fee from our funds
            agent.getWallet().subtractFunds(fee);

            //subtract the quantity sold
            agent.getWallet().subtractQuantity(quantity);

            //figure out the total price we bought the stock for
            final double bought = (getPriceBuy() * getQuantityBuy());

            //figure out the total price we sold the stock for
            final double sold = (getPriceSell() * getQuantitySell());

            //what is the total amount of fees paid
            final double totalFees = (getFeeBuy() + getFeeSell());

            //did we win or lose?
            if (bought > sold - totalFees) {

                //track our amount lost not including fees
                setAmount(bought - sold);

                //assign the result
                setResult(Result.Lose);

            } else {

                //track our amount won not including fees
                setAmount(sold - bought);

                //assign the result
                setResult(Result.Win);
            }


        }
    }

    public Order getOrderBuy() {
        return this.orderBuy;
    }

    private void setOrderBuy(Order orderBuy) {
        this.orderBuy = orderBuy;
    }

    public Order getOrderSell() {
        return this.orderSell;
    }

    private void setOrderSell(Order orderSell) {
        this.orderSell = orderSell;
    }

    public void setFinish(long finish) {
        this.finish = finish;
    }

    private long getFinish() {
        return this.finish;
    }

    private long getStart() {
        return this.start;
    }

    public double getPriceMin() {
        return this.priceMin;
    }

    public void setPriceMin(double priceMin) {
        this.priceMin = priceMin;
    }

    public double getPriceMax() {
        return this.priceMax;
    }

    public void setPriceMax(double priceMax) {
        this.priceMax = priceMax;
    }

    public void checkPriceMinMax(double price) {

        if (price < getPriceMin()) {
            setPriceMin(price);
        } else if (price > getPriceMax()) {
            setPriceMax(price);
        }
    }

    public String getProductId() {
        return this.productId;
    }

    public Candle getCandle() {
        return this.candle;
    }

    public double getFeeBuy() {
        return getFee(orderBuy);
    }

    public double getFeeSell() {
        return getFee(orderSell);
    }

    private double getFee(Order order) {

        double fee = 0;

        try {

            //parse the sell fee
            fee = Double.parseDouble(order.getFill_fees());

        } catch (NumberFormatException e) {

            //unable to parse, fee is 0
            fee = 0;
        }

        //return our fee
        return fee;
    }

    public double getPriceBuy() {
        return getPrice(getOrderBuy());
    }

    public double getPriceSell() {
        return getPrice(getOrderSell());
    }

    private double getPrice(Order order) {

        //get the purchase price from the order and parse
        BigDecimal price = BigDecimal.valueOf(Double.parseDouble(order.getPrice()));

        //let's round the price
        price.setScale(ROUND_DECIMALS_PRICE, RoundingMode.HALF_DOWN);

        //return the price
        return price.doubleValue();
    }

    public float getQuantityBuy() {
        return getQuantity(getOrderBuy());
    }

    public float getQuantitySell() {
        return getQuantity(getOrderSell());
    }

    private float getQuantity(Order order) {

        //get the order size and the filled size
        double sizeOrder = Double.parseDouble(order.getSize());
        double sizeFilled = Double.parseDouble(order.getFilled_size());

        //what is the quantity of the order
        BigDecimal quantity;

        //if the size of the order doesn't match the amount that was filled
        if (sizeOrder != sizeFilled) {

            //only the filled size was actually used so let's get the quantity correct
            quantity = BigDecimal.valueOf(sizeFilled);

        } else {

            //we will use the order size
            quantity = BigDecimal.valueOf(sizeOrder);

            //then we will round the quantity
            quantity.setScale(AgentHelper.ROUND_DECIMALS_QUANTITY, RoundingMode.HALF_DOWN);
        }

        //return our quantity
        return quantity.floatValue();
    }

    public int getCountRejectedBuy() {
        return this.countRejectedBuy;
    }

    public int getCountRejectedSell() {
        return this.countRejectedSell;
    }

    public int getCountCancelBuy() {
        return this.countCancelBuy;
    }

    public int getCountCancelSell() {
        return this.countCancelSell;
    }

    public void addCountRejectedBuy() {
        this.countRejectedBuy++;
    }

    public void addCountRejectedSell() {
        this.countRejectedSell++;
    }

    public void addCountCancelBuy() {
        this.countCancelBuy++;
    }

    public void addCountCancelSell() {
        this.countCancelSell++;
    }

    private void setResult(Result result) {
        this.result = result;
    }

    public Result getResult() {
        return this.result;
    }

    private void setReasonSell(ReasonSell reason) {
        this.reason = reason;
    }

    public ReasonSell getReasonSell() {
        return this.reason;
    }

    public double getAmount() {
        return this.amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public long getDuration() {
        return getFinish() - getStart();
    }

    public int getAttempts() {
        return this.attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public double[] getPriceHistory() {
        return this.priceHistory;
    }

    public void addPriceHistory(double price) {

        //we don't need to add if it matches the latest price
        if (price == getPriceHistory()[getPriceHistory().length - 1])
            return;

        //update every period in our array
        for (int i = 0; i < getPriceHistory().length - 1; i++) {

            //move the value back
            getPriceHistory()[i] = getPriceHistory()[i + 1];
        }

        //add the new price to the end
        getPriceHistory()[getPriceHistory().length - 1] = price;
    }

    public double getHardStopPrice() {
        return this.hardStopPrice;
    }

    public void setHardStopPrice(double hardStopPrice) {
        this.hardStopPrice = hardStopPrice;
    }

    public void adjustHardStopPrice(Agent agent, double newPrice) {

        //what is the increase we check to see if we set a new hard stop amount
        double increase = (agent.getWallet().getPurchasePrice() * HARD_STOP_RATIO);

        //if the price has increased some more, let's set a new hard stop
        if (newPrice > getHardStopPrice() + increase && newPrice > agent.getWallet().getPurchasePrice() + increase) {

            //set our new hard stop limit slightly below the current stock price
            setHardStopPrice(newPrice - increase);

            //write hard stop amount to our log file
            displayMessage(agent, "New hard stop $" + getHardStopPrice(), true);
        }
    }
}