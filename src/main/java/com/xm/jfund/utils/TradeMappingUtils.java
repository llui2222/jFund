package com.xm.jfund.utils;

import com.xm.jfund.trade.TradeObjectType;
import jAnalystUtils.TradeInfo.TradeType;

import java.util.HashMap;
import java.util.Map;

public class TradeMappingUtils {

    private static final Map<TradeObjectType, TradeType> TRADE_TYPE_MAPPINGS = new HashMap<TradeObjectType, TradeType>() {{
        put(TradeObjectType.OPEN_TRADE, TradeType.OpenTrade);
        put(TradeObjectType.CLOSE_TRADE, TradeType.CloseTrade);
        put(TradeObjectType.TRADE_UPDATE, TradeType.UpdateTrade);
    }};

    public static TradeType getTradeType(final TradeObjectType tradeObjectType) {
        return TRADE_TYPE_MAPPINGS.get(tradeObjectType);
    }
}
