package com.gamesbykevin.tradingbot;

import com.coinbase.exchange.api.GdaxApiApplication;
import com.coinbase.exchange.api.accounts.AccountService;
import com.coinbase.exchange.api.entity.Product;
import com.coinbase.exchange.api.exchange.Signature;
import com.coinbase.exchange.api.marketdata.MarketDataService;
import com.coinbase.exchange.api.products.ProductService;
import com.coinbase.exchange.api.websocketfeed.message.Subscribe;
import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.util.PropertyUtil;
import com.gamesbykevin.tradingbot.websocket.MyWebsocketFeed;
import com.google.gson.*;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.gamesbykevin.tradingbot.agent.Agent.DELAY;
import static com.gamesbykevin.tradingbot.agent.Agent.displayMessage;
import static com.gamesbykevin.tradingbot.util.Email.sendEmail;

public class Main {

    public static void main(String[] args) {

        try {

            displayMessage("Starting...", false);

            //load the properties from our application.properties
            PropertyUtil.loadProperties();

            //send notification our bot is starting
            //sendEmail("Trading Bot Hello", "Starting");

            SpringApplicationBuilder springApp = new SpringApplicationBuilder().properties(PropertyUtil.getProperties());
            springApp.sources(GdaxApiApplication.class);
            springApp.web(false);
            ConfigurableApplicationContext context = springApp.run();
            Main app = new Main(context);
            app.loop();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private AccountService accountService;
    private MarketDataService marketService;
    private ProductService productService;

    private static Gson GSON;
    private MyWebsocketFeed websocketFeed;
    private String[] productIds;
    private final HashMap<String, Agent> agents;

    /**
     * Our end point to the apis
     */
    public static final String ENDPOINT = "https://api.gdax.com";

    /**
     * How much money do we start with
     */
    public static double FUNDS = 1000.00d;

    private Main(ConfigurableApplicationContext context) {

        ConfigurableListableBeanFactory factory = context.getBeanFactory();

        accountService = factory.getBean(AccountService.class);
        marketService = factory.getBean(MarketDataService.class);
        productService = factory.getBean(ProductService.class);

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

        //we will invest in each product equally
        final double funds = FUNDS / (double)productIds.length;

        //create new hash map of agents
        this.agents = new HashMap<>();

        //add an agent for each product we are trading
        for (String productId : productIds) {
            try {
                this.agents.put(productId, new Agent(productId, funds));
                Thread.sleep(5000);
                displayMessage("Agent created - " + productId, true);
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

    public void loop() {

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
                displayMessage("Total assets $" + total + ", Starting funds $" + FUNDS, true);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Get GSon object
     * @return Object used to parse json string
     */
    public static Gson getGson() {

        if (GSON == null) {

            //create our GSON object specifically to handle the unique datetime format in the json response
            GSON = new GsonBuilder().registerTypeAdapter(DateTime.class, new JsonSerializer<DateTime>() {
                @Override
                public JsonElement serialize(DateTime json, Type typeOfSrc, JsonSerializationContext context) {
                    return new JsonPrimitive(ISODateTimeFormat.dateTime().print(json));
                }
            }).registerTypeAdapter(DateTime.class, new JsonDeserializer<DateTime>() {
                @Override
                public DateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                    return ISODateTimeFormat.dateTime().parseDateTime(json.getAsString());
                }
            }).create();
        }

        return GSON;
    }
}