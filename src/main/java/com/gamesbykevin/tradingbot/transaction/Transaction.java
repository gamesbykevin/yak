package com.gamesbykevin.tradingbot.transaction;

import com.coinbase.exchange.api.orders.Order;
import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.agent.AgentHelper;

import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonBuy;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static com.gamesbykevin.tradingbot.agent.AgentHelper.NOTIFICATION_EVERY_TRANSACTION;
import static com.gamesbykevin.tradingbot.util.Email.sendEmail;

public class Transaction {

    //track time of the transaction
    private long start = 0, finish = 0;

    //store our buy and sell orders
    private Order buy, sell;

    /**
     * The result of the transaction
     */
    public enum Result {
        Win, Lose
    }

    //the result of the transaction
    private Result result = null;

    //the amount of the transaction
    private double amount;

    //the current rsi values at buy and sell
    private double rsiBuy, rsiSell;

    //the reason why we bought
    private TransactionHelper.ReasonBuy reasonBuy;

    //the reason why we sold
    private TransactionHelper.ReasonSell reasonSell;

    public Transaction() {
        //default constructor
    }

    public void setResult(final Result result) {
        this.result = result;
    }

    public Result getResult() {
        return this.result;
    }

    private void setBuy(final Order buy) {
        this.buy = buy;

        //store the time of the transaction
        this.start = System.currentTimeMillis();
    }

    public Order getBuy() {
        return this.buy;
    }

    private void setSell(final Order sell) {
        this.sell = sell;

        //store the time of the transaction
        this.finish = System.currentTimeMillis();
    }

    public Order getSell() {
        return this.sell;
    }

    private void setAmount(final double amount) {
        this.amount = amount;
    }

    public double getAmount() {
        return this.amount;
    }

    public void update(final Agent agent, final Order order) {

        //our notification messages
        String subject = null, text = null, summary = "";

        //get the purchase price from the order
        BigDecimal price = BigDecimal.valueOf(Double.parseDouble(order.getPrice()));
        price.setScale(AgentHelper.ROUND_DECIMALS_PRICE, RoundingMode.HALF_DOWN);

        //get the quantity from the order
        BigDecimal quantity = BigDecimal.valueOf(Double.parseDouble(order.getSize()));
        quantity.setScale(AgentHelper.ROUND_DECIMALS_QUANTITY, RoundingMode.HALF_DOWN);

        if (order.getSide().equalsIgnoreCase(AgentHelper.Action.Buy.getDescription())) {

            //assign our buy order
            setBuy(order);

            //store rsi for the buy transaction
            setRsiBuy(agent.getRsiCurrent());

            //assign our reason for buying
            setReasonBuy(agent.getReasonBuy());

            //if our buy order has been filled, update our wallet to have the current purchase price
            agent.getWallet().setPurchasePrice(price.doubleValue());

            //update our available funds based on our purchase
            agent.getWallet().setFunds(agent.getWallet().getFunds() - (price.doubleValue() * quantity.doubleValue()));

            //add the quantity to our wallet
            agent.getWallet().setQuantity(agent.getWallet().getQuantity() + quantity.doubleValue());

            //setup our notification message
            subject = "Purchase " + agent.getProductId();
            text = "Buy " + agent.getProductId() + " quantity: " + quantity + " @ $" + agent.getWallet().getPurchasePrice();

            //display the transaction
            agent.displayMessage(text, true);

        } else if (order.getSide().equalsIgnoreCase(AgentHelper.Action.Sell.getDescription())) {

            //assign our sell order
            setSell(order);

            //store rsi for the sell transaction
            setRsiSell(agent.getRsiCurrent());

            //assign our reason for selling
            setReasonSell(agent.getReasonSell());

            //if our sell order has been filled, update our wallet with our new funds
            agent.getWallet().setFunds(agent.getWallet().getFunds() + (price.doubleValue() * quantity.doubleValue()));

            //update the quantity as well
            agent.getWallet().setQuantity(agent.getWallet().getQuantity() - quantity.doubleValue());

            //figure out the total price we bought the stock for
            final double bought = (agent.getWallet().getPurchasePrice() * quantity.doubleValue());

            //figure out the total price we sold the stock for
            final double sold = (price.doubleValue() * quantity.doubleValue());

            agent.displayMessage("Reason buy: " + agent.getReasonBuy().getDescription(), true);
            agent.displayMessage("Reason sell: " + agent.getReasonSell().getDescription(), true);

            //did we win or lose?
            if (bought > sold) {

                //track our amount
                setAmount(bought - sold);

                //assign the result
                setResult(Result.Lose);

                //setup our subject
                subject = "We lost $" + getAmount();

            } else {

                //track our amount
                setAmount(sold - bought);

                //assign the result
                setResult(Result.Win);

                //we win
                subject = "We made $" + getAmount();
            }

            //the transaction description
            text = "Sell " + agent.getProductId() + " quantity: " + quantity + " @ $" + price + ", purchase $" + agent.getWallet().getPurchasePrice() + ", remaining funds $" + agent.getWallet().getFunds();

            //include the duration description
            summary = getDurationSummaryDesc(getDuration());
            agent.displayMessage(summary, true);

        } else {
            throw new RuntimeException("Side not handled here: " + order.getSide());
        }

        //display and write to log
        agent.displayMessage(subject, true);
        agent.displayMessage(text, true);
        agent.displayMessage("RSI: Buy - " + getRsiBuy() + ", Sell - " + getRsiSell(), true);

        //are we going to notify every transaction?
        if (NOTIFICATION_EVERY_TRANSACTION && subject != null && text != null)
            sendEmail(subject, text + "\n" + summary);
    }

    public long getDuration() {
        return (finish - start);
    }

    private String getDurationSummaryDesc(final long duration) {

        //display the time it took to sell the stock
        return "Duration of the order from buy to sell: " + getDurationDesc(duration);
    }

    public static String getDurationDesc(final long duration) {

        return String.format(
            "%02d:%02d:%02d",
            TimeUnit.MILLISECONDS.toHours(duration),
            TimeUnit.MILLISECONDS.toMinutes(duration) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(duration)),
            TimeUnit.MILLISECONDS.toSeconds(duration) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration))
        );
    }

    private void setReasonBuy(ReasonBuy reasonBuy) {
        this.reasonBuy = reasonBuy;
    }

    private void setReasonSell(ReasonSell reasonSell) {
        this.reasonSell = reasonSell;
    }

    private void setRsiBuy(final double rsiBuy) {
        this.rsiBuy = rsiBuy;
    }

    private void setRsiSell(final double rsiSell) {
        this.rsiSell = rsiSell;
    }

    public double getRsiBuy() {
        return this.rsiBuy;
    }

    public double getRsiSell() {
        return this.rsiSell;
    }

    public ReasonSell getReasonSell() {
        return this.reasonSell;
    }

    public ReasonBuy getReasonBuy() {
        return this.reasonBuy;
    }
}