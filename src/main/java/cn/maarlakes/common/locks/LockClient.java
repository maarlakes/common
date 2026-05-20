package cn.maarlakes.common.locks;

import jakarta.annotation.Nonnull;

/**
 * 锁客户端，根据 {@link LockContext} 获取对应的 {@link Mutex} 实例。
 *
 * @author linjpxc
 */
public interface LockClient {

    /**
     * 获取同步锁（可重入，不支持异步解锁）。
     */
    @Nonnull
    Mutex getMutex(@Nonnull LockContext context);

    /**
     * 获取异步锁（不可重入，支持跨线程解锁）。
     */
    @Nonnull
    Mutex getAsyncMutex(@Nonnull LockContext context);

    void remove(@Nonnull String key);

    void clear();
}
