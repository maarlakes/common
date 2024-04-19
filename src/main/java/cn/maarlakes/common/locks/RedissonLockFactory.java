package cn.maarlakes.common.locks;

import jakarta.annotation.Nonnull;
import org.redisson.api.RedissonClient;

import java.util.concurrent.locks.Lock;

/**
 * @author linjpxc
 */
public class RedissonLockFactory implements LockFactory {

    private final RedissonClient redissonClient;

    public RedissonLockFactory(@Nonnull RedissonClient redissonClient) {
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
