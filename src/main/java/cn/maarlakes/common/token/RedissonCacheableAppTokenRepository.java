package cn.maarlakes.common.token;

import jakarta.annotation.Nonnull;
import org.redisson.api.LocalCachedMapOptions;
import org.redisson.api.RMap;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.Codec;
import org.redisson.codec.Kryo5Codec;

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
    protected final Codec codec = new Kryo5Codec();
    protected final TokenFactory<T, A, V> tokenFactory;
    protected final RMap<A, T> mapCache;

    public RedissonCacheableAppTokenRepository(@Nonnull RedissonClient client, @Nonnull String namespace, @Nonnull TokenFactory<T, A, V> tokenFactory) {
        this.client = client;
        this.namespace = namespace;
        this.tokenFactory = tokenFactory;

        this.mapCache = client.getLocalCachedMap(this.namespace, this.codec, LocalCachedMapOptions.defaults());
    }

    @Nonnull
    @Override
    public CompletionStage<List<T>> getTokensAsync() {
        return this.mapCache.readAllValuesAsync().thenApply(ArrayList::new);
    }

    @Nonnull
    @Override
    public CompletionStage<Void> clearAsync() {
       this.mapCache.clear();
       return CompletableFuture.completedFuture(null);
    }

    @Nonnull
    @Override
    public CompletionStage<Void> removeAsync(@Nonnull A appId) {
        return this.mapCache.removeAsync(appId).thenRun(() -> {
        });
    }

    @Nonnull
    @Override
    public CompletionStage<Void> removeAsync(@Nonnull T token) {
        return this.mapCache.removeAsync(token.getAppId(), token).thenRun(() -> {
        });
    }

    @Nonnull
    @Override
    @SuppressWarnings("unchecked")
    public CompletionStage<T> getTokenAsync(@Nonnull A appId) {
        return this.mapCache.getAsync(appId)
                .thenCompose(token -> {
                    if (token == null) {
                        return (CompletionStage<T>) this.createToken(appId);
                    }
                    return CompletableFuture.completedFuture(token);
                });
    }

    protected CompletionStage<? extends T> createToken(@Nonnull A appId) {
        final RSemaphore semaphore = this.client.getSemaphore(this.namespace + ":lock:" + appId);
        return semaphore.trySetPermitsAsync(1)
                .thenCompose(v -> semaphore.acquireAsync())
                .thenCompose(v -> this.mapCache.getAsync(appId))
                .thenCompose(token -> {
                    if (token == null) {
                        return this.tokenFactory.createToken(appId)
                                .thenCompose(t -> this.putTokenAsync(t).thenApply(tmp -> t));
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

    protected CompletionStage<? extends T> putTokenAsync(@Nonnull T token) {
        return this.mapCache.putAsync(token.getAppId(), token);
    }
}
