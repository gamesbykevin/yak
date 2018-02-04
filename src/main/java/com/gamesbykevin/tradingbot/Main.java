package com.gamesbykevin.tradingbot;

import com.coinbase.exchange.api.GdaxApiApplication;
import com.coinbase.exchange.api.accounts.AccountService;
import com.coinbase.exchange.api.entity.Product;
import com.coinbase.exchange.api.exchange.Signature;
import com.coinbase.exchange.api.marketdata.MarketDataService;
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

    private AccountService accountService;
    private MarketDataService marketService;
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
            sendEmail("Trading Bot Hello", "Starting");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Main(ConfigurableApplicationContext context) {

        ConfigurableListableBeanFactory factory = context.getBeanFactory();
        this.accountService = factory.getBean(AccountService.class);
        this.marketService = factory.getBean(MarketDataService.class);
        this.productService = factory.getBean(ProductService.class);

        //create the main log file
        this.writer = LogFile.getPrintWriter("main-" + getDateDesc() + ".log");

        //load our products we will be trading
        loadProducts();
    }

    private void loadProducts() {

        List<Product> tmp = productService.getProducts();
        List<String> tmp1 = new ArrayList<>();

        //only get the USD products
        for (Product product : tmp) {
            if (product.getId().endsWith("-USD"))
                tmp1.add(product.getId());
        }

        productIds = new String[tmp1.size()];

        for (int i = 0; i < productIds.length; i++) {
            productIds[i] = tmp1.get(i);
        }
    }

    @Override
    public void run() {

        //initialize
        init();

        while(true) {

            try {

                //get the updated information
                websocketFeed.subscribe(new Subscribe(productIds));

                //sleep for a second
                Thread.sleep(DELAY);

                double total = 0;

                for (Agent agent : agents.values()) {
                    total += agent.getAssets();
                }

                //print current funds
                displayMessage("Total assets $" + total + ", Starting funds $" + FUNDS, true, writer);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void init() {

        //we will invest in each product equally
        final double funds = FUNDS / (double)productIds.length;

        //create new hash map of agents
        this.agents = new HashMap<>();

        //add an agent for each product we are trading
        for (String productId : productIds) {

            try {

                //create new agent
                Agent agent = new Agent(productId, funds);

                //add to list
                this.agents.put(productId, agent);

                //display agent is created
                displayMessage("Agent created - " + productId, true, writer);

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
    }
}