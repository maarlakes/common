package cn.maarlakes.common.locks;

import jakarta.annotation.Nonnull;

import java.util.concurrent.locks.Lock;

/**
 * @author linjpxc
 */
public interface LockFactory {

    @Nonnull
    Lock createLock(@Nonnull LockContext context);
}
