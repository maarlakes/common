package cn.maarlakes.common.token.access.weixin;

import cn.maarlakes.common.token.access.AppId;
import jakarta.annotation.Nonnull;

public interface WeixinAppId extends AppId {

    @Nonnull
    static WeixinAppId of(@Nonnull String appId) {
        return new DefaultWeixinAppId(appId);
    }
}
