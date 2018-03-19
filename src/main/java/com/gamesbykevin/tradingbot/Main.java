package com.gamesbykevin.tradingbot;

import com.coinbase.exchange.api.GdaxApiApplication;
import com.coinbase.exchange.api.entity.Product;
import com.coinbase.exchange.api.exchange.Signature;
import com.coinbase.exchange.api.orders.OrderService;
import com.coinbase.exchange.api.products.ProductService;
import com.coinbase.exchange.api.websocketfeed.message.Subscribe;
import com.gamesbykevin.tradingbot.agent.AgentHelper;
import com.gamesbykevin.tradingbot.agent.AgentManager;
import com.gamesbykevin.tradingbot.calculator.Calculator;
import com.gamesbykevin.tradingbot.product.Ticker;
import com.gamesbykevin.tradingbot.transaction.Transaction;
import com.gamesbykevin.tradingbot.util.GSon;
import com.gamesbykevin.tradingbot.util.LogFile;
import com.gamesbykevin.tradingbot.util.PropertyUtil;
import com.gamesbykevin.tradingbot.websocket.MyWebsocketFeed;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.gamesbykevin.tradingbot.calculator.Calculator.ENDPOINT_TICKER;
import static com.gamesbykevin.tradingbot.calculator.Calculator.PERIOD_DURATION;
import static com.gamesbykevin.tradingbot.util.Email.getFileDateDesc;
import static com.gamesbykevin.tradingbot.util.Email.sendEmail;
import static com.gamesbykevin.tradingbot.util.JSon.getJsonResponse;
import static com.gamesbykevin.tradingbot.util.PropertyUtil.displayMessage;

@SpringBootApplication
public class Main implements Runnable {

    //used to place orders
    private static OrderService ORDER_SERVICE;

    //used to retrieve gdax products
    private ProductService productService;

    //web socket feed (unstable as it gives old price information sometimes)
    private MyWebsocketFeed websocketFeed;

    //list of agents trading for each coin
    private HashMap<String, AgentManager> agentManagers;

    //Our end point to the apis
    public static String ENDPOINT;

    //How much money do we start with
    public static double FUNDS;

    //where we write our log file(s)
    private PrintWriter writer;

    //how long do we sleep the thread for
    public static long THREAD_DELAY;

    //how long until we send an overall update
    public static long NOTIFICATION_DELAY;

    //which currencies do we want to trade with (separated by comma)
    public static String[] TRADING_CURRENCIES;

    //the previous time we sent a notification
    private long previous;

    //what are the previous total assets
    private double previousTotal = 0;

    /**
     * Are we paper trading? default true
     */
    public static boolean PAPER_TRADING = true;

    /**
     * Are we using the websocket connection?
     */
    public static boolean WEBSOCKET_ENABLED = false;

    //our list of products
    private List<Product> products;

    //how long has the bot been running
    private final long start;

    /**
     * Warn the user before we actually start (no paper trading)
     */
    public static long DELAY_STARTUP = 15000L;

    public static void main(String[] args) {

        try {

            displayMessage("Starting...", false, null);

            //load the properties from our application.properties
            PropertyUtil.loadProperties();

            SpringApplicationBuilder springApp = new SpringApplicationBuilder().properties(PropertyUtil.getProperties());
            springApp.sources(GdaxApiApplication.class);
            springApp.web(false);
            ConfigurableApplicationContext context = springApp.run();

            Main app = new Main(context);
            Thread thread = new Thread(app);
            thread.start();

        } catch (Exception e) {
            displayMessage("Trading bot not started...", false, null);
            e.printStackTrace();
        }
    }

