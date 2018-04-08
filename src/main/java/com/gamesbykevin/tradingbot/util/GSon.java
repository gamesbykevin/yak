package com.gamesbykevin.tradingbot.util;

import com.google.gson.*;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

import java.lang.reflect.Type;

public final class GSon {

    private static Gson GSON;

    /**
     * Get GSon object
     * @return Object used to parse json string
     */
    public static final Gson getGson() {

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