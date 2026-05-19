package cn.maarlakes.common.token.access.weixin;

import jakarta.annotation.Nonnull;

import java.util.Objects;

class DefaultWeixinAppId implements WeixinAppId {

    private final String appId;

    DefaultWeixinAppId(@Nonnull String appId) {
        this.appId = Objects.requireNonNull(appId);
    }

    @Nonnull
    @Override
    public String getAppId() {
        return this.appId;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof WeixinAppId) {
            return Objects.equals(this.appId, ((WeixinAppId) o).getAppId());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(appId);
    }

    @Override
    public String toString() {
        return "weixin@" + this.appId;
    }
}
