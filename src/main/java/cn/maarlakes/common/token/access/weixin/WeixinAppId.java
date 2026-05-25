package cn.maarlakes.common.token.access.weixin;

import cn.maarlakes.common.token.access.AppId;
import jakarta.annotation.Nonnull;

/**
 * 微信应用标识接口，扩展 {@link AppId} 用于标识微信平台的公众号/小程序/企业微信等应用。
 *
 * <p>微信相关的 {@link cn.maarlakes.common.token.access.AccessTokenProvider} 通过
 * {@code appId instanceof WeixinAppId} 判断是否支持该应用标识。
 *
 * <p>通过 {@link #of} 静态工厂方法创建实例。
 *
 * @author linjpxc
 */
public interface WeixinAppId extends AppId {

    /**
     * 创建微信应用标识实例。
     *
     * @param appId 微信 AppID（如公众号的 AppID）
     * @return 微信应用标识
     */
    @Nonnull
    static WeixinAppId of(@Nonnull String appId) {
        return new DefaultWeixinAppId(appId);
    }
}
