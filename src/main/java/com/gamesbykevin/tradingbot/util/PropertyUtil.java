package com.gamesbykevin.tradingbot.util;

import com.gamesbykevin.tradingbot.Main;
import com.gamesbykevin.tradingbot.agent.AgentHelper;
import com.gamesbykevin.tradingbot.calculator.*;
import com.gamesbykevin.tradingbot.calculator.strategy.*;
import com.gamesbykevin.tradingbot.wallet.Wallet;

import java.io.FileInputStream;
import java.io.PrintWriter;
import java.util.Properties;

import static com.gamesbykevin.tradingbot.util.Email.getTextDateDesc;

public class PropertyUtil {

    public static final String PROPERTY_FILE = "./application.properties";

    private static Properties PROPERTIES;

    public static final long SECONDS_PER_MINUTE = 60L;

    public static final long MILLISECONDS_PER_SECOND = 1000L;

    //how many milliseconds are there per minute
    public static final long MILLISECONDS_PER_MINUTE = MILLISECONDS_PER_SECOND * SECONDS_PER_MINUTE;

    public static Properties getProperties() {

        if (PROPERTIES == null) {

            PROPERTIES = new Properties();

            try {

                //call this when running the project in intellij
                PROPERTIES.load(Main.class.getClassLoader().getResourceAsStream(PROPERTY_FILE));

                //call this when you create an executable .jar and place the application.properties file in the same directory as the .jar
                //PROPERTIES.load(new FileInputStream(PROPERTY_FILE));

            } catch(Exception ex) {
                ex.printStackTrace();
                System.exit(10);
            }
        }

        return PROPERTIES;
    }

    public static void loadProperties() {

        displayMessage("Loading properties: " + PROPERTY_FILE);

        //our api url endpoint
        Main.ENDPOINT = getProperties().getProperty("gdax.api.baseUrl");

        //is the websocket enabled?
        Main.WEBSOCKET_ENABLED = Boolean.parseBoolean(getProperties().getProperty("websocketEnabled"));

        //how long is each thread
        Main.THREAD_DELAY = Long.parseLong(getProperties().getProperty("threadDelay"));

        //grab the email address from our config
        Email.EMAIL_NOTIFICATION_ADDRESS = getProperties().getProperty("emailNotification");

        //our gmail login we need so we have an smtp server to send emails
        Email.GMAIL_SMTP_USERNAME = getProperties().getProperty("gmailUsername");
        Email.GMAIL_SMTP_PASSWORD = getProperties().getProperty("gmailPassword");

        //our starting total funds
        Main.FUNDS = Double.parseDouble(getProperties().getProperty("funds"));

        //are we paper trading, or using real money
        Main.PAPER_TRADING = Boolean.parseBoolean(getProperties().getProperty("paperTrading"));

        //which crypto currencies do we want to trade
        Main.TRADING_CURRENCIES = getProperties().getProperty("tradingCurrencies").split(",");

        if (Main.TRADING_CURRENCIES.length < 1)
            throw new RuntimeException("You haven't specified what products you want to trade in your properties file");

        //get how long we wait until sending a notification delay of total assets
        Main.NOTIFICATION_DELAY = Long.parseLong(getProperties().getProperty("notificationDelay"));

        //make sure minimum value is entered
        if (Main.NOTIFICATION_DELAY * MILLISECONDS_PER_MINUTE < MILLISECONDS_PER_MINUTE) {
            Main.NOTIFICATION_DELAY = MILLISECONDS_PER_MINUTE;
        } else {
            Main.NOTIFICATION_DELAY = Main.NOTIFICATION_DELAY * MILLISECONDS_PER_MINUTE;
        }

        //what is our hard stop ratio, that we set as our purchased stock price rises
        AgentHelper.HARD_STOP_RATIO = Float.parseFloat(getProperties().getProperty("hardStopRatio"));

        //do we send a notification for every transaction?
        AgentHelper.NOTIFICATION_EVERY_TRANSACTION = Boolean.parseBoolean(getProperties().getProperty("notificationEveryTransaction"));

        //how much money can we afford to lose before we stop trading
        Wallet.STOP_TRADING_RATIO = Float.parseFloat(getProperties().getProperty("stopTradingRatio"));

        //how long is each candle?
        Calculator.PERIOD_DURATION = Integer.parseInt(getProperties().getProperty("periodDuration"));

        //how many periods do we need in our history to start trading?
        Calculator.HISTORICAL_PERIODS_MINIMUM = Integer.parseInt(getProperties().getProperty("historyMinimum"));
    }

    public static synchronized void displayMessage(final String message) {
        displayMessage(message,null);
    }

    public static synchronized void displayMessage(final String message, PrintWriter writer) {

        //print to console
        System.out.println(message);
        System.out.flush();

        if (writer != null) {
            writer.println(getTextDateDesc() + ":  " + message);
            writer.flush();
        }
    }

    public static synchronized void displayMessage(final Exception e, PrintWriter writer) {
        displayMessage(getErrorMessage(e), writer);
    }

    private static String getErrorMessage(Exception e) {

        String message = "";

        try {

            message += e.getMessage() + "\n\t\t";;

            StackTraceElement[] stack = e.getStackTrace();

            for (StackTraceElement s : stack) {
                message = message + s.toString() + "\n\t\t";
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return message;
    }
}