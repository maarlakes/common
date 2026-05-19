package cn.maarlakes.common.token.access;

import jakarta.annotation.Nonnull;

import java.time.Instant;
import java.util.Objects;

class DefaultAccessToken implements AccessToken {
    private static final long serialVersionUID = -3867002315430777184L;

    private final AppId appId;
    private final String token;
    private final Instant expiresAt;

    DefaultAccessToken(@Nonnull AppId appId, @Nonnull String token, @Nonnull Instant expiresAt) {
        this.appId = appId;
        this.token = token;
        this.expiresAt = expiresAt;
    }

    @Nonnull
    @Override
    public Instant getExpiresAt() {
        return this.expiresAt;
    }

    @Nonnull
    @Override
    public AppId getAppId() {
        return this.appId;
    }

    @Nonnull
    @Override
    public String getToken() {
        return this.token;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof AccessToken) {
            final AccessToken that = (AccessToken) o;
            return Objects.equals(appId, that.getAppId()) && Objects.equals(token, that.getToken()) && Objects.equals(expiresAt, that.getExpiresAt());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(appId, token, expiresAt);
    }

    @Override
    public String toString() {
        return this.appId.toString();
    }
}
