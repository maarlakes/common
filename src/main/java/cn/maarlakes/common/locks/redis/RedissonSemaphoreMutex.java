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
 * <p>通过将 Redis 信号量的许可数设为 1 来实现分布式互斥语义。不可重入：
 * 同一线程对同一 key 连续两次 {@link #lock()} 会阻塞。</p>
 *
 * <h3>空闲 key 自动清理</h3>
 * <p>为避免 Redis 中残留无用的信号量 key，此实现在 {@link #unlock()} 时检测锁是否空闲
 * （无人持有），如果空闲则为 key 设置 TTL（{@link #idleTimeout}）。下次 {@link #lock()}
 * 时清除 TTL。空闲 key 在 TTL 到期后由 Redis 自动删除。</p>
 *
 * <h3>信号量初始化</h3>
 * <p>每次加锁前调用 {@link RSemaphore#trySetPermits(int)} 确保信号量在 Redis 中存在。
 * 如果信号量已存在（返回 {@code false}），不会覆盖现有许可数。</p>
 *
 * @author linjpxc
 * @see Mutex
 * @see RedissonLockClient
 */
final class RedissonSemaphoreMutex implements Mutex {

    private static final Logger log = LoggerFactory.getLogger(RedissonSemaphoreMutex.class);

    private final String key;
    private final RSemaphore semaphore;
    private final Duration idleTimeout;

    /**
     * 创建 Redisson 信号量互斥锁。
     *
     * @param key          锁的业务标识（不含 namespace 前缀）
     * @param semaphore    Redisson RSemaphore 实例
     * @param idleTimeout  空闲 key 的 TTL 时长，超时后 Redis 自动删除
     */
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

    /**
     * 阻塞式获取锁。
     *
     * <p>使用自旋 + 中断恢复模式：如果线程在等待过程中被中断，保存中断标志，
     * 在 finally 块中恢复中断状态后继续重试。这确保了 {@link Mutex#lock()} 不抛出
     * InterruptedException 的契约，同时不会丢失中断信号。</p>
     */
    @Override
    public void lock() {
        boolean interrupted = false;
        try {
            while (true) {
                try {
                    this.semaphore.trySetPermits(1);
                    this.semaphore.acquire();
                    // 加锁成功后清除空闲 TTL，防止锁在使用中被 Redis 删除
                    this.semaphore.clearExpire();
                    if (log.isTraceEnabled()) {
                        log.trace("Redisson 信号量 [{}] 加锁成功", this.key);
                    }
                    return;
                } catch (InterruptedException e) {
                    interrupted = true;
                    if (log.isTraceEnabled()) {
                        log.trace("Redisson 信号量 [{}] 加锁被中断，恢复中断标志后重试", this.key);
                    }
                }
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 可中断地获取锁。
     *
     * <p>先通过 {@link RSemaphore#trySetPermits(int)} 确保信号量存在，
     * 然后 {@link RSemaphore#acquire()} 获取一个许可，最后清除空闲 TTL。</p>
     *
     * @throws InterruptedException 如果等待锁时线程被中断
     */
    @Override
    public void lockInterruptibly() throws InterruptedException {
        this.semaphore.trySetPermits(1);
        this.semaphore.acquire();
        this.semaphore.clearExpire();
        if (log.isTraceEnabled()) {
            log.trace("Redisson 信号量 [{}] 可中断加锁成功", this.key);
        }
    }

    /**
     * 非阻塞式尝试获取锁。
     *
     * @return {@code true} 表示成功获取锁，{@code false} 表示锁不可用
     */
    @Override
    public boolean tryLock() {
        this.semaphore.trySetPermits(1);
        if (this.semaphore.tryAcquire(1)) {
            this.semaphore.clearExpire();
            if (log.isTraceEnabled()) {
                log.trace("Redisson 信号量 [{}] 尝试加锁成功", this.key);
            }
            return true;
        }
        if (log.isDebugEnabled()) {
            log.debug("Redisson 信号量 [{}] 尝试加锁失败", this.key);
        }
        return false;
    }

    /**
     * 在指定时间内尝试获取锁。
     *
     * @param time 最大等待时间
     * @param unit 时间单位
     * @return {@code true} 表示在超时前成功获取锁
     * @throws InterruptedException 如果等待锁时线程被中断
     */
    @Override
    public boolean tryLock(long time, @Nonnull TimeUnit unit) throws InterruptedException {
        this.semaphore.trySetPermits(1);
        if (this.semaphore.tryAcquire(time, unit)) {
            this.semaphore.clearExpire();
            if (log.isTraceEnabled()) {
                log.trace("Redisson 信号量 [{}] 在 {}{} 内加锁成功", this.key, time, unit);
            }
            return true;
        }
        if (log.isDebugEnabled()) {
            log.debug("Redisson 信号量 [{}] 在 {}{} 内加锁超时", this.key, time, unit);
        }
        return false;
    }

    /**
     * 释放锁并管理空闲 key 的 TTL。
     *
     * <p>释放流程：</p>
     * <ol>
     *     <li>调用 {@link RSemaphore#release()} 归还许可</li>
     *     <li>立即调用 {@link RSemaphore#tryAcquire()} 非阻塞地尝试重新获取许可，
     *         以检测信号量是否空闲（无人持有）</li>
     *     <li>如果获取成功（说明信号量空闲），为 key 设置 TTL，
     *         然后再次 release 归还刚才 tryAcquire 获取的许可</li>
     *     <li>如果获取失败（说明有其他线程在等待），不设置 TTL</li>
     * </ol>
     */
    @Override
    public void unlock() {
        this.semaphore.release();
        if (log.isTraceEnabled()) {
            log.trace("Redisson 信号量 [{}] 已释放许可", this.key);
        }

        // 非阻塞检测信号量是否空闲（无人持有）
        if (this.semaphore.tryAcquire()) {
            try {
                this.semaphore.expire(this.idleTimeout);
                if (log.isDebugEnabled()) {
                    log.debug("Redisson 信号量 [{}] 空闲，设置 TTL={}", this.key, this.idleTimeout);
                }
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
