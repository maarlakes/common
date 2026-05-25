package cn.maarlakes.common.locks;

import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * {@link Mutex} 的 {@link AutoCloseable} 包装器，支持 try-with-resources 语法自动释放锁。
 *
 * <p>使用此工具类可以避免手动编写 try-finally 解锁代码，确保即使在异常情况下锁也能被正确释放。</p>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * // 阻塞获取锁，退出 try 块后自动释放
 * try (LockGuard guard = LockGuard.lock(lockClient.getMutex(context))) {
 *     // 业务逻辑
 * }
 *
 * // 带超时的锁获取
 * try (LockGuard guard = LockGuard.tryLock(mutex, 5, TimeUnit.SECONDS)) {
 *     // 业务逻辑
 * } catch (SyncLockTimeoutException e) {
 *     // 获取锁超时处理
 * }
 * }</pre>
 *
 * <h3>线程安全性</h3>
 * <p>{@link #close()} 方法通过 {@code volatile} 读取 {@link #locked} 标志进行快速判断，
 * 再通过 {@code synchronized} 确保只有一个线程能执行解锁操作，防止重复释放。</p>
 *
 * @author linjpxc
 * @see Mutex
 */
public final class LockGuard implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(LockGuard.class);

    private final Mutex mutex;

    // volatile 确保多线程场景下对 locked 标志的可见性
    private volatile boolean locked;

    private LockGuard(@Nonnull Mutex mutex) {
        this.mutex = mutex;
        this.locked = true;
    }

    /**
     * 阻塞式获取锁并返回 {@link LockGuard}。
     *
     * <p>内部调用 {@link Mutex#lockInterruptibly()}，因此支持中断。</p>
     *
     * @param mutex 要获取的锁
     * @return 包装了已获取锁的 LockGuard 实例
     * @throws InterruptedException 如果等待锁时被中断
     */
    @Nonnull
    public static LockGuard lock(@Nonnull Mutex mutex) throws InterruptedException {
        mutex.lockInterruptibly();
        if (log.isDebugEnabled()) {
            log.debug("通过 LockGuard 获取锁 [{}]", mutex.key());
        }
        return new LockGuard(mutex);
    }

    /**
     * 在指定时间内尝试获取锁。
     *
     * @param mutex 要获取的锁
     * @param time  最大等待时间
     * @param unit  时间单位
     * @return 包装了已获取锁的 LockGuard 实例
     * @throws SyncLockTimeoutException 如果获取锁超时
     * @throws InterruptedException     如果等待锁时被中断
     */
    @Nonnull
    public static LockGuard tryLock(@Nonnull Mutex mutex, long time, @Nonnull TimeUnit unit)
            throws InterruptedException {
        if (!mutex.tryLock(time, unit)) {
            log.warn("LockGuard 获取锁 [{}] 超时，等待了 {}{}", mutex.key(), time, unit);
            throw new SyncLockTimeoutException();
        }
        if (log.isDebugEnabled()) {
            log.debug("通过 LockGuard 在 {}{} 内获取锁 [{}]", time, unit, mutex.key());
        }
        return new LockGuard(mutex);
    }

    /**
     * 释放锁。
     *
     * <p>此方法是幂等的：多次调用只会执行一次解锁操作。
     * 通过 {@code volatile} 读取 {@code locked} 标志进行快速判断，
     * 再通过 {@code synchronized} 块确保只有一个线程能将 {@code locked} 置为 {@code false}
     * 并执行解锁，防止两个线程同时看到 {@code locked == true} 而导致重复解锁。</p>
     */
    @Override
    public synchronized void close() {
        if (this.locked) {
            this.locked = false;
            this.mutex.unlock();
            if (log.isTraceEnabled()) {
                log.trace("LockGuard 释放锁 [{}]", this.mutex.key());
            }
        }
    }
}
