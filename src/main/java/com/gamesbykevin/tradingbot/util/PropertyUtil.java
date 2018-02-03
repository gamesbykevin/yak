package com.gamesbykevin.tradingbot.util;

import com.gamesbykevin.tradingbot.Main;
import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.rsi.Calculator;
import com.gamesbykevin.tradingbot.wallet.Wallet;

import java.util.Properties;

import static com.gamesbykevin.tradingbot.agent.Agent.displayMessage;

public class PropertyUtil {

    public static final String PROPERTY_FILE = "./application.properties";

    private static Properties PROPERTIES;

    public static Properties getProperties() {

        if (PROPERTIES == null) {

            PROPERTIES = new Properties();

            try {
                //call this when running the project in intellij
                PROPERTIES.load(Main.class.getClassLoader().getResourceAsStream(PROPERTY_FILE));

                //call this when you create an executable .jar and place the config.properties file in the same directory as the .jar
                //properties.load(new FileInputStream(PROPERTY_FILE));
            } catch(Exception ex) {
                ex.printStackTrace();
                System.exit(10);
            }
        }

        return PROPERTIES;
    }

    public static void loadProperties() {

        displayMessage("Loading properties: " + PROPERTY_FILE, false);

        //grab the email address from our config
        Email.EMAIL_NOTIFICATION_ADDRESS = getProperties().getProperty("emailNotification");

        //our gmail login we need so we have an smtp server to send emails
        Email.GMAIL_SMTP_USERNAME = getProperties().getProperty("gmailUsername");
        Email.GMAIL_SMTP_PASSWORD = getProperties().getProperty("gmailPassword");

        //our starting total funds
        Main.FUNDS = Double.parseDouble(getProperties().getProperty("funds"));

        //what is the support line
        Wallet.SUPPORT_LINE = Float.parseFloat(getProperties().getProperty("supportLine"));

        //what is the resistance line
        Wallet.RESISTANCE_LINE = Float.parseFloat(getProperties().getProperty("resistanceLine"));

        //how long do we hold onto stock until we sell to cut our losses
        Wallet.SELL_RATIO = Float.parseFloat(getProperties().getProperty("sellRatio"));

        //how much money can we afford to lose before we stop trading
        Wallet.STOP_TRADING_RATIO = Float.parseFloat(getProperties().getProperty("stopTradingRatio"));

        //how many periods to we use to calculate rsi
        Calculator.PERIODS = Integer.parseInt(getProperties().getProperty("periods"));
    }
}