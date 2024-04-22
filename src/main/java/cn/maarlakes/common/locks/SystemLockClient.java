package cn.maarlakes.common.locks;

import jakarta.annotation.Nonnull;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author linjpxc
 */
public class SystemLockClient implements LockClient {

    protected final ConcurrentMap<String, Lock> locks = new ConcurrentHashMap<>();

    @Nonnull
    @Override
    public Lock createLock(@Nonnull LockContext context) {
        return this.locks.computeIfAbsent(context.key(), k -> new ReentrantLock(context.isFair()));
    }
}
