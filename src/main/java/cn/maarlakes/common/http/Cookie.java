package cn.maarlakes.common.http;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * HTTP Cookie 模型，遵循 RFC 6265 规范。
 *
 * <p>包含 Cookie 的所有属性：名称、值、域、路径、过期时间、Secure、HttpOnly、SameSite。
 * 通过 {@link #builder(String)} 创建构建器，支持链式设置各属性。
 *
 * <p>实现 {@link Comparable}，按名称排序。实现 {@link java.io.Serializable} 以支持序列化。
 *
 * @author linjpxc
 */
public interface Cookie extends Comparable<Cookie>, Serializable {

    /** Cookie 名称。 */
    @Nonnull
    String name();

    /** Cookie 值。 */
    String value();

    /** Cookie 的域属性。 */
    String domain();

    /** Cookie 的路径属性。 */
    String path();

    /** Cookie 的最大存活时间（秒），0 表示会话 Cookie。 */
    long maxAge();

    /** 是否仅通过 HTTPS 传输。 */
    boolean isSecure();

    /** 是否限制 JavaScript 访问。 */
    boolean isHttpOnly();

    /** SameSite 属性（Strict、Lax、None）。 */
    SameSite sameSite();

    Integer version();

    LocalDateTime expires();

    @Nonnull
    static Builder builder(@Nonnull String name) {
        return new CookieBuilder(name);
    }

    static Cookie parse(@Nonnull String value) {
        return Cookies.parse(value);
    }

    interface Builder {

        @Nonnull
        Builder value(@Nonnull String value);

        @Nonnull
        Builder domain(String domain);

        @Nonnull
        Builder path(String path);

        @Nonnull
        Builder maxAge(long maxAge);

        @Nonnull
        Builder isSecure(boolean isSecure);

        @Nonnull
        Builder isHttpOnly(boolean isHttpOnly);

        @Nonnull
        Builder sameSite(SameSite sameSite);

        @Nonnull
        default Builder sameSite(String sameSite) {
            if (sameSite == null) {
                return this.sameSite((SameSite) null);
            }
            return this.sameSite(SameSite.of(sameSite));
        }

        @Nonnull
        Builder version(Integer version);

        @Nonnull
        Builder expires(LocalDateTime expires);

        @Nonnull
        Cookie build();
    }

    enum SameSite {
        Lax,
        Strict,
        None;

        @Nullable
        public static SameSite of(@Nonnull String value) {
            for (SameSite sameSite : SameSite.values()) {
                if (sameSite.name().equalsIgnoreCase(value)) {
                    return sameSite;
                }
            }
            return null;
        }
    }
}
