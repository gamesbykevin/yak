package com.gamesbykevin.tradingbot.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import static com.gamesbykevin.tradingbot.util.PropertyUtil.displayMessage;

public class JSon {

    /**
     * We need to set a timeout so we aren't waiting forever
     */
    public static final int HTTP_REQUEST_TIMEOUT = 3000;

    /**
     * Perform GET rest call and give us the json response
     * @param link The url we want to access
     * @return The response in json string format
     */
    public static synchronized String getJsonResponse(String link) {

        String result = "";

        HttpURLConnection connection = null;

        try {

            //display our endpoint
            displayMessage(link);

            URL url = new URL(link);
            connection = (HttpURLConnection)url.openConnection();

            //set time out so we aren't waiting forever
            connection.setConnectTimeout(HTTP_REQUEST_TIMEOUT);
            connection.setReadTimeout(HTTP_REQUEST_TIMEOUT * 2);

            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");

            if (connection.getResponseCode() != 200)
                throw new RuntimeException("Failed : HTTP error code : " + connection.getResponseCode());

            BufferedReader br = new BufferedReader(new InputStreamReader((connection.getInputStream())));

            String output;

            while ((output = br.readLine()) != null) {
                result += output;
            }

            br.close();
            br = null;

        } catch (Exception e) {
            e.printStackTrace();
        } finally {

            if (connection != null) {

                try {
                    //after we are done, let's disconnect
                    connection.disconnect();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

            }
        }

        connection = null;

        return result;
    }
}