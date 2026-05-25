package cn.maarlakes.common.token;

import jakarta.annotation.Nonnull;

import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * 可缓存的 Token 仓库接口，扩展 {@link TokenRepository} 增加批量查询、清理和移除能力。
 *
 * <p>实现类负责在内存或外部存储（如 Redis）中缓存 Token，避免重复创建。
 * 缓存的粒度以应用标识（appId）为键，每个 appId 对应一个 Token。
 *
 * <p>提供的能力：
 * <ul>
 *   <li>{@link #getTokensAsync()} — 获取所有已缓存的 Token</li>
 *   <li>{@link #clearAsync()} — 清空所有缓存</li>
 *   <li>{@link #removeAsync(AppToken)} / {@link #removeAsync(Object)} — 移除指定 Token</li>
 * </ul>
 *
 * @param <T> Token 类型
 * @param <A> 应用标识类型
 * @param <V> Token 值类型
 * @author linjpxc
 */
public interface CacheableTokenRepository<T extends AppToken<A, V>, A, V> extends TokenRepository<T, A, V> {

    /**
     * 异步获取所有已缓存的 Token。
     *
     * @return 异步返回 Token 列表
     */
    @Nonnull
    CompletionStage<List<T>> getTokensAsync();

    /**
     * 同步获取所有已缓存的 Token，阻塞等待异步操作完成。
     *
     * @return Token 列表
     */
    @Nonnull
    default List<T> getTokens() {
        return Tokens.join(this.getTokensAsync());
    }

    /**
     * 异步清空所有已缓存的 Token。
     *
     * @return 异步操作完成信号
     */
    @Nonnull
    CompletionStage<Void> clearAsync();

    /**
     * 同步清空所有已缓存的 Token，阻塞等待异步操作完成。
     */
    default void clear() {
        Tokens.join(this.clearAsync());
    }

    /**
     * 异步移除指定的 Token。默认实现通过 {@link #removeAsync(Object)} 按 appId 移除。
     *
     * @param token 要移除的 Token
     * @return 异步操作完成信号
     */
    @Nonnull
    default CompletionStage<Void> removeAsync(@Nonnull T token) {
        return this.removeAsync(token.getAppId());
    }

    /**
     * 同步移除指定的 Token，阻塞等待异步操作完成。
     *
     * @param token 要移除的 Token
     */
    default void remove(@Nonnull T token) {
        Tokens.join(this.removeAsync(token));
    }

    /**
     * 异步移除指定应用标识对应的 Token。
     *
     * @param appId 应用标识
     * @return 异步操作完成信号
     */
    @Nonnull
    CompletionStage<Void> removeAsync(@Nonnull A appId);

    /**
     * 同步移除指定应用标识对应的 Token，阻塞等待异步操作完成。
     *
     * @param appId 应用标识
     */
    default void remove(@Nonnull A appId) {
        Tokens.join(this.removeAsync(appId));
    }
}
