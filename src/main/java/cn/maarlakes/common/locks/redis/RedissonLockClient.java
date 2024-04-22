package cn.maarlakes.common.locks.redis;

import cn.maarlakes.common.locks.LockClient;
import cn.maarlakes.common.locks.LockContext;
import jakarta.annotation.Nonnull;
import org.redisson.api.RedissonClient;

import java.util.concurrent.locks.Lock;

/**
 * @author linjpxc
 */
public class RedissonLockClient implements LockClient {

    private final RedissonClient redissonClient;

    public RedissonLockClient(@Nonnull RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Nonnull
    @Override
    public Lock createLock(@Nonnull LockContext context) {
        if (context.isFair()) {
            this.redissonClient.getFairLock(context.key());
        }
        return this.redissonClient.getLock(context.key());
    }
}
