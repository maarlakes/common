package cn.maarlakes.common.locks;

import jakarta.annotation.Nonnull;

import java.util.concurrent.TimeUnit;

/**
 * 互斥锁抽象，统一同步锁和异步锁的接口。
 *
 * <p>通过 {@link #isReentrant()} 和 {@link #isAsync()} 可在运行时感知锁的能力。</p>
 *
 * @author linjpxc
 */
public interface Mutex {

    /**
     * 锁的 key。
     */
    @Nonnull
    String key();

    /**
     * 是否支持可重入。
     */
    boolean isReentrant();

    /**
     * 是否支持异步（跨线程）解锁。
     */
    boolean isAsync();

    /**
     * 获取锁，阻塞直到成功。
     */
    void lock();

    /**
     * 获取锁，阻塞直到成功或被中断。
     */
    void lockInterruptibly() throws InterruptedException;

    /**
     * 尝试获取锁，立即返回。
     *
     * @return 是否成功获取
     */
    boolean tryLock();

    /**
     * 在指定时间内尝试获取锁。
     *
     * @return 是否成功获取
     */
    boolean tryLock(long time, @Nonnull TimeUnit unit) throws InterruptedException;

    /**
     * 释放锁。
     */
    void unlock();
}
