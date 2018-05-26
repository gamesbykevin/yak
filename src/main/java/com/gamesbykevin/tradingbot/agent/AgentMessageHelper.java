package com.gamesbykevin.tradingbot.agent;

import com.gamesbykevin.tradingbot.trade.Trade.Result;
import com.gamesbykevin.tradingbot.trade.TradeHelper;

import static com.gamesbykevin.tradingbot.agent.AgentHelper.round;
import static com.gamesbykevin.tradingbot.agent.AgentManagerHelper.displayMessage;

public class AgentMessageHelper {

    protected static void displayMessageOrderPending(Agent agent, double currentPrice) {

        //construct message
        String message = "Waiting. Product " + agent.getProductId();
        message += " Current $" + currentPrice;

        if (agent.getTrade().getOrderBuy() != null) {
            message += ", Purchase $" + round(agent.getWallet().getPurchasePrice());
        } else {
            message += ", Purchase $" + round(agent.getTrade().getPriceBuy());
        }

        message += ", Hard Stop $" + round(agent.getTrade().getHardStopPrice());
        message += ", Quantity: " + agent.getWallet().getQuantity();

        //we are waiting
        displayMessage(agent, message, true);
    }

    protected static void displayMessageAllTradesSummary(Agent agent) {

        //display wins and losses
        displayMessage(agent, TradeHelper.getDescWins(agent), true);
        displayMessage(agent, TradeHelper.getDescLost(agent), true);

        //display the total fees paid
        displayMessage(agent, "Fees $" + TradeHelper.getTotalFees(agent), true);

        //display average transaction time
        displayMessage(agent, TradeHelper.getAverageDurationDesc(agent), true);

        //display the total $ amount invested in stocks
        displayMessage(agent, AgentHelper.getStockInvestmentDesc(agent), true);

        //display the count and reasons why we sold our stock
        TradeHelper.displaySellReasonCount(agent, Result.Win);
        TradeHelper.displaySellReasonCount(agent, Result.Lose);

    }

    protected static void displayMessageCheckSellWaiting(Agent agent, double currentPrice) {

        //construct message
        String message = "Waiting. Product " + agent.getProductId();
        message += " Current $" + currentPrice;
        message += ", Purchase $" + agent.getWallet().getPurchasePrice();
        message += ", Hard Stop $" + round(agent.getTrade().getHardStopPrice());
        message += ", Quantity: " + agent.getWallet().getQuantity();

        //we are waiting
        displayMessage(agent, message, true);

    }

    protected static void displayMessagePriceDecline(Agent agent) {

        String message = "Price Decline: ";

        //construct our message
        for (int i = 0; i < agent.getTrade().getPriceHistory().length; i++) {
            message += "$" + agent.getTrade().getPriceHistory()[i];

            if (i < agent.getTrade().getPriceHistory().length - 1)
                message += ", ";
        }

        //display the recent prices so we can see the decline
        displayMessage(agent, message, true);
    }
}