package cn.maarlakes.common.locks;

import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 基于 JVM 本地的锁客户端实现。
 *
 * <p>通过 {@link ConcurrentHashMap} 缓存 {@link Mutex} 实例，保证同一 key 对应唯一的锁对象。
 * {@link ConcurrentHashMap#compute} 的原子性确保了在并发场景下不会为同一 key 创建不同类型的锁。</p>
 *
 * <h3>适用场景</h3>
 * <p>适用于有限的静态 key 集合（如固定的业务锁名称，例如 "inventory-sync"、"report-generation" 等）。
 * 锁实例一旦创建不会被自动清理。</p>
 *
 * <h3>不适用场景</h3>
 * <p><b>不适用于动态 key</b>（如包含用户 ID、订单 ID 等可变参数的 key），
 * 因为锁实例缓存在 {@link ConcurrentHashMap} 中且不会自动清理，动态 key 会导致内存持续增长。
 * 动态 key 场景应使用分布式锁实现（如 {@code RedissonLockClient}）。</p>
 *
 * <h3>绑定不变量</h3>
 * <p>同一个 key 只能使用 {@link #getMutex(LockContext)} 或 {@link #getAsyncMutex(LockContext)} 其中一种。
 * 混用会抛出 {@link MutexBindingException}，此限制由 {@link ConcurrentHashMap#compute} 的原子性保证。</p>
 *
 * @author linjpxc
 * @see LockClient
 * @see ReentrantMutex
 * @see SemaphoreMutex
 */
public class SystemLockClient implements LockClient {

    private static final Logger log = LoggerFactory.getLogger(SystemLockClient.class);

    protected final ConcurrentMap<String, Mutex> mutexes = new ConcurrentHashMap<>();

    /**
     * 获取或创建同步锁实例。
     *
     * <p>如果该 key 已有缓存的同步锁实例则直接复用，否则创建新的 {@link ReentrantMutex}。
     * 如果该 key 已绑定为异步锁，抛出 {@link MutexBindingException}。</p>
     *
     * @param context 锁上下文
     * @return 同步锁实例
     * @throws MutexBindingException 如果该 key 已绑定为异步锁
     */
    @Nonnull
    @Override
    public Mutex getMutex(@Nonnull LockContext context) {
        // compute 的 lambda 在 ConcurrentHashMap 内部同步执行，保证同一 key 的原子性
        return this.mutexes.compute(context.key(), (key, existing) -> {
            if (existing != null) {
                checkBinding(key, existing, false);
                if (log.isTraceEnabled()) {
                    log.trace("复用已有同步锁 [{}]", key);
                }
                return existing;
            }
            if (log.isDebugEnabled()) {
                log.debug("创建同步锁 [{}]，fair={}", key, context.isFair());
            }
            return new ReentrantMutex(key, context.isFair());
        });
    }

    /**
     * 获取或创建异步锁实例。
     *
     * <p>如果该 key 已有缓存的异步锁实例则直接复用，否则创建新的 {@link SemaphoreMutex}。
     * 如果该 key 已绑定为同步锁，抛出 {@link MutexBindingException}。</p>
     *
     * @param context 锁上下文
     * @return 异步锁实例
     * @throws MutexBindingException 如果该 key 已绑定为同步锁
     */
    @Nonnull
    @Override
    public Mutex getAsyncMutex(@Nonnull LockContext context) {
        return this.mutexes.compute(context.key(), (key, existing) -> {
            if (existing != null) {
                checkBinding(key, existing, true);
                if (log.isTraceEnabled()) {
                    log.trace("复用已有异步锁 [{}]", key);
                }
                return existing;
            }
            if (log.isDebugEnabled()) {
                log.debug("创建异步锁 [{}]，fair={}", key, context.isFair());
            }
            return new SemaphoreMutex(key, context.isFair());
        });
    }

    /**
     * 检查锁类型绑定不变量。
     *
     * <p>如果已存在的锁类型与请求的类型不一致，记录警告日志并抛出异常。</p>
     *
     * @param key     锁 key
     * @param existing 已存在的锁实例
     * @param async   当前请求的是否为异步锁
     * @throws MutexBindingException 如果类型不一致
     */
    private static void checkBinding(String key, Mutex existing, boolean async) {
        if (existing.isAsync() != async) {
            final String existingType = existing.isAsync() ? "异步" : "同步";
            final String requestedType = async ? "异步" : "同步";
            log.warn("锁 key '{}' 已绑定为{}锁，无法切换为{}锁", key, existingType, requestedType);
            throw new MutexBindingException(
                    "Key '" + key + "' already bound as " + (existing.isAsync() ? "async" : "sync") + " mutex");
        }
    }

    /**
     * 移除指定 key 对应的锁实例。
     *
     * <p>此操作不会释放当前被持有的锁，仅从缓存中移除。
     * 下次对该 key 调用 {@link #getMutex} 或 {@link #getAsyncMutex} 时将创建新实例。</p>
     *
     * @param key 要移除的锁 key
     */
    @Override
    public void remove(@Nonnull String key) {
        this.mutexes.remove(key);
        if (log.isDebugEnabled()) {
            log.debug("移除锁 [{}]，剩余={}", key, this.mutexes.size());
        }
    }

    /**
     * 清除所有锁实例。
     *
     * <p>此操作不会释放当前被持有的锁。适用于应用关闭或需要完全重置锁状态时调用。</p>
     */
    @Override
    public void clear() {
        final int size = this.mutexes.size();
        this.mutexes.clear();
        if (log.isDebugEnabled()) {
            log.debug("清除所有锁，共移除 {} 个", size);
        }
    }
}
