package cn.maarlakes.common.token.weixin;

import cn.maarlakes.common.token.ExpirationAppToken;
import jakarta.annotation.Nonnull;

import java.time.LocalDateTime;

/**
 * @author linjpxc
 */
public interface WeixinToken extends ExpirationAppToken<String, String> {

    @Nonnull
    static WeixinToken of(@Nonnull String appId, @Nonnull String token, @Nonnull LocalDateTime expirationTime) {
        return new DefaultWeixinToken(appId, token, expirationTime);
    }
}
