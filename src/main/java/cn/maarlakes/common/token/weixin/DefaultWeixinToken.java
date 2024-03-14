package cn.maarlakes.common.token.weixin;

import jakarta.annotation.Nonnull;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * @author linjpxc
 */
class DefaultWeixinToken implements WeixinToken {
    private static final long serialVersionUID = -60854212353897856L;

    private final String appId;
    private final String token;
    private final LocalDateTime expirationTime;

    DefaultWeixinToken(String appId, String token, LocalDateTime expirationTime) {
        this.appId = appId;
        this.token = token;
        this.expirationTime = expirationTime;
    }

    @Nonnull
    @Override
    public String getAppId() {
        return this.appId;
    }

    @Nonnull
    @Override
    public String getToken() {
        return this.token;
    }

    @Nonnull
    @Override
    public LocalDateTime getExpirationTime() {
        return this.expirationTime;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof WeixinToken) {
            final WeixinToken that = (WeixinToken) object;
            return Objects.equals(appId, that.getAppId()) && Objects.equals(token, that.getToken()) && Objects.equals(expirationTime, that.getExpirationTime());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(appId, token, expirationTime);
    }

    @Override
    public String toString() {
        return this.appId;
    }
}
