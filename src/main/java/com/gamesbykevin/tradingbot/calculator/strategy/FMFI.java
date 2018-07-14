package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.Period.Fields;
import com.gamesbykevin.tradingbot.calculator.indicator.williams.Fractal;
import com.gamesbykevin.tradingbot.calculator.indicator.williams.Fractal.Status;
import com.gamesbykevin.tradingbot.calculator.indicator.williams.MarketFacilitationIndex;

import java.util.List;

/**
 * Fractal / Market Facilitation Index
 */
public class FMFI extends Strategy {

    //configurable values
    private static final int PERIODS_FRACTAL = 5;

    //how to access our indicator objects
    private static int INDEX_FRACTAL;
    private static int INDEX_MARKET;

    public FMFI() {

        //call parent
        super(Key.FMFI);

        //add indicators
        INDEX_FRACTAL = addIndicator(new Fractal(PERIODS_FRACTAL));
        INDEX_MARKET = addIndicator(new MarketFacilitationIndex());
    }

    @Override
    public boolean hasBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //get our indicators
        Fractal fractal = (Fractal)getIndicator(INDEX_FRACTAL);
        MarketFacilitationIndex market = (MarketFacilitationIndex)getIndicator(INDEX_MARKET);

        //which fractal should we check
        int indexOffset = (PERIODS_FRACTAL / 2) + 1;

        //get the status of our fractal
        Status status = fractal.getStatusList().get(fractal.getStatusList().size() - indexOffset);

        //make sure we have a buy fractal first
        if (status == Status.Both || status == Status.Buy) {

            Period curr = history.get(history.size() - 1);
            Period prev = history.get(history.size() - 2);

            //green confirms the trend and if the price is increasing let's buy
            if (hasGreen(history, market) && curr.close > prev.close)
                return true;
        }

        //no signal
        return false;
    }

    @Override
    public boolean hasSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //get our indicators
        Fractal fractal = (Fractal)getIndicator(INDEX_FRACTAL);
        MarketFacilitationIndex market = (MarketFacilitationIndex)getIndicator(INDEX_MARKET);

        //which fractal should we check
        int indexOffset = (PERIODS_FRACTAL / 2) + 1;

        //get the status of our fractal
        Status status = fractal.getStatusList().get(fractal.getStatusList().size() - indexOffset);

        //make sure we have a sell fractal first
        if (status == Status.Both || status == Status.Sell) {

            Period curr = history.get(history.size() - 1);
            Period prev = history.get(history.size() - 2);

            //protect our investment
            if (hasFake(history, market) || hasFade(history, market) || hasSquat(history, market))
                goShort(agent, getShortLow(history));

            //green confirms the trend and if the price is decreasing let's sell
            if (hasGreen(history, market) && curr.close < prev.close) {
                goShort(agent, getShortLow(history));
                return true;
            }
        }

        //no signal
        return false;
    }

    private boolean hasGreen(List<Period> history, MarketFacilitationIndex market) {

        //get the current and previous values
        Period currVol = history.get(history.size() - 1);
        Period prevVol = history.get(history.size() - 2);
        double currIdx = market.getMarketFacilitationIndex().get(market.getMarketFacilitationIndex().size() - 1);
        double prevIdx = market.getMarketFacilitationIndex().get(market.getMarketFacilitationIndex().size() - 2);

        //if both values are up we are green
        return (currVol.volume > prevVol.volume) && (currIdx > prevIdx);
    }

    private boolean hasFade(List<Period> history, MarketFacilitationIndex market) {

        //get the current and previous values
        Period currVol = history.get(history.size() - 1);
        Period prevVol = history.get(history.size() - 2);
        double currIdx = market.getMarketFacilitationIndex().get(market.getMarketFacilitationIndex().size() - 1);
        double prevIdx = market.getMarketFacilitationIndex().get(market.getMarketFacilitationIndex().size() - 2);

        //if both values are down we have a fade
        return (currVol.volume < prevVol.volume) && (currIdx < prevIdx);
    }

    private boolean hasFake(List<Period> history, MarketFacilitationIndex market) {

        //get the current and previous values
        Period currVol = history.get(history.size() - 1);
        Period prevVol = history.get(history.size() - 2);
        double currIdx = market.getMarketFacilitationIndex().get(market.getMarketFacilitationIndex().size() - 1);
        double prevIdx = market.getMarketFacilitationIndex().get(market.getMarketFacilitationIndex().size() - 2);

        //if volume is down and index is up we have a fake
        return (currVol.volume < prevVol.volume) && (currIdx > prevIdx);
    }

    private boolean hasSquat(List<Period> history, MarketFacilitationIndex market) {

        //get the current and previous values
        Period currVol = history.get(history.size() - 1);
        Period prevVol = history.get(history.size() - 2);
        double currIdx = market.getMarketFacilitationIndex().get(market.getMarketFacilitationIndex().size() - 1);
        double prevIdx = market.getMarketFacilitationIndex().get(market.getMarketFacilitationIndex().size() - 2);

        //if volume is up and index is down we have a squat
        return (currVol.volume > prevVol.volume) && (currIdx < prevIdx);
    }
}