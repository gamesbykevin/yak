package com.gamesbykevin.tradingbot.util;

import com.gamesbykevin.tradingbot.Main;
import com.gamesbykevin.tradingbot.agent.AgentHelper;
import com.gamesbykevin.tradingbot.calculator.Calculator;
import com.gamesbykevin.tradingbot.wallet.Wallet;

import java.io.FileInputStream;
import java.io.PrintWriter;
import java.util.Calendar;
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

        displayMessage("Loading properties: " + PROPERTY_FILE, false, null);

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

        //what is the support line
        AgentHelper.SUPPORT_LINE = Float.parseFloat(getProperties().getProperty("supportLine"));

        //what is the resistance line
        AgentHelper.RESISTANCE_LINE = Float.parseFloat(getProperties().getProperty("resistanceLine"));

        //how long do we hold onto stock until we sell to cut our losses
        AgentHelper.SELL_LOSS_RATIO = Float.parseFloat(getProperties().getProperty("sellLossRatio"));

        //how long do we hold onto stock until we sell
        AgentHelper.SELL_GAIN_RATIO = Float.parseFloat(getProperties().getProperty("sellGainRatio"));

        //do we send a notification for every transaction?
        AgentHelper.NOTIFICATION_EVERY_TRANSACTION = Boolean.parseBoolean(getProperties().getProperty("notificationEveryTransaction"));

        //how much money can we afford to lose before we stop trading
        Wallet.STOP_TRADING_RATIO = Float.parseFloat(getProperties().getProperty("stopTradingRatio"));

        //how many periods to we use to calculate calculator
        Calculator.PERIODS_RSI = Integer.parseInt(getProperties().getProperty("periodsRsi"));

        //get the number of periods for our long ema calculation
        Calculator.PERIODS_EMA_LONG = Integer.parseInt(getProperties().getProperty("periodsEmaLong"));

        //get the number of periods for our extended ema calculation
        Calculator.PERIODS_EMA_SHORT = Integer.parseInt(getProperties().getProperty("periodsEmaShort"));

        //how long is each candle?
        Calculator.PERIOD_DURATION = Integer.parseInt(getProperties().getProperty("periodDuration"));

        //how long to calculate moving average volume?
        Calculator.PERIODS_MAV = Integer.parseInt(getProperties().getProperty("periodsMAV"));
    }

    public static synchronized void displayMessage(final String message, final boolean write, PrintWriter writer) {

        //print to console
        System.out.println(message);
        System.out.flush();

        if (write) {
            writer.println(getTextDateDesc() + ":  " + message);
            writer.flush();
        }
    }

    public static synchronized void displayMessage(final Exception e, final boolean write, PrintWriter writer) {
        displayMessage(getErrorMessage(e), write, writer);
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