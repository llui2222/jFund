package com.xm.jfund.trade;

import com.xm.jfund.client.trade.model.Exposure;
import com.xm.jfund.client.trade.model.Side;
import com.xm.jfund.client.trade.model.Trade;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class TradeObjectFactory {

    public static TradeObject create(final mt4jUtils.Trade mt4Trade, final TradeObjectType tradeType, final double contractSize) {
        final double volume = (double) mt4Trade.getVolume() * 0.01D * contractSize;
        final LocalDateTime executedTimestamp = LocalDateTime.ofEpochSecond(mt4Trade.getOpen_time(), 0, ZoneOffset.UTC);
        return TradeObject.create(mt4Trade.isBuy(), String.valueOf(mt4Trade.getOrder()), mt4Trade.getLogin(), mt4Trade.getSymbol(), volume, mt4Trade.getOpen_price(), tradeType, executedTimestamp, mt4Trade.getComment());
    }

    public static TradeObject create(final Trade trade, final TradeObjectType tradeType) {
        final LocalDateTime executedTimestamp = LocalDateTime.now(ZoneOffset.UTC);
        return TradeObject.create(Side.BUY == trade.getSide(), trade.getTradeId().toString(), Long.valueOf(trade.getTakerLogin()), trade.getSymbol(), trade.getVolume().doubleValue(), trade.getAveragePrice().doubleValue(), tradeType, executedTimestamp, trade.getRequestId());
    }

    public static TradeObject create(final Exposure exposure, final TradeObjectType tradeType) {
        final LocalDateTime executedTimestamp = LocalDateTime.now(ZoneOffset.UTC);
        double openPrice = 0d;
        final int baseExposureCmp = exposure.getTotalBaseCurrencyExposure().compareTo(BigDecimal.ZERO);
        if (baseExposureCmp != 0) {
            openPrice = Math.abs(exposure.getTotalQuoteCurrencyExposure().divide(exposure.getTotalBaseCurrencyExposure(), RoundingMode.HALF_UP).abs().doubleValue());
        }
        return TradeObject.create(baseExposureCmp >= 0, String.valueOf("BaseExposure"), Long.valueOf(exposure.getTakerLogin()), exposure.getSymbol(), exposure.getTotalBaseCurrencyExposure().abs().doubleValue(), openPrice, tradeType, executedTimestamp, null);
    }
}
