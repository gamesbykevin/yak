package com.gamesbykevin.tradingbot.calculator.strategy;

import com.gamesbykevin.tradingbot.agent.AgentManager.TradingStrategy;
import com.sun.org.apache.bcel.internal.generic.BREAKPOINT;

public class StrategyHelper {

    public static boolean setupValues(TradingStrategy strategy, int index) {

        try {

            //setup our variables per strategy
            switch (strategy) {

                case ADL:

                    //do this check
                    if (ADL.LIST_DEFAULT[index] > 0)
                        ;

                    //there is nothing to configure here so exit
                    break;

                case ADX:
                    ADX.PERIODS_ADX = ADX.LIST_PERIODS_ADX[index];
                    ADX.PERIODS_SMA = ADX.LIST_PERIODS_SMA[index];
                    ADX.TREND_ADX = ADX.LIST_TREND_ADX[index];
                    break;

                case BB:
                    BB.PERIODS_BB = BB.LIST_PERIODS_BB[index];
                    break;

                case BBER:
                    BBER.RSI_LINE = BBER.LIST_RSI_LINE[index];
                    EMA.PERIODS_EMA_LONG = BBER.LIST_PERIODS_EMA_LONG[index];
                    EMA.PERIODS_EMA_SHORT = BBER.LIST_PERIODS_EMA_SHORT[index];
                    RSI.PERIODS_RSI = BBER.LIST_PERIODS_RSI[index];
                    BB.PERIODS_BB = BBER.LIST_PERIODS_BB[index];
                    break;

                case BBR:
                    BBR.RESISTANCE_LINE = BBR.LIST_RESISTANCE_LINE[index];
                    BBR.SUPPORT_LINE = BBR.LIST_SUPPORT_LINE[index];
                    BB.PERIODS_BB = BBR.LIST_PERIODS_BB[index];
                    RSI.PERIODS_RSI = BBR.LIST_PERIODS_RSI[index];
                    break;

                case EMA:
                    EMA.PERIODS_EMA_LONG = EMA.LIST_PERIODS_EMA_LONG[index];
                    EMA.PERIODS_EMA_SHORT = EMA.LIST_PERIODS_EMA_SHORT[index];
                    break;

                case EMAR:
                    EMA.PERIODS_EMA_SHORT = EMAR.LIST_PERIODS_EMA_SHORT[index];
                    EMA.PERIODS_EMA_LONG = EMAR.LIST_PERIODS_EMA_LONG[index];
                    RSI.PERIODS_RSI = EMAR.LIST_PERIODS_RSI[index];
                    EMAR.RSI_LINE = EMAR.LIST_RSI_LINE[index];
                    break;

                case EMAS:
                    EMAS.PERIODS_SMA_PRICE = EMAS.LIST_PERIODS_SMA_PRICE[index];
                    EMA.PERIODS_EMA_SHORT = EMAS.LIST_PERIODS_EMA_SHORT[index];
                    EMA.PERIODS_EMA_LONG = EMAS.LIST_PERIODS_EMA_LONG[index];
                    break;

                case EMASV:
                    EMASV.PERIODS_EMA_SHORT = EMASV.LIST_PERIODS_EMA_SHORT[index];
                    EMASV.PERIODS_SMA_PRICE = EMASV.LIST_PERIODS_SMA_PRICE[index];
                    EMASV.PERIODS_SMA_VOLUME = EMASV.LIST_PERIODS_SMA_VOLUME[index];
                    EMA.PERIODS_EMA_LONG = EMASV.LIST_PERIODS_EMA_LONG[index];
                    EMA.PERIODS_EMA_SHORT = EMASV.LIST_PERIODS_EMA_SHORT[index];
                    break;

                case EMV:
                    EMV.PERIODS_EMV = EMV.LIST_PERIODS_EMV[index];
                    break;

                case EMVS:
                    EMVS.PERIODS_SMA = EMVS.LIST_PERIODS_SMA[index];
                    EMV.PERIODS_EMV = EMVS.LIST_PERIODS_EMV[index];
                    break;

                case HA:
                    HA.PERIODS_HA = HA.LIST_PERIODS_HA[index];
                    break;

                case MACD:
                    MACD.PERIODS_SMA_TREND = MACD.LIST_PERIODS_SMA_TREND[index];
                    MACD.PERIODS_MACD = MACD.LIST_PERIODS_MACD[index];
                    EMA.PERIODS_EMA_SHORT = MACD.LIST_PERIODS_EMA_SHORT[index];
                    EMA.PERIODS_EMA_LONG = MACD.LIST_PERIODS_EMA_LONG[index];
                    break;

                case MACDD:
                    EMA.PERIODS_EMA_SHORT = MACDD.LIST_PERIODS_EMA_SHORT[index];
                    EMA.PERIODS_EMA_LONG = MACDD.LIST_PERIODS_EMA_LONG[index];
                    MACD.PERIODS_MACD = MACDD.LIST_PERIODS_MACD[index];
                    MACDD.PERIODS_MACD = MACDD.LIST_PERIODS_MACD[index];
                    MACD.PERIODS_SMA_TREND = MACDD.LIST_PERIODS_SMA_TREND[index];
                    break;

                case MACS:
                    MACS.PERIODS_MACS_FAST = MACS.LIST_PERIODS_MACS_FAST[index];
                    MACS.PERIODS_MACS_SLOW = MACS.LIST_PERIODS_MACS_SLOW[index];
                    MACS.PERIODS_MACS_TREND = MACS.LIST_PERIODS_MACS_TREND[index];
                    break;

                case NP:
                    NVI.PERIODS_EMA = NP.LIST_PERIODS_EMA_NVI[index];
                    PVI.PERIODS_EMA = NP.LIST_PERIODS_EMA_PVI[index];
                    break;

                case NR:
                    NR.PERIODS_NR = NR.LIST_PERIODS_NR[index];
                    break;

                case NVI:
                    NVI.PERIODS_EMA = NVI.LIST_PERIODS_EMA[index];
                    break;

                case OA:
                    OA.PERIODS_LONG = OA.LIST_PERIODS_LONG[index];
                    OA.PERIODS_SHORT = OA.LIST_PERIODS_SHORT[index];
                    OBV.PERIODS_OBV = OA.LIST_PERIODS_OBV[index];

                    break;

                case OBV:
                    OBV.PERIODS_OBV = OBV.LIST_PERIODS_OBV[index];
                    break;

                case PVI:
                    PVI.PERIODS_EMA = PVI.LIST_PERIODS_EMA[index];
                    break;

                case RSI:
                    RSI.RESISTANCE_LINE = RSI.LIST_RESISTANCE_LINE[index];
                    RSI.SUPPORT_LINE = RSI.LIST_SUPPORT_LINE[index];
                    RSI.PERIODS_RSI = RSI.LIST_PERIODS_RSI[index];
                    RSI.PERIODS_SMA_PRICE = RSI.LIST_PERIODS_SMA_PRICE[index];
                    break;

                case RSIA:
                    ADX.PERIODS_ADX = RSIA.LIST_PERIODS_ADX[index];
                    ADX.TREND_ADX = RSIA.LIST_TREND_ADX[index];
                    RSI.PERIODS_RSI = RSIA.LIST_PERIODS_RSI[index];
                    RSIA.SUPPORT_LINE = RSIA.LIST_SUPPORT_LINE[index];
                    RSIA.RESISTANCE_LINE = RSIA.LIST_RESISTANCE_LINE[index];
                    break;

                case RSIM:
                    EMA.PERIODS_EMA_LONG = RSIM.LIST_PERIODS_EMA_LONG[index];
                    EMA.PERIODS_EMA_SHORT = RSIM.LIST_PERIODS_EMA_SHORT[index];
                    MACD.PERIODS_SMA_TREND = RSIM.LIST_PERIODS_SMA_TREND[index];
                    MACD.PERIODS_MACD = RSIM.LIST_PERIODS_MACD[index];
                    RSI.PERIODS_RSI = RSIM.LIST_PERIODS_RSI[index];
                    RSIM.RESISTANCE_LINE = RSIM.LIST_RESISTANCE_LINE[index];
                    RSIM.SUPPORT_LINE = RSIM.LIST_SUPPORT_LINE[index];
                    RSIM.PERIODS_MACD = RSIM.LIST_PERIODS_MACD[index];
                    break;

                case SO:
                    SO.PERIODS_SO = SO.LIST_PERIODS_SO[index];
                    SO.PERIODS_SMA_SO = SO.LIST_PERIODS_SMA_SO[index];
                    SO.PERIODS_SMA_PRICE_LONG = SO.LIST_PERIODS_SMA_PRICE_LONG[index];
                    SO.PERIODS_SMA_PRICE_SHORT = SO.LIST_PERIODS_SMA_PRICE_SHORT[index];
                    SO.OVER_SOLD = SO.LIST_OVER_SOLD[index];
                    SO.OVER_BOUGHT = SO.LIST_OVER_BOUGHT[index];
                    break;

                case SOC:
                    SO.PERIODS_SMA_SO = SOC.LIST_PERIODS_SMA[index];
                    SO.PERIODS_SO = SOC.LIST_PERIODS_SO[index];
                    SO.PERIODS_SMA_PRICE_LONG = SOC.LIST_PERIODS_SMA_PRICE_LONG[index];
                    SO.PERIODS_SMA_PRICE_SHORT = SOC.LIST_PERIODS_SMA_PRICE_SHORT[index];
                    break;

                case SOD:
                    SOD.PERIODS_SO = SOD.LIST_PERIODS_SO[index];
                    SO.PERIODS_SMA_SO = SOD.LIST_PERIODS_SMA[index];
                    SO.PERIODS_SO = SOD.LIST_PERIODS_SO[index];
                    SO.PERIODS_SMA_PRICE_LONG = SOD.LIST_PERIODS_SMA_PRICE_LONG[index];
                    SO.PERIODS_SMA_PRICE_SHORT = SOD.LIST_PERIODS_SMA_PRICE_SHORT[index];
                    break;

                case SOEMA:
                    SOEMA.SO_INDICATOR = SOEMA.LIST_SO_INDICATOR[index];
                    SOEMA.PERIODS_SO = SOEMA.LIST_PERIODS_SO[index];
                    SOEMA.PERIODS_SMA_SO = SOEMA.LIST_PERIODS_SMA_SO[index];
                    SOEMA.PERIODS_LONG = SOEMA.LIST_PERIODS_LONG[index];
                    EMA.PERIODS_EMA_LONG = SOEMA.LIST_PERIODS_EMA_LONG[index];
                    EMA.PERIODS_EMA_SHORT = SOEMA.LIST_PERIODS_EMA_SHORT[index];
                    SO.PERIODS_SMA_SO = SOEMA.LIST_PERIODS_SMA_SO[index];
                    SO.PERIODS_SO = SOEMA.LIST_PERIODS_SO[index];
                    break;

                case SR:
                    SR.PERIODS_LONG = SR.LIST_PERIODS_LONG[index];
                    SR.PERIODS_SHORT = SR.LIST_PERIODS_SHORT[index];
                    SR.OVER_BOUGHT = SR.LIST_OVER_BOUGHT[index];
                    SR.OVER_SOLD = SR.LIST_OVER_SOLD[index];
                    SR.PERIODS_STOCH_RSI = SR.LIST_PERIODS_STOCH_RSI[index];
                    RSI.PERIODS_RSI = SR.PERIODS_STOCH_RSI;
                    break;

                case TWO_RSI:
                    TWO_RSI.MIN_RSI = TWO_RSI.LIST_MIN_RSI[index];
                    TWO_RSI.MAX_RSI = TWO_RSI.LIST_MAX_RSI[index];
                    TWO_RSI.PERIODS_SMA = TWO_RSI.LIST_PERIODS_SMA[index];
                    RSI.PERIODS_RSI = TWO_RSI.LIST_PERIODS_RSI[index];
                    break;

                default:
                    throw new RuntimeException("Strategy not handled: " + strategy);
            }

        } catch (Exception e) {
            return false;
        }

        return true;
    }
}