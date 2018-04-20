package com.gamesbykevin.tradingbot.agent;

import com.gamesbykevin.tradingbot.util.PropertyUtil;

import java.io.PrintWriter;

public class AgentManagerHelper {

    protected static void displayMessage(String message, PrintWriter writer) {
        PropertyUtil.displayMessage(message, writer);
    }

    public static void displayMessage(Exception e, PrintWriter writer) {
        PropertyUtil.displayMessage(e, writer);
    }

    public static void displayMessage(Agent agent, String message, boolean write) {
        displayMessage(agent.getProductId() + "-" + agent.getTradingStrategy() + " " + agent.getHardStopRatio() + ": " + message, write ? agent.getWriter() : null);
    }
}