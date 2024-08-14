package cn.maarlakes.common.utils;

import java.io.Serializable;
import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;

/**
 * @author linjpxc
 */
public class ThreadPoolConfig implements Serializable {
    private static final long serialVersionUID = -5308215216571695253L;

    private int coreSize = Runtime.getRuntime().availableProcessors();
    private int maximumSize = Runtime.getRuntime().availableProcessors() * 2;
    private Duration keepAliveTime = Duration.ofMinutes(1L);
    private String threadNamePrefix = "thread-pool-";
    private BlockingQueue<Runnable> queue;
    private RejectedExecutionHandler rejectedHandler;

    public int getCoreSize() {
        return coreSize;
    }

    public void setCoreSize(int coreSize) {
        this.coreSize = coreSize;
    }

    public int getMaximumSize() {
        return maximumSize;
    }

    public void setMaximumSize(int maximumSize) {
        this.maximumSize = maximumSize;
    }

    public Duration getKeepAliveTime() {
        return keepAliveTime;
    }

    public void setKeepAliveTime(Duration keepAliveTime) {
        this.keepAliveTime = keepAliveTime;
    }

    public String getThreadNamePrefix() {
        return threadNamePrefix;
    }

    public void setThreadNamePrefix(String threadNamePrefix) {
        this.threadNamePrefix = threadNamePrefix;
    }

    public BlockingQueue<Runnable> getQueue() {
        return queue;
    }

    public void setQueue(BlockingQueue<Runnable> queue) {
        this.queue = queue;
    }

    public RejectedExecutionHandler getRejectedHandler() {
        return rejectedHandler;
    }

    public void setRejectedHandler(RejectedExecutionHandler rejectedHandler) {
        this.rejectedHandler = rejectedHandler;
    }
}
