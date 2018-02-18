package com.gamesbykevin.tradingbot.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class JSon {

    /**
     * Perform GET rest call and give us the json response
     * @param link The url we want to access
     * @return The response in json string format
     */
    public static synchronized String getJsonResponse(String link) {

        String result = "";

        try {

            URL url = new URL(link);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");

            if (conn.getResponseCode() != 200)
                throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());

            BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));

            String output;

            while ((output = br.readLine()) != null) {
                result += output;
            }

            conn.disconnect();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }
}