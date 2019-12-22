package com.xm.jfund.trade;

import java.time.LocalDateTime;

public final class TradeObject {
    private final boolean isBuy;
    private final String id;
    private final long account;
    private final String symbol;
    private final double volume;
    private final double openPrice;
    private final TradeObjectType type;
    private final LocalDateTime executedTimestamp;
    private final String comment;

    private TradeObject(final boolean isBuy,
                        final String id,
                        final long account,
                        final String symbol,
                        final double volume,
                        final double openPrice,
                        final TradeObjectType type,
                        final LocalDateTime executedTimestamp,
                        final String comment) {
        this.isBuy = isBuy;
        this.id = id;
        this.account = account;
        this.symbol = symbol;
        this.volume = volume;
        this.openPrice = openPrice;
        this.type = type;
        this.executedTimestamp = executedTimestamp;
        this.comment = comment;
    }

    public static TradeObject create(final boolean isBuy,
                                     final String id,
                                     final long account,
                                     final String symbol,
                                     final double volume,
                                     final double openPrice,
                                     final TradeObjectType type,
                                     final LocalDateTime executedTimestamp,
                                     final String comment) {
        return new TradeObject(isBuy, id, account, symbol, volume, openPrice, type, executedTimestamp, comment);
    }

    public boolean isBuy() {
        return isBuy;
    }

    public String getId() {
        return id;
    }

    public long getAccount() {
        return account;
    }

    public String getSymbol() {
        return symbol;
    }

    public double getVolume() {
        return volume;
    }

    public double getOpenPrice() {
        return openPrice;
    }

    public TradeObjectType getType() {
        return type;
    }

    public LocalDateTime getExecutedTimestamp() {
        return executedTimestamp;
    }

    public String getComment() {
        return comment;
    }
}
