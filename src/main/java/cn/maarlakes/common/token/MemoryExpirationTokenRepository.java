package cn.maarlakes.common.token;

import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * 基于内存的带过期管理的 Token 仓库实现。
 *
 * <p>在 {@link MemoryCacheableAppTokenRepository} 的基础上增加过期自动刷新逻辑：
 * 每次通过 {@link #getTokenAsync} 获取 Token 时，会自动检查是否已过期，
 * 如果已过期则触发 {@link #refreshAsync} 强制刷新。
 *
 * <p>过期清理（{@link #removeExpiredTokenAsync}）会扫描所有已缓存的 Token，
 * 移除已过期的条目，释放内存空间。
 *
 * @param <T> Token 类型（必须带过期时间）
 * @param <A> 应用标识类型
 * @param <V> Token 值类型
 * @author linjpxc
 */
public class MemoryExpirationTokenRepository<T extends ExpirationAppToken<A, V>, A, V> extends MemoryCacheableAppTokenRepository<T, A, V> implements ExpirationTokenRepository<T, A, V> {
    private static final Logger log = LoggerFactory.getLogger(MemoryExpirationTokenRepository.class);
    private static final int MAX_REFRESH_DEPTH = 3;

    public MemoryExpirationTokenRepository(@Nonnull TokenFactory<T, A, V> tokenFactory) {
        super(tokenFactory);
    }

    /**
     * 异步获取指定应用的 Token，自动处理过期逻辑。
     *
     * <p>流程：先从缓存获取 Token → 检查是否过期 → 如果已过期则刷新，否则直接返回。
     *
     * @param appId 应用标识
     * @return 异步返回有效的（未过期的）Token
     */
    @Nonnull
    @Override
    public CompletionStage<T> getTokenAsync(@Nonnull A appId) {
        return this.getTokenAsync(appId, 0);
    }

    private CompletionStage<T> getTokenAsync(@Nonnull A appId, int depth) {
        return super.getTokenAsync(appId)
                .thenCompose(token -> {
                    if (Tokens.isExpired(token)) {
                        if (depth >= MAX_REFRESH_DEPTH) {
                            log.warn("Token 刷新次数超过限制：appId={}，最大深度={}", appId, MAX_REFRESH_DEPTH);
                            final CompletableFuture<T> fail = new CompletableFuture<>();
                            fail.completeExceptionally(new TokenException("Token 刷新次数超过限制"));
                            return fail;
                        }
                        if (log.isDebugEnabled()) {
                            log.debug("Token 已过期，触发刷新：appId={}，深度={}，过期时间={}", appId, depth + 1, token.getExpiresAt());
                        }
                        return this.refreshAsync(token, depth + 1);
                    }
                    return CompletableFuture.completedFuture(token);
                });
    }

    /**
     * 异步移除所有已过期的 Token。
     *
     * <p>扫描缓存中的所有 Token，对每个已过期的 Token 执行 CAS 移除（确保只移除与检查时相同的实例）。
     */
    @Nonnull
    @Override
    public CompletionStage<Void> removeExpiredTokenAsync() {
        return this.getExpiredTokensAsync().thenAccept(tokens -> {
            if (!tokens.isEmpty()) {
                if (log.isInfoEnabled()) {
                    log.info("开始清理内存中的过期 Token，共 {} 条", tokens.size());
                }
            }
            tokens.forEach(token -> {
                final CompletableFuture<T> future = this.cacheTokens.get(token.getAppId());
                if (future != null && future.isDone() && !future.isCompletedExceptionally() && token.equals(future.getNow(null))) {
                    this.cacheTokens.remove(token.getAppId(), future);
                }
            });
            if (!tokens.isEmpty()) {
                if (log.isInfoEnabled()) {
                    log.info("过期 Token 清理完成，共移除 {} 条", tokens.size());
                }
            }
        });
    }

    /**
     * 异步刷新指定的 Token。
     *
     * <p>先从缓存中移除旧 Token（CAS 保证安全性），然后重新获取。
     *
     * @param token 需要刷新的 Token
     * @return 异步返回刷新后的新 Token
     */
    @Nonnull
    @Override
    public CompletionStage<T> refreshAsync(@Nonnull T token) {
        return this.refreshAsync(token, 0);
    }

    private CompletionStage<T> refreshAsync(@Nonnull T token, int depth) {
        final CompletableFuture<T> future = this.cacheTokens.get(token.getAppId());
        if (future != null && future.isDone() && !future.isCompletedExceptionally() && token.equals(future.getNow(null))) {
            this.cacheTokens.remove(token.getAppId(), future);
            if (log.isDebugEnabled()) {
                log.debug("已从缓存移除旧 Token，准备重新创建：appId={}", token.getAppId());
            }
        }
        return this.getTokenAsync(token.getAppId(), depth);
    }
}
