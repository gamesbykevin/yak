package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.Period.Fields;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;

import java.util.ArrayList;
import java.util.List;

import static com.gamesbykevin.tradingbot.calculator.strategy.SMA.calculateSMA;

/**
 * ADL / OBV
 */
public class AO extends Strategy {

    //period length
    private static final int PERIODS_SMA_SHORT = 20;
    private static final int PERIODS_SMA_LONG = 50;

    //list of averages
    private List<Double> smaShortADL, smaShortOBV, smaShortPrice;
    private List<Double> smaLongADL, smaLongOBV,smaLongPrice;

    //our indicators
    private ADL objADL;
    private OBV objOBV;

    private final int periodsSmaShort, periodsSmaLong;

    public AO() {
        this(PERIODS_SMA_SHORT, PERIODS_SMA_LONG);
    }

    public AO(int periodsSmaShort, int periodsSmaLong) {

        //store our desired period length
        this.periodsSmaShort = periodsSmaShort;
        this.periodsSmaLong = periodsSmaLong;

        //create our indicators
        this.objADL = new ADL();
        this.objOBV = new OBV();

        //create our lists
        this.smaShortADL = new ArrayList<>();
        this.smaLongADL = new ArrayList<>();
        this.smaShortOBV = new ArrayList<>();
        this.smaLongOBV = new ArrayList<>();
        this.smaShortPrice = new ArrayList<>();
        this.smaLongPrice = new ArrayList<>();
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //get recent closing $
        double close = getRecent(history, Fields.Close);

        //get current adl value
        double volumeADL = getRecent(objADL.getVolume());

        //get current obv value
        double volumeOBV = getRecent(objOBV.getVolume());

        //if a indicator is below 0 we won't trade
        if (volumeADL < 0 || volumeOBV < 0)
            return;

        //for bullish divergence the closing price needs to be less than both short and long averages
        if (close < getRecent(smaShortPrice) && close < getRecent(smaLongPrice)) {

            //we also need our adl to be above the short and long
            if (volumeADL > getRecent(smaShortADL) && volumeADL > getRecent(smaLongADL)) {

                //finally we also need our obv to be above the short and long
                if (volumeOBV > getRecent(smaShortOBV) && volumeOBV > getRecent(smaLongOBV))
                    agent.setBuy(true);
            }
        }

        //display our data
        displayData(agent, agent.hasBuy());
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //get recent closing price
        double close = getRecent(history, Fields.Close);

        //if the recent closing price is below our short and long sma's sell
        if (close < getRecent(smaShortPrice) && close < getRecent(smaLongPrice))
            agent.setReasonSell(ReasonSell.Reason_Strategy);

        //display our data
        displayData(agent, agent.getReasonSell() != null);
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display the information
        objADL.displayData(agent, write);
        display(agent, "ADL Short: ", smaShortADL, write);
        display(agent, "ADL Long: ", smaLongADL, write);

        objOBV.displayData(agent, write);
        display(agent, "OBV Short: ", smaShortOBV, write);
        display(agent, "OBV Long: ", smaLongOBV, write);

        display(agent, "Close $ Short: ", smaShortPrice, write);
        display(agent, "Close $ Long: ", smaLongPrice, write);
    }

    @Override
    public void calculate(List<Period> history) {

        //calculate our indicators
        this.objADL.calculate(history);
        this.objOBV.calculate(history);

        //calculate obv sma
        calculateSMA(objOBV.getVolume(), smaShortOBV, periodsSmaShort);
        calculateSMA(objOBV.getVolume(), smaLongOBV, periodsSmaLong);

        //calculate price sma
        calculateSMA(history, smaShortPrice, periodsSmaShort, Fields.Close);
        calculateSMA(history, smaLongPrice, periodsSmaLong, Fields.Close);

        //calculate adl sma
        calculateSMA(objADL.getVolume(), smaShortADL, periodsSmaShort);
        calculateSMA(objADL.getVolume(), smaLongADL, periodsSmaLong);
    }
}