package com.xm.jfund.trade;

import com.xm.jfund.client.trade.model.Exposure;
import com.xm.jfund.client.trade.model.Side;
import com.xm.jfund.client.trade.model.Trade;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TradeObjectFactoryTest {

    private static final UUID uuID = UUID.randomUUID();
    private static final int ID = 1;
    private static final String SYMBOL = "EURUSD";
    private static final String TAKER_NAME = "3000";
    private static final int LOGIN_ACCOUNT = 1;
    private static final BigDecimal OPEN_PRICE = BigDecimal.valueOf(1.5);
    private static final BigDecimal CONTRACT_SIZE = BigDecimal.valueOf(1000);
    private static final BigDecimal BASE_ASSETS = BigDecimal.TEN;
    private static final BigDecimal QUOTE_ASSETS = BASE_ASSETS.multiply(OPEN_PRICE).negate();
    @SuppressWarnings("BigDecimalMethodWithoutRoundingCalled")
    private static final int MT4_VOLUME = BASE_ASSETS.divide(CONTRACT_SIZE).divide(BigDecimal.valueOf(0.01)).intValue();
    private static final int OPEN_TIME = 1000;

    private static final String COMMENT = "comment";

    @Mock
    private mt4jUtils.Trade mt4Trade;
    @Mock
    private Trade trade;

    @Test
    public void shouldCreateLongTradeFromMt4Trade() {
        mockMt4Trade(ID, LOGIN_ACCOUNT, true, MT4_VOLUME, OPEN_TIME, SYMBOL, 1.5, COMMENT);

        final TradeObject tradeObject = TradeObjectFactory.create(mt4Trade, TradeObjectType.OPEN_TRADE, CONTRACT_SIZE.doubleValue());

        verifyTradeObject(tradeObject, String.valueOf(ID), true, LOGIN_ACCOUNT, SYMBOL, BASE_ASSETS.doubleValue(), OPEN_PRICE.doubleValue(), TradeObjectType.OPEN_TRADE, COMMENT);

        assertThat(tradeObject.getExecutedTimestamp()).isEqualTo(LocalDateTime.ofEpochSecond(1000, 0, ZoneOffset.UTC));
    }

    @Test
    public void shouldCreateShortTradeFromMt4Trade() {

        mockMt4Trade(ID, LOGIN_ACCOUNT, false, MT4_VOLUME, OPEN_TIME, SYMBOL, OPEN_PRICE.doubleValue(), COMMENT);

        final TradeObject tradeObject = TradeObjectFactory.create(mt4Trade, TradeObjectType.OPEN_TRADE, CONTRACT_SIZE.doubleValue());

        verifyTradeObject(tradeObject, String.valueOf(ID), false, LOGIN_ACCOUNT, SYMBOL, BASE_ASSETS.doubleValue(), OPEN_PRICE.doubleValue(), TradeObjectType.OPEN_TRADE, COMMENT);

        assertThat(tradeObject.getExecutedTimestamp()).isEqualTo(LocalDateTime.ofEpochSecond(OPEN_TIME, 0, ZoneOffset.UTC));
    }

    @Test
    public void shouldCreateLongTradeFromTradeServiceTrade() {
        mockTrade(Side.BUY, String.valueOf(LOGIN_ACCOUNT), SYMBOL, BASE_ASSETS, OPEN_PRICE, COMMENT);

        final TradeObject tradeObject = TradeObjectFactory.create(trade, TradeObjectType.OPEN_TRADE);

        verifyTradeObject(tradeObject, uuID.toString(), true, LOGIN_ACCOUNT, SYMBOL, BASE_ASSETS.doubleValue(), OPEN_PRICE.doubleValue(), TradeObjectType.OPEN_TRADE, COMMENT);
    }

    @Test
    public void shouldCreateShortTradeFromTradeServiceTrade() {
        mockTrade(Side.SELL, String.valueOf(LOGIN_ACCOUNT), SYMBOL, BASE_ASSETS, OPEN_PRICE, COMMENT);

        final TradeObject tradeObject = TradeObjectFactory.create(trade, TradeObjectType.OPEN_TRADE);

        verifyTradeObject(tradeObject, uuID.toString(), false, LOGIN_ACCOUNT, SYMBOL, BASE_ASSETS.doubleValue(), OPEN_PRICE.doubleValue(), TradeObjectType.OPEN_TRADE, COMMENT);
    }

    @Test
    public void shouldCreateLongTradeFromTradeServiceExposure() {
        final Exposure exposure = Exposure.create(BASE_ASSETS, QUOTE_ASSETS, SYMBOL, TAKER_NAME, String.valueOf(LOGIN_ACCOUNT), 0L);

        final TradeObject tradeObject = TradeObjectFactory.create(exposure, TradeObjectType.OPEN_TRADE);

        verifyTradeObject(tradeObject, "BaseExposure", true, LOGIN_ACCOUNT, SYMBOL, BASE_ASSETS.doubleValue(), OPEN_PRICE.doubleValue(), TradeObjectType.OPEN_TRADE, null);
    }

    @Test
    public void shouldCreateShortTradeFromTradeServiceExposure() {
        final Exposure exposure = Exposure.create(BASE_ASSETS.negate(), QUOTE_ASSETS.negate(), SYMBOL, TAKER_NAME, String.valueOf(LOGIN_ACCOUNT), 0L);

        final TradeObject tradeObject = TradeObjectFactory.create(exposure, TradeObjectType.OPEN_TRADE);

        verifyTradeObject(tradeObject, "BaseExposure", false, LOGIN_ACCOUNT, SYMBOL, BASE_ASSETS.doubleValue(), OPEN_PRICE.doubleValue(), TradeObjectType.OPEN_TRADE, null);
    }

    @Test
    public void shouldCreateZeroTradeFromTradeServiceExposure() {
        final Exposure exposure = Exposure.create(BigDecimal.ZERO, BigDecimal.ZERO, SYMBOL, TAKER_NAME, String.valueOf(LOGIN_ACCOUNT), 0L);

        final TradeObject tradeObject = TradeObjectFactory.create(exposure, TradeObjectType.OPEN_TRADE);

        verifyTradeObject(tradeObject, "BaseExposure", true, LOGIN_ACCOUNT, SYMBOL, 0d, 0d, TradeObjectType.OPEN_TRADE, null);
    }

    private void mockTrade(final Side side, final String taker, final String symbol, final BigDecimal volume, final BigDecimal averagePrice, final String comment) {
        when(trade.getTradeId()).thenReturn(uuID);
        when(trade.getRequestId()).thenReturn(comment);
        when(trade.getSide()).thenReturn(side);
        when(trade.getTakerLogin()).thenReturn(taker);
        when(trade.getSymbol()).thenReturn(symbol);
        when(trade.getVolume()).thenReturn(volume);
        when(trade.getAveragePrice()).thenReturn(averagePrice);
    }

    private void mockMt4Trade(final int id, final int login, final boolean isBuy, final int volume, final int openTime, final String symbol, final Double openPrice, final String comment) {
        when(mt4Trade.getOrder()).thenReturn(id);
        when(mt4Trade.getLogin()).thenReturn(login);
        when(mt4Trade.isBuy()).thenReturn(isBuy);
        when(mt4Trade.getVolume()).thenReturn(volume);
        when(mt4Trade.getOpen_price()).thenReturn(openPrice);
        when(mt4Trade.getSymbol()).thenReturn(symbol);
        when(mt4Trade.getOpen_time()).thenReturn(openTime);
        when(mt4Trade.getComment()).thenReturn(comment);
    }

    private void verifyTradeObject(final TradeObject tradeObject, final String id, final boolean isBuy, final int taker, final String symbol, final double volume, final double openPrice, final TradeObjectType tradeObjectType, final String comment) {
        assertThat(tradeObject).isNotNull();
        assertThat(tradeObject.getId()).isEqualTo(id);
        assertThat(tradeObject.isBuy()).isEqualTo(isBuy);
        assertThat(tradeObject.getAccount()).isEqualTo(taker);
        assertThat(tradeObject.getSymbol()).isEqualTo(symbol);
        assertThat(tradeObject.getVolume()).isEqualTo(volume);
        assertThat(tradeObject.getOpenPrice()).isEqualTo(openPrice);
        assertThat(tradeObject.getType()).isEqualTo(tradeObjectType);
        assertThat(tradeObject.getComment()).isEqualTo(comment);
    }
}