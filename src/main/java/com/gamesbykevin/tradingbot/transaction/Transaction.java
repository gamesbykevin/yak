package com.gamesbykevin.tradingbot.transaction;

import com.coinbase.exchange.api.entity.Product;
import com.coinbase.exchange.api.orders.Order;
import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.agent.AgentHelper;

import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.TimeUnit;

import static com.gamesbykevin.tradingbot.agent.AgentHelper.NOTIFICATION_EVERY_TRANSACTION;
import static com.gamesbykevin.tradingbot.agent.AgentManagerHelper.displayMessage;
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

    //the reason why we sold
    private TransactionHelper.ReasonSell reason;

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

    public void update(final Agent agent, final Product product, final Order order) {

        //our notification messages
        String subject = "", text = "", summary = "";

        //get the purchase price from the order
        BigDecimal price = BigDecimal.valueOf(Double.parseDouble(order.getPrice()));
        price.setScale(AgentHelper.ROUND_DECIMALS_PRICE, RoundingMode.HALF_DOWN);

        //get the quantity from the order
        BigDecimal quantity = BigDecimal.valueOf(Double.parseDouble(order.getSize()));
        quantity.setScale(AgentHelper.ROUND_DECIMALS_QUANTITY, RoundingMode.HALF_DOWN);

        if (order.getSide().equalsIgnoreCase(AgentHelper.Action.Buy.getDescription())) {

            //assign our buy order
            setBuy(order);

            //if our buy order has been filled, update our wallet to have the current purchase price
            agent.getWallet().setPurchasePrice(price.doubleValue());

            //update our available funds based on our purchase
            agent.getWallet().setFunds(agent.getWallet().getFunds() - (price.doubleValue() * quantity.doubleValue()));

            //add the quantity to our wallet
            agent.getWallet().setQuantity(agent.getWallet().getQuantity() + quantity.doubleValue());

            //setup our notification message
            subject = "Purchase " + product.getId();
            text = "Buy " + product.getId() + " quantity: " + quantity + " @ $" + agent.getWallet().getPurchasePrice();

            //display the transaction
            displayMessage(agent, text, true);

        } else if (order.getSide().equalsIgnoreCase(AgentHelper.Action.Sell.getDescription())) {

            //assign our sell order
            setSell(order);

            //did we pay any fees?
            double fees = 0;

            try {

                //parse our fees to double
                fees = Double.parseDouble(order.getFill_fees());

            } catch (Exception e) {
                e.printStackTrace();
            }

            //assign our reason for selling
            setReasonSell(agent.getReasonSell());

            //if our sell order has been filled, update our wallet with our new funds
            agent.getWallet().setFunds(agent.getWallet().getFunds() + (price.doubleValue() * quantity.doubleValue()));

            //also subtract our fees
            agent.getWallet().setFunds(agent.getWallet().getFunds() - fees);

            //update the quantity as well
            agent.getWallet().setQuantity(agent.getWallet().getQuantity() - quantity.doubleValue());

            //figure out the total price we bought the stock for
            final double bought = (agent.getWallet().getPurchasePrice() * quantity.doubleValue());

            //figure out the total price we sold the stock for
            final double sold = (price.doubleValue() * quantity.doubleValue());

            //what is the reason for selling
            displayMessage(agent, "Reason sell: " + agent.getReasonSell().getDescription(), true);

            //display the amount of fees that we paid
            displayMessage(agent, "Fees $" + fees, true);

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

            //start off with the product to start the transaction description
            text = "Sell " + product.getId();

            //what is the quantity
            text += ", quantity: " + quantity;

            //what is the price
            text += " @ $" + price;

            //how much did we pay initially
            text += ", purchase $" + agent.getWallet().getPurchasePrice();

            //display our fees
            text += ", fees $" + fees;

            //how much $ do we have left
            text += ", remaining funds $" + agent.getWallet().getFunds();

            //include the duration description
            summary = getDurationSummaryDesc(getDuration());
            displayMessage(agent, summary, true);

        } else {
            throw new RuntimeException("Side not handled here: " + order.getSide());
        }

        //display and write to log
        displayMessage(agent, subject, true);
        displayMessage(agent, text, true);

        //are we going to notify every transaction?
        if (!agent.isSimulation() && NOTIFICATION_EVERY_TRANSACTION && subject.length() > 0 && text.length() > 0)
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

    private void setReasonSell(ReasonSell reason) {
        this.reason = reason;
    }

    public ReasonSell getReasonSell() {
        return this.reason;
    }
}