package cn.maarlakes.common.token;

import jakarta.annotation.Nonnull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * @author linjpxc
 */
public class MemoryExpirationTokenRepository<T extends ExpirationAppToken<A, V>, A, V> extends MemoryCacheableAppTokenRepository<T, A, V> implements ExpirationTokenRepository<T, A, V> {

    public MemoryExpirationTokenRepository(@Nonnull TokenFactory<T, A, V> tokenFactory) {
        super(tokenFactory);
    }

    @Nonnull
    @Override
    public CompletionStage<T> getTokenAsync(@Nonnull A appId) {
        return super.getTokenAsync(appId)
                .thenCompose(token -> {
                    if (Tokens.isExpired(token)) {
                        return this.refreshAsync(token);
                    }
                    return CompletableFuture.completedFuture(token);
                });
    }

    @Nonnull
    @Override
    public CompletionStage<Void> removeExpiredTokenAsync() {
        return this.getExpiredTokensAsync().thenAccept(tokens -> tokens.forEach(token -> {
            final CompletableFuture<T> future = this.cacheTokens.get(token.getAppId());
            if (future != null && future.isDone() && !future.isCompletedExceptionally() && token.equals(future.getNow(null))) {
                this.cacheTokens.remove(token.getAppId(), future);
            }
        }));
    }

    @Nonnull
    @Override
    public CompletionStage<T> refreshAsync(@Nonnull T token) {
        final CompletableFuture<T> future = this.cacheTokens.get(token.getAppId());
        if (future != null && future.isDone() && !future.isCompletedExceptionally() && token.equals(future.getNow(null))) {
            this.cacheTokens.remove(token.getAppId(), future);
        }
        return this.getTokenAsync(token.getAppId());
    }
}
