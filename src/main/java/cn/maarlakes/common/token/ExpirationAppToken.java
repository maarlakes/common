package cn.maarlakes.common.token;

import jakarta.annotation.Nonnull;

import java.time.LocalDateTime;

/**
 * @author linjpxc
 */
public interface ExpirationAppToken<A, T> extends AppToken<A, T> {

    @Nonnull
    LocalDateTime getExpirationTime();

    default boolean isExpired() {
        return Tokens.isExpired(this);
    }
}
