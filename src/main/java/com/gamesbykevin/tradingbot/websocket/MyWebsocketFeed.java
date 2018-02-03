package com.gamesbykevin.tradingbot.websocket;

import com.coinbase.exchange.api.exchange.Signature;
import com.coinbase.exchange.api.websocketfeed.message.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamesbykevin.tradingbot.Main;
import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.product.Ticker;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.swing.*;
import javax.websocket.*;
import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.HashMap;

import static com.gamesbykevin.tradingbot.agent.Agent.displayMessage;

@Component
@ClientEndpoint
public class MyWebsocketFeed {

    static Logger log = LoggerFactory.getLogger(MyWebsocketFeed.class);

    private Signature signature;
    private Session userSession = null;
    private MessageHandler messageHandler;
    private String passphrase;
    private String key;
    private final HashMap<String, Agent> agents;

    @Autowired
    public MyWebsocketFeed(
            @Value("${websocket.baseUrl}") String websocketUrl,
            @Value("${gdax.key}") String key,
            @Value("${gdax.passphrase}") String passphrase,
            Signature signature,
            HashMap<String, Agent> agents) {

        this.key = key;
        this.passphrase = passphrase;
        this.signature = signature;
        this.agents = agents;

        try {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            container.connectToServer(this, new URI(websocketUrl));
        } catch (Exception e) {
            System.out.println("Could not connect to remote server: " + e.getMessage() + ", " + e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

    /**
     * Callback hook for Connection open events.
     * @param userSession the userSession which is opened.
     */
    @OnOpen
    public void onOpen(Session userSession) {
        //System.out.println("MyWebsocketFeed.onOpen()");
        this.userSession = userSession;
    }

    /**
     * Callback hook for Connection close events.
     *
     * @param userSession the userSession which is getting closed.
     * @param reason      the reason for connection close
     */
    @OnClose
    public void onClose(Session userSession, CloseReason reason) {
        //System.out.println("MyWebsocketFeed.onClose()");
        this.userSession = null;
    }

    /**
     * Callback hook for OrderBookMessage Events. This method will be invoked when a client send a message.
     * @param message The text message
     */
    @OnMessage
    public void onMessage(String message) {
        if (messageHandler != null) {
            messageHandler.handleMessage(message);
        }
    }

    /**
     * register message handler
     * @param msgHandler
     */
    public void addMyMessageHandler(MessageHandler msgHandler) {
        //System.out.println("MyWebsocketFeed.addMyMessageHandler()");
        this.messageHandler = msgHandler;
    }

    /**
     * Send a message.
     * @param message
     */
    public void sendMessage(String message) {
        //System.out.println("MyWebsocketFeed.sendMessage()");
        this.userSession.getAsyncRemote().sendText(message);
    }

    public void subscribe(Subscribe msg) {
        //System.out.println("MyWebsocketFeed.subscribe()");
        final String jsonSubscribeMessage = signObject(msg);

        addMyMessageHandler(json -> {

            SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {

                @Override
                public Void doInBackground() {

                    //display message
                    //displayMessage("message: " + json, false);

                    //parse json string to java object
                    Ticker ticker = Main.getGson().fromJson(json, Ticker.class);

                    //update the appropriate agent with the current stock price
                    agents.get(ticker.product_id).addHistory(ticker.price);

                    return null;
                }

                @Override
                public void done() {
                    //System.out.println("Done!!! " + new Date().toString());
                }
            };

            worker.execute();

        });

        //System.out.println("jsonSubscribeMessage: " + jsonSubscribeMessage);

        // send message to web socket
        sendMessage(jsonSubscribeMessage);
    }

    // TODO - get this into postHandle intercepter.
    public String signObject(Subscribe jsonObj) {
        Gson gson = new Gson();
        String jsonString = gson.toJson(jsonObj);

        String timestamp = Instant.now().getEpochSecond() + "";
        jsonObj.setKey(key);
        jsonObj.setTimestamp(timestamp);
        jsonObj.setPassphrase(passphrase);
        jsonObj.setSignature(signature.generate("", "GET", jsonString, timestamp));

        return gson.toJson(jsonObj);
    }

    public interface MessageHandler {
        void handleMessage(String message);
    }
}