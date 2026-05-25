package cn.maarlakes.common.token.access;

import jakarta.annotation.Nonnull;

import java.time.Instant;
import java.util.Objects;

/**
 * {@link AccessToken} 的默认不可变实现。
 *
 * <p>通过包私有的构造函数和 {@link AccessToken#of} 静态工厂方法控制实例创建。
 *
 * <p>{@link #equals(Object)} 和 {@link #hashCode()} 基于 appId、token、expiresAt 三个字段，
 * 其中 token 值参与相等性判断（确保同一个 AppId 在不同时刻获取的 Token 不会误判为相等）。
 *
 * <p>{@link #toString()} 返回 appId 的字符串表示，避免在日志中泄露敏感的 Token 值。
 *
 * @author linjpxc
 */
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
