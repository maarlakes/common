package cn.maarlakes.common.token;

import jakarta.annotation.Nonnull;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.Codec;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

/**
 * @author linjpxc
 */
public class RedissonExpirationTokenRepository<T extends ExpirationAppToken<A, V>, A, V> extends RedissonCacheableAppTokenRepository<T, A, V> implements ExpirationTokenRepository<T, A, V> {
    public RedissonExpirationTokenRepository(@Nonnull RedissonClient client, @Nonnull String namespace, Codec codec, @Nonnull TokenFactory<T, A, V> tokenFactory) {
        super(client, namespace, codec, tokenFactory);
    }

    @Nonnull
    @Override
    public CompletionStage<Void> removeExpiredTokenAsync() {
        return this.getMapCache().clearExpireAsync().thenRun(() -> {
        });
    }

    @Nonnull
    @Override
    public CompletionStage<T> refreshAsync(@Nonnull T token) {
        final RMapCache<A, T> map = this.getMapCache();
        return map.removeAsync(token.getAppId(), token)
                .toCompletableFuture()
                .thenCompose(r -> this.getTokenAsync(token.getAppId()));
    }

    @Override
    protected CompletionStage<? extends T> putTokenAsync(@Nonnull RMapCache<A, T> map, @Nonnull T token) {
        final Duration duration = Duration.between(LocalDateTime.now(), token.getExpirationTime()).abs();
        return map.putAsync(token.getAppId(), token, duration.toMillis(), TimeUnit.MILLISECONDS);
    }
}
