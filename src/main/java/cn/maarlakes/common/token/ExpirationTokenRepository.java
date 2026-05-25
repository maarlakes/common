package cn.maarlakes.common.token;

import jakarta.annotation.Nonnull;

import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

/**
 * 带过期管理的 Token 仓库接口，扩展 {@link RefreshableTokenRepository} 增加过期清理能力。
 *
 * <p>这是 Token 仓库接口继承链中最完整的层，具备以下全部能力：
 * <ul>
 *   <li>基础获取（继承自 {@link TokenRepository}）</li>
 *   <li>缓存管理（继承自 {@link CacheableTokenRepository}）</li>
 *   <li>主动刷新（继承自 {@link RefreshableTokenRepository}）</li>
 *   <li>过期清理（本接口定义）</li>
 * </ul>
 *
 * @param <T> Token 类型（必须带过期时间）
 * @param <A> 应用标识类型
 * @param <V> Token 值类型
 * @author linjpxc
 */
public interface ExpirationTokenRepository<T extends ExpirationAppToken<A, V>, A, V> extends RefreshableTokenRepository<T, A, V> {

    /**
     * 异步移除所有已过期的 Token。
     *
     * @return 异步操作完成信号
     */
    @Nonnull
    CompletionStage<Void> removeExpiredTokenAsync();

    /**
     * 同步移除所有已过期的 Token，阻塞等待异步操作完成。
     */
    default void removeExpiredToken() {
        Tokens.join(this.removeExpiredTokenAsync());
    }

    /**
     * 异步获取所有已过期的 Token。
     *
     * <p>默认实现从所有已缓存的 Token 中过滤出已过期的（通过 {@link Tokens#isExpired(ExpirationAppToken)} 判断）。
     *
     * @return 异步返回已过期的 Token 列表
     */
    @Nonnull
    default CompletionStage<List<T>> getExpiredTokensAsync() {
        return this.getTokensAsync().thenApply(tokens -> tokens.stream().filter(Tokens::isExpired).collect(Collectors.toList()));
    }

    /**
     * 同步获取所有已过期的 Token，阻塞等待异步操作完成。
     *
     * @return 已过期的 Token 列表
     */
    @Nonnull
    default List<T> getExpiredTokens() {
        return Tokens.join(this.getExpiredTokensAsync());
    }
}
