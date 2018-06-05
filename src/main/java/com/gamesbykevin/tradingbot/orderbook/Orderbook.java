package com.gamesbykevin.tradingbot.orderbook;

import org.omg.CORBA.PUBLIC_MEMBER;

public class Orderbook {

    private long sequence;

    //our list of bids (buy)
    private final Bid[] bids;

    //our list of asks (sell)
    private final Ask[] asks;

    public Orderbook(int size) {
        this.bids = new Bid[size];
        this.asks = new Ask[size];
    }

    public long getSequence() {
        return sequence;
    }

    public void setSequence(long sequence) {
        this.sequence = sequence;
    }

    public Bid[] getBids() {
        return bids;
    }

    public Ask[] getAsks() {
        return asks;
    }

    public void addAsk(int index, double price, double size, int numOrders) {
        getAsks()[index] = new Ask();
        getAsks()[index].price = price;
        getAsks()[index].size = size;
        getAsks()[index].numOrders = numOrders;
    }

    public void addBid(int index, double price, double size, int numOrders) {
        getBids()[index] = new Bid();
        getBids()[index].price = price;
        getBids()[index].size = size;
        getBids()[index].numOrders = numOrders;
    }

    private class Bid {

        public double price;
        public double size;
        public int numOrders;
    }

    private class Ask {

        public double price;
        public double size;
        public int numOrders;
    }
}