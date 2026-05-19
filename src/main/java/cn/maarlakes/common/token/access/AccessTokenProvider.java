package cn.maarlakes.common.token.access;

import cn.maarlakes.common.token.TokenFactory;
import jakarta.annotation.Nonnull;

public interface AccessTokenProvider extends TokenFactory<AccessToken, AppId, String> {

    boolean supported(@Nonnull AppId appId);
}
