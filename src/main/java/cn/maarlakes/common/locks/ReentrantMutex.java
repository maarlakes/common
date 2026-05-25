package cn.maarlakes.common.locks;

import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 基于 {@link ReentrantLock} 的可重入互斥锁，仅适用于同步场景。
 *
 * <p>此类是对 {@link ReentrantLock} 的简单封装，实现了 {@link Mutex} 接口的同步锁契约：
 * 可重入（同一线程可多次加锁）、不支持异步解锁（加锁和解锁必须在同一线程）。</p>
 *
 * <h3>线程 confinement</h3>
 * <p>{@link #unlock()} 必须由持有锁的线程调用。如果其他线程尝试解锁，行为取决于
 * {@link ReentrantLock} 的实现（通常抛出 {@link IllegalMonitorStateException}）。</p>
 *
 * <h3>适用场景</h3>
 * <p>适用于 {@link SystemLockClient} 管理的 JVM 本地锁。当 AOP 拦截器检测到方法返回类型
 * 不属于异步类型时，会自动选择此锁。如需跨线程解锁，应使用 {@link SemaphoreMutex}。</p>
 *
 * @author linjpxc
 * @see Mutex
 * @see SemaphoreMutex
 * @see SystemLockClient
 */
public final class ReentrantMutex implements Mutex {

    private static final Logger log = LoggerFactory.getLogger(ReentrantMutex.class);

    private final String key;
    private final ReentrantLock lock;

    /**
     * 创建可重入互斥锁。
     *
     * @param key  锁的唯一标识
     * @param fair 是否使用公平锁。公平锁按请求顺序分配，避免饥饿但吞吐量较低
     */
    public ReentrantMutex(@Nonnull String key, boolean fair) {
        this.key = key;
        this.lock = new ReentrantLock(fair);
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
     * <p>此方法不响应中断。如果锁被其他线程持有，当前线程将一直阻塞直到获取成功。
     * 如需可中断行为，请使用 {@link #lockInterruptibly()}。</p>
     */
    @Override
    public void lock() {
        this.lock.lock();
        if (log.isTraceEnabled()) {
            log.trace("可重入锁 [{}] 加锁成功", this.key);
        }
    }

    /**
     * 可中断地获取锁。
     *
     * @throws InterruptedException 如果等待锁时线程被中断
     */
    @Override
    public void lockInterruptibly() throws InterruptedException {
        this.lock.lockInterruptibly();
        if (log.isTraceEnabled()) {
            log.trace("可重入锁 [{}] 可中断加锁成功", this.key);
        }
    }

    /**
     * 非阻塞式尝试获取锁。
     *
     * @return {@code true} 表示成功获取锁，{@code false} 表示锁不可用
     */
    @Override
    public boolean tryLock() {
        final boolean acquired = this.lock.tryLock();
        if (log.isTraceEnabled()) {
            log.trace("可重入锁 [{}] 尝试加锁：{}", this.key, acquired);
        }
        return acquired;
    }

    /**
     * 在指定时间内尝试获取锁。
     *
     * @param time 最大等待时间
     * @param unit 时间单位
     * @return {@code true} 表示在超时前成功获取锁，{@code false} 表示超时未获取
     * @throws InterruptedException 如果等待锁时线程被中断
     */
    @Override
    public boolean tryLock(long time, @Nonnull TimeUnit unit) throws InterruptedException {
        final boolean acquired = this.lock.tryLock(time, unit);
        if (acquired) {
            if (log.isTraceEnabled()) {
                log.trace("可重入锁 [{}] 在 {}{} 内加锁成功", this.key, time, unit);
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("可重入锁 [{}] 在 {}{} 内加锁超时", this.key, time, unit);
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
        this.lock.unlock();
        if (log.isTraceEnabled()) {
            log.trace("可重入锁 [{}] 解锁成功", this.key);
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
        return "ReentrantMutex{key='" + key + "'}";
    }
}
