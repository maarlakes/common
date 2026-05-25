package cn.maarlakes.common.token;

import jakarta.annotation.Nonnull;

import java.util.concurrent.CompletionStage;

/**
 * 可刷新的 Token 仓库接口，扩展 {@link CacheableTokenRepository} 增加主动刷新能力。
 *
 * <p>刷新（refresh）与获取（getToken）的区别：
 * <ul>
 *   <li>{@code getToken} — 优先使用缓存，缓存不存在时才创建</li>
 *   <li>{@code refresh} — 强制重新创建 Token，无论缓存中是否存在旧 Token</li>
 * </ul>
 *
 * <p>典型使用场景：Token 即将过期或已过期时，调用方主动触发刷新以获取新的有效 Token。
 *
 * @param <T> Token 类型（必须带过期时间）
 * @param <A> 应用标识类型
 * @param <V> Token 值类型
 * @author linjpxc
 */
public interface RefreshableTokenRepository<T extends ExpirationAppToken<A, V>, A, V> extends CacheableTokenRepository<T, A, V> {

    /**
     * 异步刷新指定的 Token，强制重新创建。
     *
     * @param token 需要刷新的 Token
     * @return 异步返回刷新后的新 Token
     */
    @Nonnull
    CompletionStage<T> refreshAsync(@Nonnull T token);

    /**
     * 同步刷新指定的 Token，阻塞等待异步操作完成。
     *
     * @param token 需要刷新的 Token
     * @return 刷新后的新 Token
     */
    @Nonnull
    default T refresh(@Nonnull T token) {
        return Tokens.join(this.refreshAsync(token));
    }
}
