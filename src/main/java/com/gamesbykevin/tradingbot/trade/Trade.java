package com.gamesbykevin.tradingbot.trade;

import com.coinbase.exchange.api.orders.Order;
import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.agent.AgentHelper;
import com.gamesbykevin.tradingbot.calculator.Calculator.Candle;
import com.gamesbykevin.tradingbot.order.BasicOrderHelper.Action;
import com.gamesbykevin.tradingbot.order.BasicOrderHelper.Status;
import com.gamesbykevin.tradingbot.trade.TradeHelper.ReasonSell;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static com.gamesbykevin.tradingbot.agent.AgentHelper.CURRENT_PRICE_HISTORY;
import static com.gamesbykevin.tradingbot.agent.AgentHelper.ROUND_DECIMALS_PRICE;
import static com.gamesbykevin.tradingbot.agent.AgentManagerHelper.displayMessage;

public class Trade {

    //what is the lowest and highest $ of the stock during this trade
    private double priceMin;
    private double priceMax;

    //the $ we bought / sold
    private Double priceBuy;
    private Double priceSell;

    //the $ fees
    private Double feeBuy;
    private Double feeSell;

    //the quantity bought / sold
    private Float quantityBuy;
    private Float quantitySell;

    //which product is this trade for
    private final String productId;

    //which candle are we trading
    private final Candle candle;

    //when did this trade start
    private final long start;

    //when did this trade finish?
    private long finish = 0;

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

    //we will sell if the $ goes above this price
    private double hardSellPrice = 0;

    /**
     * How many prices in our history do we look at to confirm we are below the hard stop $
     */
    public static final int HARD_STOP_PRICE_HISTORY_CONFIRM = 5;

    /**
     * How many prices in our history do we look at to confirm the price increase isn't a false signal
     */
    public static final int HARD_STOP_PRICE_INCREASE_CONFIRM = 5;

    public Trade(String productId, Candle candle) {

        //our price history needs to be as long as our hard stop history confirm
        if (CURRENT_PRICE_HISTORY < HARD_STOP_PRICE_HISTORY_CONFIRM)
            throw new RuntimeException("The length of price history (" + CURRENT_PRICE_HISTORY + ") is shorter than the hard stop confirm (" + HARD_STOP_PRICE_HISTORY_CONFIRM + ")");

        //track our product and candle
        this.productId = productId;
        this.candle = candle;

        //track when this trade first started
        this.start = System.currentTimeMillis();

        //create new array to track recent periods
        this.priceHistory = new double[CURRENT_PRICE_HISTORY + 1];

        //reset our values
        restart();
    }

    /**
     * Update the trade only if a buy or sell order was filled
     * @param agent Our agent making the trade
     */
    public void update(final Agent agent) {

        //we can't continue if we don't have an order
        if (agent.getOrder() == null)
            return;

        //we can't continue if the order wasn't completed
        if (!agent.getOrder().getStatus().equalsIgnoreCase(Status.Filled.getDescription()) &&
                !agent.getOrder().getStatus().equalsIgnoreCase(Status.Done.getDescription()))
            return;

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

        //reset the attempts back to 0
        setAttempts(0);
    }

    public Order getOrderBuy() {
        return this.orderBuy;
    }

    public Order getOrderSell() {
        return this.orderSell;
    }

    private void setOrderBuy(Order orderBuy) {
        this.orderBuy = orderBuy;
        feeBuy = new Double(getFee(getOrderBuy()));
        priceBuy = new Double(getPrice(getOrderBuy()));
        quantityBuy = new Float(getQuantity(getOrderBuy()));
    }

    private void setOrderSell(Order orderSell) {
        this.orderSell = orderSell;
        feeSell = new Double(getFee(getOrderSell()));
        priceSell = new Double(getPrice(getOrderSell()));
        quantitySell = new Float(getQuantity(getOrderSell()));
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
        return feeBuy;
    }

    public double getFeeSell() {
        return feeSell;
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
        return priceBuy;
    }

    public double getPriceSell() {
        return priceSell;
    }

