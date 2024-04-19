package cn.maarlakes.common.locks;

import jakarta.annotation.Nonnull;

/**
 * @author linjpxc
 */
public interface LockContext {

    @Nonnull
    String key();

    boolean isFair();

    @Nonnull
    static LockContext create(@Nonnull String key) {
        return new DefaultLockContext(key, false);
    }

    @Nonnull
    static LockContext create(@Nonnull String key, boolean fair) {
        return new DefaultLockContext(key, fair);
    }
}
