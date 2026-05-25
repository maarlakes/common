package cn.maarlakes.common.locks.redis;

import cn.maarlakes.common.locks.LockClient;
import cn.maarlakes.common.locks.LockContext;
import cn.maarlakes.common.locks.Mutex;
import jakarta.annotation.Nonnull;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * 基于 Redisson 的分布式锁客户端实现。
 *
 * <p>通过 Redisson 的 {@link RedissonClient} 创建分布式锁，支持跨 JVM 进程的锁协调。
 * 所有锁 key 会自动添加 namespace 前缀，格式为 {@code namespace:key}，
 * 避免不同应用之间的锁 key 冲突。</p>
 *
 * <h3>与 SystemLockClient 的区别</h3>
 * <ul>
 *     <li>{@link cn.maarlakes.common.locks.SystemLockClient SystemLockClient} — JVM 本地锁，
 *         缓存锁实例，适用于静态 key</li>
 *     <li>{@code RedissonLockClient} — 分布式锁，每次调用创建新实例，支持动态 key，
 *         适用于跨进程互斥</li>
 * </ul>
 *
 * <h3>namespace 策略</h3>
 * <p>namespace 参数用于隔离不同应用的锁 key。构造时自动补充冒号后缀：
 * 输入 {@code "myapp"} 实际存储为 {@code "myapp:"}。最终 key 格式为 {@code "myapp:lockKey"}。</p>
 *
 * @author linjpxc
 * @see LockClient
 * @see RedissonMutex
 * @see RedissonSemaphoreMutex
 */
public class RedissonLockClient implements LockClient {

    private static final Logger log = LoggerFactory.getLogger(RedissonLockClient.class);

    private final RedissonClient redissonClient;
    private final String namespace;
    private final Duration idleTimeout;

    /**
     * 创建 Redisson 锁客户端，使用默认空闲超时（1 分钟）。
     *
     * @param redissonClient Redisson 客户端实例
     * @param namespace      锁 key 的命名空间前缀，自动补充冒号后缀
     */
    public RedissonLockClient(@Nonnull RedissonClient redissonClient, @Nonnull String namespace) {
        this(redissonClient, namespace, Duration.ofMinutes(1L));
    }

    /**
     * 创建 Redisson 锁客户端。
     *
     * @param redissonClient Redisson 客户端实例
     * @param namespace      锁 key 的命名空间前缀，自动补充冒号后缀
     * @param idleTimeout    异步锁（信号量）空闲后的 TTL 时长，超时后 Redis 自动删除 key
     */
    public RedissonLockClient(@Nonnull RedissonClient redissonClient, @Nonnull String namespace, Duration idleTimeout) {
        this.redissonClient = redissonClient;
        this.idleTimeout = idleTimeout;
        if (namespace.endsWith(":")) {
            this.namespace = namespace;
        } else {
            this.namespace = namespace + ":";
        }
        log.info("RedissonLockClient 初始化完成，namespace='{}'，idleTimeout={}", this.namespace, idleTimeout);
    }

    /**
     * 创建同步锁（可重入）。
     *
     * <p>每次调用创建新的 {@link RedissonMutex} 实例，不复用。key 会被拼接为
     * {@code namespace + context.key()} 后传给 Redisson。</p>
     *
     * @param context 锁上下文
     * @return Redisson 可重入锁实例
     */
    @Nonnull
    @Override
    public Mutex getMutex(@Nonnull LockContext context) {
        final String namespacedKey = this.namespace + context.key();
        if (log.isDebugEnabled()) {
            log.debug("创建 Redisson 同步锁 [{}]（实际 key='{}'），fair={}，leaseTime={}ms",
                    context.key(), namespacedKey, context.isFair(), context.leaseTime());
        }
        return new RedissonMutex(context.key(), this.getLock(context.isFair(), namespacedKey), context.leaseTime());
    }

    /**
     * 创建异步锁（不可重入，支持跨线程解锁）。
     *
     * <p>每次调用创建新的 {@link RedissonSemaphoreMutex} 实例。</p>
     *
     * @param context 锁上下文
     * @return Redisson 信号量锁实例
     */
    @Nonnull
    @Override
    public Mutex getAsyncMutex(@Nonnull LockContext context) {
        if (log.isDebugEnabled()) {
            log.debug("创建 Redisson 异步锁 [{}]（实际 key='{}'）",
                    context.key(), this.namespace + context.key());
        }
        return new RedissonSemaphoreMutex(context.key(), this.redissonClient.getSemaphore(this.namespace + context.key()), this.idleTimeout);
    }

    /**
     * 从 Redis 中删除指定 key 对应的锁。
     *
     * <p>直接删除 Redis 中的 key，不区分锁类型。慎用：如果锁正在被持有，删除会导致互斥语义失效。</p>
     *
     * @param key 锁 key（不含 namespace 前缀）
     */
    @Override
    public void remove(@Nonnull String key) {
        final String namespacedKey = this.namespace + key;
        if (log.isDebugEnabled()) {
            log.debug("移除 Redis 锁 key [{}]（实际 key='{}'）", key, namespacedKey);
        }
        this.redissonClient.getBucket(namespacedKey).delete();
    }

    /**
     * 清除 namespace 下的所有锁。
     *
     * <p>通过 {@code deleteByPattern} 批量删除 namespace 前缀下的所有 key。
     * 注意：在 Redis 中 key 数量较多时，此操作可能较慢。</p>
     */
    @Override
    public void clear() {
        if (log.isDebugEnabled()) {
            log.debug("清除 Redis 锁，pattern={}*", this.namespace);
        }
        this.redissonClient.getKeys().deleteByPattern(this.namespace + "*");
    }

    /**
     * 根据 fair 标志获取对应的 Redisson 锁实例。
     *
     * @param isFair        是否公平锁
     * @param namespacedKey 包含 namespace 的完整 key
     * @return Redisson RLock 实例
     */
    private RLock getLock(boolean isFair, String namespacedKey) {
        if (isFair) {
            if (log.isTraceEnabled()) {
                log.trace("获取 Redisson 公平锁：{}", namespacedKey);
            }
            return this.redissonClient.getFairLock(namespacedKey);
        }
        if (log.isTraceEnabled()) {
            log.trace("获取 Redisson 非公平锁：{}", namespacedKey);
        }
        return this.redissonClient.getLock(namespacedKey);
    }
}
