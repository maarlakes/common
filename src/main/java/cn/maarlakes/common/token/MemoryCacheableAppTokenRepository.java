package cn.maarlakes.common.token;

import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 基于 {@link ConcurrentHashMap} 的内存缓存 Token 仓库实现。
 *
 * <p>使用 {@link ConcurrentHashMap#computeIfAbsent} 保证同一个 appId 不会并发创建多个 Token：
 * 当多个线程同时请求同一个 appId 的 Token 时，只有一个线程会实际执行创建逻辑，其余线程等待同一个 {@link CompletableFuture}。
 *
 * <p>创建失败的 Token 会被自动从缓存中移除，后续请求会重新尝试创建。
 *
 * <p>线程安全性：所有操作基于 {@link ConcurrentHashMap} 的原子方法，线程安全。
 *
 * @param <T> Token 类型
 * @param <A> 应用标识类型
 * @param <V> Token 值类型
 * @author linjpxc
 */
public class MemoryCacheableAppTokenRepository<T extends AppToken<A, V>, A, V> implements CacheableTokenRepository<T, A, V> {
    private static final Logger log = LoggerFactory.getLogger(MemoryCacheableAppTokenRepository.class);

    /** 缓存映射：appId → Token 的异步结果 */
    protected final ConcurrentMap<A, CompletableFuture<T>> cacheTokens = new ConcurrentHashMap<>();

    /** Token 工厂，用于在缓存未命中时创建新 Token */
    protected final TokenFactory<T, A, V> tokenFactory;

    public MemoryCacheableAppTokenRepository(@Nonnull TokenFactory<T, A, V> tokenFactory) {
        this.tokenFactory = tokenFactory;
    }

    /**
     * 异步获取所有已缓存且创建成功的 Token。
     *
     * <p>跳过尚未完成或异常完成的 Future，只返回成功创建的 Token。
     *
     * @return 异步返回 Token 列表
     */
    @Nonnull
    @Override
    public CompletionStage<List<T>> getTokensAsync() {
        final List<T> tokens = new ArrayList<>();
        for (CompletableFuture<T> future : this.cacheTokens.values()) {
            if (future.isDone() && !future.isCompletedExceptionally()) {
                final T token = future.getNow(null);
                if (token != null) {
                    tokens.add(token);
                }
            }
        }
        return CompletableFuture.completedFuture(tokens);
    }

    @Nonnull
    @Override
    public CompletionStage<Void> clearAsync() {
        final int size = this.cacheTokens.size();
        this.cacheTokens.clear();
        if (log.isDebugEnabled()) {
            log.debug("已清空内存 Token 缓存，共移除 {} 条记录", size);
        }
        return CompletableFuture.completedFuture(null);
    }

    @Nonnull
    @Override
    public CompletionStage<Void> removeAsync(@Nonnull T token) {
        final CompletableFuture<T> future = this.cacheTokens.get(token.getAppId());
        if (future != null && future.isDone() && !future.isCompletedExceptionally()
                && token.equals(future.getNow(null))) {
            this.cacheTokens.remove(token.getAppId(), future);
            if (log.isDebugEnabled()) {
                log.debug("从内存缓存移除 Token：appId={}", token.getAppId());
            }
        }
        return CompletableFuture.completedFuture(null);
    }

    @Nonnull
    @Override
    public CompletionStage<Void> removeAsync(@Nonnull A appId) {
        this.cacheTokens.remove(appId);
        if (log.isDebugEnabled()) {
            log.debug("从内存缓存移除 Token：appId={}", appId);
        }
        return CompletableFuture.completedFuture(null);
    }

    /**
     * 异步获取指定应用的 Token。
     *
     * <p>使用 {@code computeIfAbsent} 保证原子性：
     * <ol>
     *   <li>如果缓存中已有该 appId 对应的 Future，直接返回（缓存命中）</li>
     *   <li>如果缓存中没有，创建新的 Future，通过 {@link TokenFactory} 异步创建 Token（缓存未命中）</li>
     * </ol>
     *
     * <p>创建失败时会自动从缓存中移除对应的 Future，确保后续请求可以重试。
     *
     * @param appId 应用标识
     * @return 异步返回对应的 Token
     */
    @Nonnull
    @Override
    public CompletionStage<T> getTokenAsync(@Nonnull A appId) {
        return this.cacheTokens.computeIfAbsent(appId, key -> {
            if (log.isDebugEnabled()) {
                log.debug("内存缓存未命中，开始创建 Token：appId={}", key);
            }
            final CompletableFuture<T> future = new CompletableFuture<>();
            this.tokenFactory.createToken(key)
                    .thenAccept(t -> {
                        if (log.isInfoEnabled()) {
                            log.info("Token 创建成功：appId={}，过期时间={}", key, t instanceof ExpirationAppToken ? ((ExpirationAppToken<?, ?>) t).getExpiresAt() : "无");
                        }
                        future.complete(t);
                    })
                    .exceptionally(error -> {
                        this.cacheTokens.remove(key, future);
                        final TokenException ex = Tokens.newTokenException(error);
                        log.warn("Token 创建失败：appId={}，原因：{}", key, ex.getMessage());
                        future.completeExceptionally(ex);
                        return null;
                    });
            return future;
        });
    }
}
