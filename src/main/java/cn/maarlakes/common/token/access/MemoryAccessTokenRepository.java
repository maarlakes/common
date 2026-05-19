package cn.maarlakes.common.token.access;

import cn.maarlakes.common.token.MemoryExpirationTokenRepository;
import jakarta.annotation.Nonnull;

public class MemoryAccessTokenRepository extends MemoryExpirationTokenRepository<AccessToken, AppId, String> implements AccessTokenRepository {
    public MemoryAccessTokenRepository(@Nonnull AccessTokenFactory tokenFactory) {
        super(tokenFactory);
    }
}
