package cn.maarlakes.common.locks;

import jakarta.annotation.Nonnull;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 基于 {@link ReentrantLock} 的可重入互斥锁，仅适用于同步场景。
 *
 * <p>unlock 必须由持有锁的线程调用。</p>
 *
 * @author linjpxc
 */
public final class ReentrantMutex implements Mutex {

    private final String key;
    private final ReentrantLock lock;

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

    @Override
    public void lock() {
        this.lock.lock();
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        this.lock.lockInterruptibly();
    }

    @Override
    public boolean tryLock() {
        return this.lock.tryLock();
    }

    @Override
    public boolean tryLock(long time, @Nonnull TimeUnit unit) throws InterruptedException {
        return this.lock.tryLock(time, unit);
    }

    @Override
    public void unlock() {
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
        return "ReentrantMutex{key='" + key + "'}";
    }
}
