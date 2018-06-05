package com.coinbase.exchange.api.entity;

import java.util.List;

/**
 * Created by irufus on 8/3/15.
 */
public class ProductOrderBook {

    private Long sequence;
    private List<List<String>> bids;
    private List<List<String>> asks;

    public List<List<String>> getAsks() {
        return asks;
    }

    public void setAsks(List<List<String>> asks) {
        this.asks = asks;
    }

    public List<List<String>> getBids() {
        return bids;
    }

    public void setBids(List<List<String>> bids) {
        this.bids = bids;
    }

    public Long getSequence() {
        return sequence;
    }

    public void setSequence(Long sequence) {
        this.sequence = sequence;
    }
}