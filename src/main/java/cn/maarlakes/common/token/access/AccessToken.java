package cn.maarlakes.common.token.access;

import cn.maarlakes.common.token.ExpirationAppToken;
import jakarta.annotation.Nonnull;

import java.time.Instant;

/**
 * 访问令牌接口，表示一个带过期时间的 Access Token。
 *
 * <p>泛型特化为：
 * <ul>
 *   <li>应用标识类型 — {@link AppId}</li>
 *   <li>Token 值类型 — {@link String}</li>
 * </ul>
 *
 * <p>通过 {@link #of} 静态工厂方法创建实例。
 *
 * @author linjpxc
 */
public interface AccessToken extends ExpirationAppToken<AppId, String> {

    /**
     * 创建一个 AccessToken 实例。
     *
     * @param appId     应用标识
     * @param token     Token 值字符串
     * @param expiresAt 过期时间点（UTC）
     * @return 新的 AccessToken 实例
     */
    @Nonnull
    static AccessToken of(@Nonnull AppId appId, @Nonnull String token, @Nonnull Instant expiresAt) {
        return new DefaultAccessToken(appId, token, expiresAt);
    }
}
