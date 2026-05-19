package cn.maarlakes.common.token;

import jakarta.annotation.Nonnull;

import java.util.concurrent.CompletionStage;

/**
 * @author linjpxc
 */
public interface RefreshableTokenRepository<T extends ExpirationAppToken<A, V>, A, V> extends CacheableTokenRepository<T, A, V> {

    @Nonnull
    CompletionStage<T> refreshAsync(@Nonnull T token);

    @Nonnull
    default T refresh(@Nonnull T token) {
        return Tokens.join(this.refreshAsync(token));
    }
}
