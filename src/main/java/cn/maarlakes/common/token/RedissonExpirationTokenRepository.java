package cn.maarlakes.common.token;

import jakarta.annotation.Nonnull;
import org.redisson.api.RedissonClient;

import java.util.concurrent.CompletionStage;

/**
 * @author linjpxc
 */
public class RedissonExpirationTokenRepository<T extends ExpirationAppToken<A, V>, A, V> extends RedissonCacheableAppTokenRepository<T, A, V> implements ExpirationTokenRepository<T, A, V> {

    public RedissonExpirationTokenRepository(@Nonnull RedissonClient client, @Nonnull String namespace, @Nonnull TokenFactory<T, A, V> tokenFactory) {
        super(client, namespace, tokenFactory);
    }

    @Nonnull
    @Override
    public CompletionStage<Void> removeExpiredTokenAsync() {
        return this.mapCache.clearExpireAsync().thenRun(() -> {
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
