package cn.maarlakes.common.locks;

import jakarta.annotation.Nonnull;

import java.util.concurrent.TimeUnit;

/**
 * 互斥锁的统一抽象接口。
 *
 * <p>本接口屏蔽了底层锁实现的差异（JVM 本地锁、Redis 分布式锁等），
 * 提供一致的加锁/解锁契约。通过 {@link #isReentrant()} 和 {@link #isAsync()}
 * 两个能力标记，调用方可以在运行时感知当前锁实例的行为特性。</p>
 *
 * <h3>同步锁 vs 异步锁</h3>
 * <ul>
 *     <li><b>同步锁</b>（{@code isAsync() == false}）：可重入，加锁和解锁必须在同一线程。
 *         典型实现：{@link ReentrantMutex}、{@code RedissonMutex}。</li>
 *     <li><b>异步锁</b>（{@code isAsync() == true}）：不可重入，支持跨线程解锁。
 *         适用于 {@link java.util.concurrent.CompletionStage} 等异步编程模型，
 *         其中加锁线程和解锁线程可能不同。典型实现：{@link SemaphoreMutex}、{@code RedissonSemaphoreMutex}。</li>
 * </ul>
 *
 * <h3>生命周期</h3>
 * <p>锁实例由 {@link LockClient} 创建和管理。对于 JVM 本地锁，{@link SystemLockClient}
 * 内部会缓存锁实例；对于分布式锁，{@code RedissonLockClient} 每次创建新实例。
 * 使用完毕后应调用 {@link #unlock()} 释放锁，也可以通过 {@link LockGuard}
 * 配合 try-with-resources 自动释放。</p>
 *
 * @author linjpxc
 * @see LockClient
 * @see LockGuard
 * @see ReentrantMutex
 * @see SemaphoreMutex
 */
public interface Mutex {

    /**
     * 锁的唯一标识。
     *
     * <p>相同 key 的锁在语义上互斥。key 的来源取决于使用方式：
     * AOP 场景下由 {@link SpelLockKeyResolver} 解析 {@link SyncLock#value()} 得到；
     * 编程式场景下由调用方直接指定。</p>
     */
    @Nonnull
    String key();

    /**
     * 是否支持可重入。
     *
     * <p>可重入锁允许同一线程对同一 key 多次加锁而不阻塞，每次加锁需要对应一次解锁。
     * 不可重入锁在同一线程重复加锁时会阻塞或失败。</p>
     */
    boolean isReentrant();

    /**
     * 是否支持异步（跨线程）解锁。
     *
     * <p>异步锁允许在加锁线程之外的线程执行解锁操作，适用于异步编程模型。
     * 非异步锁要求加锁和解锁在同一线程，否则行为未定义。</p>
     */
    boolean isAsync();

    /**
     * 阻塞式获取锁。
     *
     * <p>如果锁被其他线程持有，当前线程将阻塞直到成功获取。
     * 此方法不响应中断，如需可中断行为请使用 {@link #lockInterruptibly()}。</p>
     */
    void lock();

    /**
     * 可中断地获取锁。
     *
     * <p>行为与 {@link #lock()} 相同，但在等待过程中如果线程被中断，
     * 将抛出 {@link InterruptedException} 并清除中断状态。</p>
     *
     * @throws InterruptedException 如果等待锁时线程被中断
     */
    void lockInterruptibly() throws InterruptedException;

    /**
     * 非阻塞式尝试获取锁。
     *
     * <p>立即返回，不会阻塞等待。如果锁可用则获取成功并返回 {@code true}，
     * 否则返回 {@code false}。</p>
     *
     * @return {@code true} 表示成功获取锁，{@code false} 表示锁不可用
     */
    boolean tryLock();

    /**
     * 在指定时间内尝试获取锁。
     *
     * <p>如果锁在指定时间内变得可用，则获取成功并返回 {@code true}。
     * 如果超时仍未获取到锁，返回 {@code false}。</p>
     *
     * @param time 最大等待时间
     * @param unit 时间单位
     * @return {@code true} 表示在超时前成功获取锁，{@code false} 表示超时未获取
     * @throws InterruptedException 如果等待锁时线程被中断
     */
    boolean tryLock(long time, @Nonnull TimeUnit unit) throws InterruptedException;

    /**
     * 释放锁。
     *
     * <p>对于同步锁，必须由持有锁的线程调用；对于异步锁，可以从任意线程调用。
     * 如果当前线程不持有此锁，行为取决于底层实现（可能抛出异常或静默忽略）。</p>
     */
    void unlock();
}
