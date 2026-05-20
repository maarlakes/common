package cn.maarlakes.common.locks;

import jakarta.annotation.Nonnull;

import java.util.concurrent.TimeUnit;

/**
 * @author linjpxc
 */
public interface LockContext {

    /**
     * 锁的 key
     */
    @Nonnull
    String key();

    /**
     * 是否公平锁
     */
    boolean isFair();

    /**
     * 获取锁的等待时间（毫秒）。
     * -1 表示无限等待，0 表示不等待（tryLock），大于 0 表示超时等待。
     */
    long waitTime();

    /**
     * 锁的持有时间（毫秒），主要用于分布式锁的自动释放。
     * -1 表示依赖实现的默认值（如 Redisson 的 watchdog 机制）。
     */
    long leaseTime();

    @Nonnull
    static LockContext create(@Nonnull String key) {
        return new DefaultLockContext(key, false, -1L, -1L);
    }

    @Nonnull
    static LockContext create(@Nonnull String key, boolean fair) {
        return new DefaultLockContext(key, fair, -1L, -1L);
    }

    /**
     * 使用毫秒值创建 LockContext。
     */
    @Nonnull
    static LockContext create(@Nonnull String key, boolean fair, long waitTimeMillis, long leaseTimeMillis) {
        return new DefaultLockContext(key, fair, waitTimeMillis, leaseTimeMillis);
    }

    @Nonnull
    static LockContext create(@Nonnull String key, boolean fair, long waitTime, @Nonnull TimeUnit unit) {
        return new DefaultLockContext(key, fair, unit.toMillis(waitTime), -1L);
    }

    @Nonnull
    static LockContext create(@Nonnull String key, boolean fair, long waitTime, long leaseTime, @Nonnull TimeUnit unit) {
        return new DefaultLockContext(key, fair, unit.toMillis(waitTime), unit.toMillis(leaseTime));
    }
}
