package com.xm.jfund.trade;

import org.junit.Test;
import org.junit.Assert;

public class TradeObjectTest {
    @Test
    public void testCreateWithLong() {
        final TradeObject tradeObject = TradeObject.create(
            true,
            "100",
            10017020101L,
            "EURUSD",
            1,
            1.22,
            null,
            null,
            "No Comment"
        );

        Assert.assertEquals(tradeObject.getAccount(), 10017020101L);
    }
}
