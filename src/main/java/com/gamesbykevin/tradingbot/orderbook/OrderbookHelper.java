package com.gamesbykevin.tradingbot.orderbook;

import com.coinbase.exchange.api.entity.ProductOrderBook;

public class OrderbookHelper {

    public static Orderbook createOrderBook(ProductOrderBook productOrderBook) {

        //create new order book
        Orderbook orderbook = new Orderbook(productOrderBook.getAsks().size());

        //update sequence
        orderbook.setSequence(productOrderBook.getSequence());

        //add our (bids / asks)
        for (int i = 0; i < productOrderBook.getAsks().size(); i++) {

            double askPrice = Double.parseDouble(productOrderBook.getAsks().get(i).get(0));
            double askSize = Double.parseDouble(productOrderBook.getAsks().get(i).get(1));
            int askNumOrders = Integer.parseInt(productOrderBook.getAsks().get(i).get(2));
            orderbook.addAsk(i, askPrice, askSize, askNumOrders);

            double bidPrice = Double.parseDouble(productOrderBook.getBids().get(i).get(0));
            double bidSize = Double.parseDouble(productOrderBook.getBids().get(i).get(1));
            int bidNumOrders = Integer.parseInt(productOrderBook.getBids().get(i).get(2));
            orderbook.addBid(i, bidPrice, bidSize, bidNumOrders);
        }

        //return our populated object
        return orderbook;
    }

}