package cn.maarlakes.common.token.access;

import cn.maarlakes.common.token.RedissonExpirationTokenRepository;
import jakarta.annotation.Nonnull;
import org.redisson.api.RedissonClient;

import java.time.Duration;

/**
 * 基于 Redis 的 Access Token 仓库实现。
 *
 * <p>继承 {@link RedissonExpirationTokenRepository} 的全部能力（分布式缓存、分布式锁、过期检查、刷新），
 * 并将泛型特化为 {@link AccessToken} / {@link AppId} / {@link String}。
 *
 * <p>适用于多实例部署场景，Token 通过 Redis 在所有实例间共享。
 * 默认的 Redis Map 命名空间为 {@code "access-token"}，可通过构造参数自定义。
 *
 * @author linjpxc
 */
public class RedissonAccessTokenRepository extends RedissonExpirationTokenRepository<AccessToken, AppId, String> implements AccessTokenRepository {

    /**
     * 使用默认命名空间 {@code "access-token"} 和默认锁超时时间（5 分钟）构造。
     *
     * @param client       Redisson 客户端
     * @param tokenFactory Access Token 工厂
     */
    public RedissonAccessTokenRepository(@Nonnull RedissonClient client, @Nonnull AccessTokenFactory tokenFactory) {
        this(client, "access-token", tokenFactory);
    }

    /**
     * 使用自定义命名空间和默认锁超时时间（5 分钟）构造。
     *
     * @param client       Redisson 客户端
     * @param namespace    Redis Map 的命名空间
     * @param tokenFactory Access Token 工厂
     */
    public RedissonAccessTokenRepository(@Nonnull RedissonClient client, @Nonnull String namespace, @Nonnull AccessTokenFactory tokenFactory) {
        super(client, namespace, tokenFactory);
    }

    /**
     * 使用自定义命名空间和锁超时时间构造。
     *
     * @param client            Redisson 客户端
     * @param namespace         Redis Map 的命名空间
     * @param tokenFactory      Access Token 工厂
     * @param lockAcquireTimeout 获取分布式锁的超时时间
     */
    public RedissonAccessTokenRepository(@Nonnull RedissonClient client, @Nonnull String namespace, @Nonnull AccessTokenFactory tokenFactory, @Nonnull Duration lockAcquireTimeout) {
        super(client, namespace, tokenFactory, lockAcquireTimeout);
    }
}
