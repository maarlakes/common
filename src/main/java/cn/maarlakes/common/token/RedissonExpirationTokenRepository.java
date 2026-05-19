package cn.maarlakes.common.token;

import jakarta.annotation.Nonnull;
import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * @author linjpxc
 */
public class RedissonExpirationTokenRepository<T extends ExpirationAppToken<A, V>, A, V> extends RedissonCacheableAppTokenRepository<T, A, V> implements ExpirationTokenRepository<T, A, V> {

    public RedissonExpirationTokenRepository(@Nonnull RedissonClient client, @Nonnull String namespace, @Nonnull TokenFactory<T, A, V> tokenFactory) {
        super(client, namespace, tokenFactory);
    }

    public RedissonExpirationTokenRepository(@Nonnull RedissonClient client, @Nonnull String namespace, @Nonnull TokenFactory<T, A, V> tokenFactory, @Nonnull Duration lockAcquireTimeout) {
        super(client, namespace, tokenFactory, lockAcquireTimeout);
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
        return this.getExpiredTokensAsync().thenCompose(tokens -> {
            if (tokens.isEmpty()) {
                return CompletableFuture.completedFuture(null);
            }
            @SuppressWarnings("unchecked")
            final A[] keys = tokens.stream().map(AppToken::getAppId).toArray(size -> (A[]) new Object[size]);
            return this.mapCache.fastRemoveAsync(keys).thenApply(v -> null);
        });
    }

    @Nonnull
    @Override
    public CompletionStage<T> refreshAsync(@Nonnull T token) {
        return this.mapCache.removeAsync(token.getAppId(), token)
                .toCompletableFuture()
                .thenCompose(r -> this.getTokenAsync(token.getAppId()));
    }
}
