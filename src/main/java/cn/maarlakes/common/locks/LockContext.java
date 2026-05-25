package cn.maarlakes.common.locks;

import jakarta.annotation.Nonnull;

import java.util.concurrent.TimeUnit;

/**
 * 不可变的锁配置上下文，描述获取锁所需的全部参数。
 *
 * <p>每个 {@link LockContext} 实例包含四个属性：锁 key、公平性标志、等待时间和持有时间。
 * 通过静态工厂方法创建，实例创建后不可修改。</p>
 *
 * <h3>waitTime 语义</h3>
 * <table>
 *     <tr><th>值</th><th>含义</th></tr>
 *     <tr><td>{@code -1}</td><td>无限等待（阻塞直到获取锁，默认值）</td></tr>
 *     <tr><td>{@code 0}</td><td>不等待（立即返回，tryLock 语义）</td></tr>
 *     <tr><td>{@code > 0}</td><td>超时等待（在指定时间内尝试获取锁，超时后失败）</td></tr>
 * </table>
 *
 * <h3>leaseTime 语义</h3>
 * <table>
 *     <tr><th>值</th><th>含义</th></tr>
 *     <tr><td>{@code -1}</td><td>依赖实现的默认值（如 Redisson 的 watchdog 自动续期机制）</td></tr>
 *     <tr><td>{@code > 0}</td><td>显式指定锁的持有时间，到期后自动释放（主要用于分布式锁）</td></tr>
 * </table>
 *
 * @author linjpxc
 * @see DefaultLockContext
 * @see LockClient
 */
public interface LockContext {

    /**
     * 锁的唯一标识 key。
     *
     * <p>相同 key 的 {@link LockContext} 在 {@link LockClient} 中映射到同一个 {@link Mutex} 实例。</p>
     */
    @Nonnull
    String key();

    /**
     * 是否使用公平锁。
     *
     * <p>公平锁按照线程请求的顺序分配锁，避免线程饥饿，但吞吐量通常低于非公平锁。</p>
     */
    boolean isFair();

    /**
     * 获取锁的等待时间（毫秒）。
     *
     * @return 等待时间，-1 表示无限等待，0 表示不等待，大于 0 表示超时等待
     */
    long waitTime();

    /**
     * 锁的持有时间（毫秒）。
     *
     * <p>主要用于分布式锁的自动释放机制。到期后锁由底层实现自动释放，
     * 防止因持有者崩溃导致的死锁。</p>
     *
     * @return 持有时间，-1 表示依赖实现的默认值
     */
    long leaseTime();

    /**
     * 使用默认配置（非公平、无限等待、无持有时间限制）创建锁上下文。
     *
     * @param key 锁的唯一标识
     * @return 锁上下文实例
     */
    @Nonnull
    static LockContext create(@Nonnull String key) {
        return new DefaultLockContext(key, false, -1L, -1L);
    }

    /**
     * 指定公平性创建锁上下文，其余使用默认配置。
     *
     * @param key  锁的唯一标识
     * @param fair 是否使用公平锁
     * @return 锁上下文实例
     */
    @Nonnull
    static LockContext create(@Nonnull String key, boolean fair) {
        return new DefaultLockContext(key, fair, -1L, -1L);
    }

    /**
     * 使用毫秒值指定等待时间和持有时间创建锁上下文。
     *
     * @param key             锁的唯一标识
     * @param fair            是否使用公平锁
     * @param waitTimeMillis  等待时间（毫秒），-1 无限等待，0 不等待
     * @param leaseTimeMillis 持有时间（毫秒），-1 使用默认值
     * @return 锁上下文实例
     */
    @Nonnull
    static LockContext create(@Nonnull String key, boolean fair, long waitTimeMillis, long leaseTimeMillis) {
        return new DefaultLockContext(key, fair, waitTimeMillis, leaseTimeMillis);
    }

    /**
     * 使用指定时间单位创建锁上下文，持有时间使用默认值。
     *
     * @param key      锁的唯一标识
     * @param fair     是否使用公平锁
     * @param waitTime 等待时间
     * @param unit     时间单位
     * @return 锁上下文实例
     */
    @Nonnull
    static LockContext create(@Nonnull String key, boolean fair, long waitTime, @Nonnull TimeUnit unit) {
        return new DefaultLockContext(key, fair, unit.toMillis(waitTime), -1L);
    }

    /**
     * 使用指定时间单位创建锁上下文，同时指定等待时间和持有时间。
     *
     * @param key       锁的唯一标识
     * @param fair      是否使用公平锁
     * @param waitTime  等待时间
     * @param leaseTime 持有时间
     * @param unit      时间单位
     * @return 锁上下文实例
     */
    @Nonnull
    static LockContext create(@Nonnull String key, boolean fair, long waitTime, long leaseTime, @Nonnull TimeUnit unit) {
        return new DefaultLockContext(key, fair, unit.toMillis(waitTime), unit.toMillis(leaseTime));
    }
}
