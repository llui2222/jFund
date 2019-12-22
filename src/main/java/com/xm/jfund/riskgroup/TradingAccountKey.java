package com.xm.jfund.riskgroup;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class TradingAccountKey {

    private final String takerName;
    private final String takerLogin;

    private TradingAccountKey(final String takerName, final String takerLogin) {
        this.takerName = takerName;
        this.takerLogin = takerLogin;
    }

    public static TradingAccountKey create(final String taker, final String marginAccount) {
        return new TradingAccountKey(taker, marginAccount);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final TradingAccountKey that = (TradingAccountKey) o;

        return new EqualsBuilder()
            .append(takerName, that.takerName)
            .append(takerLogin, that.takerLogin)
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
            .append(takerName)
            .append(takerLogin)
            .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
            .append("takerName", takerName)
            .append("takerLogin", takerLogin)
            .toString();
    }
}