    private double getPrice(Order order) {

        if (order == null)
            return 0;

        //get the purchase price from the order and parse
        BigDecimal price = BigDecimal.valueOf(Double.parseDouble(order.getPrice()));

        //let's round the price
        price.setScale(ROUND_DECIMALS_PRICE, RoundingMode.HALF_DOWN);

        //return the price
        return price.doubleValue();
    }

    public float getQuantityBuy() {
        return quantityBuy;
    }

    public float getQuantitySell() {
        return quantitySell;
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

    public double getHardSellPrice() {
        return this.hardSellPrice;
    }

    public void setHardSellPrice(double hardSellPrice) {
        this.hardSellPrice = hardSellPrice;
    }

    private void setResult(Result result) {
        this.result = result;
    }

    public Result getResult() {
        return this.result;
    }

    public void setReasonSell(ReasonSell reason) {
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

    public double getCurrentPrice() {
        return getPriceHistory()[getPriceHistory().length - 1];
    }

    public double[] getPriceHistory() {
        return this.priceHistory;
    }

    public double getPriceHistoryLow() {

        //our final result
        double low = 0;

        //search for the lowest price in our history
        for (int index = 0; index < getPriceHistory().length; index++) {

            //if we haven't found one yet or the current price is lower than previous
            if (low == 0 || getPriceHistory()[index] < low)
                low = getPriceHistory()[index];

        }

        //return result
        return low;
    }

    private void resetPriceHistory() {
        for (int i = 0; i < getPriceHistory().length; i++) {
            getPriceHistory()[i] = 0;
        }
    }

    public void updatePriceHistory(double price) {

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

    /**
     * Look at the latest stock prices to confirm $ is valid<br>
     * We need to check a few prices because sometimes the price will change for 1 second then rebound immediately
     * @param price Current $
     * @return true if the latest prices are above the current price, false otherwise
     */
    public boolean hasConfirmedIncrease(final double price) {

        //look at the most recent historical periods to confirm
        for (int i = 1; i <= HARD_STOP_PRICE_INCREASE_CONFIRM; i++) {

            //get the latest price from the history
            double tmp = getPriceHistory()[getPriceHistory().length - i];

            //if this price is greater than our history, return false
            if (price > tmp || tmp <= 0)
                return false;
        }

        //the current price is below our recent history so we have confirmed increase
        return true;
    }

    /**
     * Look at the latest stock prices to confirm $ is below the hard stop $<br>
     * We need to check a few prices because sometimes the price will change for 1 second then rebound immediately
     * @return true if the latest prices are below the hard stop price, false otherwise
     */
    public boolean hasConfirmedHardStop() {

        //look at the most recent historical periods to confirm
        for (int i = 1; i <= HARD_STOP_PRICE_HISTORY_CONFIRM; i++) {

            //get the latest price from the history
            double price = getPriceHistory()[getPriceHistory().length - i];

            //if the historical price is $0 we don't have enough data yet
            if (price == 0)
                return false;

            //if the historical price is above the hard stop $ we can't confirm
            if (price > getHardStopPrice())
                return false;
        }

        //all recent prices are below so we return true
        return true;
    }

    public double getHardStopPrice() {
        return this.hardStopPrice;
    }

    public void setHardStopPrice(double hardStopPrice) {
        this.hardStopPrice = hardStopPrice;
    }

    public void goShort(Agent agent, final double price) {

        //we only adjust hard stop if it is higher than the previous
        if (price > getHardStopPrice()) {

            //assign our new hard stop
            setHardStopPrice(price);

            //write hard stop amount to our log file
            displayMessage(agent, "New hard stop $" + getHardStopPrice(), true);
        }
    }

    public void restart() {

        //remove our orders
        orderBuy = null;
        orderSell = null;

        //the $ we bought / sold
        priceBuy = null;
        priceSell = null;

        //the quantity bought / sold
        quantityBuy = null;
        quantitySell = null;

        //the $ fees
        feeBuy = null;
        feeSell = null;

        //set default values
        setPriceMin(0);
        setPriceMax(0);

        //reset $
        setHardStopPrice(0);
        setHardSellPrice(0);

        //reset our $ history
        resetPriceHistory();

        //reset attempts
        setAttempts(0);
    }
}