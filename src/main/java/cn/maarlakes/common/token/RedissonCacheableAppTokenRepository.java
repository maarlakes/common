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
 * 基于 Redisson {@link RMap} 的分布式缓存 Token 仓库实现。
 *
 * <p>使用 Redis 作为存储后端，通过 Redisson 的 {@link RMap} 实现 Token 的持久化缓存，
 * 支持跨 JVM 实例共享 Token。
 *
 * <h3>分布式锁机制</h3>
 * <p>使用 Redisson 信号量（{@link RSemaphore}）实现分布式互斥，防止多个实例并发创建同一个 appId 的 Token：
 * <ol>
 *   <li>尝试获取信号量（{@code tryAcquireAsync}），超时时间由 {@link #lockAcquireTimeout} 控制</li>
 *   <li>获取成功后执行双重检查：再次从 Redis 读取，确认仍无缓存（防止其他实例已创建）</li>
 *   <li>创建 Token 后写入 Redis，最后释放信号量</li>
 *   <li>信号量释放后设置 30 分钟 TTL 自动清理</li>
 * </ol>
 *
 * <p>序列化使用 {@link Kryo5Codec}，适合跨实例共享场景。
 *
 * @param <T> Token 类型
 * @param <A> 应用标识类型
 * @param <V> Token 值类型
 * @author linjpxc
 */
public class RedissonCacheableAppTokenRepository<T extends AppToken<A, V>, A, V> implements CacheableTokenRepository<T, A, V> {
    private static final Logger log = LoggerFactory.getLogger(RedissonCacheableAppTokenRepository.class);
    private static final Codec CODEC = new Kryo5Codec();

    /** Redisson 客户端 */
    protected final RedissonClient client;

    /** Redis Map 的命名空间（键名） */
    protected final String namespace;

    /** Token 工厂，用于在缓存未命中时创建新 Token */
    protected final TokenFactory<T, A, V> tokenFactory;

    /** Redis Map 缓存实例 */
    protected final RMap<A, T> mapCache;

    /** 获取分布式锁的超时时间，默认 5 分钟 */
    protected final Duration lockAcquireTimeout;

    /**
     * 使用默认锁超时时间（5 分钟）构造。
     *
     * @param client       Redisson 客户端
     * @param namespace    Redis Map 的命名空间
     * @param tokenFactory Token 工厂
     */
    public RedissonCacheableAppTokenRepository(@Nonnull RedissonClient client, @Nonnull String namespace, @Nonnull TokenFactory<T, A, V> tokenFactory) {
        this(client, namespace, tokenFactory, Duration.ofMinutes(5));
    }

    /**
     * 使用自定义锁超时时间构造。
     *
     * @param client            Redisson 客户端
     * @param namespace         Redis Map 的命名空间
     * @param tokenFactory      Token 工厂
     * @param lockAcquireTimeout 获取分布式锁的超时时间
     */
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
        return this.mapCache.deleteAsync().thenApply(v -> {
            log.info("已清空 Redis Token 缓存：namespace={}", this.namespace);
            return null;
        });
    }

    @Nonnull
    @Override
    public CompletionStage<Void> removeAsync(@Nonnull A appId) {
        return this.mapCache.removeAsync(appId).thenRun(() -> {
            if (log.isDebugEnabled()) {
                log.debug("从 Redis 移除 Token：namespace={}，appId={}", this.namespace, appId);
            }
        });
    }

    @Nonnull
    @Override
    public CompletionStage<Void> removeAsync(@Nonnull T token) {
        return this.mapCache.removeAsync(token.getAppId(), token).thenRun(() -> {
            if (log.isDebugEnabled()) {
                log.debug("从 Redis 移除 Token（精确匹配）：namespace={}，appId={}", this.namespace, token.getAppId());
            }
        });
    }

    /**
     * 异步获取指定应用的 Token。
     *
     * <p>先从 Redis 缓存查询，如果存在则直接返回；如果不存在则通过 {@link #createToken} 创建。
     *
     * @param appId 应用标识
     * @return 异步返回对应的 Token
     */
    @Nonnull
    @Override
    public CompletionStage<T> getTokenAsync(@Nonnull A appId) {
        return this.mapCache.getAsync(appId)
                .thenCompose(token -> {
                    if (token == null) {
                        if (log.isDebugEnabled()) {
                            log.debug("Redis 缓存未命中，进入创建流程：namespace={}，appId={}", this.namespace, appId);
                        }
                        return this.createToken(appId);
                    }
                    if (log.isTraceEnabled()) {
                        log.trace("Redis 缓存命中：namespace={}，appId={}", this.namespace, appId);
                    }
                    return CompletableFuture.completedFuture(token);
                });
    }

    /**
     * 在分布式锁保护下创建 Token 并写入 Redis。
     *
     * <p>流程：
     * <ol>
     *   <li>初始化并获取信号量（分布式锁）</li>
     *   <li>获取锁后执行双重检查，再次从 Redis 读取确认无缓存</li>
     *   <li>通过 {@link TokenFactory} 创建 Token</li>
     *   <li>将 Token 写入 Redis（{@code putIfAbsent} 保证幂等）</li>
     *   <li>释放信号量</li>
     * </ol>
     *
     * @param appId 应用标识
     * @return 异步返回创建好的 Token
     */
    protected CompletionStage<T> createToken(@Nonnull A appId) {
        final RSemaphore semaphore = this.client.getSemaphore(this.namespace + ":lock:" + appId);
        return semaphore.trySetPermitsAsync(1)
                .thenCompose(v -> semaphore.tryAcquireAsync(this.lockAcquireTimeout.toMillis(), TimeUnit.MILLISECONDS))
                .thenCompose(acquired -> {
                    if (log.isDebugEnabled()) {
                        log.debug("分布式锁获取结果：appId={}，获取到锁={}", appId, acquired);
                    }
                    return this.mapCache.getAsync(appId)
                            .thenCompose(token -> {
                                if (token == null) {
                                    if (acquired) {
                                        if (log.isDebugEnabled()) {
                                            log.debug("双重检查确认缓存为空，开始创建 Token：appId={}", appId);
                                        }
                                        return this.tokenFactory.createToken(appId)
                                                .thenCompose(this::putTokenAsync);
                                    }
                                    log.warn("获取分布式锁超时，无法创建 Token：appId={}，超时时间={}ms", appId, this.lockAcquireTimeout.toMillis());
                                    final CompletableFuture<T> future = new CompletableFuture<>();
                                    future.completeExceptionally(new TokenException("获取分锁超时"));
                                    return future;
                                }
                                if (log.isDebugEnabled()) {
                                    log.debug("双重检查发现已有缓存，跳过创建：appId={}", appId);
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
                            });
                });
    }

    /**
     * 安全释放信号量，释放后设置 30 分钟 TTL 自动清理。
     *
     * <p>释放失败时会尝试删除信号量作为兜底恢复手段。
     *
     * @param semaphore 待释放的信号量
     */
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

    /**
     * 将 Token 写入 Redis，使用 {@code putIfAbsent} 保证幂等（如果其他实例已写入则返回已存在的值）。
     *
     * @param token 待写入的 Token
     * @return 异步返回最终的 Token（可能是刚写入的，也可能是其他实例已写入的）
     */
    protected CompletionStage<T> putTokenAsync(@Nonnull T token) {
        return this.mapCache.putIfAbsentAsync(token.getAppId(), token)
                .thenApply(existing -> {
                    if (existing != null) {
                        if (log.isDebugEnabled()) {
                            log.debug("Token 写入时发现已存在，使用已有值：appId={}", token.getAppId());
                        }
                        return existing;
                    }
                    log.info("Token 创建并写入 Redis 成功：namespace={}，appId={}", this.namespace, token.getAppId());
                    return token;
                });
    }
}
