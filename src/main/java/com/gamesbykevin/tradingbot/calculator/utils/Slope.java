package com.gamesbykevin.tradingbot.calculator.utils;

import com.gamesbykevin.tradingbot.calculator.Period;

import java.util.List;

/**
 * Slope
 */
public class Slope {

    public static float getSlope(double x1, double x2, double y1, double y2) {
        return (float)(y2 - y1) / (float)(x2 - x1);
    }
}