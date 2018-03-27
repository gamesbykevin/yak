package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.Agent;
import com.gamesbykevin.tradingbot.calculator.Period;
import com.gamesbykevin.tradingbot.calculator.Period.Fields;
import com.gamesbykevin.tradingbot.transaction.TransactionHelper.ReasonSell;

import java.util.ArrayList;
import java.util.List;

import static com.gamesbykevin.tradingbot.calculator.strategy.SMA.calculateSMA;

/**
 * On balance volume / Accumulation Distribution Line
 */
public class OA extends Strategy {

    //the list of our sma price values
    private List<Double> smaPriceLong, smaPriceShort;

    //the list of our sma obv values
    private List<Double> smaObvLong, smaObvShort;

    //the list of our sma adl values
    private List<Double> smaAdlLong, smaAdlShort;

    //our adl object
    private ADL adlObj;

    //our obv object
    private OBV obvObj;

    private static final int PERIODS_LONG = 65;
    private static final int PERIODS_SHORT = 20;

    //our long and short periods
    private final int periodsLong, periodsShort;

    public OA() {
        this(PERIODS_LONG, PERIODS_SHORT);
    }

    public OA(int periodsLong, int periodsShort) {

        //call parent with default value
        super(0);

        //assign our values
        this.periodsLong = periodsLong;
        this.periodsShort = periodsShort;

        //create our lists for calculation
        this.smaPriceShort = new ArrayList<>();
        this.smaPriceLong = new ArrayList<>();
        this.smaObvShort = new ArrayList<>();
        this.smaObvLong = new ArrayList<>();
        this.smaAdlShort = new ArrayList<>();
        this.smaAdlLong = new ArrayList<>();

        //create our objects to do initial calculation
        this.adlObj = new ADL();
        this.obvObj = new OBV();
    }

    @Override
    public void checkBuySignal(Agent agent, List<Period> history, double currentPrice) {

        //get the latest value
        double close = history.get(history.size() - 1).close;

        //make sure current closing price is below both sma averages
        if (close < getRecent(smaPriceLong) && close < getRecent(smaPriceShort)) {

            //get the latest value
            double valueADL = getRecent(adlObj.getAccumulationDistributionLine());

            //make sure adl is above both sma averages
            if (valueADL > getRecent(smaAdlLong) && valueADL > getRecent(smaAdlShort)) {

                //get the latest value
                double valueOBV = getRecent(obvObj.getVolume());

                //make sure obv is above both sma averages and then we can sell
                if (valueOBV > getRecent(smaObvLong) && valueOBV > getRecent(smaObvShort))
                    agent.setBuy(true);
            }
        }

        //display our data
        displayData(agent, agent.hasBuy());
    }

    @Override
    public void checkSellSignal(Agent agent, List<Period> history, double currentPrice) {

        //get the latest value
        double close = history.get(history.size() - 1).close;

        //make sure current closing price is below both sma averages
        if (close > getRecent(smaPriceLong) && close > getRecent(smaPriceShort)) {

            //get the latest value
            double valueADL = getRecent(adlObj.getAccumulationDistributionLine());

            //make sure adl is above both sma averages
            if (valueADL < getRecent(smaAdlLong) && valueADL < getRecent(smaAdlShort)) {

                //get the latest value
                double valueOBV = getRecent(obvObj.getVolume());

                //make sure obv is above both sma averages and then we can sell
                if (valueOBV < getRecent(smaObvLong) && valueOBV < getRecent(smaObvShort))
                    agent.setReasonSell(ReasonSell.Reason_Strategy);
            }
        }

        //display our data
        displayData(agent, agent.getReasonSell() != null);
    }

    @Override
    public void displayData(Agent agent, boolean write) {

        //display the information
        display(agent, "SMA Price Long: ", smaPriceLong, periodsShort / 5, write);
        display(agent, "SMA Price Short: ", smaPriceShort, periodsShort / 5, write);
        display(agent, "SMA OBV Long: ", smaObvLong, periodsShort / 5, write);
        display(agent, "SMA OBV Short: ", smaObvShort, periodsShort / 5, write);
        display(agent, "SMA ADL Long: ", smaAdlLong, periodsShort / 5, write);
        display(agent, "SMA ADL Short: ", smaAdlShort, periodsShort / 5, write);

        this.adlObj.displayData(agent, write);
        this.obvObj.displayData(agent, write);
    }

    @Override
    public void calculate(List<Period> history) {

        //calculate these first
        this.adlObj.calculate(history);
        this.obvObj.calculate(history);

        //now calculate our sma lists
        calculateSMA(adlObj.getAccumulationDistributionLine(), smaAdlShort, periodsShort);
        calculateSMA(adlObj.getAccumulationDistributionLine(), smaAdlLong, periodsLong);
        calculateSMA(obvObj.getVolume(), smaObvShort, periodsShort);
        calculateSMA(obvObj.getVolume(), smaObvLong, periodsLong);
        calculateSMA(history, smaPriceShort, periodsShort, Fields.Close);
        calculateSMA(history, smaPriceLong, periodsLong, Fields.Close);
    }
}