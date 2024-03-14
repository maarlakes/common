package cn.maarlakes.common.token;

import jakarta.annotation.Nonnull;
import org.redisson.api.RMapCache;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.Codec;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * @author linjpxc
 */
public class RedissonCacheableAppTokenRepository<T extends AppToken<A, V>, A, V> implements CacheableTokenRepository<T, A, V> {

    protected final RedissonClient client;
    protected final String namespace;
    protected final Codec codec;
    protected final TokenFactory<T, A, V> tokenFactory;

    public RedissonCacheableAppTokenRepository(@Nonnull RedissonClient client, @Nonnull String namespace, Codec codec, @Nonnull TokenFactory<T, A, V> tokenFactory) {
        this.client = client;
        this.namespace = namespace;
        this.codec = codec;
        this.tokenFactory = tokenFactory;
    }

    @Nonnull
    @Override
    public CompletionStage<List<T>> getTokensAsync() {
        return this.getMapCache().readAllValuesAsync().thenApply(ArrayList::new);
    }

    @Nonnull
    @Override
    public CompletionStage<Void> clearAsync() {
        this.getMapCache().clear();
        return CompletableFuture.runAsync(() -> this.getMapCache().clear());
    }

    @Nonnull
    @Override
    public CompletionStage<Void> removeAsync(@Nonnull A appId) {
        return this.getMapCache().removeAsync(appId).thenRun(() -> {
        });
    }

    @Nonnull
    @Override
    public CompletionStage<Void> removeAsync(@Nonnull T token) {
        return this.getMapCache().removeAsync(token.getAppId(), token).thenRun(() -> {
        });
    }

    @Nonnull
    @Override
    @SuppressWarnings("unchecked")
    public CompletionStage<T> getTokenAsync(@Nonnull A appId) {
        final RMapCache<A, T> map = this.getMapCache();
        return map.getAsync(appId)
                .thenCompose(token -> {
                    if (token == null) {
                        return (CompletionStage<T>) this.createToken(appId);
                    }
                    return CompletableFuture.completedFuture(token);
                });
    }

    protected CompletionStage<? extends T> createToken(@Nonnull A appId) {
        final RSemaphore semaphore = this.client.getSemaphore(this.namespace + ":lock:" + appId);
        final RMapCache<A, T> map = this.getMapCache();
        return semaphore.trySetPermitsAsync(1)
                .thenCompose(v -> semaphore.acquireAsync())
                .thenCompose(v -> map.getAsync(appId))
                .thenCompose(token -> {
                    if (token == null) {
                        return this.tokenFactory.createToken(appId)
                                .thenCompose(t -> this.putTokenAsync(map, t).thenApply(tmp -> t));
                    } else {
                        return CompletableFuture.completedFuture(token);
                    }
                }).handle((r, e) -> {
                    semaphore.releaseAsync();
                    if (e != null) {
                        throw Tokens.newTokenException(e);
                    }
                    return r;
                });
    }

    protected CompletionStage<? extends T> putTokenAsync(@Nonnull RMapCache<A, T> map, @Nonnull T token) {
        return map.putAsync(token.getAppId(), token);
    }

    protected RMapCache<A, T> getMapCache() {
        if (this.codec == null) {
            return this.client.getMapCache(this.namespace);
        }
        return this.client.getMapCache(this.namespace, this.codec);
    }
}
