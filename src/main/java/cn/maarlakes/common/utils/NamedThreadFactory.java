package cn.maarlakes.common.utils;

import jakarta.annotation.Nonnull;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author linjpxc
 */
public class NamedThreadFactory implements ThreadFactory {

    private final AtomicInteger id = new AtomicInteger(0);
    private final ThreadGroup group;
    private final String namePrefix;
    private final boolean daemon;
    private final Integer priority;

    public NamedThreadFactory(@Nonnull String namePrefix) {
        this(null, namePrefix, true);
    }

    public NamedThreadFactory(@Nonnull String namePrefix, Integer priority) {
        this(null, namePrefix, true, priority);
    }

    public NamedThreadFactory(@Nonnull String namePrefix, boolean daemon) {
        this(null, namePrefix, daemon, null);
    }

    public NamedThreadFactory(@Nonnull String namePrefix, boolean daemon, Integer priority) {
        this(null, namePrefix, daemon, priority);
    }

    public NamedThreadFactory(ThreadGroup group, @Nonnull String namePrefix, Integer priority) {
        this(group, namePrefix, true, priority);
    }

    public NamedThreadFactory(ThreadGroup group, @Nonnull String namePrefix, boolean daemon) {
        this(group, namePrefix, daemon, null);
    }

    public NamedThreadFactory(ThreadGroup group, @Nonnull String namePrefix, boolean daemon, Integer priority) {
        this.group = group;
        this.namePrefix = namePrefix;
        this.daemon = daemon;
        this.priority = priority;
    }

    @Override
    public Thread newThread(@Nonnull Runnable action) {
        final Thread thread = new Thread(this.group, action, namePrefix + "-" + id.incrementAndGet());
        thread.setDaemon(this.daemon);
        if (this.priority != null) {
            thread.setPriority(this.priority);
        }
        return thread;
    }
}
