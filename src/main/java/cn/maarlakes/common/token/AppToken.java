package cn.maarlakes.common.token;

import jakarta.annotation.Nonnull;

import java.io.Serializable;

/**
 * Token 模型的顶层接口，将应用标识（appId）与 Token 值关联在一起。
 *
 * <p>所有具体的 Token 类型（如 {@code AccessToken}）都继承此接口。实现类应为不可变对象，
 * 通过 {@code appId} 和 {@code token} 唯一确定一个 Token 实例。
 *
 * <p>继承 {@link Serializable} 以支持序列化存储（如 Redis）。
 *
 * @param <A> 应用标识类型
 * @param <T> Token 值类型
 * @author linjpxc
 */
public interface AppToken<A, T> extends Serializable {

    /**
     * 获取应用标识。
     *
     * @return 关联的应用标识
     */
    @Nonnull
    A getAppId();

    /**
     * 获取 Token 值。
     *
     * @return Token 的实际值（如访问令牌字符串）
     */
    @Nonnull
    T getToken();
}
