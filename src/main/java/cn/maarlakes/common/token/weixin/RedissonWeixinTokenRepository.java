package cn.maarlakes.common.token.weixin;

import cn.maarlakes.common.token.RedissonExpirationTokenRepository;
import jakarta.annotation.Nonnull;
import org.redisson.api.RedissonClient;

/**
 * @author linjpxc
 */
public class RedissonWeixinTokenRepository extends RedissonExpirationTokenRepository<WeixinToken, String, String> implements WeixinTokenRepository {

    public RedissonWeixinTokenRepository(@Nonnull RedissonClient client, @Nonnull WeixinTokenFactory tokenFactory) {
        this(client, "weixin-token", tokenFactory);
    }

    public RedissonWeixinTokenRepository(@Nonnull RedissonClient client, @Nonnull String namespace, @Nonnull WeixinTokenFactory tokenFactory) {
        super(client, namespace, tokenFactory);
    }
}
