package cn.maarlakes.common.http;

import jakarta.annotation.Nonnull;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * {@link Cookie.Builder} 的默认实现，提供链式调用的 Cookie 构建器。
 *
 * <p>所有属性可选设置，仅 name 为必填。{@link #build()} 时将所有已设置的属性
 *传递给 {@link DefaultCookie} 构造函数创建不可变的 Cookie 实例。</p>
 *
 * @author linjpxc
 */
final class CookieBuilder implements Cookie.Builder {

    private String name;

    private String value;
    private String domain;
    private String path;
    private long maxAge;
    private boolean isSecure;
    private boolean isHttpOnly;
    private Cookie.SameSite sameSite;
    private Integer version;
    private LocalDateTime expires;

    CookieBuilder() {

    }

    CookieBuilder(String name) {
        this.name(name);
    }

    Cookie.Builder name(String name) {
        if (name == null) {
            throw new NullPointerException("name");
        }
        name = name.trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("name");
        }
        this.name = name;
        return this;
    }

    @Nonnull
    @Override
    public Cookie.Builder value(@Nonnull String value) {
        this.value = Objects.requireNonNull(value);
        return this;
    }

    @Nonnull
    @Override
    public Cookie.Builder domain(String domain) {
        this.domain = domain;
        return this;
    }

    @Nonnull
    @Override
    public Cookie.Builder path(String path) {
        this.path = path;
        return this;
    }

    @Nonnull
    @Override
    public Cookie.Builder maxAge(long maxAge) {
        this.maxAge = maxAge;
        return this;
    }

    @Nonnull
    @Override
    public Cookie.Builder isSecure(boolean isSecure) {
        this.isSecure = isSecure;
        return this;
    }

    @Nonnull
    @Override
    public Cookie.Builder isHttpOnly(boolean isHttpOnly) {
        this.isHttpOnly = isHttpOnly;
        return this;
    }

    @Nonnull
    @Override
    public Cookie.Builder sameSite(Cookie.SameSite sameSite) {
        this.sameSite = sameSite;
        return this;
    }

    @Nonnull
    @Override
    public Cookie.Builder version(Integer version) {
        this.version = version;
        return this;
    }

    @Nonnull
    @Override
    public Cookie.Builder expires(LocalDateTime expires) {
        this.expires = expires;
        return this;
    }

    @Nonnull
    @Override
    public Cookie build() {
        return new DefaultCookie(this.name, this.value, this.domain, this.path, this.maxAge, this.isSecure, this.isHttpOnly, this.sameSite, this.version, this.expires);
    }
}
