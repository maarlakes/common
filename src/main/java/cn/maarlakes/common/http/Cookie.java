package cn.maarlakes.common.http;

import jakarta.annotation.Nonnull;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author linjpxc
 */
public interface Cookie extends Comparable<Cookie>, Serializable {

    @Nonnull
    String name();

    String value();

    String domain();

    String path();

    long maxAge();

    boolean isSecure();

    boolean isHttpOnly();

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
