package cn.maarlakes.common.token;

import jakarta.annotation.Nonnull;

import java.time.Instant;

/**
 * 带过期时间的 Token 接口，扩展 {@link AppToken} 增加过期判定能力。
 *
 * <p>Token 的过期时间由 {@link #getExpiresAt()} 返回的 {@link Instant} 决定，
 * {@link #isExpired()} 默认通过比较过期时间与当前时间来判断。
 *
 * <p>实现类应确保过期时间在创建时确定，之后不再变化（不可变对象）。
 *
 * @param <A> 应用标识类型
 * @param <T> Token 值类型
 * @author linjpxc
 */
public interface ExpirationAppToken<A, T> extends AppToken<A, T> {

    /**
     * 获取 Token 的过期时间点。
     *
     * @return 过期的绝对时间（UTC）
     */
    @Nonnull
    Instant getExpiresAt();

    /**
     * 判断 Token 是否已过期。
     *
     * <p>默认实现基于 {@link Instant#now()} 与 {@link #getExpiresAt()} 的比较。
     * 当过期时间早于当前时间时返回 {@code true}。
     *
     * @return 如果已过期返回 {@code true}，否则返回 {@code false}
     */
    default boolean isExpired() {
        return Tokens.isExpired(this);
    }
}
