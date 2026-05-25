package cn.maarlakes.common.token;

import jakarta.annotation.Nonnull;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * 基于 Redis 的带过期管理的 Token 仓库实现。
 *
 * <p>在 {@link RedissonCacheableAppTokenRepository} 的基础上增加过期自动刷新逻辑：
 * 每次通过 {@link #getTokenAsync} 获取 Token 时，会自动检查是否已过期，
 * 如果已过期则触发 {@link #refreshAsync} 强制刷新。
 *
 * <p>过期清理（{@link #removeExpiredTokenAsync}）使用 Redisson 的 {@code fastRemoveAsync}
 * 批量删除已过期的 Token，效率高于逐个删除。
 *
 * @param <T> Token 类型（必须带过期时间）
 * @param <A> 应用标识类型
 * @param <V> Token 值类型
 * @author linjpxc
 */
public class RedissonExpirationTokenRepository<T extends ExpirationAppToken<A, V>, A, V> extends RedissonCacheableAppTokenRepository<T, A, V> implements ExpirationTokenRepository<T, A, V> {
    private static final Logger log = LoggerFactory.getLogger(RedissonExpirationTokenRepository.class);

    public RedissonExpirationTokenRepository(@Nonnull RedissonClient client, @Nonnull String namespace, @Nonnull TokenFactory<T, A, V> tokenFactory) {
        super(client, namespace, tokenFactory);
    }

    public RedissonExpirationTokenRepository(@Nonnull RedissonClient client, @Nonnull String namespace, @Nonnull TokenFactory<T, A, V> tokenFactory, @Nonnull Duration lockAcquireTimeout) {
        super(client, namespace, tokenFactory, lockAcquireTimeout);
    }

    /**
     * 异步获取指定应用的 Token，自动处理过期逻辑。
     *
     * <p>流程：先从 Redis 缓存获取 Token → 检查是否过期 → 如果已过期则刷新，否则直接返回。
     *
     * @param appId 应用标识
     * @return 异步返回有效的（未过期的）Token
     */
    @Nonnull
    @Override
    public CompletionStage<T> getTokenAsync(@Nonnull A appId) {
        return super.getTokenAsync(appId)
                .thenCompose(token -> {
                    if (Tokens.isExpired(token)) {
                        if (log.isDebugEnabled()) {
                            log.debug("Token 已过期，触发刷新：namespace={}，appId={}，过期时间={}", this.namespace, appId, token.getExpiresAt());
                        }
                        return this.refreshAsync(token);
                    }
                    return CompletableFuture.completedFuture(token);
                });
    }

    /**
     * 异步移除所有已过期的 Token。
     *
     * <p>通过 {@link #getExpiredTokensAsync()} 获取过期 Token 列表，然后使用
     * {@code fastRemoveAsync} 批量从 Redis 中删除。
     */
    @Nonnull
    @Override
    public CompletionStage<Void> removeExpiredTokenAsync() {
        return this.getExpiredTokensAsync().thenCompose(tokens -> {
            if (tokens.isEmpty()) {
                return CompletableFuture.completedFuture(null);
            }
            log.info("开始清理 Redis 中的过期 Token：namespace={}，共 {} 条", this.namespace, tokens.size());
            @SuppressWarnings("unchecked")
            final A[] keys = tokens.stream().map(AppToken::getAppId).toArray(size -> (A[]) new Object[size]);
            return this.mapCache.fastRemoveAsync(keys).thenApply(v -> {
                log.info("过期 Token 清理完成：namespace={}，共移除 {} 条", this.namespace, tokens.size());
                return null;
            });
        });
    }

    /**
     * 异步刷新指定的 Token。
     *
     * <p>先从 Redis 中精确移除旧 Token（CAS 语义），然后重新获取。
     *
     * @param token 需要刷新的 Token
     * @return 异步返回刷新后的新 Token
     */
    @Nonnull
    @Override
    public CompletionStage<T> refreshAsync(@Nonnull T token) {
        return this.mapCache.removeAsync(token.getAppId(), token)
                .toCompletableFuture()
                .thenCompose(r -> {
                    if (log.isDebugEnabled()) {
                        log.debug("已从 Redis 移除旧 Token，准备重新创建：namespace={}，appId={}", this.namespace, token.getAppId());
                    }
                    return this.getTokenAsync(token.getAppId());
                });
    }
}
