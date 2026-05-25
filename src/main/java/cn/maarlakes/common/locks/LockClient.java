package cn.maarlakes.common.locks;

import jakarta.annotation.Nonnull;

/**
 * 锁客户端，根据 {@link LockContext} 创建和管理 {@link Mutex} 实例的工厂接口。
 *
 * <p>本接口定义了锁实例的获取、移除和清除操作。实现类负责底层锁的创建和生命周期管理，
 * 调用方通过 {@link #getMutex(LockContext)} 或 {@link #getAsyncMutex(LockContext)}
 * 获取锁实例后，使用 {@link Mutex} 接口进行加锁和解锁操作。</p>
 *
 * <h3>绑定不变量</h3>
 * <p>同一个 key 只能绑定一种锁类型（同步或异步）。首次调用 {@link #getMutex} 或
 * {@link #getAsyncMutex} 时确定绑定类型，后续对同一 key 调用另一种方法将抛出
 * {@link MutexBindingException}。此限制确保同一 key 的所有操作使用一致的锁语义。</p>
 *
 * <h3>实现类</h3>
 * <ul>
 *     <li>{@link SystemLockClient} — 基于 JVM 本地 {@link java.util.concurrent.ConcurrentHashMap} 缓存，
 *         适用于有限的静态 key 集合</li>
 *     <li>{@code RedissonLockClient} — 基于 Redisson 的分布式锁实现，
 *         适用于跨进程的锁协调</li>
 * </ul>
 *
 * <h3>线程安全性</h3>
 * <p>所有实现类必须保证线程安全。在并发环境下，多个线程同时调用 {@link #getMutex} 或
 * {@link #getAsyncMutex} 获取同一 key 的锁时，必须保证绑定不变量不被违反。</p>
 *
 * @author linjpxc
 * @see Mutex
 * @see LockContext
 * @see SystemLockClient
 */
public interface LockClient {

    /**
     * 获取同步锁（可重入，不支持异步解锁）。
     *
     * <p>返回的 {@link Mutex} 实例 {@code isReentrant() == true}、{@code isAsync() == false}。
     * 如果该 key 已绑定为异步锁，抛出 {@link MutexBindingException}。</p>
     *
     * @param context 锁上下文，包含 key、公平性、超时等配置
     * @return 对应 key 的同步锁实例
     * @throws MutexBindingException 如果该 key 已绑定为异步锁
     */
    @Nonnull
    Mutex getMutex(@Nonnull LockContext context);

    /**
     * 获取异步锁（不可重入，支持跨线程解锁）。
     *
     * <p>返回的 {@link Mutex} 实例 {@code isReentrant() == false}、{@code isAsync() == true}。
     * 如果该 key 已绑定为同步锁，抛出 {@link MutexBindingException}。</p>
     *
     * @param context 锁上下文，包含 key、公平性、超时等配置
     * @return 对应 key 的异步锁实例
     * @throws MutexBindingException 如果该 key 已绑定为同步锁
     */
    @Nonnull
    Mutex getAsyncMutex(@Nonnull LockContext context);

    /**
     * 移除指定 key 对应的锁实例。
     *
     * <p>从客户端的内部缓存中移除该 key 的锁实例。此操作不会释放当前被持有的锁，
     * 仅影响后续对该 key 的锁获取行为（下次获取将创建新实例）。</p>
     *
     * @param key 要移除的锁 key
     */
    void remove(@Nonnull String key);

    /**
     * 清除所有已缓存的锁实例。
     *
     * <p>从客户端的内部缓存中移除所有锁实例。此操作不会释放当前被持有的锁。
     * 适用于应用关闭或重置场景。</p>
     */
    void clear();
}
