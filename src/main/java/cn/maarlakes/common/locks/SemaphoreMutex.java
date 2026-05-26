package cn.maarlakes.common.locks;

import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * 基于 {@link Semaphore Semaphore(1)} 的互斥锁，支持异步（跨线程）解锁。
 *
 * <p>通过将信号量许可数设为 1 来实现互斥语义。与 {@link ReentrantMutex} 的关键区别：</p>
 * <ul>
 *     <li><b>不可重入</b>：同一线程对同一锁连续两次 {@link #lock()} 会阻塞（信号量的许可已被耗尽）</li>
 *     <li><b>支持跨线程解锁</b>：{@link #unlock()} 可以在任意线程调用，
 *         因为 {@link Semaphore#release()} 不要求调用线程持有许可</li>
 * </ul>
 *
 * <h3>设计选择</h3>
 * <p>选择 {@link Semaphore} 而非 {@link java.util.concurrent.locks.ReentrantLock} 是因为异步编程模型（如
 * {@link java.util.concurrent.CompletionStage}）中，加锁和解锁通常在不同线程执行，
 * 而 {@link java.util.concurrent.locks.ReentrantLock} 要求同一线程解锁。</p>
 *
 * @author linjpxc
 * @see Mutex
 * @see ReentrantMutex
 * @see SystemLockClient
 */
public final class SemaphoreMutex implements Mutex {

    private static final Logger log = LoggerFactory.getLogger(SemaphoreMutex.class);

    private final String key;
    private final Semaphore semaphore;

    /**
     * 创建信号量互斥锁。
     *
     * @param key  锁的唯一标识
     * @param fair 是否使用公平锁。公平锁按线程等待顺序分配许可
     */
    public SemaphoreMutex(@Nonnull String key, boolean fair) {
        this.key = key;
        this.semaphore = new Semaphore(1, fair);
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
     * <p>内部调用 {@link Semaphore#acquireUninterruptibly()}，因此不响应中断。
     * 如果线程在等待过程中被中断，中断标志会被 {@link Semaphore} 内部清除，
     * 线程会继续等待直到获取许可。获取成功后中断标志不会被恢复。
     * 如需可中断行为，请使用 {@link #lockInterruptibly()}。</p>
     */
    @Override
    public void lock() {
        this.semaphore.acquireUninterruptibly();
        if (log.isTraceEnabled()) {
            log.trace("信号量锁 [{}] 加锁成功", this.key);
        }
    }

    /**
     * 可中断地获取锁。
     *
     * @throws InterruptedException 如果等待锁时线程被中断
     */
    @Override
    public void lockInterruptibly() throws InterruptedException {
        this.semaphore.acquire();
        if (log.isTraceEnabled()) {
            log.trace("信号量锁 [{}] 可中断加锁成功", this.key);
        }
    }

    /**
     * 非阻塞式尝试获取锁。
     *
     * @return {@code true} 表示成功获取锁，{@code false} 表示锁不可用
     */
    @Override
    public boolean tryLock() {
        final boolean acquired = this.semaphore.tryAcquire();
        if (log.isTraceEnabled()) {
            log.trace("信号量锁 [{}] 尝试加锁：{}", this.key, acquired);
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
        final boolean acquired = this.semaphore.tryAcquire(time, unit);
        if (acquired) {
            if (log.isTraceEnabled()) {
                log.trace("信号量锁 [{}] 在 {}{} 内加锁成功", this.key, time, unit);
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("信号量锁 [{}] 在 {}{} 内加锁超时", this.key, time, unit);
            }
        }
        return acquired;
    }

    /**
     * 释放锁。
     *
     * <p>将信号量的许可归还。可以在任意线程调用，不要求与加锁线程相同。</p>
     */
    @Override
    public void unlock() {
        this.semaphore.release();
        if (log.isTraceEnabled()) {
            log.trace("信号量锁 [{}] 解锁成功", this.key);
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
        return "SemaphoreMutex{key='" + key + "'}";
    }
}
