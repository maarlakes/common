package cn.maarlakes.common.token;

import jakarta.annotation.Nonnull;

import java.util.concurrent.CompletionStage;

/**
 * Token 仓库的基础接口，提供根据应用标识获取 Token 的能力。
 *
 * <p>这是整个 Token 存储体系的最底层契约，上层接口（{@link CacheableTokenRepository}、
 * {@link RefreshableTokenRepository}、{@link ExpirationTokenRepository}）在此基础上逐步扩展缓存、刷新、过期管理等能力。
 *
 * <p>所有方法均提供异步（{@code *Async}）和同步两种形式，同步方法内部调用异步方法后阻塞等待结果。
 *
 * @param <T> Token 类型
 * @param <A> 应用标识类型
 * @param <V> Token 值类型
 * @author linjpxc
 */
public interface TokenRepository<T extends AppToken<A, V>, A, V> {

    /**
     * 异步获取指定应用的 Token。如果仓库中不存在，则通过 {@link TokenFactory} 创建。
     *
     * @param appId 应用标识
     * @return 异步返回对应的 Token
     */
    @Nonnull
    CompletionStage<T> getTokenAsync(@Nonnull A appId);

    /**
     * 同步获取指定应用的 Token，阻塞等待异步操作完成。
     *
     * @param appId 应用标识
     * @return 对应的 Token
     * @throws TokenException 如果获取过程中发生异常
     */
    @Nonnull
    default T getToken(@Nonnull A appId) {
       return Tokens.join(this.getTokenAsync(appId));
    }
}
