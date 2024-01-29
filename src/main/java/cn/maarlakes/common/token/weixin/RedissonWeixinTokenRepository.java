package cn.maarlakes.common.token.weixin;

import cn.maarlakes.common.token.RedissonExpirationTokenRepository;
import jakarta.annotation.Nonnull;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.Codec;

/**
 * @author linjpxc
 */
public class RedissonWeixinTokenRepository extends RedissonExpirationTokenRepository<WeixinToken, String, String> implements WeixinTokenRepository {

    public RedissonWeixinTokenRepository(@Nonnull RedissonClient client, @Nonnull WeixinTokenFactory tokenFactory) {
        this(client, "weixin-token", null, tokenFactory);
    }

    public RedissonWeixinTokenRepository(@Nonnull RedissonClient client, Codec codec, @Nonnull WeixinTokenFactory tokenFactory) {
        this(client, "weixin-token", codec, tokenFactory);
    }

    public RedissonWeixinTokenRepository(@Nonnull RedissonClient client, @Nonnull String namespace, @Nonnull WeixinTokenFactory tokenFactory) {
        this(client, namespace, null, tokenFactory);
    }

    public RedissonWeixinTokenRepository(@Nonnull RedissonClient client, @Nonnull String namespace, Codec codec, @Nonnull WeixinTokenFactory tokenFactory) {
        super(client, namespace, codec, tokenFactory);
    }
}
