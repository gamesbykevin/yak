package com.gamesbykevin.tradingbot.order;

public class BasicOrderHelper {

    public static float TRADE_RISK_RATIO;

    //the allowed ratio range
    public static final float TRADE_RISK_RATIO_MIN = 0.01f;
    public static final float TRADE_RISK_RATIO_MAX = 1.00f;

    public enum Action {

        Buy("buy"),
        Sell("sell");

        private final String description;

        Action(String description) {
            this.description = description;
        }

        public String getDescription() {
            return this.description;
        }
    }

    /**
     * The possible status of our limit order
     */
    public enum Status {

        Pending("pending"),
        Open("open"),
        Done("done"),
        Filled("filled"),
        Cancelled("cancelled"),
        Rejected("rejected");

        private final String description;

        Status(String description) {
            this.description = description;
        }

        public String getDescription() {
            return this.description;
        }
    }
}