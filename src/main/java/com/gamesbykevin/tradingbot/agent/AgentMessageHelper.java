package com.gamesbykevin.tradingbot.agent;

import com.gamesbykevin.tradingbot.trade.Trade.Result;
import com.gamesbykevin.tradingbot.trade.TradeHelper;
import com.gamesbykevin.tradingbot.util.Email;

import static com.gamesbykevin.tradingbot.agent.AgentHelper.round;
import static com.gamesbykevin.tradingbot.agent.AgentManagerHelper.displayMessage;
import static com.gamesbykevin.tradingbot.wallet.Wallet.STOP_TRADING_RATIO;

public class AgentMessageHelper {

    protected static void displayMessageLimitIncrease(Agent agent, double oldLimit, double newLimit) {
        displayMessage(agent, "Good news, stop trading limit has increased", true);
        displayMessage(agent, "    Funds $" + AgentHelper.round(agent.getWallet().getFunds()), true);
        displayMessage(agent, "Old limit $" + AgentHelper.round(oldLimit), true);
        displayMessage(agent, "New limit $" + AgentHelper.round(newLimit), true);
        displayMessage(agent, "If your funds fall below the new limit we will stop trading", true);
    }

    protected static void displayMessageStopTrading(Agent agent) {

        String subject = "We stopped trading";
        String text1 = "Started $" + AgentHelper.round(agent.getWallet().getInitialFunds());
        String text2 = "Funds   $" + AgentHelper.round(agent.getWallet().getFunds());
        String text3 = "Limit   $" + AgentHelper.round(STOP_TRADING_RATIO * agent.getWallet().getFundsBeforeTrade());
        String text4 = "Fees    $" + AgentHelper.round(TradeHelper.getTotalFees(agent));
        displayMessage(agent, subject, true);
        displayMessage(agent, text1, true);
        displayMessage(agent, text2, true);
        displayMessage(agent, text3, true);
        displayMessage(agent, text4, true);

        //include the funds in our message
        String message = text1 + "\n" + text2 + "\n" + text3 + "\n" + text4 + "\n";

        //also include the summary of wins/losses
        message += TradeHelper.getDescWins(agent) + "\n";
        message += TradeHelper.getDescLost(agent) + "\n";

        //send email notification
        Email.sendEmail(subject + " (" + agent.getProductId() + "-" + agent.getStrategyKey() + ")", message);
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

    protected static void displayMessageOrderPending(Agent agent, double currentPrice) {

        //construct message
        String message = "Waiting. Product " + agent.getProductId();
        message += ", Current $" + currentPrice;
        message += ", Purchase $" + round(agent.getTrade().getPriceBuy());
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