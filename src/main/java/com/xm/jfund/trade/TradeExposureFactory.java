package com.xm.jfund.trade;

import com.xm.jfund.client.trade.model.Exposure;
import com.xm.jfund.client.trade.model.Side;
import com.xm.jfund.client.trade.model.Trade;
import jAnalystUtils.TradeExposure;

import java.math.BigDecimal;

public class TradeExposureFactory {

    public static TradeExposure create(final Trade trade) {

        final boolean isBuy = trade.getSide() == Side.BUY;
        final BigDecimal volume = isBuy ? trade.getVolume() : trade.getVolume().negate();
        return TradeExposure.create(isBuy, volume.doubleValue(), volume.multiply(trade.getAveragePrice()).negate().doubleValue());
    }

    public static TradeExposure create(final Exposure exposure) {
        final boolean isBuy = exposure.getTotalBaseCurrencyExposure().compareTo(BigDecimal.ZERO) >= 0;
        return TradeExposure.create(isBuy, exposure.getTotalBaseCurrencyExposure().doubleValue(), exposure.getTotalQuoteCurrencyExposure().doubleValue());
    }
}
