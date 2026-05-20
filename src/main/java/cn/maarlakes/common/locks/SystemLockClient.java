package cn.maarlakes.common.locks;

import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 基于 JVM 本地的锁客户端实现。
 *
 * <p>适用于有限的静态 key 集合（如固定的业务锁名称）。内部通过
 * {@link ConcurrentHashMap} 缓存 {@link Mutex} 实例以保证同一 key 的互斥语义。</p>
 *
 * <p><b>不适用于动态 key 场景</b>（如包含用户 ID、订单 ID 等可变参数的 key），
 * 因为锁实例不会自动清理，动态 key 会导致内存持续增长。动态 key 场景应使用
 * 分布式锁实现（如 {@code RedissonLockClient}）。</p>
 *
 * <p>同一个 key 只能使用 {@link #getMutex} 或 {@link #getAsyncMutex} 其中一种，
 * 混用会抛出 {@link MutexBindingException}。</p>
 *
 * @author linjpxc
 */
public class SystemLockClient implements LockClient {

    private static final Logger log = LoggerFactory.getLogger(SystemLockClient.class);

    protected final ConcurrentMap<String, Mutex> mutexes = new ConcurrentHashMap<>();

    @Nonnull
    @Override
    public Mutex getMutex(@Nonnull LockContext context) {
        return this.mutexes.compute(context.key(), (key, existing) -> {
            if (existing != null) {
                checkBinding(key, existing, false);
                return existing;
            }
            if (log.isDebugEnabled()) {
                log.debug("创建同步锁 [{}]，fair={}", key, context.isFair());
            }
            return new ReentrantMutex(key, context.isFair());
        });
    }

    @Nonnull
    @Override
    public Mutex getAsyncMutex(@Nonnull LockContext context) {
        return this.mutexes.compute(context.key(), (key, existing) -> {
            if (existing != null) {
                checkBinding(key, existing, true);
                return existing;
            }
            if (log.isDebugEnabled()) {
                log.debug("创建异步锁 [{}]，fair={}", key, context.isFair());
            }
            return new SemaphoreMutex(key, context.isFair());
        });
    }

    private static void checkBinding(String key, Mutex existing, boolean async) {
        if (existing.isAsync() != async) {
            throw new MutexBindingException(
                    "Key '" + key + "' already bound as " + (existing.isAsync() ? "async" : "sync") + " mutex");
        }
    }

    /**
     * 移除指定 key 对应的锁实例。
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
