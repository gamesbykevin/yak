package com.gamesbykevin.tradingbot.product;

import static com.gamesbykevin.tradingbot.Main.ENDPOINT;

/**
 * Data model we will use GSON to bind data for the Products that we can trade
 */
public class Product {

    public static final String ENDPOINT_PRODUCTS = ENDPOINT + "/products";

    public String id;
    public String base_currency;
    public String quote_currency;
    public double base_min_size;
    public double base_max_size;
    public double quote_increment;
    public String display_name;
    public String status;
    public boolean margin_enabled;
    public String status_message;
    public double min_market_funds;
    public double max_market_funds;
    public boolean post_only;
    public boolean limit_only;
    public boolean cancel_only;

    //I added this to track my own custom data
    public double currentPrice;
    public float rsi;
}