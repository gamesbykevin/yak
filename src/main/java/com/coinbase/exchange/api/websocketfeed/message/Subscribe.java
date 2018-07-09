package com.coinbase.exchange.api.websocketfeed.message;

public class Subscribe {

    private String type;
    private String[] product_ids;
    private Channels[] channels;

    // Used for signing the subscribe message to the Websocket feed
    private String signature;
    private String passphrase;
    private String timestamp;
    private String apiKey;

    public Subscribe(String[] product_ids) {
        this.type = "subscribe";
        //this.product_ids = product_ids;

        this.channels = new Channels[1];
        this.channels[0] = new Channels();
        this.channels[0].name = "ticker";
        this.channels[0].product_ids = product_ids;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String[] getProduct_ids() {
        return product_ids;
    }

    public void setProduct_ids(String[] product_ids) {
        this.product_ids = product_ids;
    }

    public Subscribe setSignature(String signature) {
        this.signature = signature;
        return this;
    }

    public Subscribe  setPassphrase(String passphrase) {
        this.passphrase = passphrase;
        return this;
    }

    public Subscribe setTimestamp(String timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public Subscribe setKey(String apiKey) {
        this.apiKey = apiKey;
        return this;
    }

    private class Channels {

        private String name;
        private String[] product_ids;
    }
}