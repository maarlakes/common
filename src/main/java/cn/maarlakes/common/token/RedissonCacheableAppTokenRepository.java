package cn.maarlakes.common.token;

import jakarta.annotation.Nonnull;
import org.redisson.api.RMap;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.Codec;
import org.redisson.codec.Kryo5Codec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

/**
 * @author linjpxc
 */
public class RedissonCacheableAppTokenRepository<T extends AppToken<A, V>, A, V> implements CacheableTokenRepository<T, A, V> {
    private static final Logger log = LoggerFactory.getLogger(RedissonCacheableAppTokenRepository.class);
    private static final Codec CODEC = new Kryo5Codec();

    protected final RedissonClient client;
    protected final String namespace;
    protected final TokenFactory<T, A, V> tokenFactory;
    protected final RMap<A, T> mapCache;
    protected final Duration lockAcquireTimeout;

    public RedissonCacheableAppTokenRepository(@Nonnull RedissonClient client, @Nonnull String namespace, @Nonnull TokenFactory<T, A, V> tokenFactory) {
        this(client, namespace, tokenFactory, Duration.ofMinutes(5));
    }

    public RedissonCacheableAppTokenRepository(@Nonnull RedissonClient client, @Nonnull String namespace, @Nonnull TokenFactory<T, A, V> tokenFactory, @Nonnull Duration lockAcquireTimeout) {
        this.client = client;
        this.namespace = namespace;
        this.tokenFactory = tokenFactory;
        this.lockAcquireTimeout = lockAcquireTimeout;

        this.mapCache = client.getMap(this.namespace, CODEC);
    }

    @Nonnull
    @Override
    public CompletionStage<List<T>> getTokensAsync() {
        return this.mapCache.readAllValuesAsync().thenApply(ArrayList::new);
    }

    @Nonnull
    @Override
    public CompletionStage<Void> clearAsync() {
        return this.mapCache.deleteAsync().thenApply(v -> null);
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
    public CompletionStage<T> getTokenAsync(@Nonnull A appId) {
        return this.mapCache.getAsync(appId)
                .thenCompose(token -> {
                    if (token == null) {
                        return this.createToken(appId);
                    }
                    return CompletableFuture.completedFuture(token);
                });
    }

    protected CompletionStage<T> createToken(@Nonnull A appId) {
        final RSemaphore semaphore = this.client.getSemaphore(this.namespace + ":lock:" + appId);
        return semaphore.trySetPermitsAsync(1)
                .thenCompose(v -> semaphore.tryAcquireAsync(this.lockAcquireTimeout.toMillis(), TimeUnit.MILLISECONDS))
                .thenCompose(acquired -> this.mapCache.getAsync(appId)
                        .thenCompose(token -> {
                            if (token == null) {
                                if (acquired) {
                                    return this.tokenFactory.createToken(appId)
                                            .thenCompose(this::putTokenAsync);
                                }
                                final CompletableFuture<T> future = new CompletableFuture<>();
                                future.completeExceptionally(new TokenException("获取分锁超时"));
                                return future;
                            }
                            return CompletableFuture.completedFuture(token);
                        })
                        .whenComplete((result, error) -> {
                            if (acquired) {
                                this.releaseSafely(semaphore);
                            }
                        })
                        .exceptionally(error -> {
                            throw Tokens.newTokenException(error);
                        }));
    }

    private void releaseSafely(@Nonnull RSemaphore semaphore) {
        semaphore.releaseAsync()
                .thenAccept(v -> this.client.getKeys().expireAsync(
                        semaphore.getName(), 30, TimeUnit.MINUTES
                ).exceptionally(e -> {
                    log.debug("设置信号量TTL失败", e);
                    return null;
                }))
                .exceptionally(e -> {
                    log.error("释放锁失败，尝试删除信号量恢复", e);
                    semaphore.deleteAsync()
                            .exceptionally(ex -> {
                                log.error("删除信号量也失败", ex);
                                return null;
                            });
                    return null;
                });
    }

    protected CompletionStage<T> putTokenAsync(@Nonnull T token) {
        return this.mapCache.putIfAbsentAsync(token.getAppId(), token)
                .thenApply(existing -> existing != null ? existing : token);
    }
}
