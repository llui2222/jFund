package com.xm.jfund.utils;

import jAnalystUtils.TradeInfo;

public final class SubmittedTradeMarker {

    public static SubmittedTradeMarker create(final String comment, final TradeInfo tradeInfo) {
        return new SubmittedTradeMarker(comment, tradeInfo);
    }

    public String getComment() {
        return mComment;
    }

    public TradeInfo getTradeInfo() {
        return mTradeInfo;
    }

    private SubmittedTradeMarker(final String comment, final TradeInfo tradeInfo) {
        mComment = comment;
        mTradeInfo = tradeInfo;
    }

    private final String mComment;
    private final TradeInfo mTradeInfo;
}
