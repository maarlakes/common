package cn.maarlakes.common.locks;

import jakarta.annotation.Nonnull;

import java.util.concurrent.TimeUnit;

/**
 * {@link Mutex} 的 {@link AutoCloseable} 包装器，支持 try-with-resources 语法。
 *
 * <pre>{@code
 * try (LockGuard guard = LockGuard.lock(lockClient.getMutex(context))) {
 *     // 业务逻辑
 * }
 * }</pre>
 *
 * @author linjpxc
 */
public final class LockGuard implements AutoCloseable {

    private final Mutex mutex;
    private boolean locked;

    private LockGuard(@Nonnull Mutex mutex) {
        this.mutex = mutex;
        this.locked = true;
    }

    /**
     * 获取锁并返回 LockGuard。
     *
     * @throws InterruptedException 如果等待锁时被中断
     */
    @Nonnull
    public static LockGuard lock(@Nonnull Mutex mutex) throws InterruptedException {
        mutex.lockInterruptibly();
        return new LockGuard(mutex);
    }

    /**
     * 尝试在指定时间内获取锁。
     *
     * @throws SyncLockTimeoutException 如果获取锁超时
     * @throws InterruptedException     如果等待锁时被中断
     */
    @Nonnull
    public static LockGuard tryLock(@Nonnull Mutex mutex, long time, @Nonnull TimeUnit unit)
            throws InterruptedException {
        if (!mutex.tryLock(time, unit)) {
            throw new SyncLockTimeoutException();
        }
        return new LockGuard(mutex);
    }

    @Override
    public synchronized void close() {
        if (this.locked) {
            this.locked = false;
            this.mutex.unlock();
        }
    }
}
