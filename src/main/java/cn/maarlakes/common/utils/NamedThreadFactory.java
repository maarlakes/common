package cn.maarlakes.common.utils;

import jakarta.annotation.Nonnull;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author linjpxc
 */
public class NamedThreadFactory implements ThreadFactory {

    private final String namePrefix;
    private final AtomicInteger id = new AtomicInteger(0);

    public NamedThreadFactory(@Nonnull String namePrefix) {
        this.namePrefix = namePrefix;
    }

    @Override
    public Thread newThread(@Nonnull Runnable action) {
        return new Thread(action, namePrefix + "-" + id.incrementAndGet());
    }
}
