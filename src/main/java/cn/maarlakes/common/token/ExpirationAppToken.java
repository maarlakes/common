package cn.maarlakes.common.token;

import jakarta.annotation.Nonnull;

import java.time.Instant;

/**
 * @author linjpxc
 */
public interface ExpirationAppToken<A, T> extends AppToken<A, T> {

    @Nonnull
    Instant getExpiresAt();

    default boolean isExpired() {
        return Tokens.isExpired(this);
    }
}
