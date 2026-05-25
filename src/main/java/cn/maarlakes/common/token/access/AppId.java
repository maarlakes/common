package cn.maarlakes.common.token.access;

import jakarta.annotation.Nonnull;

import java.io.Serializable;

/**
 * 应用标识接口，用于唯一确定一个需要 Token 的应用。
 *
 * <p>不同的 Token 提供者通过检查 AppId 的实际类型来决定是否支持该应用。
 * 例如，微信相关的 Provider 只支持 {@link cn.maarlakes.common.token.access.weixin.WeixinAppId} 类型。
 *
 * <p>继承 {@link Serializable} 以支持序列化存储。
 *
 * @author linjpxc
 */
public interface AppId extends Serializable {

    /**
     * 获取应用标识字符串。
     *
     * @return 应用的唯一标识（如微信的 AppID）
     */
    @Nonnull
    String getAppId();
}
