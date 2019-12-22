package com.xm.jfund.utils;

import com.xm.jfund.utils.StrategySymbolLimits.TYPE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.Map;

public class CurrentActiveThresholdTypeAuditHandler {
    private static final String sCurrentActiveSymbolThresholdParameter = "sCurrentActiveSymbolThresholdParameter";
    private static final String loggerFileName = "current_active_symbol_thresholds";
    private static final Logger sCurrentActiveSymbolThresholdLogger = LoggerFactory.getLogger("sCurrentActiveSymbolThresholdLogger");

    public static void initializeCurrentActiveStrategySymbolLimits(
        final Map<Integer,StrategyParameters> strategyParametersMap) {

        for (final StrategyParameters sp : strategyParametersMap.values()) {
            for (final Map<StrategySymbolLimits.TYPE, StrategySymbolLimits> st : sp.getSymbolLimits().values()) {
                final int strategyId = st.get(TYPE.STANDARD).getStrategyId();
                final String symbolName = st.get(TYPE.STANDARD).getSymbolName();
                if (st.containsKey(TYPE.CATCH_UP)) {
                    log(strategyId, symbolName, TYPE.CATCH_UP, INITIALIZATION.YES);
                }

                else {
                    log(strategyId, symbolName, TYPE.STANDARD, INITIALIZATION.YES);
                }
            }
        }
    }

    public static void updateCurrentActiveStrategySymbolLimits(final int strategyId, final String destSymbol,
                                                               final TYPE currentExecutionType) {
        log(strategyId, destSymbol, currentExecutionType, INITIALIZATION.NO);
    }

    private static void log(final int strategyId, final String symbol, final TYPE currentExecutionType,
                            final INITIALIZATION initialization) {
        MDC.put(sCurrentActiveSymbolThresholdParameter, loggerFileName);
        sCurrentActiveSymbolThresholdLogger.info(
            String.format(
                "Strategy: %d, Symbol: %s, Type: %s, Startup: %s", strategyId, symbol, currentExecutionType, initialization));
    }

    private enum INITIALIZATION{
        YES, NO
    }
}
