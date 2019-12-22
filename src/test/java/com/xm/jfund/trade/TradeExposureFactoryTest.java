package com.xm.jfund.trade;

import com.xm.jfund.client.trade.model.Exposure;
import com.xm.jfund.client.trade.model.Side;
import com.xm.jfund.client.trade.model.Trade;
import jAnalystUtils.TradeExposure;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TradeExposureFactoryTest {

    @Mock
    private Trade trade;
    @Mock
    private Exposure exposure;

    @Test
    public void shouldCreateLongTradeExposureFromTrade() {
        mockTrade(Side.BUY, BigDecimal.valueOf(100), BigDecimal.valueOf(1.5));

        final TradeExposure tradeExposure = TradeExposureFactory.create(trade);

        assertThat(tradeExposure).isNotNull();
        assertThat(tradeExposure.mq1).isEqualTo(100d);
        assertThat(tradeExposure.mq2).isEqualTo(-150d);
    }

    @Test
    public void shouldCreateShortTradeExposureFromTrade() {
        mockTrade(Side.SELL, BigDecimal.valueOf(100), BigDecimal.valueOf(1.5));

        final TradeExposure tradeExposure = TradeExposureFactory.create(trade);

        assertThat(tradeExposure).isNotNull();
        assertThat(tradeExposure.mq1).isEqualTo(-100d);
        assertThat(tradeExposure.mq2).isEqualTo(150d);
    }

    @Test
    public void shouldCreateLongTradeExposureFromExposure() {
        mockExposure(BigDecimal.valueOf(100), BigDecimal.valueOf(-150));

        final TradeExposure tradeExposure = TradeExposureFactory.create(exposure);

        assertThat(tradeExposure).isNotNull();
        assertThat(tradeExposure.mq1).isEqualTo(100d);
        assertThat(tradeExposure.mq2).isEqualTo(-150d);
    }

    @Test
    public void shouldCreateShortTradeExposureFromExposure() {
        mockExposure(BigDecimal.valueOf(-100), BigDecimal.valueOf(150));

        final TradeExposure tradeExposure = TradeExposureFactory.create(exposure);

        assertThat(tradeExposure).isNotNull();
        assertThat(tradeExposure.mq1).isEqualTo(-100d);
        assertThat(tradeExposure.mq2).isEqualTo(150d);
    }

    private void mockTrade(final Side side, final BigDecimal volume, final BigDecimal openPrice) {
        when(trade.getSide()).thenReturn(side);
        when(trade.getVolume()).thenReturn(volume);
        when(trade.getAveragePrice()).thenReturn(openPrice);
    }

    private void mockExposure(final BigDecimal baseExposure, final BigDecimal quoteExposure) {
        when(exposure.getTotalBaseCurrencyExposure()).thenReturn(baseExposure);
        when(exposure.getTotalQuoteCurrencyExposure()).thenReturn(quoteExposure);
    }
}