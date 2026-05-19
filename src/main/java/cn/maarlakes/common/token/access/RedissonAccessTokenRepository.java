package cn.maarlakes.common.token.access;

import cn.maarlakes.common.token.RedissonExpirationTokenRepository;
import jakarta.annotation.Nonnull;
import org.redisson.api.RedissonClient;

import java.time.Duration;

public class RedissonAccessTokenRepository extends RedissonExpirationTokenRepository<AccessToken, AppId, String> implements AccessTokenRepository {

    public RedissonAccessTokenRepository(@Nonnull RedissonClient client, @Nonnull AccessTokenFactory tokenFactory) {
        this(client, "access-token", tokenFactory);
    }

    public RedissonAccessTokenRepository(@Nonnull RedissonClient client, @Nonnull String namespace, @Nonnull AccessTokenFactory tokenFactory) {
        super(client, namespace, tokenFactory);
    }

    public RedissonAccessTokenRepository(@Nonnull RedissonClient client, @Nonnull String namespace, @Nonnull AccessTokenFactory tokenFactory, @Nonnull Duration lockAcquireTimeout) {
        super(client, namespace, tokenFactory, lockAcquireTimeout);
    }
}
