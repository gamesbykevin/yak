package com.gamesbykevin.tradingbot.wallet;

import com.coinbase.exchange.api.entity.NewLimitOrderSingle;
import com.coinbase.exchange.api.entity.Product;
import com.coinbase.exchange.api.orders.Order;
import com.gamesbykevin.tradingbot.Main;
import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.rsi.Calculator;

import java.math.BigDecimal;

import static com.gamesbykevin.tradingbot.rsi.Calculator.PERIODS;
import static com.gamesbykevin.tradingbot.util.Email.sendEmail;

public class Wallet {

    //money we have to invest
    private double funds = 0;

    //quantity of stock we bought
    private double quantity = 0;

    //the price we bought the stock
    private double purchasePrice = 0;

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

    /**
     * If we lose an overall % of our funds let's stop the bleeding
     */
    public static float STOP_TRADING_RATIO;

    //how many funds did we start with
    private final double startingFunds;

    //should we stop trading
    private boolean stopTrading = false;

    public Wallet(double funds) {
        this.funds = funds;
        this.startingFunds = funds;
    }

    public void setStopTrading(boolean stopTrading) {
        this.stopTrading = stopTrading;
    }

    public boolean hasStopTrading() {
        return this.stopTrading;
    }

    public void setFunds(double funds) {
        this.funds = funds;
    }

    public double getFunds() {
        return this.funds;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public double getQuantity() {
        return this.quantity;
    }

    public synchronized void update(final Agent agent, final Calculator calculator, final Product product, final double currentPrice) {

        String subject = null, text = null;

        //if we have quantity check current stock price
        if (quantity > 0) {

            final double priceHigh = purchasePrice;
            final double priceGain = purchasePrice + (purchasePrice * SELL_GAIN_RATIO);
            final double priceLow = purchasePrice - (purchasePrice * SELL_LOSS_RATIO);

            //do we sell the stock
            boolean sell = false;

            if (currentPrice > priceHigh && calculator.getRsi() >= RESISTANCE_LINE) {

                //it grew enough, sell it
                sell = true;

            } else if (currentPrice >= priceGain) {

                //regardless of rsi, if we made enough money to sell it
                sell = true;

            } else if (currentPrice <= priceLow) {

                //it dropped enough, sell it
                sell = true;

            } else {

                agent.displayMessage("Waiting. Product " + product.getId() + " Current $" + currentPrice + ", Purchase $" + purchasePrice + ", Quantity: " + getQuantity(), true);
            }

            //if we are selling our stock
            if (sell) {

                //if we aren't paper trading sell the stock for real
                if (!Main.PAPER_TRADING) {
                    final boolean success = createLimitOrder(product, currentPrice, "sell");

                    if (!success) {
                        what do we do if we fail?
                    }
                }

                final double result = (currentPrice * getQuantity());

                //add the money back to our total funds
                setFunds(getFunds() + result);

                final double priceBought = (this.purchasePrice * getQuantity());
                final double priceSold = (currentPrice * getQuantity());

                //display money changed
                if (priceBought > priceSold) {
                    subject = "We lost $" + (priceBought - priceSold);
                } else {
                    subject = "We made $" + (priceSold - priceBought);
                }

                //display the transaction
                text = "Sell " + product.getId() + " quantity: " + getQuantity() + " @ $" + currentPrice + " remaining funds $" + getFunds();

                //display message(s)
                agent.displayMessage(subject, true);
                agent.displayMessage(text, true);

                //reset quantity back to 0
                setQuantity(0);
            }

            //if we lost too much money and have no quantity we will stop trading
            if (getFunds() < (STOP_TRADING_RATIO * startingFunds) && getQuantity() <= 0)
                setStopTrading(true);

            if (hasStopTrading()) {
                subject = "We stopped trading " + product.getId();
                text = "Funds dropped below our comfort level ($" + getFunds() + "). Stopped Trading for " + product.getId();
                agent.displayMessage(text,true);
            }

        } else {

            boolean buy = false;

            //if the stock is oversold we are on the right track
            if (calculator.getRsi() < SUPPORT_LINE) {

                switch (calculator.getTrend()) {

                    case Upward:

                        //if the rsi is low and we see a constant upward trend, we will buy
                        if (calculator.getBreaks() < 1) {
                            buy = true;
                            agent.displayMessage("There is a constant upward trend", true);
                        }
                        break;

                    //we like downward trends
                    case Downward:

                        //there is a downward trend but some breaks so we think it will go back upwards
                        if (calculator.getBreaks() > (int)(PERIODS / 2)) {
                            buy = true;
                            agent.displayMessage("There is a downward trend, but we see at least half of the periods with breaks so we will buy", true);
                        } else if (calculator.getBreaks() < 1) {
                            agent.displayMessage("There is a constant downward trend, and we will wait a little longer to buy", true);
                        } else {
                            agent.displayMessage("There is a downward trend, but not enough breaks to buy", true);
                        }
                        break;
                }

            } else {

                //if there is a constant upward trend lets buy anyway regardless of rsi
                switch (calculator.getTrend()) {
                    case Upward:
                        if (calculator.getBreaks() < 1) {
                            buy = true;
                            agent.displayMessage("There is a constant upward trend, so we will buy", true);
                        }
                        break;
                }
            }

            //are we buying stock
            if (buy) {

                //if we aren't paper trading buy the stock for real
                if (!Main.PAPER_TRADING) {
                    final boolean success = createLimitOrder(product, currentPrice, "buy");

                    if (!success) {
                        what do we do if we fail?
                    }
                }

                //how much can we buy?
                final double availableQuantity = getFunds() / currentPrice;

                //store the purchase price
                this.purchasePrice = currentPrice;

                //store the quantity we bought
                setQuantity(availableQuantity);

                //our funds are now gone
                setFunds(0);

                //display the transaction
                agent.displayMessage("Buy " + product.getId() + " quantity: " + getQuantity() + " @ $" + this.purchasePrice, true);

            } else {
                agent.displayMessage("Waiting. Product " + product.getId() + ", Available funds $" + getFunds(), true);
            }
        }

        //send message only if we stopped trading for this coin
        if (subject != null && text != null && hasStopTrading())
            sendEmail(subject, text);
    }

    private boolean createLimitOrder(Product product, double currentPrice, String action) {

        boolean success = false;

        Main.getOrderService();

        //the price we want to buy/sell
        BigDecimal price = new BigDecimal(1000.00);

        //the quantity
        BigDecimal size = new BigDecimal(.1);

        NewLimitOrderSingle limitOrder = new NewLimitOrderSingle();
        limitOrder.setProduct_id(product.getId());

        //are we buying or selling
        limitOrder.setSide(action);

        //this is a limit order
        limitOrder.setType("limit");

        //our price
        limitOrder.setPrice(price);

        //our quantity
        limitOrder.setSize(size);

        //create limit order
        Order order = Main.getOrderService().createOrder(limitOrder);

        //keep checking the status of our order until we get the result we want
        while(true) {

            System.out.println("Order status: " + order.getStatus());

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if(order.getStatus().equals("filled")) {

                //we are successful
                success = true;
                break;
            }

            //get the order from gdax again so we can check the status
            order = Main.getOrderService().getOrder(order.getId());
        }

        //cancel order
        //String result = service.cancelOrder("c04ad9d2-7a57-4133-a3e9-bc789cd1e6fe");

        //print result
        //System.out.println(result);

        return success;
    }
}