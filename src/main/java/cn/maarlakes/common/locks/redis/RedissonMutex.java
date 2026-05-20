package cn.maarlakes.common.locks.redis;

import cn.maarlakes.common.locks.Mutex;
import jakarta.annotation.Nonnull;
import org.redisson.api.RLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Redisson {@link RLock} 的互斥锁适配器。
 *
 * <p>RLock 本身支持可重入，unlock 必须由持有线程调用。</p>
 *
 * @author linjpxc
 */
final class RedissonMutex implements Mutex {

    private static final Logger log = LoggerFactory.getLogger(RedissonMutex.class);

    private final String key;
    private final RLock lock;
    private final long leaseTimeMillis;

    RedissonMutex(@Nonnull String key, @Nonnull RLock lock, long leaseTimeMillis) {
        this.key = key;
        this.lock = lock;
        this.leaseTimeMillis = leaseTimeMillis;
    }

    @Nonnull
    @Override
    public String key() {
        return this.key;
    }

    @Override
    public boolean isReentrant() {
        return true;
    }

    @Override
    public boolean isAsync() {
        return false;
    }

    @Override
    public void lock() {
        if (this.leaseTimeMillis > 0) {
            log.debug("加锁 [{}]，leaseTime={}ms", this.key, this.leaseTimeMillis);
            this.lock.lock(this.leaseTimeMillis, TimeUnit.MILLISECONDS);
        } else {
            log.debug("加锁 [{}]", this.key);
            this.lock.lock();
        }
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        if (this.leaseTimeMillis > 0) {
            log.debug("可中断加锁 [{}]，leaseTime={}ms", this.key, this.leaseTimeMillis);
            this.lock.lockInterruptibly(this.leaseTimeMillis, TimeUnit.MILLISECONDS);
        } else {
            log.debug("可中断加锁 [{}]", this.key);
            this.lock.lockInterruptibly();
        }
    }

    @Override
    public boolean tryLock() {
        if (this.leaseTimeMillis > 0) {
            try {
                final boolean acquired = this.lock.tryLock(0, this.leaseTimeMillis, TimeUnit.MILLISECONDS);
                log.debug("尝试加锁 [{}]，leaseTime={}ms：{}", this.key, this.leaseTimeMillis, acquired);
                return acquired;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        final boolean acquired = this.lock.tryLock();
        log.debug("尝试加锁 [{}]：{}", this.key, acquired);
        return acquired;
    }

    @Override
    public boolean tryLock(long time, @Nonnull TimeUnit unit) throws InterruptedException {
        final boolean acquired;
        if (this.leaseTimeMillis > 0) {
            acquired = this.lock.tryLock(time, this.leaseTimeMillis, unit);
        } else {
            acquired = this.lock.tryLock(time, unit);
        }
        log.debug("尝试加锁 [{}]，等待 {}{}：{}", this.key, time, unit, acquired);
        return acquired;
    }

    @Override
    public void unlock() {
        log.debug("解锁 [{}]", this.key);
        this.lock.unlock();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Mutex) {
            return Objects.equals(this.key, ((Mutex) o).key());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(key);
    }

    @Override
    public String toString() {
        return "RedissonMutex{key='" + key + "'}";
    }
}
