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
 * @author linjpxc
 */
public class RedissonLockClient implements LockClient {
    private static final Logger log = LoggerFactory.getLogger(RedissonLockClient.class);

    private final RedissonClient redissonClient;
    private final String namespace;
    private final Duration idleTimeout;

    public RedissonLockClient(@Nonnull RedissonClient redissonClient, @Nonnull String namespace) {
        this(redissonClient, namespace, Duration.ofMinutes(1L));
    }

    public RedissonLockClient(@Nonnull RedissonClient redissonClient, @Nonnull String namespace, Duration idleTimeout) {
        this.redissonClient = redissonClient;
        this.idleTimeout = idleTimeout;
        if (namespace.endsWith(":")) {
            this.namespace = namespace;
        } else {
            this.namespace = namespace + ":";
        }
        if (log.isDebugEnabled()) {
            log.debug("RedissonLockClient 初始化完成，namespace='{}'，idleTimeout={}", this.namespace, idleTimeout);
        }
    }

    @Nonnull
    @Override
    public Mutex getMutex(@Nonnull LockContext context) {
        final String namespacedKey = this.namespace + context.key();
        if (log.isDebugEnabled()) {
            log.debug("创建同步锁 [{}]，fair={}，leaseTime={}ms", context.key(), context.isFair(), context.leaseTime());
        }
        return new RedissonMutex(context.key(), this.getLock(context.isFair(), namespacedKey), context.leaseTime());
    }

    @Nonnull
    @Override
    public Mutex getAsyncMutex(@Nonnull LockContext context) {
        if (log.isDebugEnabled()) {
            log.debug("创建异步锁 [{}]", context.key());
        }
        return new RedissonSemaphoreMutex(context.key(), this.redissonClient.getSemaphore(this.namespace + context.key()), this.idleTimeout);
    }

    @Override
    public void remove(@Nonnull String key) {
        final String namespacedKey = this.namespace + key;
        if (log.isDebugEnabled()) {
            log.debug("移除锁 key [{}]", namespacedKey);
        }
        this.redissonClient.getBucket(namespacedKey).delete();
    }

    @Override
    public void clear() {
        if (log.isDebugEnabled()) {
            log.debug("清除所有锁，pattern={}*", this.namespace);
        }
        this.redissonClient.getKeys().deleteByPattern(this.namespace + "*");
    }

    private RLock getLock(boolean isFair, String namespacedKey) {
        if (isFair) {
            return this.redissonClient.getFairLock(namespacedKey);
        }
        return this.redissonClient.getLock(namespacedKey);
    }
}
