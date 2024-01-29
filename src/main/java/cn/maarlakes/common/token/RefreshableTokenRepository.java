package cn.maarlakes.common.token;

import jakarta.annotation.Nonnull;

import java.util.concurrent.CompletionStage;

/**
 * @author linjpxc
 */
public interface RefreshableTokenRepository<T extends ExpirationAppToken<A, V>, A, V> extends CacheableTokenRepository<T, A, V> {

    @Nonnull
    @Override
    default CompletionStage<T> getTokenAsync(@Nonnull A appId) {
        return this.getTokenAsync(appId, true);
    }

    @SuppressWarnings("unchecked")
    default CompletionStage<T> getTokenAsync(@Nonnull A appId, boolean autoRefresh) {
        if (autoRefresh) {
            return (CompletionStage<T>) Tokens.autoRefreshAsync(this, this.getTokenAsync(appId));
        }
        return this.getTokenAsync(appId);
    }

    @Nonnull
    CompletionStage<T> refreshAsync(@Nonnull T token);

    @Nonnull
    default T refresh(@Nonnull T token) {
        try {
            return this.refreshAsync(token).toCompletableFuture().get();
        } catch (Exception e) {
            throw Tokens.newTokenException(e);
        }
    }
}
