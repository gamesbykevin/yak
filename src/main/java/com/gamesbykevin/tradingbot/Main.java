package com.gamesbykevin.tradingbot;

import com.coinbase.exchange.api.GdaxApiApplication;
import com.coinbase.exchange.api.entity.Product;
import com.coinbase.exchange.api.exchange.Signature;
import com.coinbase.exchange.api.orders.OrderService;
import com.coinbase.exchange.api.products.ProductService;
import com.coinbase.exchange.api.websocketfeed.message.Subscribe;
import com.gamesbykevin.tradingbot.agent.Agent;
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

import static com.gamesbykevin.tradingbot.util.Email.getDateDesc;
import static com.gamesbykevin.tradingbot.util.Email.sendEmail;
import static com.gamesbykevin.tradingbot.util.PropertyUtil.displayMessage;

@SpringBootApplication
public class Main implements Runnable {

    private static OrderService ORDER_SERVICE;
    private ProductService productService;

    private MyWebsocketFeed websocketFeed;
    private String[] productIds;
    private HashMap<String, Agent> agents;

    //Our end point to the apis
    public static String ENDPOINT;

    //How much money do we start with
    public static double FUNDS;

    //where we write our log file(s)
    private PrintWriter writer;

    //how long do we sleep the thread for
    public static final long DELAY = 1000L;

    //how long until we send an overall update
    public static long NOTIFICATION_DELAY;

    //the previous time
    private long previous;

    /**
     * Are we paper trading?
     */
    public static boolean PAPER_TRADING = true;

    //our list of products
    private List<Product> products;

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

            //send notification our bot is starting
            sendEmail("Trading Bot Started", "Paper trading: " + (PAPER_TRADING ? "Enabled" : "Disabled"));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Main(ConfigurableApplicationContext context) {

        ConfigurableListableBeanFactory factory = context.getBeanFactory();
        ORDER_SERVICE = factory.getBean(OrderService.class);
        this.productService = factory.getBean(ProductService.class);

        //create the main log file
        this.writer = LogFile.getPrintWriter("main-" + getDateDesc() + ".log");

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

        List<Product> tmp1 = new ArrayList<>();

        //only get the USD products
        for (Product product : tmp) {
            if (product.getId().endsWith("-USD"))
                this.products.add(product);
        }
    }

    @Override
    public void run() {

        //initialize
        init();

        while(true) {

            try {

                //if we have a connection and aren't currently trying to connect
                if (websocketFeed.hasConnection() && !websocketFeed.isConnecting()) {

                    //subscribe to get the updated information
                    websocketFeed.subscribe(new Subscribe(productIds));

                    //sleep for a second
                    Thread.sleep(DELAY);

                    //text of our notification message
                    String subject = "", text = "\nStarted with $" + FUNDS + "\n";

                    double total = 0;

                    for (Agent agent : agents.values()) {

                        //get the total assets for the current product
                        final double assets = agent.getAssets();

                        //add to our total
                        total += assets;

                        //add to our details
                        text = text + agent.getProductId() + " - $" + assets + "\n";
                    }

                    subject = "Total assets $" + total;

                    //print current funds
                    displayMessage(subject, true, writer);

                    //if enough time has passed send ourselves a notification
                    if (System.currentTimeMillis() - previous >= NOTIFICATION_DELAY) {

                        //send our total assets in an email
                        sendEmail(subject, text);

                        //update the timer
                        previous = System.currentTimeMillis();
                    }

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

            } catch (Exception e) {

                e.printStackTrace();
                displayMessage(e, true, writer);
            }
        }
    }

    private void init() {

        //we will invest in each product equally
        final double funds = FUNDS / (double)productIds.length;

        //create new hash map of agents
        this.agents = new HashMap<>();

        //add an agent for each product we are trading
        for (Product product : products) {

            try {

                //create new agent
                Agent agent = new Agent(product, funds);

                //add to list
                this.agents.put(product.getId(), agent);

                //display agent is created
                displayMessage("Agent created - " + product.getId(), true, writer);

                //sleep for a few seconds
                Thread.sleep(DELAY);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //create our web socket feed
        websocketFeed = new MyWebsocketFeed(
            PropertyUtil.getProperties().getProperty("websocket.baseUrl"),
            PropertyUtil.getProperties().getProperty("gdax.key"),
            PropertyUtil.getProperties().getProperty("gdax.passphrase"),
            new Signature(PropertyUtil.getProperties().getProperty("gdax.secret")),
            this.agents
        );

        //store the last time we checked
        previous = System.currentTimeMillis();
    }
}