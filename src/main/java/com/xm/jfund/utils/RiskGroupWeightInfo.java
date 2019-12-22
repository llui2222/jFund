package com.xm.jfund.utils;

public final class RiskGroupWeightInfo {

    public static RiskGroupWeightInfo create(final String groupName, final double weight, final boolean isClient) {
        return new RiskGroupWeightInfo(groupName, weight, isClient);
    }

    public String getGroupName() {
        return mGroupName;
    }

    public double getWeight() {
        return mWeight;
    }

    public boolean isClient() {
        return mIsClient;
    }

    private RiskGroupWeightInfo(final String groupName, final double weight, final boolean isClient) {
        mGroupName = groupName;
        mWeight = weight;
        mIsClient = isClient;
    }

    private final String mGroupName;
    private final double mWeight;
    private final boolean mIsClient;
}
