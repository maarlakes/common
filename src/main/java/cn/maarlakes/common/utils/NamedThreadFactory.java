package cn.maarlakes.common.utils;


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
    private final Thread.UncaughtExceptionHandler uncaughtExceptionHandler;

    public NamedThreadFactory(String namePrefix) {
        this(null, namePrefix, false, null, null);
    }

    public NamedThreadFactory(String namePrefix, Integer priority) {
        this(null, namePrefix, false, priority, null);
    }

    public NamedThreadFactory(String namePrefix, boolean daemon) {
        this(null, namePrefix, daemon, null, null);
    }

    public NamedThreadFactory(String namePrefix, boolean daemon, Integer priority) {
        this(null, namePrefix, daemon, priority, null);
    }

    public NamedThreadFactory(ThreadGroup group, String namePrefix, Integer priority) {
        this(group, namePrefix, false, priority, null);
    }

    public NamedThreadFactory(ThreadGroup group, String namePrefix, boolean daemon) {
        this(group, namePrefix, daemon, null, null);
    }

    public NamedThreadFactory(ThreadGroup group, String namePrefix, boolean daemon, Integer priority) {
        this(group, namePrefix, daemon, priority, null);
    }

    public NamedThreadFactory(String namePrefix, boolean daemon, Integer priority, Thread.UncaughtExceptionHandler uncaughtExceptionHandler) {
        this(null, namePrefix, daemon, priority, uncaughtExceptionHandler);
    }

    public NamedThreadFactory(ThreadGroup group, String namePrefix, boolean daemon, Integer priority, Thread.UncaughtExceptionHandler uncaughtExceptionHandler) {
        this.group = group;
        this.namePrefix = namePrefix;
        this.daemon = daemon;
        this.priority = priority;
        this.uncaughtExceptionHandler = uncaughtExceptionHandler;
    }

    @Override
    public Thread newThread(Runnable action) {
        final Thread thread = new Thread(this.group, action, namePrefix + id.incrementAndGet());
        thread.setDaemon(this.daemon);
        if (this.priority != null) {
            thread.setPriority(this.priority);
        }
        if (this.uncaughtExceptionHandler != null) {
            thread.setUncaughtExceptionHandler(this.uncaughtExceptionHandler);
        }
        return thread;
    }
}
