package com.gamesbykevin.tradingbot;

import com.coinbase.exchange.api.GdaxApiApplication;
import com.coinbase.exchange.api.entity.Product;
import com.coinbase.exchange.api.exchange.Signature;
import com.coinbase.exchange.api.orders.OrderService;
import com.coinbase.exchange.api.products.ProductService;
import com.coinbase.exchange.api.websocketfeed.message.Subscribe;
import com.gamesbykevin.tradingbot.agent.AgentManager;
import com.gamesbykevin.tradingbot.product.Ticker;
import com.gamesbykevin.tradingbot.util.GSon;
import com.gamesbykevin.tradingbot.util.HistoryTracker;
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

import static com.gamesbykevin.tradingbot.MainHelper.displayNextStatusUpdateDesc;
import static com.gamesbykevin.tradingbot.MainHelper.manageStatusUpdate;
import static com.gamesbykevin.tradingbot.calculator.Calculator.ENDPOINT_TICKER;
import static com.gamesbykevin.tradingbot.trade.TradeHelper.getDurationDesc;
import static com.gamesbykevin.tradingbot.util.Email.sendEmail;
import static com.gamesbykevin.tradingbot.util.JSon.getJsonResponse;
import static com.gamesbykevin.tradingbot.util.LogFile.getFilenameMain;
import static com.gamesbykevin.tradingbot.util.PropertyUtil.DEBUG;
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

    //which strategies do we want to trade with (separated by comma)
    public static String[] TRADING_STRATEGIES;

    /**
     * Are we paper trading? default true
     */
    public static boolean PAPER_TRADING = true;

    /**
     * When we paper trade are we tracking fees? if yes we will treat paper trading like a market order
     */
    public static boolean PAPER_TRADING_FEES = true;

    /**
     * Are we using the web socket connection?
     */
    public static boolean WEBSOCKET_ENABLED = false;

    //our list of products
    private static List<Product> PRODUCTS;

    //our list of all US products
    private static List<Product> PRODUCTS_ALL_USD;

    /**
     * Warn the user before we actually start (when using real money)
     */
    public static long DELAY_STARTUP = 20000L;

    public static void main(String[] args) {

        try {

            displayMessage("Starting...");

            //load the properties from our application.properties
            PropertyUtil.loadProperties();

            SpringApplicationBuilder springApp = new SpringApplicationBuilder().properties(PropertyUtil.getProperties());
            springApp.sources(GdaxApiApplication.class);
            springApp.web(false);
            ConfigurableApplicationContext context = springApp.run();

            Main app = new Main(context);
            Thread thread = new Thread(app);
            thread.start();

            //create our history tracker
            createHistoryTracker(app);

        } catch (Exception e) {
            displayMessage("Trading bot not started...");
            e.printStackTrace();
        }
    }

    private Main(ConfigurableApplicationContext context) {

        if (WEBSOCKET_ENABLED)
            throw new RuntimeException("Stopping because the web socket needs to be tested first before we test!!!!!!!!!!");

        ConfigurableListableBeanFactory factory = context.getBeanFactory();
        ORDER_SERVICE = factory.getBean(OrderService.class);
        this.productService = factory.getBean(ProductService.class);

        //display message of bot starting
        if (PAPER_TRADING) {

            //warning no real money used
            displayMessage("INFO: No real money used", getWriter());

        } else {

            //if we are using real money, let's only focus on 1 trading strategy
            if (TRADING_STRATEGIES.length != 1)
                throw new RuntimeException("When using real money you can only have 1 strategy");

            //display message and pause if using real money
            displayMessage("WARNING: We are trading with real money!!!!!!!!!!!", getWriter());
            displayMessage("Stop this process if this is incorrect!!!!!!!");
            displayMessage("Otherwise we will resume in " + (DELAY_STARTUP / 1000) + " seconds.");

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

    private static void createHistoryTracker(Main main) {

        //create a new history tracker
        if (!DEBUG) {
            displayMessage("Creating history tracker", main.getWriter());
            HistoryTracker historyTracker = new HistoryTracker();
            displayMessage("History tracker created", main.getWriter());
        } else {
            displayMessage("History tracker not created because we are debugging", main.getWriter());
        }
    }

    private void loadProducts() {

        try {

            //make the service call to gdax to get all coins
            List<Product> tmp = productService.getProducts();

            //create new list of products we want to trade
            PRODUCTS = new ArrayList<>();

            //create new list of all us products
            PRODUCTS_ALL_USD = new ArrayList<>();

            //figure out which products we are trading
            for (int i = 0; i < tmp.size(); i++) {

                //make sure we only add products we want to trade
                for (int x = 0; x < TRADING_CURRENCIES.length; x++) {

                    //does this product match, if yes we will add it
                    if (tmp.get(i).getId().trim().equalsIgnoreCase(TRADING_CURRENCIES[x].trim())) {

                        //add to list
                        getProducts().add(tmp.get(i));

                        //exit loop
                        break;
                    }
                }

                //add all US $ products to this list
                if (tmp.get(i).getId().trim().contains("-USD"))
                    PRODUCTS_ALL_USD.add(tmp.get(i));
            }

        } catch (Exception e) {

            //write to log
            displayMessage(e, getWriter());

            //throw exception
            throw e;
        }

        //make sure we are trading at least 1 product
        if (getProducts().isEmpty() || PRODUCTS_ALL_USD.isEmpty())
            throw new RuntimeException("No products were found");
    }

    @Override
    public void run() {

        //current time
        final long timePreInit = System.currentTimeMillis();

        //initialize
        init();

        //only need to create subscription once
        Subscribe subscribe = new Subscribe(TRADING_CURRENCIES);

        try {

            //current time
            final long timePostInit = System.currentTimeMillis();

            //send notification our bot is starting
            sendEmail("Trading Bot Started: " + getDurationDesc(timePostInit - timePreInit), "Paper trading: " + (PAPER_TRADING ? "On (fake money)" : "Off, You are using real funds!!!!!"));

            while (true) {

                try {

                    //if not null we are using the web socket connection
                    if (websocketFeed != null) {

                        //if we have a connection and aren't currently trying to connect
                        if (websocketFeed.hasConnection() && !websocketFeed.isConnecting()) {

                            //subscribe to get the updated information
                            websocketFeed.subscribe(subscribe);

                            //show when next notification message will take place
                            displayNextStatusUpdateDesc();

                            //sleep for a brief moment
                            Thread.sleep(THREAD_DELAY);

                        } else {

                            //if we don't have a connection and we aren't trying to connect, let's start connecting
                            if (!websocketFeed.isConnecting()) {

                                displayMessage("Lost connection, attempting to re-connect", getWriter());
                                websocketFeed.connect();

                            } else {

                                //we are trying to connect and just need to be patient
                                displayMessage("Waiting to connect...", getWriter());
                            }
                        }

                    } else {

                        //if we aren't using the web socket we need to check each manager one by one
                        for (int i = 0; i < TRADING_CURRENCIES.length; i++) {

                            try {

                                //get the agent manager of the coin we want to trade
                                AgentManager agentManager = getAgentManagers().get(TRADING_CURRENCIES[i]);

                                //get json response from ticker
                                final String json = getJsonResponse(String.format(ENDPOINT_TICKER, agentManager.getProductId()));

                                //convert to pojo
                                Ticker ticker = GSon.getGson().fromJson(json, Ticker.class);

                                /*
                                //get json response from order book
                                final String jsonOB = getJsonResponse(String.format(ENDPOINT_ORDER_BOOK, agentManager.getProductId()));

                                //convert to Order book class
                                Orderbook orderbook = OrderbookHelper.createOrderBook(GSon.getGson().fromJson(jsonOB, ProductOrderBook.class));
                                */

                                //sometimes we don't get a successful response so let's check for null
                                if (ticker != null)
                                    agentManager.update(ticker.price);

                                //show when next notification message will take place
                                displayNextStatusUpdateDesc();

                                //sleep for a brief moment
                                Thread.sleep(THREAD_DELAY);

                            } catch (Exception e1) {

                                e1.printStackTrace();
                                displayMessage(e1, getWriter());
                            }
                        }
                    }

                    //display total assets update
                    manageStatusUpdate(this);

                } catch (Exception e) {

                    e.printStackTrace();
                    displayMessage(e, getWriter());
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            displayMessage(ex, getWriter());
        }
    }

    private void init() {

        //we will invest in each product equally
        final double funds = FUNDS / (double)getProducts().size();

        //create new hash map of agents
        this.agentManagers = new HashMap<>();

        //add an agent for each product we are trading
        for (int i = 0; i < getProducts().size(); i++) {

            //create new manager agent
            AgentManager agentManager = new AgentManager(getProducts().get(i), funds);

            //add manager to list
            getAgentManagers().put(getProducts().get(i).getId(), agentManager);

            //display agent is created
            displayMessage("Agent created - " + getProducts().get(i).getId(), getWriter());

            try {

                //sleep for a few seconds
                Thread.sleep(THREAD_DELAY * 3);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //create our web socket feed if the websocket is enabled
        if (WEBSOCKET_ENABLED) {

            displayMessage("Connected via websocket...", getWriter());

            this.websocketFeed = new MyWebsocketFeed(
                PropertyUtil.getProperties().getProperty("websocket.baseUrl"),
                PropertyUtil.getProperties().getProperty("gdax.key"),
                PropertyUtil.getProperties().getProperty("gdax.passphrase"),
                new Signature(PropertyUtil.getProperties().getProperty("gdax.secret")),
                getAgentManagers()
            );

        } else {

            displayMessage("Websocket is not enabled...", getWriter());
        }
    }

    protected static List<Product> getProducts() {
        return PRODUCTS;
    }

    public static List<Product> getProductsAllUsd() {
        return PRODUCTS_ALL_USD;
    }

    protected final PrintWriter getWriter() {

        //create the main log file and place in our root logs directory
        if (this.writer == null)
            this.writer = LogFile.getPrintWriter(getFilenameMain(), LogFile.getLogDirectory());

        return this.writer;
    }

    protected final HashMap<String, AgentManager> getAgentManagers() {
        return this.agentManagers;
    }

    public static final OrderService getOrderService() {
        return ORDER_SERVICE;
    }
}