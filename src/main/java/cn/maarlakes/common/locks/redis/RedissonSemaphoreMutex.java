package cn.maarlakes.common.locks.redis;

import cn.maarlakes.common.locks.Mutex;
import jakarta.annotation.Nonnull;
import org.redisson.api.RSemaphore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Redisson {@link RSemaphore} 的异步互斥锁，支持跨线程解锁。
 *
 * <p>不可重入：同一线程对同一 key 连续两次 acquire 会阻塞。</p>
 *
 * <p>key 自动清理：unlock 后设置 TTL，lock 时移除 TTL。
 * 空闲 key 在 TTL 到期后自动从 Redis 中删除。</p>
 *
 * @author linjpxc
 */
final class RedissonSemaphoreMutex implements Mutex {

    private static final Logger log = LoggerFactory.getLogger(RedissonSemaphoreMutex.class);

    private final String key;
    private final RSemaphore semaphore;
    private final Duration idleTimeout;

    RedissonSemaphoreMutex(@Nonnull String key, @Nonnull RSemaphore semaphore, Duration idleTimeout) {
        this.key = key;
        this.semaphore = semaphore;
        this.idleTimeout = idleTimeout;
    }

    @Nonnull
    @Override
    public String key() {
        return this.key;
    }

    @Override
    public boolean isReentrant() {
        return false;
    }

    @Override
    public boolean isAsync() {
        return true;
    }

    @Override
    public void lock() {
        boolean interrupted = false;
        try {
            while (true) {
                try {
                    this.semaphore.trySetPermits(1);
                    this.semaphore.acquire();
                    this.semaphore.clearExpire();
                    log.debug("信号量 [{}] 获取成功", this.key);
                    return;
                } catch (InterruptedException e) {
                    interrupted = true;
                    log.debug("信号量 [{}] 获取被中断，重试中", this.key);
                }
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        this.semaphore.trySetPermits(1);
        this.semaphore.acquire();
        this.semaphore.clearExpire();
        log.debug("信号量 [{}] 获取成功（可中断）", this.key);
    }

    @Override
    public boolean tryLock() {
        this.semaphore.trySetPermits(1);
        if (this.semaphore.tryAcquire(1)) {
            this.semaphore.clearExpire();
            log.debug("信号量 [{}] 尝试获取成功", this.key);
            return true;
        }
        log.debug("信号量 [{}] 尝试获取失败", this.key);
        return false;
    }

    @Override
    public boolean tryLock(long time, @Nonnull TimeUnit unit) throws InterruptedException {
        this.semaphore.trySetPermits(1);
        if (this.semaphore.tryAcquire(time, unit)) {
            this.semaphore.clearExpire();
            log.debug("信号量 [{}] 在 {}{} 内获取成功", this.key, time, unit);
            return true;
        }
        log.debug("信号量 [{}] 在 {}{} 内获取超时", this.key, time, unit);
        return false;
    }

    @Override
    public void unlock() {
        this.semaphore.release();
        log.debug("信号量 [{}] 已释放", this.key);
        if (this.semaphore.tryAcquire()) {
            try {
                this.semaphore.expire(this.idleTimeout);
                log.debug("信号量 [{}] 空闲，设置 TTL={}", this.key, this.idleTimeout);
            } finally {
                this.semaphore.release();
            }
        }
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
        return "RedissonSemaphoreMutex{key='" + key + "'}";
    }
}
