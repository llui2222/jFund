package com.xm.jfund.riskgroup;

public class TradingAccountRiskGroup {

    private final String groupName;
    private final TradingAccountKey key;

    private TradingAccountRiskGroup(final String groupName, final String taker, final String marginAccount) {
        this.groupName = groupName;
        this.key = TradingAccountKey.create(taker, marginAccount);
    }

    public static TradingAccountRiskGroup create(final String groupName, final String taker, final String marginAccount) {
        return new TradingAccountRiskGroup(groupName, taker, marginAccount);
    }

    public boolean isMember(final String taker, final String marginAccount) {
        return key.equals(TradingAccountKey.create(taker, marginAccount));
    }

    public String getGroupName() {
        return groupName;
    }
}