    private Main(ConfigurableApplicationContext context) {

        //track the start time
        this.start = System.currentTimeMillis();

        ConfigurableListableBeanFactory factory = context.getBeanFactory();
        ORDER_SERVICE = factory.getBean(OrderService.class);
        this.productService = factory.getBean(ProductService.class);

        //create the main log file
        this.writer = LogFile.getPrintWriter("main-" + getFileDateDesc() + ".log");

        //display message of bot starting
        if (PAPER_TRADING) {
            displayMessage("INFO: No real money used", true, writer);
        } else {

            //display message and pause if using real money
            displayMessage("WARNING: We are trading with real money!!!!!!!!!!!", true, writer);
            displayMessage("Stop this process if this is incorrect!!!!!!!", false, null);

            try {
                //sleep for a short period before we actually start
                Thread.sleep(DELAY_STARTUP);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //load our products we will be trading
        loadProducts();
    }

    public static OrderService getOrderService() {
        return ORDER_SERVICE;
    }

    private void loadProducts() {

        List<Product> tmp = productService.getProducts();

        //create new list of products we want to trade
        this.products = new ArrayList<>();

        //figure out which products we are trading
        for (Product product : tmp) {

            //make sure we only add products we want to trade
            for (String productId : TRADING_CURRENCIES) {

                //does this product match, if yes we will add it
                if (product.getId().trim().equalsIgnoreCase(productId.trim())) {

                    //add to list
                    this.products.add(product);

                    //exit loop
                    break;
                }
            }
        }

        //make sure we are trading at least 1 product
        if (this.products.isEmpty())
            throw new RuntimeException("No products were found");
    }

    @Override
    public void run() {

        try {

            //initialize
            init();

            //only need to create subscription once
            Subscribe subscribe = new Subscribe(TRADING_CURRENCIES);

            //send notification our bot is starting
            sendEmail("Trading Bot Started", "Paper trading: " + (PAPER_TRADING ? "On" : "Off"));

            while (true) {

                try {

                    //if not null we are using the web socket connection
                    if (websocketFeed != null) {

                        //if we have a connection and aren't currently trying to connect
                        if (websocketFeed.hasConnection() && !websocketFeed.isConnecting()) {

                            //subscribe to get the updated information
                            websocketFeed.subscribe(subscribe);

                            //sleep for a second
                            Thread.sleep(THREAD_DELAY);

                            //display total assets update
                            manageStatusUpdate();

                        } else {

                            //if we don't have a connection and we aren't trying to connect, let's start connecting
                            if (!websocketFeed.isConnecting()) {

                                displayMessage("Lost connection, attempting to re-connect", true, writer);
                                websocketFeed.connect();

                            } else {

                                //we are trying to connect and just need to be patient
                                displayMessage("Waiting to connect...", true, writer);
                            }
                        }

                    } else {

                        //we aren't using web socket since it is null
                        for (AgentManager agentManager : agentManagers.values()) {

                            try {

                                //get json response from ticker
                                final String json = getJsonResponse(String.format(ENDPOINT_TICKER, agentManager.getProductId()));

                                //convert to pojo
                                Ticker ticker = GSon.getGson().fromJson(json, Ticker.class);

                                //sometimes we don't get a successful response so let's check for null
                                if (ticker != null)
                                    agentManager.update(ticker.price);

                                //sleep for a second
                                Thread.sleep(THREAD_DELAY);

                                //display total assets update
                                manageStatusUpdate();

                            } catch (Exception e1) {

                                e1.printStackTrace();
                                displayMessage(e1, true, writer);
                            }
                        }
                    }

                } catch (Exception e) {

                    e.printStackTrace();
                    displayMessage(e, true, writer);
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            displayMessage(ex, true, writer);
        }
    }

    private void manageStatusUpdate() {

        //text of our notification message
        String subject = "", text = "\n";

        //how much did we start with
        //text = text + "Started with $" + FUNDS + "\n";

        //how long has the bot been running
        text = text + "Bot Running: " + Transaction.getDurationDesc(System.currentTimeMillis() - start) + "\n\n";

        double total = 0;

        for (AgentManager agentManager : agentManagers.values()) {

            //get the total assets for the current product
            final double assets = agentManager.getTotalAssets();

            //add to our total
            total += assets;

            //add to our details
            text = text + agentManager.getProductId() + " - $" + AgentHelper.formatValue(assets) + "\n";

            //display each agent's funds as well
            text = text + agentManager.getAgentDetails();

            //add line break in the end
            text = text + "\n";

        }

        subject = "Total assets $" + AgentHelper.formatValue(total);

        //print our total assets if they have changed
        if (total != previousTotal) {
            displayMessage(subject, true, writer);
            previousTotal = total;
        }

        //if enough time has passed send ourselves a notification
        if (System.currentTimeMillis() - previous >= NOTIFICATION_DELAY) {

            //send our total assets in an email
            sendEmail(subject, text);

            //update the timer
            previous = System.currentTimeMillis();

            //lets also write our info to the logs
            for (AgentManager agentManager : agentManagers.values()) {

                //write the details of each agent to a log file
                agentManager.writeAgentDetails();
            }

        } else {

            //how much time remaining
            final long duration = NOTIFICATION_DELAY - (System.currentTimeMillis() - previous);

            //show when the next notification message will be sent
            displayMessage("Next notification message in " + Transaction.getDurationDesc(duration), false, null);
        }
    }

    private void init() {

        //we will invest in each product equally
        final double funds = FUNDS / (double)products.size();

        //create new hash map of agents
        this.agentManagers = new HashMap<>();

        //identify our duration
        Calculator.Duration duration = null;

        //search for the desired duration
        for (Calculator.Duration tmpDuration : Calculator.Duration.values()) {

            //if the numbers match we found it
            if (tmpDuration.duration == PERIOD_DURATION) {
                duration = tmpDuration;
                break;
            }
        }

        //we can't start until we find our duration
        if (duration == null)
            throw new RuntimeException("Desired duration doesn't match: " + PERIOD_DURATION);

        //add an agent for each product we are trading
        for (Product product : products) {

            try {

                //create new manager agent
                AgentManager agentManager = new AgentManager(product, funds, duration);

                //add manager to list
                this.agentManagers.put(product.getId(), agentManager);

                //display agent is created
                displayMessage("Agent created - " + product.getId(), true, writer);

                //sleep for a few seconds
                Thread.sleep(THREAD_DELAY * 3);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //create our web socket feed if the websocket is enabled
        if (WEBSOCKET_ENABLED) {

            displayMessage("Connected via websocket...", true, writer);

            websocketFeed = new MyWebsocketFeed(
                PropertyUtil.getProperties().getProperty("websocket.baseUrl"),
                PropertyUtil.getProperties().getProperty("gdax.key"),
                PropertyUtil.getProperties().getProperty("gdax.passphrase"),
                new Signature(PropertyUtil.getProperties().getProperty("gdax.secret")),
                this.agentManagers
            );

        } else {

            displayMessage("Websocket is not enabled...", true, writer);
        }

        //store the last time we checked
        previous = System.currentTimeMillis();
    }
}