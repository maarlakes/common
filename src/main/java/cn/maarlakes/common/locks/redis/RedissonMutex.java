package cn.maarlakes.common.locks.redis;

import cn.maarlakes.common.locks.Mutex;
import jakarta.annotation.Nonnull;
import org.redisson.api.RLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Redisson {@link RLock} 的可重入互斥锁适配器。
 *
 * <p>RLock 是 Redisson 提供的分布式可重入锁，加锁和解锁必须在同一线程。</p>
 *
 * <h3>leaseTime 与 watchdog</h3>
 * <ul>
 *     <li>{@code leaseTime == -1} — 使用 Redisson 的 watchdog 机制自动续期，默认每 30 秒续期一次，
 *         持有者线程存活期间锁不会过期</li>
 *     <li>{@code leaseTime > 0} — 显式指定锁的持有时间（毫秒），到期后 Redis 自动删除 key 释放锁，
 *         适用于无法保证持有者一定会调用 unlock 的场景（如持有者可能崩溃）</li>
 * </ul>
 *
 * @author linjpxc
 * @see Mutex
 * @see RedissonLockClient
 */
final class RedissonMutex implements Mutex {

    private static final Logger log = LoggerFactory.getLogger(RedissonMutex.class);

    private final String key;
    private final RLock lock;
    private final long leaseTimeMillis;

    /**
     * 创建 Redisson 互斥锁。
     *
     * @param key             锁的业务标识（不含 namespace 前缀）
     * @param lock            Redisson RLock 实例（已包含 namespace 前缀）
     * @param leaseTimeMillis 持有时间（毫秒），-1 使用 watchdog，>0 显式指定
     */
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

    /**
     * 阻塞式获取锁。
     *
     * <p>根据 leaseTime 配置选择调用方式：
     * 有 leaseTime 时使用 {@code lock(leaseTime, MILLISECONDS)} 确保到期自动释放；
     * 无 leaseTime 时使用 {@code lock()} 启用 watchdog 自动续期。</p>
     */
    @Override
    public void lock() {
        if (this.leaseTimeMillis > 0) {
            if (log.isTraceEnabled()) {
                log.trace("Redisson 可重入锁 [{}] 加锁，leaseTime={}ms", this.key, this.leaseTimeMillis);
            }
            this.lock.lock(this.leaseTimeMillis, TimeUnit.MILLISECONDS);
        } else {
            if (log.isTraceEnabled()) {
                log.trace("Redisson 可重入锁 [{}] 加锁（watchdog 自动续期）", this.key);
            }
            this.lock.lock();
        }
    }

    /**
     * 可中断地获取锁。
     *
     * @throws InterruptedException 如果等待锁时线程被中断
     */
    @Override
    public void lockInterruptibly() throws InterruptedException {
        if (this.leaseTimeMillis > 0) {
            if (log.isTraceEnabled()) {
                log.trace("Redisson 可重入锁 [{}] 可中断加锁，leaseTime={}ms", this.key, this.leaseTimeMillis);
            }
            this.lock.lockInterruptibly(this.leaseTimeMillis, TimeUnit.MILLISECONDS);
        } else {
            if (log.isTraceEnabled()) {
                log.trace("Redisson 可重入锁 [{}] 可中断加锁（watchdog 自动续期）", this.key);
            }
            this.lock.lockInterruptibly();
        }
    }

    /**
     * 非阻塞式尝试获取锁。
     *
     * <p>对于有 leaseTime 的情况，调用三参数的 {@code tryLock(0, leaseTime, MILLISECONDS)}：
     * 第一个参数 0 表示不等待，第二个参数是持有时间。
     * 如果在此过程中线程被中断，捕获异常并恢复中断标志，返回 {@code false}（符合
     * {@link Mutex#tryLock()} 不抛出 InterruptedException 的契约）。</p>
     *
     * @return {@code true} 表示成功获取锁，{@code false} 表示锁不可用
     */
    @Override
    public boolean tryLock() {
        if (this.leaseTimeMillis > 0) {
            try {
                final boolean acquired = this.lock.tryLock(0, this.leaseTimeMillis, TimeUnit.MILLISECONDS);
                if (log.isDebugEnabled()) {
                    log.debug("Redisson 可重入锁 [{}] 尝试加锁，leaseTime={}ms：{}", this.key, this.leaseTimeMillis, acquired);
                }
                return acquired;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        final boolean acquired = this.lock.tryLock();
        if (log.isDebugEnabled()) {
            log.debug("Redisson 可重入锁 [{}] 尝试加锁：{}", this.key, acquired);
        }
        return acquired;
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
        final boolean acquired;
        if (this.leaseTimeMillis > 0) {
            acquired = this.lock.tryLock(time, this.leaseTimeMillis, unit);
        } else {
            acquired = this.lock.tryLock(time, unit);
        }
        if (acquired) {
            if (log.isTraceEnabled()) {
                log.trace("Redisson 可重入锁 [{}] 在 {}{} 内加锁成功", this.key, time, unit);
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Redisson 可重入锁 [{}] 在 {}{} 内加锁超时", this.key, time, unit);
            }
        }
        return acquired;
    }

    /**
     * 释放锁。
     *
     * <p>必须由持有锁的线程调用。可重入锁需要与加锁次数相同的解锁次数才能完全释放。</p>
     */
    @Override
    public void unlock() {
        if (log.isTraceEnabled()) {
            log.trace("Redisson 可重入锁 [{}] 解锁", this.key);
        }
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
