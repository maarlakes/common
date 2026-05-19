package cn.maarlakes.common.token.access;

import cn.maarlakes.common.token.ExpirationAppToken;
import jakarta.annotation.Nonnull;

import java.time.Instant;

public interface AccessToken extends ExpirationAppToken<AppId, String> {

    @Nonnull
    static AccessToken of(@Nonnull AppId appId, @Nonnull String token, @Nonnull Instant expiresAt) {
        return new DefaultAccessToken(appId, token, expiresAt);
    }
}
