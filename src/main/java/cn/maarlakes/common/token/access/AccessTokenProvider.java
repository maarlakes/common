package cn.maarlakes.common.token.access;

import cn.maarlakes.common.token.TokenFactory;
import jakarta.annotation.Nonnull;

/**
 * Access Token 提供者接口，扩展 {@link TokenFactory} 增加能力声明。
 *
 * <p>在多 Provider 场景下，{@link DefaultAccessTokenFactory} 会遍历所有注册的 Provider，
 * 通过 {@link #supported(AppId)} 判断当前 Provider 是否能处理给定的 AppId，
 * 选择第一个支持的 Provider 进行 Token 创建。
 *
 * <p>例如，微信相关的 Provider 仅在 AppId 类型为 {@link cn.maarlakes.common.token.access.weixin.WeixinAppId} 时返回 {@code true}。
 *
 * @author linjpxc
 */
public interface AccessTokenProvider extends TokenFactory<AccessToken, AppId, String> {

    /**
     * 判断当前 Provider 是否支持处理给定的 AppId。
     *
     * @param appId 应用标识
     * @return 如果支持返回 {@code true}，否则返回 {@code false}
     */
    boolean supported(@Nonnull AppId appId);
}
