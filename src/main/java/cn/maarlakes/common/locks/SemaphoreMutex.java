package cn.maarlakes.common.locks;

import jakarta.annotation.Nonnull;

import java.util.Objects;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * 基于 {@link Semaphore Semaphore(1)} 的互斥锁，支持异步（跨线程）解锁。
 *
 * <p>不可重入：同一线程对同一 key 连续两次 acquire 会阻塞。</p>
 *
 * @author linjpxc
 */
public final class SemaphoreMutex implements Mutex {

    private final String key;
    private final Semaphore semaphore;

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

    @Override
    public void lock() {
        this.semaphore.acquireUninterruptibly();
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        this.semaphore.acquire();
    }

    @Override
    public boolean tryLock() {
        return this.semaphore.tryAcquire();
    }

    @Override
    public boolean tryLock(long time, @Nonnull TimeUnit unit) throws InterruptedException {
        return this.semaphore.tryAcquire(time, unit);
    }

    @Override
    public void unlock() {
        this.semaphore.release();
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
