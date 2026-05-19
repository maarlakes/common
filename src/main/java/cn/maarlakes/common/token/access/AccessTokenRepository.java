package cn.maarlakes.common.token.access;

import cn.maarlakes.common.token.ExpirationTokenRepository;

public interface AccessTokenRepository extends ExpirationTokenRepository<AccessToken, AppId, String> {
}
